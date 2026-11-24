package dev.ddgrcf.sidekick.core.store

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dev.ddgrcf.sidekick.core.noise.DHKeyPair
import dev.ddgrcf.sidekick.core.noise.NoiseSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.Base64

@Serializable
data class SecretPrefs(
    val relayUrl: String = "",
    val accessToken: String = "",
    val accessExpiresAtMs: Long = 0L,
    val refreshToken: String = "",
    val userId: String = "",
    val userLogin: String = "",
    val deviceId: String = "",
    val noiseStaticPrivB64: String = "",
    val peerPins: Map<String, String> = emptyMap(),
    val lastBridgeId: String = "",
    val themeMode: String = "system",
)

object SecretPrefsSerializer : Serializer<SecretPrefs> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: SecretPrefs = SecretPrefs()

    override suspend fun readFrom(input: InputStream): SecretPrefs {
        val text = input.readBytes().toString(Charsets.UTF_8)
        if (text.isBlank()) return defaultValue
        return try {
            json.decodeFromString<SecretPrefs>(text)
        } catch (e: Exception) {
            throw CorruptionException("Cannot read SecretPrefs", e)
        }
    }

    override suspend fun writeTo(t: SecretPrefs, output: OutputStream) {
        output.write(json.encodeToString(t).toByteArray(Charsets.UTF_8))
    }
}

class SecretStore(
    private val dataStore: DataStore<SecretPrefs>,
) {
    val prefs: Flow<SecretPrefs> = dataStore.data

    suspend fun current(): SecretPrefs = dataStore.data.first()

    suspend fun updateRelayUrl(url: String) {
        dataStore.updateData { it.copy(relayUrl = url.trim().trimEnd('/')) }
    }

    suspend fun updateTokens(
        accessToken: String,
        expiresAtMs: Long,
        refreshToken: String,
        userId: String = "",
        deviceId: String = "",
    ) {
        dataStore.updateData { cur ->
            cur.copy(
                accessToken = accessToken,
                accessExpiresAtMs = expiresAtMs,
                refreshToken = refreshToken.ifEmpty { cur.refreshToken },
                userId = userId.ifEmpty { cur.userId },
                deviceId = deviceId.ifEmpty { cur.deviceId },
            )
        }
    }

    suspend fun updateDeviceId(deviceId: String) {
        dataStore.updateData { it.copy(deviceId = deviceId) }
    }

    suspend fun updateLastBridgeId(bridgeId: String) {
        dataStore.updateData { it.copy(lastBridgeId = bridgeId) }
    }

    suspend fun updateThemeMode(mode: String) {
        val normalized = when (mode.lowercase()) {
            "dark", "light" -> mode.lowercase()
            else -> "system"
        }
        dataStore.updateData { it.copy(themeMode = normalized) }
    }

    suspend fun ensureNoiseKeyPair(): DHKeyPair {
        val cur = current()
        if (cur.noiseStaticPrivB64.isNotEmpty()) {
            val priv = Base64.getDecoder().decode(cur.noiseStaticPrivB64)
            return NoiseSession.keyPairFromPrivate(priv)
        }
        val kp = NoiseSession.generateKeyPair()
        val privB64 = Base64.getEncoder().encodeToString(kp.privateKey)
        dataStore.updateData { it.copy(noiseStaticPrivB64 = privB64) }
        return kp
    }

    suspend fun pinPeer(deviceId: String, pubkey: ByteArray) {
        val b64 = Base64.getEncoder().encodeToString(pubkey)
        dataStore.updateData { cur ->
            cur.copy(peerPins = cur.peerPins + (deviceId to b64))
        }
    }

    suspend fun getPeerPin(deviceId: String): ByteArray? {
        val b64 = current().peerPins[deviceId] ?: return null
        return Base64.getDecoder().decode(b64)
    }

    suspend fun clearAuth() {
        dataStore.updateData { cur ->
            SecretPrefs(
                relayUrl = cur.relayUrl,
                noiseStaticPrivB64 = cur.noiseStaticPrivB64,
                peerPins = emptyMap(),
                themeMode = cur.themeMode,
            )
        }
    }

    companion object {
        fun fingerprintFromBase64(b64: String): String {
            return try {
                val raw = Base64.getDecoder().decode(b64)
                val digest = MessageDigest.getInstance("SHA-256").digest(raw)
                digest.take(6).joinToString("") { "%02x".format(it) }
            } catch (_: Exception) {
                ""
            }
        }

        fun create(context: Context): SecretStore {
            AeadConfig.register()
            val keysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context.applicationContext, "sidekick_tink_keyset", "sidekick_tink_prefs")
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://sidekick_master_key")
                .build()
                .keysetHandle
            val aead = keysetHandle.getPrimitive(Aead::class.java)
            val ds = DataStoreFactory.create(
                serializer = AeadSerializer(aead, SecretPrefsSerializer),
                produceFile = { File(context.applicationContext.filesDir, "sidekick_secrets.pb") },
            )
            return SecretStore(ds)
        }
    }
}
