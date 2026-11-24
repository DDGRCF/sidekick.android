package dev.ddgrcf.sidekick.core.noise

import dev.ddgrcf.sidekick.core.protocol.ChunkFramer
import dev.ddgrcf.sidekick.core.protocol.ChunkReassembler

object NoisePackets {
    const val TYPE_XX_PSK3: Byte = 0x01
    const val TYPE_KK: Byte = 0x02
    const val TYPE_TRANSPORT: Byte = 0x03
}

/**
 * Wraps L2 Noise handshake + chunked encrypted transport matching Go `internal/noise`.
 */
class NoiseSession private constructor(
    val packetType: Byte,
    private var handshake: NoiseHandshakeState?,
) {
    private var sendCipher: CipherState? = null
    private var recvCipher: CipherState? = null
    private var remoteStaticKey: ByteArray? = null
    private val framer = ChunkFramer()
    private val reassembler = ChunkReassembler()

    val isReady: Boolean
        @Synchronized get() = sendCipher != null && recvCipher != null

    val remoteStatic: ByteArray?
        @Synchronized get() = remoteStaticKey?.copyOf()

    @Synchronized
    fun writeHandshake(payload: ByteArray = ByteArray(0)): Pair<ByteArray, Boolean> {
        val hs = handshake ?: error("handshake already complete")
        val (msg, res) = hs.writeMessage(payload)
        if (res != null) {
            sendCipher = res.sendCipher
            recvCipher = res.recvCipher
            remoteStaticKey = res.remoteStatic
            handshake = null
        }
        val out = ByteArray(1 + msg.size)
        out[0] = packetType
        System.arraycopy(msg, 0, out, 1, msg.size)
        return out to (res != null)
    }

    @Synchronized
    fun readHandshake(packet: ByteArray): Pair<ByteArray, Boolean> {
        if (packet.isEmpty() || packet[0] != packetType) {
            throw NoiseCryptoException("unexpected handshake packet type: ${packet.firstOrNull()}")
        }
        val hs = handshake ?: error("handshake already complete")
        val (payload, res) = hs.readMessage(packet.copyOfRange(1, packet.size))
        if (res != null) {
            sendCipher = res.sendCipher
            recvCipher = res.recvCipher
            remoteStaticKey = res.remoteStatic
            handshake = null
        }
        return payload to (res != null)
    }

    fun encryptFrame(frameBytes: ByteArray): List<ByteArray> {
        val cs = synchronized(this) { sendCipher ?: error("noise handshake incomplete") }
        val chunks = framer.chunk(frameBytes)
        return chunks.map { chunk ->
            val ct = cs.encryptWithAd(ByteArray(0), chunk)
            ByteArray(1 + ct.size).also {
                it[0] = NoisePackets.TYPE_TRANSPORT
                System.arraycopy(ct, 0, it, 1, ct.size)
            }
        }
    }

    fun decryptChunk(packet: ByteArray): ByteArray? {
        if (packet.isEmpty() || packet[0] != NoisePackets.TYPE_TRANSPORT) {
            throw NoiseCryptoException("unexpected transport packet type: ${packet.firstOrNull()}")
        }
        val cs = synchronized(this) { recvCipher ?: error("noise handshake incomplete") }
        val plain = cs.decryptWithAd(ByteArray(0), packet.copyOfRange(1, packet.size))
        return reassembler.feed(plain)
    }

    @Synchronized
    fun close() {
        sendCipher?.destroy()
        recvCipher?.destroy()
        sendCipher = null
        recvCipher = null
    }

    companion object {
        fun generateKeyPair(): DHKeyPair = NoisePrimitives.generateKeyPair()

        fun keyPairFromPrivate(priv: ByteArray): DHKeyPair = NoisePrimitives.fromPrivateKey(priv)

        fun xxPsk3(
            initiator: Boolean,
            staticKey: DHKeyPair,
            psk: ByteArray,
            deterministicEph: ByteArray? = null,
        ): NoiseSession = NoiseSession(
            packetType = NoisePackets.TYPE_XX_PSK3,
            handshake = NoiseHandshakeState.xxPsk3(initiator, staticKey, psk, deterministicEph),
        )

        fun kk(
            initiator: Boolean,
            staticKey: DHKeyPair,
            peerStatic: ByteArray,
            deterministicEph: ByteArray? = null,
        ): NoiseSession = NoiseSession(
            packetType = NoisePackets.TYPE_KK,
            handshake = NoiseHandshakeState.kk(initiator, staticKey, peerStatic, deterministicEph),
        )
    }
}
