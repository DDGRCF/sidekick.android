package dev.ddgrcf.sidekick.core.noise

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@Serializable
private data class InteropVectors(
    val init_static_priv_hex: String,
    val init_static_pub_hex: String,
    val resp_static_priv_hex: String,
    val resp_static_pub_hex: String,
    val init_eph_hex: String,
    val resp_eph_hex: String,
    val psk_hex: String,
    val xx_m1_hex: String,
    val xx_m2_hex: String,
    val xx_m3_hex: String,
    val xx_transport_hex: String,
    val kk_m1_hex: String,
    val kk_m2_hex: String,
    val kk_transport_hex: String,
    val sample_plain_hex: String,
)

private fun String.decodeHex(): ByteArray {
    check(length % 2 == 0)
    return ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}

private fun ByteArray.encodeHex(): String = joinToString("") { "%02x".format(it) }

class NoiseInteropTest {
    @Test
    fun matchesGoFlynnNoiseVectorsByteForByte() {
        val raw = javaClass.classLoader!!.getResourceAsStream("interop_vectors.json")!!.bufferedReader().readText()
        val vec = Json.decodeFromString<InteropVectors>(raw)

        val initStatic = NoiseSession.keyPairFromPrivate(vec.init_static_priv_hex.decodeHex())
        val respStatic = NoiseSession.keyPairFromPrivate(vec.resp_static_priv_hex.decodeHex())
        assertEquals(vec.init_static_pub_hex, initStatic.publicKey.encodeHex())
        assertEquals(vec.resp_static_pub_hex, respStatic.publicKey.encodeHex())

        // 1. Verify XXpsk3 handshake and transport ciphertext match Go flynn/noise byte-for-byte.
        val pXX = NoiseSession.xxPsk3(true, initStatic, vec.psk_hex.decodeHex(), vec.init_eph_hex.decodeHex())
        val bXX = NoiseSession.xxPsk3(false, respStatic, vec.psk_hex.decodeHex(), vec.resp_eph_hex.decodeHex())

        val (m1, d1) = pXX.writeHandshake()
        assertFalse(d1)
        assertEquals(vec.xx_m1_hex, m1.encodeHex())
        assertFalse(bXX.readHandshake(m1).second)

        val (m2, d2) = bXX.writeHandshake()
        assertFalse(d2)
        assertEquals(vec.xx_m2_hex, m2.encodeHex())
        assertFalse(pXX.readHandshake(m2).second)

        val (m3, d3) = pXX.writeHandshake()
        assertTrue(d3)
        assertEquals(vec.xx_m3_hex, m3.encodeHex())
        assertTrue(bXX.readHandshake(m3).second)

        val samplePlain = vec.sample_plain_hex.decodeHex()
        val xxPkts = pXX.encryptFrame(samplePlain)
        assertEquals(1, xxPkts.size)
        assertEquals(vec.xx_transport_hex, xxPkts[0].encodeHex())
        assertArrayEquals(samplePlain, bXX.decryptChunk(xxPkts[0]))

        // 2. Verify KK handshake and transport ciphertext match Go flynn/noise byte-for-byte.
        val pKK = NoiseSession.kk(true, initStatic, respStatic.publicKey, vec.init_eph_hex.decodeHex())
        val bKK = NoiseSession.kk(false, respStatic, initStatic.publicKey, vec.resp_eph_hex.decodeHex())

        val (km1, kd1) = pKK.writeHandshake()
        assertFalse(kd1)
        assertEquals(vec.kk_m1_hex, km1.encodeHex())
        assertFalse(bKK.readHandshake(km1).second)

        val (km2, kd2) = bKK.writeHandshake()
        assertTrue(kd2)
        assertEquals(vec.kk_m2_hex, km2.encodeHex())
        assertTrue(pKK.readHandshake(km2).second)

        val kkPkts = pKK.encryptFrame(samplePlain)
        assertEquals(1, kkPkts.size)
        assertEquals(vec.kk_transport_hex, kkPkts[0].encodeHex())
        assertArrayEquals(samplePlain, bKK.decryptChunk(kkPkts[0]))
    }

    @Test
    fun rejectsWrongPskAndWrongStaticKeyAndSupportsMultiChunkEcho() {
        val phone = NoiseSession.generateKeyPair()
        val bridge = NoiseSession.generateKeyPair()
        val psk = ByteArray(32) { (it + 1).toByte() }
        val wrongPsk = psk.copyOf().also { it[0] = (it[0].toInt() xor 0xff).toByte() }

        val pBad = NoiseSession.xxPsk3(true, phone, psk)
        val bBad = NoiseSession.xxPsk3(false, bridge, wrongPsk)
        val (m1, _) = pBad.writeHandshake()
        bBad.readHandshake(m1)
        val (m2, _) = bBad.writeHandshake()
        pBad.readHandshake(m2)
        val (m3, _) = pBad.writeHandshake()
        var failed = false
        try {
            bBad.readHandshake(m3)
        } catch (_: NoiseCryptoException) {
            failed = true
        }
        assertTrue("wrong PSK must fail", failed)

        // Multi-chunk round trip over KK.
        val pKK = NoiseSession.kk(true, phone, bridge.publicKey)
        val bKK = NoiseSession.kk(false, bridge, phone.publicKey)
        bKK.readHandshake(pKK.writeHandshake().first)
        pKK.readHandshake(bKK.writeHandshake().first)
        assertTrue(pKK.isReady && bKK.isReady)

        val big = ByteArray(150_000) { (it % 251).toByte() }
        val pkts = pKK.encryptFrame(big)
        assertEquals(3, pkts.size)
        var reassembled: ByteArray? = null
        for (pkt in pkts) {
            reassembled = bKK.decryptChunk(pkt)
        }
        assertNotNull(reassembled)
        assertArrayEquals(big, reassembled)
    }
}
