package dev.ddgrcf.sidekick.core.relay

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import dev.ddgrcf.sidekick.core.noise.NoisePackets
import dev.ddgrcf.sidekick.core.noise.NoiseSession
import dev.ddgrcf.sidekick.core.protocol.FrameCodec
import dev.ddgrcf.sidekick.core.store.SecretPrefs
import dev.ddgrcf.sidekick.core.store.SecretPrefsSerializer
import dev.ddgrcf.sidekick.core.store.SecretStore
import dev.ddgrcf.sidekick.proto.v1.Frame
import dev.ddgrcf.sidekick.proto.v1.HandoverSessionRequest
import dev.ddgrcf.sidekick.proto.v1.HandoverSessionResponse
import dev.ddgrcf.sidekick.proto.v1.MediaKind
import dev.ddgrcf.sidekick.proto.v1.Session
import dev.ddgrcf.sidekick.proto.v1.UploadMediaRequest
import dev.ddgrcf.sidekick.proto.v1.UploadMediaResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

class RelaySocketAndApiIntegrationTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var api: RelayApi

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        api = RelayApi(OkHttpClient())
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun testStore(scope: CoroutineScope): SecretStore {
        val ds: DataStore<SecretPrefs> = DataStoreFactory.create(
            serializer = SecretPrefsSerializer,
            scope = scope,
            produceFile = { File(tempFolder.root, "prefs.json") },
        )
        return SecretStore(ds)
    }

    @Test
    fun allDeviceAndTotpRestEndpointsMatchServerContract() = runTest {
        val baseUrl = server.url("").toString().trimEnd('/')

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"device_id":"phone-1","access_token":"tok-1","refresh_token":"rt-1","expires_at_ms":9999999999999}""",
            ),
        )
        val reg = api.registerDevice(baseUrl, "user-tok", "Pixel 9", "cHVi")
        assertEquals("phone-1", reg.device_id)
        val req1 = server.takeRequest()
        assertEquals("POST", req1.method)
        assertEquals("/devices/pair", req1.path)
        assertEquals("Bearer user-tok", req1.getHeader("Authorization"))

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"devices":[{"id":"br-1","kind":"bridge","name":"mac-studio","pubkey":"YnI=","online":true},{"id":"br-2","kind":"bridge","name":"linux","pubkey":"bHg=","online":false}]}""",
            ),
        )
        val devs = api.listDevices(baseUrl, reg.access_token)
        assertEquals(2, devs.size)
        assertTrue(devs[0].online)
        assertTrue(!devs[1].online)
        val req2 = server.takeRequest()
        assertEquals("GET", req2.method)
        assertEquals("/devices", req2.path)

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"ok":true,"enroll_id":"e-1","bridge_device_id":"br-1","bridge_name":"mac-studio","bridge_pubkey":"YnI="}""",
            ),
        )
        val app = api.approveEnroll(baseUrl, reg.access_token, "e-1", null, reg.device_id)
        assertTrue(app.ok)
        assertEquals("br-1", app.bridge_device_id)
        assertEquals("/devices/enroll/approve", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        api.revokeDevice(baseUrl, reg.access_token, "br-2")
        val reqDel = server.takeRequest()
        assertEquals("DELETE", reqDel.method)
        assertEquals("/devices/br-2", reqDel.path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"secret":"S3","otpauth_uri":"otpauth://totp/Sidekick"}"""))
        val totp = api.enrollTotp(baseUrl, reg.access_token)
        assertTrue(totp.otpauth_uri.startsWith("otpauth://"))
        assertEquals("/auth/totp/enroll", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"recovery_codes":["CODE1","CODE2"]}"""))
        val conf = api.confirmTotp(baseUrl, reg.access_token, "123456")
        assertEquals(listOf("CODE1", "CODE2"), conf.recovery_codes)
        assertEquals("/auth/totp/confirm", server.takeRequest().path)
    }

    @Test
    fun socketManagerCompletesNoiseXXpsk3AndKKOverWebSocketForHandoverAndUploadMedia() = runBlocking {
        val bridgeId = UUID.randomUUID().toString()
        val phoneId = UUID.randomUUID().toString()
        val bridgeKp = NoiseSession.generateKeyPair()
        val psk = ByteArray(32) { 0x42 }
        val pskB64 = Base64.getEncoder().encodeToString(psk)
        val bridgePubB64 = Base64.getEncoder().encodeToString(bridgeKp.publicKey)

        var serverSession: NoiseSession? = null
        var serverSocket: WebSocket? = null

        fun upgradeWithResponder() {
            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            serverSocket = webSocket
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            // Reply to L1Handshake
                            webSocket.send(
                                """{"ok":true,"paired_devices":[{"id":"$bridgeId","kind":"bridge","name":"ws-bridge","pubkey":"$bridgePubB64","online":true}]}""",
                            )
                        }

                        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                            val raw = bytes.toByteArray()
                            val peerBytes = raw.copyOfRange(0, 16)
                            val pkt = raw.copyOfRange(16, raw.size)
                            val sess = serverSession ?: run {
                                val created = if (pkt[0] == NoisePackets.TYPE_XX_PSK3) {
                                    NoiseSession.xxPsk3(initiator = false, staticKey = bridgeKp, psk = psk)
                                } else {
                                    error("set KK responder session explicitly")
                                }
                                serverSession = created
                                created
                            }

                            when (pkt[0]) {
                                NoisePackets.TYPE_XX_PSK3 -> {
                                    val (_, fin1) = sess.readHandshake(pkt)
                                    if (!fin1) {
                                        val (reply, fin2) = sess.writeHandshake()
                                        webSocket.send(wrapWithPeer(peerBytes, reply))
                                        if (fin2) return
                                    }
                                }
                                NoisePackets.TYPE_KK -> {
                                    val (_, fin1) = sess.readHandshake(pkt)
                                    if (!fin1) {
                                        val (reply, _) = sess.writeHandshake()
                                        webSocket.send(wrapWithPeer(peerBytes, reply))
                                    }
                                }
                                NoisePackets.TYPE_TRANSPORT -> {
                                    val frameBytes = sess.decryptChunk(pkt) ?: return
                                    val frame = FrameCodec.decode(frameBytes) ?: return
                                    val replyFrame = when {
                                        frame.handover_session != null -> Frame(
                                            reply_to = frame.id,
                                            handover_session_resp = HandoverSessionResponse(
                                                target_session = Session(
                                                    id = "codex-target-1",
                                                    tool = frame.handover_session!!.target_tool,
                                                    cwd = "/repo",
                                                    live = true,
                                                ),
                                                handed_over_prompt = frame.handover_session!!.handover_note,
                                            ),
                                        )
                                        frame.upload_media != null -> Frame(
                                            reply_to = frame.id,
                                            upload_media_resp = UploadMediaResponse(
                                                upload_id = frame.upload_media!!.upload_id,
                                                next_offset = frame.upload_media!!.total_size,
                                                committed = frame.upload_media!!.eof,
                                                rel_path = ".sidekick/attachments/deadbeef1234_${frame.upload_media!!.filename}",
                                                sha256_plain = frame.upload_media!!.sha256_plain,
                                            ),
                                        )
                                        else -> Frame(reply_to = frame.id)
                                    }
                                    val encrypted = sess.encryptFrame(FrameCodec.encode(replyFrame))
                                    encrypted.forEach { chunk -> webSocket.send(wrapWithPeer(peerBytes, chunk)) }
                                }
                            }
                        }
                    },
                ),
            )
        }

        val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = testStore(storeScope)
        val baseUrl = server.url("").toString().trimEnd('/')
        store.updateRelayUrl(baseUrl)
        store.updateTokens("tok-valid", System.currentTimeMillis() + 3_600_000L, "rt-valid", "user-1", phoneId)
        val phoneKp = store.ensureNoiseKeyPair()

        upgradeWithResponder()
        val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val wsClient = OkHttpClient()
        val tm = TokenManager(RelayApi(wsClient), store)
        val mgr = RelaySocketManager(wsClient, tm, store)
        mgr.start(managerScope)

        try {
            withTimeout(15_000L) {
                // Wait until L1 CONNECTED
                mgr.connectionState.first { it == ConnectionState.CONNECTED }

                // 1. Pair via Noise XXpsk3 and invoke HandoverSession RPC over encrypted WS.
                mgr.pairWithBridge(bridgeId, pskB64, bridgePubB64)
                val hoResp = mgr.request(bridgeId) { id ->
                    Frame(
                        id = id,
                        handover_session = HandoverSessionRequest(
                            source_session_id = "claude-src-1",
                            target_tool = "codex",
                            handover_note = "verify handover over noise",
                            include_context = true,
                        ),
                    )
                }
                assertEquals("codex-target-1", hoResp.handover_session_resp?.target_session?.id)
                assertEquals("codex", hoResp.handover_session_resp?.target_session?.tool)

                // Verify bridge public key was pinned cleanly.
                val pinned = store.getPeerPin(bridgeId)!!
                assertTrue(pinned.contentEquals(bridgeKp.publicKey))

                // 2. Disconnect & reconnect using Noise KK (pinned phone & bridge static keys, no PSK).
                mgr.stop()
                serverSocket?.close(1000, "reconnect")
                serverSession = NoiseSession.kk(initiator = false, staticKey = bridgeKp, peerStatic = phoneKp.publicKey)
                upgradeWithResponder()
                mgr.start(managerScope)
                mgr.connectionState.first { it == ConnectionState.CONNECTED }

                val audioBytes = "M4A-DATA-OVER-KK".toByteArray()
                val sha = MessageDigest.getInstance("SHA-256").digest(audioBytes).toByteString()
                val upResp = mgr.request(bridgeId) { id ->
                    Frame(
                        id = id,
                        upload_media = UploadMediaRequest(
                            upload_id = "up-kk-1",
                            session_id = "codex-target-1",
                            kind = MediaKind.MEDIA_KIND_AUDIO,
                            mime_type = "audio/mp4",
                            filename = "memo.m4a",
                            total_size = audioBytes.size.toLong(),
                            offset = 0L,
                            data_ = audioBytes.toByteString(),
                            eof = true,
                            sha256_plain = sha,
                        ),
                    )
                }
                assertTrue(upResp.upload_media_resp!!.committed)
                assertEquals(".sidekick/attachments/deadbeef1234_memo.m4a", upResp.upload_media_resp!!.rel_path)
            }
        } finally {
            mgr.stop()
            serverSocket?.close(1000, "done")
            managerScope.cancel()
            storeScope.cancel()
            wsClient.dispatcher.executorService.shutdown()
        }
    }

    private fun wrapWithPeer(peerUuidBytes: ByteArray, payload: ByteArray): ByteString {
        val out = ByteArray(16 + payload.size)
        System.arraycopy(peerUuidBytes, 0, out, 0, 16)
        System.arraycopy(payload, 0, out, 16, payload.size)
        return out.toByteString()
    }
}
