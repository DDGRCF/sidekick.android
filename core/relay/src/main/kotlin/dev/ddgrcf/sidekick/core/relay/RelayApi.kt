package dev.ddgrcf.sidekick.core.relay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class RelayHttpException(val statusCode: Int, val code: String, override val message: String) : IOException("$statusCode ($code): $message")
class AuthExpiredException(message: String) : IOException(message)

@Serializable
data class TokenPair(
    val access_token: String,
    val refresh_token: String,
    val expires_at_ms: Long = 0L,
    val user_id: String = "",
    val device_id: String = "",
)

sealed interface AuthResult {
    data class Authenticated(val tokens: TokenPair) : AuthResult
    data class MfaRequired(val mfaTicket: String, val totpEnrolled: Boolean) : AuthResult
}

@Serializable
data class TotpEnrollResponse(
    val secret: String,
    val otpauth_uri: String,
)

@Serializable
data class TotpConfirmResponse(
    val recovery_codes: List<String> = emptyList(),
    val access_token: String = "",
    val refresh_token: String = "",
    val expires_at_ms: Long = 0L,
    val user_id: String = "",
)

@Serializable
data class RelayDevice(
    val id: String,
    val user_id: String = "",
    val kind: String,
    val name: String,
    val pubkey: String,
    val enroll_state: String = "",
    val created_at: Long = 0L,
    val last_seen_at: Long = 0L,
    val online: Boolean = false,
)

@Serializable
data class DeviceRegisterResponse(
    val device_id: String,
    val access_token: String,
    val refresh_token: String = "",
    val expires_at_ms: Long = 0L,
)

@Serializable
data class ApproveEnrollResponse(
    val ok: Boolean = false,
    val enroll_id: String = "",
    val bridge_device_id: String = "",
    val bridge_name: String = "",
    val bridge_pubkey: String = "",
)

@Serializable
data class QrPairingPayload(
    val v: Int = 1,
    val relay: String,
    val eid: String,
    val pk: String,
    val psk: String,
)

class RelayApi(
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun exchangeAuthCode(baseUrl: String, code: String): AuthResult {
        val (status, body) = postJson(baseUrl, "/auth/token/exchange", null, json.encodeToString(mapOf("code" to code)))
        if (status == 202) {
            val mfa = json.decodeFromString<MfaJson>(body)
            return AuthResult.MfaRequired(mfa.mfa_ticket, mfa.totp_enrolled)
        }
        if (status != 200) throw parseError(status, body)
        return AuthResult.Authenticated(json.decodeFromString<TokenPair>(body))
    }

    suspend fun verifyTotp(baseUrl: String, mfaTicket: String, code: String): TokenPair {
        val payload = json.encodeToString(mapOf("mfa_ticket" to mfaTicket, "code" to code))
        val (status, body) = postJson(baseUrl, "/auth/totp/verify", null, payload)
        if (status != 200) throw parseError(status, body)
        return json.decodeFromString(body)
    }

    suspend fun devLogin(baseUrl: String): TokenPair {
        val (status, body) = postJson(baseUrl, "/auth/dev/login", null, "{}")
        if (status != 200) throw parseError(status, body)
        return json.decodeFromString(body)
    }

    suspend fun enrollTotp(baseUrl: String, accessToken: String): TotpEnrollResponse {
        val (status, body) = postJson(baseUrl, "/auth/totp/enroll", accessToken, "{}")
        if (status != 200) throw parseError(status, body)
        return json.decodeFromString(body)
    }

    suspend fun confirmTotp(baseUrl: String, accessToken: String, code: String): TotpConfirmResponse {
        val (status, body) = postJson(baseUrl, "/auth/totp/confirm", accessToken, json.encodeToString(mapOf("code" to code)))
        if (status != 200) throw parseError(status, body)
        return json.decodeFromString(body)
    }

    suspend fun refresh(baseUrl: String, refreshToken: String): TokenPair {
        val (status, body) = postJson(baseUrl, "/auth/token/refresh", null, json.encodeToString(mapOf("refresh_token" to refreshToken)))
        if (status == 401 || status == 403) {
            throw AuthExpiredException("refresh rejected ($status)")
        }
        if (status != 200) throw parseError(status, body)
        return json.decodeFromString(body)
    }

    suspend fun logout(baseUrl: String, refreshToken: String) {
        if (baseUrl.isBlank() || refreshToken.isBlank()) return
        runCatching {
            postJson(baseUrl, "/auth/logout", null, json.encodeToString(mapOf("refresh_token" to refreshToken)))
        }
    }

    suspend fun registerDevice(baseUrl: String, accessToken: String, name: String, pubkeyB64: String): DeviceRegisterResponse {
        val payload = json.encodeToString(mapOf("kind" to "phone", "name" to name, "pubkey" to pubkeyB64))
        val (status, body) = postJson(baseUrl, "/devices/pair", accessToken, payload)
        if (status != 200) throw parseError(status, body)
        return json.decodeFromString(body)
    }

    suspend fun listDevices(baseUrl: String, accessToken: String): List<RelayDevice> {
        val (status, body) = get(baseUrl, "/devices", accessToken)
        if (status != 200) throw parseError(status, body)
        return json.decodeFromString<DevicesListJson>(body).devices
    }

    suspend fun revokeDevice(baseUrl: String, accessToken: String, deviceId: String) {
        val (status, body) = delete(baseUrl, "/devices/$deviceId", accessToken)
        if (status != 200) throw parseError(status, body)
    }

    suspend fun approveEnroll(
        baseUrl: String,
        accessToken: String,
        enrollId: String?,
        userCode: String?,
        deviceId: String,
    ): ApproveEnrollResponse {
        val reqObj = ApproveEnrollReq(
            enroll_id = enrollId ?: "",
            user_code = userCode ?: "",
            device_id = deviceId,
        )
        val (status, body) = postJson(baseUrl, "/devices/enroll/approve", accessToken, json.encodeToString(reqObj))
        if (status != 200) throw parseError(status, body)
        return json.decodeFromString(body)
    }

    private suspend fun get(baseUrl: String, path: String, bearer: String?): Pair<Int, String> =
        exec(Request.Builder().url(normalizeHttp(baseUrl) + path).get().applyBearer(bearer).build())

    private suspend fun delete(baseUrl: String, path: String, bearer: String?): Pair<Int, String> =
        exec(Request.Builder().url(normalizeHttp(baseUrl) + path).delete().applyBearer(bearer).build())

    private suspend fun postJson(baseUrl: String, path: String, bearer: String?, bodyJson: String): Pair<Int, String> =
        exec(
            Request.Builder()
                .url(normalizeHttp(baseUrl) + path)
                .post(bodyJson.toRequestBody(jsonMediaType))
                .applyBearer(bearer)
                .build(),
        )

    private fun Request.Builder.applyBearer(bearer: String?): Request.Builder =
        if (!bearer.isNullOrBlank()) header("Authorization", "Bearer $bearer") else this

    private suspend fun exec(req: Request): Pair<Int, String> = withContext(Dispatchers.IO) {
        client.newCall(req).execute().use { resp ->
            resp.code to (resp.body?.string() ?: "")
        }
    }

    private fun parseError(status: Int, body: String): RelayHttpException {
        return try {
            val err = json.decodeFromString<ErrorEnvelope>(body).error
            RelayHttpException(status, err.code, err.message)
        } catch (_: Exception) {
            RelayHttpException(status, "http_$status", body.ifBlank { "HTTP $status" })
        }
    }

    @Serializable
    private data class MfaJson(val mfa_ticket: String = "", val totp_enrolled: Boolean = false)

    @Serializable
    private data class DevicesListJson(val devices: List<RelayDevice> = emptyList())

    @Serializable
    private data class ApproveEnrollReq(val enroll_id: String = "", val user_code: String = "", val device_id: String)

    @Serializable
    private data class ErrorEnvelope(val error: ErrorBody)

    @Serializable
    private data class ErrorBody(val code: String = "", val message: String = "")

    companion object {
        fun normalizeHttp(url: String): String {
            val trimmed = url.trim().trimEnd('/')
            return when {
                trimmed.startsWith("wss://") -> "https://" + trimmed.removePrefix("wss://")
                trimmed.startsWith("ws://") -> "http://" + trimmed.removePrefix("ws://")
                else -> trimmed
            }
        }

        fun normalizeWs(url: String): String {
            val trimmed = url.trim().trimEnd('/')
            return when {
                trimmed.startsWith("https://") -> "wss://" + trimmed.removePrefix("https://")
                trimmed.startsWith("http://") -> "ws://" + trimmed.removePrefix("http://")
                else -> trimmed
            }
        }
    }
}
