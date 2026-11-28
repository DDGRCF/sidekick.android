package dev.ddgrcf.sidekick.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.core.relay.PeerFrameEvent
import dev.ddgrcf.sidekick.core.relay.RelaySocketManager
import dev.ddgrcf.sidekick.core.store.HistoryEntity
import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.core.store.SessionEntity
import dev.ddgrcf.sidekick.core.store.SidekickDatabase
import dev.ddgrcf.sidekick.proto.v1.Backend
import dev.ddgrcf.sidekick.proto.v1.FileEntry
import dev.ddgrcf.sidekick.proto.v1.Frame
import dev.ddgrcf.sidekick.proto.v1.GetMediaRequest
import dev.ddgrcf.sidekick.proto.v1.GetMessagesRequest
import dev.ddgrcf.sidekick.proto.v1.GetSessionConfigRequest
import dev.ddgrcf.sidekick.proto.v1.GetWorkspaceContextRequest
import dev.ddgrcf.sidekick.proto.v1.HandoverSessionRequest
import dev.ddgrcf.sidekick.proto.v1.Hello
import dev.ddgrcf.sidekick.proto.v1.HistoryEntry
import dev.ddgrcf.sidekick.proto.v1.InputRequest
import dev.ddgrcf.sidekick.proto.v1.InterruptTurnRequest
import dev.ddgrcf.sidekick.proto.v1.Key
import dev.ddgrcf.sidekick.proto.v1.KeysRequest
import dev.ddgrcf.sidekick.proto.v1.ListCommandsRequest
import dev.ddgrcf.sidekick.proto.v1.ListFilesRequest
import dev.ddgrcf.sidekick.proto.v1.ListHistoryRequest
import dev.ddgrcf.sidekick.proto.v1.ListSessionsRequest
import dev.ddgrcf.sidekick.proto.v1.MediaKind as ProtoMediaKind
import dev.ddgrcf.sidekick.proto.v1.Message
import dev.ddgrcf.sidekick.proto.v1.OpenInNvimRequest
import dev.ddgrcf.sidekick.proto.v1.OpenInNvimResponse
import dev.ddgrcf.sidekick.proto.v1.Presence
import dev.ddgrcf.sidekick.proto.v1.PresenceSource
import dev.ddgrcf.sidekick.proto.v1.RespondRuntimeRequest
import dev.ddgrcf.sidekick.proto.v1.RuntimeAnswer
import dev.ddgrcf.sidekick.proto.v1.RuntimeMode
import dev.ddgrcf.sidekick.proto.v1.Session
import dev.ddgrcf.sidekick.proto.v1.SessionConfig
import dev.ddgrcf.sidekick.proto.v1.SetSessionConfigRequest
import dev.ddgrcf.sidekick.proto.v1.SlashCommand
import dev.ddgrcf.sidekick.proto.v1.StartSessionRequest
import dev.ddgrcf.sidekick.proto.v1.StopSessionRequest
import dev.ddgrcf.sidekick.proto.v1.Stream
import dev.ddgrcf.sidekick.proto.v1.SubscribeRequest
import dev.ddgrcf.sidekick.proto.v1.ToolInfo
import dev.ddgrcf.sidekick.proto.v1.UnsubscribeRequest
import dev.ddgrcf.sidekick.proto.v1.UploadMediaRequest
import dev.ddgrcf.sidekick.proto.v1.Workspace
import dev.ddgrcf.sidekick.proto.v1.WorkspaceContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.toByteString
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
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
    private val rpcSessionKeys = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val uiPrefs by lazy { context.getSharedPreferences("sidekick_ui_prefs", Context.MODE_PRIVATE) }
    private val hiddenHistoryKeys by lazy {
        MutableStateFlow(uiPrefs.getStringSet("hidden_history_keys", emptySet())?.toSet() ?: emptySet())
    }

    private fun historyHideKey(bridgeId: String, convId: String): String = "$bridgeId::$convId"

    private fun saveHiddenHistoryKeys(next: Set<String>) {
        hiddenHistoryKeys.value = next
        uiPrefs.edit().putStringSet("hidden_history_keys", next).apply()
    }

    fun isRpcSession(bridgeId: String, sid: String): Boolean = rpcSessionKeys.contains("$bridgeId:$sid")

    fun markRpcSession(bridgeId: String, sid: String) {
        if (bridgeId.isNotBlank() && sid.isNotBlank()) {
            rpcSessionKeys.add("$bridgeId:$sid")
        }
    }

    private fun recordBackend(bridgeId: String, s: Session) {
        val k = "$bridgeId:${s.id}"
        if (s.backend == Backend.BACKEND_RPC) {
            rpcSessionKeys.add(k)
        } else if (s.backend != Backend.BACKEND_UNSPECIFIED) {
            rpcSessionKeys.remove(k)
        }
    }

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
        list.forEach { recordBackend(bridgeId, it) }
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
        val hidden = hiddenHistoryKeys.value
        db.historyDao().deleteByBridge(bridgeId)
        db.historyDao().upsertAll(
            entries.mapNotNull { e ->
                val cid = e.conversation?.id ?: return@mapNotNull null
                if (historyHideKey(bridgeId, cid) in hidden) return@mapNotNull null
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

    suspend fun migratePendingConvMessages(bridgeId: String, fromConvId: String, toConvId: String) {
        if (fromConvId.isBlank() || toConvId.isBlank() || fromConvId == toConvId) return
        val existing = db.messageDao().observeMessages(bridgeId, fromConvId).first()
        if (existing.isNotEmpty()) {
            db.messageDao().upsertAll(existing.map { it.copy(convId = toConvId) })
        }
        db.messageDao().resetMessages(bridgeId, fromConvId)
    }

    suspend fun markSessionClosed(bridgeId: String, sid: String) {
        db.sessionDao().markClosed(bridgeId, sid)
    }

    suspend fun updateSession(bridgeId: String, session: Session) {
        recordBackend(bridgeId, session)
        db.sessionDao().upsertAll(listOf(session.toEntity(bridgeId, System.currentTimeMillis())))
    }

    suspend fun getSessionConfig(bridgeId: String, sid: String): SessionConfig? {
        val resp = socketManager.request(bridgeId) { id ->
            Frame(id = id, get_session_config = GetSessionConfigRequest(session_id = sid))
        }
        return resp.session_config
    }

    suspend fun setSessionConfig(
        bridgeId: String,
        sid: String,
        model: String = "",
        thinkingLevel: String = "",
        runtimeMode: RuntimeMode = RuntimeMode.RUNTIME_MODE_UNSPECIFIED,
    ): SessionConfig? {
        val resp = socketManager.request(bridgeId) { id ->
            Frame(
                id = id,
                set_session_config = SetSessionConfigRequest(
                    session_id = sid,
                    model = model,
                    thinking_level = thinkingLevel,
                    runtime_mode = runtimeMode,
                ),
            )
        }
        return resp.session_config
    }

    suspend fun respondRuntimeRequest(
        bridgeId: String,
        sid: String,
        requestId: String,
        decision: String = "",
        feedback: String = "",
        answers: List<RuntimeAnswer> = emptyList(),
        cancelled: Boolean = false,
    ) {
        socketManager.request(bridgeId) { id ->
            Frame(
                id = id,
                respond_runtime_request = RespondRuntimeRequest(
                    session_id = sid,
                    request_id = requestId,
                    decision = decision,
                    feedback = feedback,
                    answers = answers,
                    cancelled = cancelled,
                ),
            )
        }
    }

    suspend fun interruptTurn(bridgeId: String, sid: String) {
        socketManager.request(bridgeId) { id ->
            Frame(id = id, interrupt_turn = InterruptTurnRequest(session_id = sid))
        }
    }

    suspend fun getWorkspaceContext(
        bridgeId: String,
        sid: String,
        cwd: String = "",
        includeDiff: Boolean = false,
    ): WorkspaceContext? {
        val resp = socketManager.request(bridgeId) { id ->
            Frame(
                id = id,
                get_workspace_context = GetWorkspaceContextRequest(
                    session_id = sid,
                    cwd = cwd,
                    include_diff = includeDiff,
                ),
            )
        }
        return resp.workspace_context
    }

    suspend fun openInNvim(
        bridgeId: String,
        sid: String,
        tool: String,
        cwd: String,
        convId: String,
        stopRemoteRpc: Boolean = true,
    ): OpenInNvimResponse? {
        val resp = socketManager.request(bridgeId) { id ->
            Frame(
                id = id,
                open_in_nvim = OpenInNvimRequest(
                    session_id = sid,
                    tool = tool,
                    cwd = cwd,
                    conversation_id = convId,
                    stop_remote_rpc = stopRemoteRpc,
                ),
            )
        }
        return resp.open_in_nvim_resp
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

    suspend fun handoverSession(
        bridgeId: String,
        sourceSid: String,
        targetTool: String,
        targetSid: String? = null,
        handoverNote: String,
        includeContext: Boolean = true,
    ): Session? {
        val resp = socketManager.request(bridgeId) { id ->
            Frame(
                id = id,
                handover_session = HandoverSessionRequest(
                    source_session_id = sourceSid,
                    target_tool = targetTool,
                    target_session_id = targetSid.orEmpty(),
                    handover_note = handoverNote,
                    include_context = includeContext,
                ),
            )
        }
        val target = resp.handover_session_resp?.target_session
        if (target != null) {
            updateSession(bridgeId, target)
        }
        return target
    }

    suspend fun stopSession(bridgeId: String, sid: String) {
        socketManager.request(bridgeId) { id ->
            Frame(id = id, stop_session = StopSessionRequest(session_id = sid))
        }
        markSessionClosed(bridgeId, sid)
    }

    suspend fun removeSession(bridgeId: String, sid: String, live: Boolean) {
        if (live) {
            runCatching {
                socketManager.request(bridgeId) { id ->
                    Frame(id = id, stop_session = StopSessionRequest(session_id = sid))
                }
            }.onFailure { err ->
                if (!err.message.orEmpty().contains("not found", ignoreCase = true)) {
                    throw err
                }
            }
        }
        rpcSessionKeys.remove("$bridgeId:$sid")
        db.sessionDao().deleteBySid(bridgeId, sid)
    }

    suspend fun hideHistory(bridgeId: String, convId: String) {
        if (convId.isBlank()) return
        saveHiddenHistoryKeys(hiddenHistoryKeys.value + historyHideKey(bridgeId, convId))
        db.historyDao().deleteByConvId(bridgeId, convId)
    }

    fun observeHiddenHistoryCount(bridgeId: String): Flow<Int> =
        hiddenHistoryKeys.map { set ->
            val prefix = "$bridgeId::"
            set.count { it.startsWith(prefix) }
        }

    suspend fun restoreHiddenHistory(bridgeId: String) {
        val prefix = "$bridgeId::"
        saveHiddenHistoryKeys(hiddenHistoryKeys.value.filterNot { it.startsWith(prefix) }.toSet())
        refreshHistory(bridgeId)
    }

    suspend fun listFiles(bridgeId: String, sid: String, query: String, limit: Int = 50): List<FileEntry> {
        val resp = socketManager.request(bridgeId) { id ->
            Frame(
                id = id,
                list_files = ListFilesRequest(session_id = sid, query = query, limit = limit),
            )
        }
        return resp.files?.files ?: emptyList()
    }

    suspend fun listCommands(bridgeId: String, sid: String, tool: String): List<SlashCommand> {
        val resp = socketManager.request(bridgeId) { id ->
            Frame(
                id = id,
                list_commands = ListCommandsRequest(session_id = sid, tool = tool),
            )
        }
        return resp.commands?.commands ?: emptyList()
    }

    suspend fun fetchMedia(bridgeId: String, mediaId: String, mimeType: String, filename: String): File =
        withContext(Dispatchers.IO) {
            val safeId = mediaId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "media" }
            val ext = resolveMediaExtension(mimeType, filename)
            val cacheDir = File(context.cacheDir, "sidekick_media").apply { mkdirs() }
            val target = File(cacheDir, "$safeId.$ext")
            if (target.exists() && target.length() > 0L) {
                return@withContext target
            }

            val tmp = File(cacheDir, "$safeId.$ext.tmp")
            val digest = MessageDigest.getInstance("SHA-256")
            var offset = 0L
            var expectedSha: ByteArray? = null
            try {
                FileOutputStream(tmp).use { out ->
                    while (true) {
                        val resp = socketManager.request(bridgeId) { id ->
                            Frame(
                                id = id,
                                get_media = GetMediaRequest(media_id = mediaId, offset = offset, limit = MEDIA_CHUNK_BYTES),
                            )
                        }.media ?: error("empty media response")
                        val bytes = resp.data_.toByteArray()
                        if (bytes.isNotEmpty()) {
                            out.write(bytes)
                            digest.update(bytes)
                        }
                        if (expectedSha == null && resp.sha256_plain.size > 0) {
                            expectedSha = resp.sha256_plain.toByteArray()
                        }
                        if (resp.eof || bytes.isEmpty()) break
                        offset = resp.next_offset
                    }
                    out.flush()
                }
                if (expectedSha != null && expectedSha.isNotEmpty()) {
                    val actualSha = digest.digest()
                    require(actualSha.contentEquals(expectedSha)) { "media sha256 mismatch" }
                }
                if (!tmp.renameTo(target)) {
                    tmp.copyTo(target, overwrite = true)
                    tmp.delete()
                }
                target
            } catch (t: Throwable) {
                tmp.delete()
                throw t
            }
        }

    suspend fun uploadMedia(
        bridgeId: String,
        sid: String,
        kind: String,
        mimeType: String,
        filename: String,
        uriString: String,
    ): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(MEDIA_CHUNK_BYTES)
        var totalSize = 0L
        openAttachmentStream(uriString).use { input ->
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                totalSize += n
                require(totalSize <= MAX_UPLOAD_MEDIA_BYTES) { "单个媒体附件不能超过 16 MiB" }
                digest.update(buf, 0, n)
            }
        }
        require(totalSize > 0L) { "附件为空或无法读取" }

        val shaBytes = digest.digest().toByteString()
        val uploadId = UUID.randomUUID().toString()
        val protoKind = when (kind.lowercase()) {
            "video" -> ProtoMediaKind.MEDIA_KIND_VIDEO
            "audio" -> ProtoMediaKind.MEDIA_KIND_AUDIO
            else -> ProtoMediaKind.MEDIA_KIND_IMAGE
        }

        var offset = 0L
        var committedPath: String? = null
        openAttachmentStream(uriString).use { input ->
            while (offset < totalSize) {
                val n = input.read(buf)
                if (n <= 0) break
                val nextOffset = offset + n
                val eof = nextOffset >= totalSize
                val resp = socketManager.request(bridgeId) { id ->
                    Frame(
                        id = id,
                        upload_media = UploadMediaRequest(
                            upload_id = uploadId,
                            session_id = sid,
                            kind = protoKind,
                            mime_type = mimeType,
                            filename = filename,
                            total_size = totalSize,
                            offset = offset,
                            data_ = buf.toByteString(0, n),
                            eof = eof,
                            sha256_plain = if (eof) shaBytes else okio.ByteString.EMPTY,
                        ),
                    )
                }.upload_media_resp ?: error("empty upload response")
                if (eof) {
                    require(resp.committed && resp.rel_path.isNotBlank()) { "media upload not committed" }
                    committedPath = resp.rel_path
                }
                offset = nextOffset
            }
        }
        committedPath ?: error("media upload incomplete")
    }

    private fun openAttachmentStream(uriString: String): InputStream {
        if (uriString.startsWith("/")) {
            return File(uriString).inputStream()
        }
        return context.contentResolver.openInputStream(Uri.parse(uriString))
            ?: error("无法打开附件: $uriString")
    }

    private fun resolveMediaExtension(mimeType: String, filename: String): String {
        val rawExt = filename.substringAfterLast('.', "").lowercase()
        if (rawExt in SUPPORTED_MEDIA_EXTENSIONS) {
            return rawExt
        }
        val m = mimeType.lowercase()
        return when {
            m.contains("png") -> "png"
            m.contains("webp") -> "webp"
            m.contains("gif") -> "gif"
            m.contains("mp4") -> "mp4"
            m.contains("quicktime") -> "mov"
            m.contains("webm") -> "webm"
            m.contains("matroska") -> "mkv"
            m.contains("mpeg") -> "mp3"
            m.contains("mp4a") || m.contains("aac") -> "m4a"
            m.contains("wav") -> "wav"
            m.contains("opus") -> "opus"
            m.contains("ogg") -> "ogg"
            m.contains("flac") -> "flac"
            else -> "jpg"
        }
    }

    companion object {
        private const val MEDIA_CHUNK_BYTES = 262_144
        private const val MAX_UPLOAD_MEDIA_BYTES = 16L * 1024 * 1024
        private val SUPPORTED_MEDIA_EXTENSIONS = setOf(
            "png", "jpg", "jpeg", "webp", "gif",
            "mp4", "mov", "webm", "mkv",
            "mp3", "m4a", "wav", "ogg", "opus", "aac", "flac",
        )
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
