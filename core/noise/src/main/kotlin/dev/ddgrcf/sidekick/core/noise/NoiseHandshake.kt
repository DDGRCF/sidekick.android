package dev.ddgrcf.sidekick.core.noise

import java.util.Arrays

class CipherState(key: ByteArray? = null) {
    private var k: ByteArray? = key?.copyOf()
    private var n: Long = 0L

    fun hasKey(): Boolean = k != null

    @Synchronized
    fun encryptWithAd(ad: ByteArray, plaintext: ByteArray): ByteArray {
        val key = k ?: return plaintext.copyOf()
        val ct = NoisePrimitives.encryptChaChaPoly(key, n, ad, plaintext)
        n++
        return ct
    }

    @Synchronized
    fun decryptWithAd(ad: ByteArray, ciphertext: ByteArray): ByteArray {
        val key = k ?: return ciphertext.copyOf()
        val pt = NoisePrimitives.decryptChaChaPoly(key, n, ad, ciphertext)
        n++
        return pt
    }

    fun destroy() {
        k?.let { Arrays.fill(it, 0) }
        k = null
    }
}

internal class SymmetricState(protocolName: String) {
    private val nameBytes = protocolName.toByteArray(Charsets.US_ASCII)
    private var h: ByteArray = if (nameBytes.size <= 32) {
        ByteArray(32).also { System.arraycopy(nameBytes, 0, it, 0, nameBytes.size) }
    } else {
        NoisePrimitives.blake2s(nameBytes)
    }
    private var ck: ByteArray = h.copyOf()
    private var cs: CipherState = CipherState(null)

    fun mixHash(data: ByteArray) {
        h = NoisePrimitives.blake2s(h + data)
    }

    fun mixKey(ikm: ByteArray) {
        val (newCk, tempK) = NoisePrimitives.hkdf2(ck, ikm)
        ck = newCk
        cs = CipherState(tempK)
        Arrays.fill(tempK, 0)
    }

    fun mixKeyAndHash(ikm: ByteArray) {
        val (newCk, tempH, tempK) = NoisePrimitives.hkdf3(ck, ikm)
        ck = newCk
        mixHash(tempH)
        cs = CipherState(tempK)
        Arrays.fill(tempH, 0)
        Arrays.fill(tempK, 0)
    }

    fun encryptAndHash(plaintext: ByteArray): ByteArray {
        val ct = cs.encryptWithAd(h, plaintext)
        mixHash(ct)
        return ct
    }

    fun decryptAndHash(ciphertext: ByteArray): ByteArray {
        val pt = cs.decryptWithAd(h, ciphertext)
        mixHash(ciphertext)
        return pt
    }

    fun hasKey(): Boolean = cs.hasKey()

    fun split(): Pair<CipherState, CipherState> {
        val (tempK1, tempK2) = NoisePrimitives.hkdf2(ck, ByteArray(0))
        val c1 = CipherState(tempK1)
        val c2 = CipherState(tempK2)
        Arrays.fill(tempK1, 0)
        Arrays.fill(tempK2, 0)
        Arrays.fill(ck, 0)
        return c1 to c2
    }
}

enum class NoisePattern {
    XX_PSK3,
    KK,
}

private enum class Token {
    E,
    S,
    EE,
    ES,
    SE,
    SS,
    PSK,
}

data class HandshakeResult(
    val sendCipher: CipherState,
    val recvCipher: CipherState,
    val remoteStatic: ByteArray,
)

class NoiseHandshakeState private constructor(
    private val pattern: NoisePattern,
    private val initiator: Boolean,
    private val s: DHKeyPair,
    private var rs: ByteArray?,
    private val psk: ByteArray?,
    private val deterministicEph: ByteArray?,
) {
    private val sym: SymmetricState
    private val patterns: List<List<Token>>
    private var e: DHKeyPair? = null
    private var re: ByteArray? = null
    private var msgIndex: Int = 0
    private var myTurn: Boolean = initiator

    init {
        val protoName = when (pattern) {
            NoisePattern.XX_PSK3 -> "Noise_XXpsk3_25519_ChaChaPoly_BLAKE2s"
            NoisePattern.KK -> "Noise_KK_25519_ChaChaPoly_BLAKE2s"
        }
        sym = SymmetricState(protoName)
        // Empty prologue per Go flynn/noise config default.
        sym.mixHash(ByteArray(0))

        patterns = when (pattern) {
            NoisePattern.XX_PSK3 -> {
                require(psk != null && psk.size == 32) { "XXpsk3 requires 32-byte PSK" }
                listOf(
                    listOf(Token.E),
                    listOf(Token.E, Token.EE, Token.S, Token.ES),
                    listOf(Token.S, Token.SE, Token.PSK),
                )
            }
            NoisePattern.KK -> {
                require(rs != null && rs!!.size == 32) { "KK requires 32-byte remote static key" }
                val initStatic = if (initiator) s.publicKey else rs!!
                val respStatic = if (initiator) rs!! else s.publicKey
                sym.mixHash(initStatic)
                sym.mixHash(respStatic)
                listOf(
                    listOf(Token.E, Token.ES, Token.SS),
                    listOf(Token.E, Token.EE, Token.SE),
                )
            }
        }
    }

    val isDone: Boolean
        get() = msgIndex >= patterns.size

    fun remoteStatic(): ByteArray? = rs?.copyOf()

    fun writeMessage(payload: ByteArray = ByteArray(0)): Pair<ByteArray, HandshakeResult?> {
        check(myTurn && !isDone) { "not our turn to write handshake message" }
        val tokens = patterns[msgIndex]
        val out = ArrayList<Byte>()

        for (tok in tokens) {
            when (tok) {
                Token.E -> {
                    val eph = NoisePrimitives.generateKeyPair(deterministicEph)
                    e = eph
                    out.addAll(eph.publicKey.toList())
                    sym.mixHash(eph.publicKey)
                    if (psk != null) {
                        sym.mixKey(eph.publicKey)
                    }
                }
                Token.S -> {
                    val encS = sym.encryptAndHash(s.publicKey)
                    out.addAll(encS.toList())
                }
                Token.EE -> sym.mixKey(NoisePrimitives.dh(e!!.privateKey, re!!))
                Token.ES -> {
                    val shared = if (initiator) {
                        NoisePrimitives.dh(e!!.privateKey, rs!!)
                    } else {
                        NoisePrimitives.dh(s.privateKey, re!!)
                    }
                    sym.mixKey(shared)
                }
                Token.SE -> {
                    val shared = if (initiator) {
                        NoisePrimitives.dh(s.privateKey, re!!)
                    } else {
                        NoisePrimitives.dh(e!!.privateKey, rs!!)
                    }
                    sym.mixKey(shared)
                }
                Token.SS -> sym.mixKey(NoisePrimitives.dh(s.privateKey, rs!!))
                Token.PSK -> sym.mixKeyAndHash(psk!!)
            }
        }

        val encPayload = sym.encryptAndHash(payload)
        out.addAll(encPayload.toList())
        msgIndex++
        myTurn = !myTurn

        val result = if (isDone) finalizeSplit() else null
        return out.toByteArray() to result
    }

    fun readMessage(message: ByteArray): Pair<ByteArray, HandshakeResult?> {
        check(!myTurn && !isDone) { "not our turn to read handshake message" }
        val tokens = patterns[msgIndex]
        var offset = 0

        for (tok in tokens) {
            when (tok) {
                Token.E -> {
                    if (message.size - offset < 32) {
                        throw NoiseCryptoException("truncated ephemeral key in handshake")
                    }
                    val remoteEph = message.copyOfRange(offset, offset + 32)
                    offset += 32
                    re = remoteEph
                    sym.mixHash(remoteEph)
                    if (psk != null) {
                        sym.mixKey(remoteEph)
                    }
                }
                Token.S -> {
                    val len = if (sym.hasKey()) 48 else 32
                    if (message.size - offset < len) {
                        throw NoiseCryptoException("truncated static key in handshake")
                    }
                    val encRemoteS = message.copyOfRange(offset, offset + len)
                    offset += len
                    val decRemoteS = sym.decryptAndHash(encRemoteS)
                    if (decRemoteS.size != 32) {
                        throw NoiseCryptoException("invalid decrypted remote static size")
                    }
                    rs = decRemoteS
                }
                Token.EE -> sym.mixKey(NoisePrimitives.dh(e!!.privateKey, re!!))
                Token.ES -> {
                    val shared = if (initiator) {
                        NoisePrimitives.dh(e!!.privateKey, rs!!)
                    } else {
                        NoisePrimitives.dh(s.privateKey, re!!)
                    }
                    sym.mixKey(shared)
                }
                Token.SE -> {
                    val shared = if (initiator) {
                        NoisePrimitives.dh(s.privateKey, re!!)
                    } else {
                        NoisePrimitives.dh(e!!.privateKey, rs!!)
                    }
                    sym.mixKey(shared)
                }
                Token.SS -> sym.mixKey(NoisePrimitives.dh(s.privateKey, rs!!))
                Token.PSK -> sym.mixKeyAndHash(psk!!)
            }
        }

        val payload = sym.decryptAndHash(message.copyOfRange(offset, message.size))
        msgIndex++
        myTurn = !myTurn

        val result = if (isDone) finalizeSplit() else null
        return payload to result
    }

    private fun finalizeSplit(): HandshakeResult {
        val (c1, c2) = sym.split()
        val (send, recv) = if (initiator) c1 to c2 else c2 to c1
        psk?.let { Arrays.fill(it, 0) }
        return HandshakeResult(
            sendCipher = send,
            recvCipher = recv,
            remoteStatic = rs?.copyOf() ?: ByteArray(0),
        )
    }

    companion object {
        fun xxPsk3(
            initiator: Boolean,
            staticKey: DHKeyPair,
            psk: ByteArray,
            deterministicEph: ByteArray? = null,
        ): NoiseHandshakeState = NoiseHandshakeState(
            pattern = NoisePattern.XX_PSK3,
            initiator = initiator,
            s = staticKey,
            rs = null,
            psk = psk.copyOf(),
            deterministicEph = deterministicEph,
        )

        fun kk(
            initiator: Boolean,
            staticKey: DHKeyPair,
            peerStatic: ByteArray,
            deterministicEph: ByteArray? = null,
        ): NoiseHandshakeState = NoiseHandshakeState(
            pattern = NoisePattern.KK,
            initiator = initiator,
            s = staticKey,
            rs = peerStatic.copyOf(),
            psk = null,
            deterministicEph = deterministicEph,
        )
    }
}
