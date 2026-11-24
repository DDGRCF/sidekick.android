package dev.ddgrcf.sidekick.core.relay

import dev.ddgrcf.sidekick.core.noise.NoisePackets
import dev.ddgrcf.sidekick.core.noise.NoiseSession
import dev.ddgrcf.sidekick.core.protocol.FrameCodec
import dev.ddgrcf.sidekick.core.protocol.RpcClient
import dev.ddgrcf.sidekick.core.store.SecretStore
import dev.ddgrcf.sidekick.proto.v1.Frame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

enum class ConnectionState {
    OFFLINE,
    CONNECTING,
    CONNECTED,
}

data class PeerFrameEvent(
    val bridgeId: String,
    val frame: Frame,
)

class PinMismatchException(val bridgeId: String) : SecurityException("设备密钥已变更 (bridge=$bridgeId)")

class RelaySocketManager(
    private val client: OkHttpClient,
    private val tokenManager: TokenManager,
    private val secretStore: SecretStore,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _connectionState = MutableStateFlow(ConnectionState.OFFLINE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _bridges = MutableStateFlow<List<RelayDevice>>(emptyList())
    val bridges: StateFlow<List<RelayDevice>> = _bridges.asStateFlow()

    private val _events = MutableSharedFlow<PeerFrameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PeerFrameEvent> = _events.asSharedFlow()

    private val _pinMismatches = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val pinMismatches: SharedFlow<String> = _pinMismatches.asSharedFlow()

    private val noiseSessions = ConcurrentHashMap<String, NoiseSession>()
    private val pendingHandshakes = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val expectedBridgePub = ConcurrentHashMap<String, ByteArray>()
    private val rpcs = ConcurrentHashMap<String, RpcClient>()

    @Volatile
    private var activeSocket: WebSocket? = null
    private var loopJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch(Dispatchers.IO) {
            var backoffMs = 1_000L
            while (true) {
                val prefs = secretStore.current()
                if (prefs.relayUrl.isBlank() || prefs.deviceId.isBlank() || prefs.refreshToken.isBlank()) {
                    _connectionState.value = ConnectionState.OFFLINE
                    delay(1_000L)
                    continue
                }
                _connectionState.value = ConnectionState.CONNECTING
                val startAt = System.currentTimeMillis()
                runCatching {
                    val token = tokenManager.validAccessToken()
                    connectOnce(prefs.relayUrl, prefs.deviceId, token)
                }
                _connectionState.value = ConnectionState.OFFLINE
                clearSessions()
                if (System.currentTimeMillis() - startAt > 10_000L) {
                    backoffMs = 1_000L
                }
                val jitter = 0.8 + Random.nextDouble() * 0.4
                delay((backoffMs * jitter).toLong())
                backoffMs = minOf(backoffMs * 2L, 60_000L)
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        activeSocket?.close(1000, "stopped")
        activeSocket = null
        clearSessions()
        _connectionState.value = ConnectionState.OFFLINE
    }

    private fun clearSessions() {
        noiseSessions.values.forEach { it.close() }
        noiseSessions.clear()
        pendingHandshakes.values.forEach { it.cancel() }
        pendingHandshakes.clear()
        rpcs.values.forEach { it.failAll(IllegalStateException("disconnected")) }
        rpcs.clear()
    }

    private suspend fun connectOnce(relayUrl: String, deviceId: String, accessToken: String) {
        val done = CompletableDeferred<Unit>()
        val wsUrl = RelayApi.normalizeWs(relayUrl) + "/ws/device"
        val req = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Bearer $accessToken")
            .build()

        val ws = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val hs = json.encodeToString(L1Handshake(device_id = deviceId, kind = "phone", protocol_version = 1))
                webSocket.send(hs)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleControlText(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleBinary(bytes.toByteArray())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
                done.complete(Unit)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                done.complete(Unit)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (response?.code == 401) {
                    // Trigger refresh on next reconnect iteration.
                }
                done.complete(Unit)
            }
        })
        activeSocket = ws
        try {
            done.await()
        } finally {
            if (activeSocket === ws) activeSocket = null
        }
    }

    private fun handleControlText(text: String) {
        runCatching {
            val msg = json.decodeFromString<ControlTextMsg>(text)
            if (msg.ok) {
                _connectionState.value = ConnectionState.CONNECTED
                _bridges.value = msg.paired_devices.filter { it.kind == "bridge" }
                return
            }
            if (msg.event == "updated" && msg.device_id.isNotEmpty() && msg.kind == "bridge") {
                _bridges.value = _bridges.value.map {
                    if (it.id == msg.device_id) it.copy(online = msg.online) else it
                }
                if (!msg.online) {
                    noiseSessions.remove(msg.device_id)?.close()
                    rpcs.remove(msg.device_id)?.failAll(IllegalStateException("bridge offline"))
                }
            }
        }
    }

    private fun handleBinary(raw: ByteArray) {
        if (raw.size < 17) return
        val bb = ByteBuffer.wrap(raw, 0, 16)
        val bridgeId = UUID(bb.long, bb.long).toString()
        val pkt = raw.copyOfRange(16, raw.size)
        val sess = noiseSessions[bridgeId] ?: return

        when (pkt[0]) {
            NoisePackets.TYPE_XX_PSK3, NoisePackets.TYPE_KK -> {
                try {
                    val (_, finished) = sess.readHandshake(pkt)
                    if (!finished) {
                        val (reply, done) = sess.writeHandshake()
                        sendBinary(bridgeId, reply)
                        if (done) onHandshakeComplete(bridgeId, sess, pkt[0] == NoisePackets.TYPE_XX_PSK3)
                    } else {
                        onHandshakeComplete(bridgeId, sess, pkt[0] == NoisePackets.TYPE_XX_PSK3)
                    }
                } catch (e: Throwable) {
                    noiseSessions.remove(bridgeId)?.close()
                    pendingHandshakes.remove(bridgeId)?.completeExceptionally(e)
                }
            }
            NoisePackets.TYPE_TRANSPORT -> {
                val frameBytes = runCatching { sess.decryptChunk(pkt) }.getOrNull() ?: return
                val frame = FrameCodec.decode(frameBytes) ?: return
                val rpc = rpcs[bridgeId]
                if (rpc != null && rpc.onFrame(frame)) return
                _events.tryEmit(PeerFrameEvent(bridgeId, frame))
            }
        }
    }

    private fun onHandshakeComplete(bridgeId: String, sess: NoiseSession, isXX: Boolean) {
        val remoteStatic = sess.remoteStatic ?: ByteArray(0)
        val expected = expectedBridgePub.remove(bridgeId)
        if (expected != null && !remoteStatic.contentEquals(expected)) {
            noiseSessions.remove(bridgeId)?.close()
            _pinMismatches.tryEmit(bridgeId)
            pendingHandshakes.remove(bridgeId)?.completeExceptionally(PinMismatchException(bridgeId))
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            if (isXX && remoteStatic.size == 32) {
                secretStore.pinPeer(bridgeId, remoteStatic)
            } else {
                val pinned = secretStore.getPeerPin(bridgeId)
                if (pinned == null || !pinned.contentEquals(remoteStatic)) {
                    noiseSessions.remove(bridgeId)?.close()
                    _pinMismatches.tryEmit(bridgeId)
                    pendingHandshakes.remove(bridgeId)?.completeExceptionally(PinMismatchException(bridgeId))
                    return@launch
                }
            }
            pendingHandshakes.remove(bridgeId)?.complete(Unit)
        }
    }

    /**
     * Initiates a first-time Noise XXpsk3 pairing handshake. Refuses if psk is invalid.
     */
    suspend fun pairWithBridge(bridgeId: String, pskB64: String, expectedBridgePubB64: String?) {
        val psk = runCatching { Base64.getDecoder().decode(pskB64.trim()) }.getOrNull()
            ?: throw IllegalArgumentException("无效的 PSK，拒绝建立连接")
        require(psk.size == 32) { "PSK 必须为 32 字节，禁止无 PSK 降级" }

        if (!expectedBridgePubB64.isNullOrBlank()) {
            expectedBridgePub[bridgeId] = Base64.getDecoder().decode(expectedBridgePubB64.trim())
        }
        val kp = secretStore.ensureNoiseKeyPair()
        val sess = NoiseSession.xxPsk3(initiator = true, staticKey = kp, psk = psk)
        val ready = CompletableDeferred<Unit>()
        noiseSessions[bridgeId] = sess
        pendingHandshakes[bridgeId] = ready
        val (m1, _) = sess.writeHandshake()
        sendBinary(bridgeId, m1)
        withTimeout(15_000L) { ready.await() }
    }

    suspend fun ensureSession(bridgeId: String) {
        val existing = noiseSessions[bridgeId]
        if (existing != null && existing.isReady) return

        val pending = pendingHandshakes[bridgeId]
        if (pending != null) {
            withTimeout(15_000L) { pending.await() }
            return
        }

        val pinned = secretStore.getPeerPin(bridgeId)
            ?: throw IllegalStateException("Bridge $bridgeId is not paired yet")
        expectedBridgePub[bridgeId] = pinned
        val kp = secretStore.ensureNoiseKeyPair()
        val sess = NoiseSession.kk(initiator = true, staticKey = kp, peerStatic = pinned)
        val ready = CompletableDeferred<Unit>()
        noiseSessions[bridgeId] = sess
        pendingHandshakes[bridgeId] = ready
        val (m1, _) = sess.writeHandshake()
        sendBinary(bridgeId, m1)
        withTimeout(15_000L) { ready.await() }
    }

    suspend fun request(bridgeId: String, build: (Long) -> Frame): Frame {
        ensureSession(bridgeId)
        val rpc = rpcs.getOrPut(bridgeId) {
            RpcClient(60_000L) { frame -> sendFrame(bridgeId, frame) }
        }
        return rpc.request(build)
    }

    suspend fun sendFrame(bridgeId: String, frame: Frame) {
        ensureSession(bridgeId)
        val sess = noiseSessions[bridgeId] ?: error("no active session for $bridgeId")
        val bytes = FrameCodec.encode(frame)
        val pkts = sess.encryptFrame(bytes)
        for (p in pkts) {
            sendBinary(bridgeId, p)
        }
    }

    private fun sendBinary(bridgeId: String, payload: ByteArray) {
        val ws = activeSocket ?: error("relay websocket offline")
        val uuid = UUID.fromString(bridgeId)
        val out = ByteArray(16 + payload.size)
        val bb = ByteBuffer.wrap(out, 0, 16)
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        System.arraycopy(payload, 0, out, 16, payload.size)
        ws.send(out.toByteString())
    }

    @Serializable
    private data class L1Handshake(
        val device_id: String,
        val kind: String,
        val protocol_version: Int,
    )

    @Serializable
    private data class ControlTextMsg(
        val ok: Boolean = false,
        val paired_devices: List<RelayDevice> = emptyList(),
        val event: String = "",
        val device_id: String = "",
        val kind: String = "",
        val online: Boolean = false,
    )
}
