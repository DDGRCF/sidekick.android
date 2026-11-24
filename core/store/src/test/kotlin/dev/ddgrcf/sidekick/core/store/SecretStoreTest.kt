package dev.ddgrcf.sidekick.core.store

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SecretStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun tinkAeadSerializerEncryptsOnDiskAndRoundTripsSecrets() = runTest {
        AeadConfig.register()
        val handle = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
        val aead = handle.getPrimitive(Aead::class.java)
        val file = File(tempFolder.root, "secrets.pb")
        val ds = DataStoreFactory.create(
            serializer = AeadSerializer(aead, SecretPrefsSerializer),
            scope = backgroundScope,
            produceFile = { file },
        )
        val store = SecretStore(ds)
        assertEquals("system", store.current().themeMode)

        store.updateRelayUrl("https://relay.example.com/")
        store.updateTokens("acc-secret-123", 999999L, "ref-secret-456", "u1", "d1")
        store.updateThemeMode("light")
        val kp = store.ensureNoiseKeyPair()
        store.pinPeer("bridge-1", kp.publicKey)

        val prefs = store.prefs.first()
        assertEquals("https://relay.example.com", prefs.relayUrl)
        assertEquals("ref-secret-456", prefs.refreshToken)
        assertEquals("light", prefs.themeMode)
        assertNotNull(store.getPeerPin("bridge-1"))

        val rawBytes = file.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse("plaintext token must not appear on disk", rawBytes.contains("ref-secret-456"))

        store.clearAuth()
        val afterClear = store.current()
        assertEquals("", afterClear.refreshToken)
        assertEquals(emptyMap<String, String>(), afterClear.peerPins)
        assertEquals("light", afterClear.themeMode)
    }
}
