package dev.ddgrcf.sidekick.core.relay

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import dev.ddgrcf.sidekick.core.store.SecretPrefs
import dev.ddgrcf.sidekick.core.store.SecretPrefsSerializer
import dev.ddgrcf.sidekick.core.store.SecretStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RelayApiAndTokenTest {
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

    private fun testStore(scope: kotlinx.coroutines.CoroutineScope): SecretStore {
        val ds: DataStore<SecretPrefs> = DataStoreFactory.create(
            serializer = SecretPrefsSerializer,
            scope = scope,
            produceFile = { File(tempFolder.root, "prefs.json") },
        )
        return SecretStore(ds)
    }

    @Test
    fun proactiveRefreshAnd401LogoutFlow() = runTest {
        val store = testStore(backgroundScope)
        val baseUrl = server.url("").toString().trimEnd('/')
        store.updateRelayUrl(baseUrl)
        store.updateTokens("old-tok", 100L, "rt-1", "u1", "d1")

        // 1. Proactive refresh triggers because expiresAtMs is expired.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"access_token":"fresh-tok","refresh_token":"rt-2","expires_at_ms":${System.currentTimeMillis() + 900_000},"user_id":"u1","device_id":"d1"}"""),
        )
        val tm = TokenManager(api, store)
        val tok = tm.validAccessToken()
        assertEquals("fresh-tok", tok)
        assertEquals("rt-2", store.current().refreshToken)

        // 2. Refresh 401 clears auth and emits logout.
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"invalid_refresh_token","message":"revoked"}}"""))
        var expired = false
        try {
            tm.forceRefresh()
        } catch (_: AuthExpiredException) {
            expired = true
        }
        assertTrue(expired)
        assertEquals("", store.prefs.first().refreshToken)
    }

    @Test
    fun authExchangeAndMfaVerify() = runTest {
        val baseUrl = server.url("").toString().trimEnd('/')
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"mfa_ticket":"t1","totp_enrolled":true}"""))
        val res = api.exchangeAuthCode(baseUrl, "code-abc")
        assertTrue(res is AuthResult.MfaRequired)
        assertEquals("t1", (res as AuthResult.MfaRequired).mfaTicket)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"access_token":"a1","refresh_token":"r1","expires_at_ms":9999999,"user_id":"u1"}"""))
        val tokens = api.verifyTotp(baseUrl, "t1", "123456")
        assertEquals("a1", tokens.access_token)
    }
}
