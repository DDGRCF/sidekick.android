package dev.ddgrcf.sidekick.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.core.relay.PeerFrameEvent
import dev.ddgrcf.sidekick.core.relay.RelaySocketManager
import dev.ddgrcf.sidekick.core.store.HistoryEntity
import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.core.store.SessionEntity
import dev.ddgrcf.sidekick.core.store.SidekickDatabase
import dev.ddgrcf.sidekick.proto.v1.Frame
import dev.ddgrcf.sidekick.proto.v1.GetMediaRequest
import dev.ddgrcf.sidekick.proto.v1.GetMessagesRequest
import dev.ddgrcf.sidekick.proto.v1.Hello
import dev.ddgrcf.sidekick.proto.v1.HistoryEntry
import dev.ddgrcf.sidekick.proto.v1.InputRequest
import dev.ddgrcf.sidekick.proto.v1.Key
import dev.ddgrcf.sidekick.proto.v1.KeysRequest
import dev.ddgrcf.sidekick.proto.v1.ListHistoryRequest
import dev.ddgrcf.sidekick.proto.v1.ListSessionsRequest
import dev.ddgrcf.sidekick.proto.v1.Message
import dev.ddgrcf.sidekick.proto.v1.Presence
import dev.ddgrcf.sidekick.proto.v1.PresenceSource
import dev.ddgrcf.sidekick.proto.v1.Session
import dev.ddgrcf.sidekick.proto.v1.StartSessionRequest
import dev.ddgrcf.sidekick.proto.v1.StopSessionRequest
import dev.ddgrcf.sidekick.proto.v1.Stream
import dev.ddgrcf.sidekick.proto.v1.SubscribeRequest
import dev.ddgrcf.sidekick.proto.v1.ToolInfo
import dev.ddgrcf.sidekick.proto.v1.UnsubscribeRequest
import dev.ddgrcf.sidekick.proto.v1.Workspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
sealed interface UiPart {
    @Serializable
    data class Text(val text: String) : UiPart

    @Serializable
    data class Thinking(val text: String) : UiPart

    @Serializable
    data class ToolCall(val callId: String, val name: String, val inputJson: String, val status: String) : UiPart

    @Serializable
    data class ToolResult(val callId: String, val output: String, val isError: Boolean, val truncated: Boolean) : UiPart

    @Serializable
    data class Media(
        val mediaId: String,
        val kind: String, // "image" | "video" | "audio"
        val mimeType: String,
        val name: String,
        val sizeBytes: Long = 0L,
        val width: Int = 0,
        val height: Int = 0,
        val durationMs: Long = 0L,
        val localUri: String? = null,
        val thumbnailB64: String? = null,
        val sha256Hex: String = "",
    ) : UiPart
}

@Singleton
class SessionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val socketManager: RelaySocketManager,
    private val db: SidekickDatabase,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val connectionState: StateFlow<ConnectionState> = socketManager.connectionState
    val events: SharedFlow<PeerFrameEvent> = socketManager.events

    fun observeSessions(bridgeId: String): Flow<List<SessionEntity>> =
        db.sessionDao().observeByBridge(bridgeId)

    fun observeHistory(bridgeId: String): Flow<List<HistoryEntity>> =
        db.historyDao().observeByBridge(bridgeId)

    fun observeMessages(bridgeId: String, convId: String): Flow<List<MessageEntity>> =
        db.messageDao().observeMessages(bridgeId, convId)

    fun decodeParts(partsJson: String): List<UiPart> =
        runCatching { json.decodeFromString<List<UiPart>>(partsJson) }.getOrDefault(emptyList())

    suspend fun hello(bridgeId: String): Pair<List<ToolInfo>, List<Workspace>> {
        val resp = socketManager.request(bridgeId) { id ->
            Frame(id = id, hello = Hello(agent = "sidekick-android/0.1.0", capabilities = listOf("terminal", "history")))
        }
        val h = resp.hello
        return (h?.tools ?: emptyList()) to (h?.workspaces ?: emptyList())
    }

    suspend fun refreshSessions(bridgeId: String): List<Session> {
        val resp = socketManager.request(bridgeId) { id ->
            Frame(id = id, list_sessions = ListSessionsRequest())
        }
        val list = resp.sessions?.sessions ?: emptyList()
        val now = System.currentTimeMillis()
        db.sessionDao().deleteByBridge(bridgeId)
        db.sessionDao().upsertAll(list.map { s -> s.toEntity(bridgeId, now) })
        return list
    }

    suspend fun refreshHistory(bridgeId: String, tool: String = "", cwd: String = ""): List<HistoryEntry> {
        val resp = socketManager.request(bridgeId) { id ->
            Frame(id = id, list_history = ListHistoryRequest(tool = tool, cwd = cwd, limit = 100))
        }
        val entries = resp.history?.entries ?: emptyList()
        db.historyDao().deleteByBridge(bridgeId)
        db.historyDao().upsertAll(
            entries.mapNotNull { e ->
                val cid = e.conversation?.id ?: return@mapNotNull null
                HistoryEntity(
                    bridgeId = bridgeId,
                    convId = cid,
                    tool = e.tool,
                    cwd = e.cwd,
                    title = e.title.ifEmpty { null },
                    preview = e.preview.ifEmpty { null },
                    updatedMs = e.updated_unix_ms,
                    liveSid = e.session_id.ifEmpty { null },
                )
            },
        )
        return entries
    }

    suspend fun fetchMessages(bridgeId: String, tool: String, convId: String): Boolean {
        val after = db.messageDao().maxSeq(bridgeId, convId).coerceAtLeast(0L)
        val resp = socketManager.request(bridgeId) { id ->
            Frame(
                id = id,
                get_messages = GetMessagesRequest(tool = tool, conversation_id = convId, after_seq = after, limit = 500),
            )
        }
        val body = resp.messages ?: return false
        var wasReset = false
        if (body.reset) {
            db.messageDao().resetMessages(bridgeId, convId)
            wasReset = true
            val full = socketManager.request(bridgeId) { id ->
                Frame(id = id, get_messages = GetMessagesRequest(tool = tool, conversation_id = convId, after_seq = 0L, limit = 500))
            }.messages
            full?.messages?.let { msgs ->
                db.messageDao().upsertAll(msgs.map { it.toEntity(bridgeId, convId) })
            }
        } else if (body.messages.isNotEmpty()) {
            db.messageDao().upsertAll(body.messages.map { it.toEntity(bridgeId, convId) })
        }
        return wasReset
    }

    suspend fun saveIncomingMessage(bridgeId: String, convId: String, msg: Message) {
        if (convId.isBlank()) return
        db.messageDao().upsertAll(listOf(msg.toEntity(bridgeId, convId)))
    }

    suspend fun resetMessages(bridgeId: String, convId: String) {
        if (convId.isBlank()) return
        db.messageDao().resetMessages(bridgeId, convId)
    }

    suspend fun markSessionClosed(bridgeId: String, sid: String) {
        db.sessionDao().markClosed(bridgeId, sid)
    }

    suspend fun updateSession(bridgeId: String, session: Session) {
        db.sessionDao().upsertAll(listOf(session.toEntity(bridgeId, System.currentTimeMillis())))
    }

    suspend fun subscribe(bridgeId: String, sid: String, streams: List<Stream>, afterSeq: Long) {
        socketManager.request(bridgeId) { id ->
            Frame(id = id, subscribe = SubscribeRequest(session_id = sid, streams = streams, after_seq = afterSeq))
        }
    }

    suspend fun unsubscribe(bridgeId: String, sid: String) {
        runCatching {
            socketManager.request(bridgeId) { id ->
                Frame(id = id, unsubscribe = UnsubscribeRequest(session_id = sid))
            }
        }
    }

    suspend fun sendInput(bridgeId: String, sid: String, text: String, submit: Boolean) {
        socketManager.request(bridgeId) { id ->
            Frame(id = id, input = InputRequest(session_id = sid, text = text, submit = submit))
        }
    }

    suspend fun sendKeys(bridgeId: String, sid: String, keys: List<Key>) {
        socketManager.request(bridgeId) { id ->
            Frame(id = id, keys = KeysRequest(session_id = sid, keys = keys))
        }
    }

    suspend fun sendPresence(bridgeId: String, sid: String, typing: Boolean) {
        runCatching {
            socketManager.sendFrame(
                bridgeId,
                Frame(
                    presence = Presence(
                        session_id = sid,
                        source = PresenceSource.PRESENCE_SOURCE_REMOTE,
                        typing = typing,
                    ),
                ),
            )
        }
    }

    suspend fun startSession(bridgeId: String, tool: String, cwd: String, resumeConvId: String = ""): Session? {
        val resp = socketManager.request(bridgeId) { id ->
            Frame(
                id = id,
                start_session = StartSessionRequest(tool = tool, cwd = cwd, resume_conversation_id = resumeConvId),
            )
        }
        val sess = resp.session_started?.session
        if (sess != null) {
            updateSession(bridgeId, sess)
        }
        return sess
    }

    suspend fun stopSession(bridgeId: String, sid: String) {
        socketManager.request(bridgeId) { id ->
            Frame(id = id, stop_session = StopSessionRequest(session_id = sid))
        }
        markSessionClosed(bridgeId, sid)
    }

    suspend fun fetchMedia(bridgeId: String, mediaId: String, mimeType: String, filename: String): File =
        withContext(Dispatchers.IO) {
            val safeId = mediaId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "media" }
            val ext = when {
                mimeType.contains("png") || filename.endsWith(".png", true) -> "png"
                mimeType.contains("webp") || filename.endsWith(".webp", true) -> "webp"
                mimeType.contains("gif") || filename.endsWith(".gif", true) -> "gif"
                else -> "jpg"
            }
            val cacheDir = File(context.cacheDir, "sidekick_media").apply { mkdirs() }
            val target = File(cacheDir, "$safeId.$ext")
            if (target.exists() && target.length() > 0L) {
                return@withContext target
            }

            val out = ByteArrayOutputStream()
            var offset = 0L
            var expectedSha: ByteArray? = null
            while (true) {
                val resp = socketManager.request(bridgeId) { id ->
                    Frame(
                        id = id,
                        get_media = GetMediaRequest(media_id = mediaId, offset = offset, limit = 262_144),
                    )
                }.media ?: error("empty media response")
                val bytes = resp.data_.toByteArray()
                out.write(bytes)
                if (expectedSha == null && resp.sha256_plain.size > 0) {
                    expectedSha = resp.sha256_plain.toByteArray()
                }
                if (resp.eof || bytes.isEmpty()) break
                offset = resp.next_offset
            }
            val fullBytes = out.toByteArray()
            if (expectedSha != null && expectedSha.isNotEmpty()) {
                val actualSha = MessageDigest.getInstance("SHA-256").digest(fullBytes)
                require(actualSha.contentEquals(expectedSha)) { "media sha256 mismatch" }
            }
            val tmp = File(cacheDir, "$safeId.$ext.tmp")
            tmp.writeBytes(fullBytes)
            if (!tmp.renameTo(target)) {
                target.writeBytes(fullBytes)
                tmp.delete()
            }
            target
        }

    private fun Session.toEntity(bridgeId: String, updatedMs: Long) = SessionEntity(
        bridgeId = bridgeId,
        sid = id,
        tool = tool,
        cwd = cwd,
        title = title.ifEmpty { null },
        convId = conversation?.id?.ifEmpty { null },
        live = live,
        lastActivityMs = last_activity_unix_ms,
        updatedMs = updatedMs,
    )

    private fun Message.toEntity(bridgeId: String, convId: String): MessageEntity {
        val uiParts = parts.mapNotNull { p ->
            when {
                p.text != null -> UiPart.Text(p.text!!.text)
                p.thinking != null -> UiPart.Thinking(p.thinking!!.text)
                p.tool_call != null -> {
                    val tc = p.tool_call!!
                    UiPart.ToolCall(tc.call_id, tc.name, tc.input_json, tc.status.name)
                }
                p.tool_result != null -> {
                    val tr = p.tool_result!!
                    UiPart.ToolResult(tr.call_id, tr.output, tr.is_error, tr.truncated)
                }
                p.media != null -> {
                    val mp = p.media!!
                    val thumb = mp.thumbnail.toByteArray()
                    val thumbB64 = if (thumb.isNotEmpty()) Base64.getEncoder().encodeToString(thumb) else null
                    val shaHex = mp.sha256_plain.toByteArray().joinToString("") { "%02x".format(it) }
                    val kindStr = when (mp.kind.name) {
                        "MEDIA_KIND_VIDEO" -> "video"
                        "MEDIA_KIND_AUDIO" -> "audio"
                        else -> "image"
                    }
                    UiPart.Media(
                        mediaId = mp.media_id,
                        kind = kindStr,
                        mimeType = mp.mime_type,
                        name = mp.filename,
                        sizeBytes = mp.size_bytes,
                        width = mp.width,
                        height = mp.height,
                        durationMs = mp.duration_ms,
                        thumbnailB64 = thumbB64,
                        sha256Hex = shaHex,
                    )
                }
                else -> null
            }
        }
        return MessageEntity(
            bridgeId = bridgeId,
            convId = convId,
            seq = seq,
            role = role.name,
            partsJson = json.encodeToString(uiParts),
            createdMs = created_unix_ms,
            model = model.ifEmpty { null },
        )
    }
}
