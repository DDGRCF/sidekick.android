package dev.ddgrcf.sidekick.core.noise

import org.bouncycastle.crypto.digests.Blake2sDigest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.math.ec.rfc7748.X25519
import java.security.SecureRandom
import java.util.Arrays

class NoiseCryptoException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

data class DHKeyPair(
    val privateKey: ByteArray,
    val publicKey: ByteArray,
) {
    init {
        require(privateKey.size == 32) { "X25519 private key must be 32 bytes" }
        require(publicKey.size == 32) { "X25519 public key must be 32 bytes" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DHKeyPair) return false
        return privateKey.contentEquals(other.privateKey) && publicKey.contentEquals(other.publicKey)
    }

    override fun hashCode(): Int = 31 * privateKey.contentHashCode() + publicKey.contentHashCode()
}

internal object NoisePrimitives {
    private val secureRandom = SecureRandom()

    fun generateKeyPair(randomBytes: ByteArray? = null): DHKeyPair {
        val priv = if (randomBytes != null) {
            require(randomBytes.size == 32) { "private key seed must be 32 bytes" }
            randomBytes.copyOf()
        } else {
            ByteArray(32).also { X25519.generatePrivateKey(secureRandom, it) }
        }
        val pub = ByteArray(32)
        X25519.scalarMultBase(priv, 0, pub, 0)
        return DHKeyPair(priv, pub)
    }

    fun fromPrivateKey(priv: ByteArray): DHKeyPair {
        require(priv.size == 32) { "private key must be 32 bytes" }
        val pub = ByteArray(32)
        X25519.scalarMultBase(priv, 0, pub, 0)
        return DHKeyPair(priv.copyOf(), pub)
    }

    fun dh(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        require(privateKey.size == 32 && publicKey.size == 32) { "X25519 keys must be 32 bytes" }
        val out = ByteArray(32)
        if (!X25519.calculateAgreement(privateKey, 0, publicKey, 0, out, 0)) {
            throw NoiseCryptoException("X25519 agreement produced all-zero shared secret")
        }
        return out
    }

    fun blake2s(data: ByteArray): ByteArray {
        val d = Blake2sDigest(256)
        d.update(data, 0, data.size)
        val out = ByteArray(32)
        d.doFinal(out, 0)
        return out
    }

    fun hmacBlake2s(key: ByteArray, data: ByteArray): ByteArray {
        val hmac = HMac(Blake2sDigest(256))
        hmac.init(KeyParameter(key))
        hmac.update(data, 0, data.size)
        val out = ByteArray(32)
        hmac.doFinal(out, 0)
        return out
    }

    fun hkdf2(chainingKey: ByteArray, ikm: ByteArray): Pair<ByteArray, ByteArray> {
        val tempKey = hmacBlake2s(chainingKey, ikm)
        val out1 = hmacBlake2s(tempKey, byteArrayOf(0x01))
        val out2 = hmacBlake2s(tempKey, out1 + byteArrayOf(0x02))
        Arrays.fill(tempKey, 0)
        return out1 to out2
    }

    fun hkdf3(chainingKey: ByteArray, ikm: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
        val tempKey = hmacBlake2s(chainingKey, ikm)
        val out1 = hmacBlake2s(tempKey, byteArrayOf(0x01))
        val out2 = hmacBlake2s(tempKey, out1 + byteArrayOf(0x02))
        val out3 = hmacBlake2s(tempKey, out2 + byteArrayOf(0x03))
        Arrays.fill(tempKey, 0)
        return Triple(out1, out2, out3)
    }

    fun noiseNonce(n: Long): ByteArray {
        val nonce = ByteArray(12)
        // 4 zero bytes + 64-bit little-endian counter per Noise specification.
        nonce[4] = (n and 0xFF).toByte()
        nonce[5] = ((n ushr 8) and 0xFF).toByte()
        nonce[6] = ((n ushr 16) and 0xFF).toByte()
        nonce[7] = ((n ushr 24) and 0xFF).toByte()
        nonce[8] = ((n ushr 32) and 0xFF).toByte()
        nonce[9] = ((n ushr 40) and 0xFF).toByte()
        nonce[10] = ((n ushr 48) and 0xFF).toByte()
        nonce[11] = ((n ushr 56) and 0xFF).toByte()
        return nonce
    }

    fun encryptChaChaPoly(key: ByteArray, n: Long, ad: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = ChaCha20Poly1305()
        cipher.init(true, AEADParameters(KeyParameter(key), 128, noiseNonce(n), ad))
        val out = ByteArray(cipher.getOutputSize(plaintext.size))
        var len = cipher.processBytes(plaintext, 0, plaintext.size, out, 0)
        len += cipher.doFinal(out, len)
        return if (len == out.size) out else out.copyOf(len)
    }

    fun decryptChaChaPoly(key: ByteArray, n: Long, ad: ByteArray, ciphertext: ByteArray): ByteArray {
        if (ciphertext.size < 16) {
            throw NoiseCryptoException("ciphertext shorter than 16-byte tag")
        }
        val cipher = ChaCha20Poly1305()
        cipher.init(false, AEADParameters(KeyParameter(key), 128, noiseNonce(n), ad))
        val out = ByteArray(cipher.getOutputSize(ciphertext.size))
        return try {
            var len = cipher.processBytes(ciphertext, 0, ciphertext.size, out, 0)
            len += cipher.doFinal(out, len)
            if (len == out.size) out else out.copyOf(len)
        } catch (e: Exception) {
            throw NoiseCryptoException("AEAD decryption failed", e)
        }
    }
}
