package dev.ddgrcf.sidekick.vm

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ddgrcf.sidekick.core.ansi.AnsiLine
import dev.ddgrcf.sidekick.core.ansi.AnsiParser
import dev.ddgrcf.sidekick.core.relay.AuthResult
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.core.relay.QrPairingPayload
import dev.ddgrcf.sidekick.core.relay.RelayApi
import dev.ddgrcf.sidekick.core.relay.RelayDevice
import dev.ddgrcf.sidekick.core.relay.RelaySocketManager
import dev.ddgrcf.sidekick.core.relay.TokenManager
import dev.ddgrcf.sidekick.core.store.HistoryEntity
import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.core.store.SecretStore
import dev.ddgrcf.sidekick.core.store.SessionEntity
import dev.ddgrcf.sidekick.data.SessionRepository
import dev.ddgrcf.sidekick.data.UiPart
import dev.ddgrcf.sidekick.proto.v1.Key
import dev.ddgrcf.sidekick.proto.v1.PresenceSource
import dev.ddgrcf.sidekick.proto.v1.SlashCommandSource
import dev.ddgrcf.sidekick.proto.v1.Stream
import dev.ddgrcf.sidekick.proto.v1.ToolInfo
import dev.ddgrcf.sidekick.proto.v1.Workspace
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class QuotedMessage(
    val author: String,
    val excerpt: String,
)

data class WorkspaceFileSuggestion(
    val relPath: String,
    val isDir: Boolean = false,
    val sizeBytes: Long = 0L,
)

enum class SlashCommandSourceUi(val tag: String) {
    BUILTIN("内置"),
    SKILL("Skill"),
    PROJECT("项目"),
    USER("全局"),
    PLUGIN("插件"),
}

data class SlashCommandSuggestion(
    val name: String,
    val description: String = "",
    val argsHint: String = "",
    val source: SlashCommandSourceUi = SlashCommandSourceUi.BUILTIN,
)

fun coreBuiltinSlashCommands(tool: String): List<SlashCommandSuggestion> {
    fun b(name: String, desc: String, args: String = "") = SlashCommandSuggestion(
        name = name,
        description = desc,
        argsHint = args,
        source = SlashCommandSourceUi.BUILTIN,
    )
    return when (tool.lowercase().trim()) {
        "claude" -> listOf(
            b("compact", "压缩会话上下文（可选重点说明）", "[focus]"),
            b("clear", "清空当前对话上下文"),
            b("model", "切换当前对话模型", "<model>"),
            b("context", "查看上下文窗口占用情况"),
            b("diff", "查看当前工作区变更"),
            b("review", "代码变更审查", "[target]"),
        )
        "codex" -> listOf(
            b("compact", "压缩会话上下文摘要", "[focus]"),
            b("model", "切换模型或推理强度", "<model>"),
            b("diff", "查看工作区 Git diff"),
            b("approvals", "调整审批与权限策略"),
            b("status", "显示当前会话状态"),
            b("review", "发起代码审查", "[target]"),
        )
        "pi" -> listOf(
            b("compact", "压缩上下文并保留关键状态", "[focus]"),
            b("clear", "开启新会话 / 清空上下文"),
            b("model", "切换可用模型", "<model>"),
            b("tree", "浏览对话分支树"),
            b("reload", "重载扩展与技能配置"),
        )
        else -> listOf(
            b("compact", "压缩上下文摘要", "[focus]"),
            b("clear", "清空当前会话上下文"),
            b("status", "查看会话状态"),
            b("help", "显示内置命令帮助"),
        )
    }
}

sealed interface AuthNavEvent {
    data object Idle : AuthNavEvent
    data object LoggedIn : AuthNavEvent
    data class RequireTotp(val mfaTicket: String, val totpEnrolled: Boolean) : AuthNavEvent
}

enum class MediaKind(val label: String) {
    IMAGE("图片"),
    VIDEO("视频"),
    AUDIO("音频"),
}

data class PendingAttachment(
    val id: String,
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val kind: MediaKind,
    val durationMs: Long = 0L,
) {
    val usesBlobTier: Boolean get() = sizeBytes > 2L * 1024 * 1024
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: RelayApi,
    private val secretStore: SecretStore,
    private val tokenManager: TokenManager,
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean?> = secretStore.prefs
        .map { it.refreshToken.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode: StateFlow<String> = secretStore.prefs
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    private val _relayUrl = MutableStateFlow("https://relay.example.com")
    val relayUrl: StateFlow<String> = _relayUrl.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _navEvent = MutableStateFlow<AuthNavEvent>(AuthNavEvent.Idle)
    val navEvent: StateFlow<AuthNavEvent> = _navEvent.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = secretStore.current().relayUrl
            if (saved.isNotBlank()) _relayUrl.value = saved
            tokenManager.logoutEvents.collect {
                _navEvent.value = AuthNavEvent.Idle
            }
        }
    }

    fun setRelayUrl(url: String) {
        _relayUrl.value = url
    }

    fun buildGitHubStartUrl(): String {
        val base = RelayApi.normalizeHttp(_relayUrl.value)
        viewModelScope.launch { secretStore.updateRelayUrl(base) }
        return "$base/auth/github/start?redirect_uri=sidekick://auth/callback"
    }

    fun handleOAuthCallback(code: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                val base = secretStore.current().relayUrl.ifBlank { RelayApi.normalizeHttp(_relayUrl.value) }
                secretStore.updateRelayUrl(base)
                when (val res = api.exchangeAuthCode(base, code)) {
                    is AuthResult.Authenticated -> {
                        secretStore.updateTokens(
                            res.tokens.access_token,
                            res.tokens.expires_at_ms,
                            res.tokens.refresh_token,
                            res.tokens.user_id,
                            res.tokens.device_id,
                        )
                        ensurePhoneDeviceRegistered(base, res.tokens.access_token)
                        _navEvent.value = AuthNavEvent.LoggedIn
                    }
                    is AuthResult.MfaRequired -> {
                        _navEvent.value = AuthNavEvent.RequireTotp(res.mfaTicket, res.totpEnrolled)
                    }
                }
            }.onFailure { e ->
                _error.value = e.message ?: "登录失败"
            }
            _loading.value = false
        }
    }

    fun verifyTotp(mfaTicket: String, code: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                val base = secretStore.current().relayUrl
                val tok = api.verifyTotp(base, mfaTicket, code.trim())
                secretStore.updateTokens(tok.access_token, tok.expires_at_ms, tok.refresh_token, tok.user_id, tok.device_id)
                ensurePhoneDeviceRegistered(base, tok.access_token)
                _navEvent.value = AuthNavEvent.LoggedIn
            }.onFailure { e ->
                _error.value = e.message ?: "验证码错误"
            }
            _loading.value = false
        }
    }

    fun consumeNavEvent() {
        _navEvent.value = AuthNavEvent.Idle
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            secretStore.updateThemeMode(mode)
        }
    }

    private suspend fun ensurePhoneDeviceRegistered(base: String, accessToken: String) {
        val prefs = secretStore.current()
        if (prefs.deviceId.isNotBlank()) return
        val kp = secretStore.ensureNoiseKeyPair()
        val pubB64 = java.util.Base64.getEncoder().encodeToString(kp.publicKey)
        val model = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android" }
        val reg = api.registerDevice(base, accessToken, model, pubB64)
        secretStore.updateTokens(reg.access_token, reg.expires_at_ms, reg.refresh_token, deviceId = reg.device_id)
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: RelayApi,
    private val tokenManager: TokenManager,
    private val socketManager: RelaySocketManager,
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = socketManager.connectionState

    private val _bridges = MutableStateFlow<List<RelayDevice>>(emptyList())
    val bridges: StateFlow<List<RelayDevice>> = _bridges.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            socketManager.bridges.collect { list ->
                if (list.isNotEmpty()) _bridges.value = list
            }
        }
        viewModelScope.launch {
            socketManager.pinMismatches.collect { bid ->
                _error.value = "设备 $bid 密钥已变更，已拒绝连接（防 MITM）"
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            _error.value = null
            runCatching {
                val devs = tokenManager.withValidToken { url, tok -> api.listDevices(url, tok) }
                _bridges.value = devs.filter { it.kind == "bridge" }
            }.onFailure { e ->
                _error.value = e.message
            }
            _refreshing.value = false
        }
    }
}

@HiltViewModel
class BridgeViewModel @Inject constructor(
    private val repo: SessionRepository,
) : ViewModel() {
    private val _bridgeId = MutableStateFlow("")
    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions: StateFlow<List<SessionEntity>> = _sessions.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntity>>(emptyList())
    val history: StateFlow<List<HistoryEntity>> = _history.asStateFlow()

    private val _tools = MutableStateFlow<List<ToolInfo>>(emptyList())
    val tools: StateFlow<List<ToolInfo>> = _tools.asStateFlow()

    private val _workspaces = MutableStateFlow<List<Workspace>>(emptyList())
    val workspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var observeJob: Job? = null

    fun load(bridgeId: String) {
        if (_bridgeId.value == bridgeId) return
        _bridgeId.value = bridgeId
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            launch { repo.observeSessions(bridgeId).collect { _sessions.value = it } }
            launch { repo.observeHistory(bridgeId).collect { _history.value = it } }
            launch {
                repo.events.collect { ev ->
                    if (ev.bridgeId != bridgeId) return@collect
                    val se = ev.frame.event ?: return@collect
                    when {
                        se.updated != null -> repo.updateSession(bridgeId, se.updated!!)
                        se.closed != null -> repo.markSessionClosed(bridgeId, se.session_id)
                    }
                }
            }
        }
        refresh()
    }

    fun refresh() {
        val bid = _bridgeId.value
        if (bid.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                val (toolsList, wsList) = repo.hello(bid)
                _tools.value = toolsList
                _workspaces.value = wsList
                repo.refreshSessions(bid)
                repo.refreshHistory(bid)
            }.onFailure { e ->
                _error.value = e.message ?: "加载失败"
            }
            _loading.value = false
        }
    }

    fun isStructured(tool: String): Boolean {
        val info = _tools.value.firstOrNull { it.name == tool }
        return info?.structured_history ?: (tool != "antigravity" && tool != "agy")
    }

    fun startOrResume(
        tool: String,
        cwd: String,
        resumeConvId: String = "",
        onStarted: (sid: String, convId: String, structured: Boolean) -> Unit,
    ) {
        val bid = _bridgeId.value
        if (bid.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                val sess = repo.startSession(bid, tool, cwd, resumeConvId)
                if (sess != null) {
                    onStarted(sess.id, sess.conversation?.id ?: resumeConvId, isStructured(tool))
                }
            }.onFailure { e ->
                _error.value = e.message ?: "启动会话失败"
            }
            _loading.value = false
        }
    }
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repo: SessionRepository,
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = repo.connectionState

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _screenLines = MutableStateFlow<List<AnsiLine>>(emptyList())
    val screenLines: StateFlow<List<AnsiLine>> = _screenLines.asStateFlow()

    private val _screenRevision = MutableStateFlow(0L)
    val screenRevision: StateFlow<Long> = _screenRevision.asStateFlow()

    private val _localTyping = MutableStateFlow(false)
    val localTyping: StateFlow<Boolean> = _localTyping.asStateFlow()

    private val _pendingAttachments = MutableStateFlow<List<PendingAttachment>>(emptyList())
    val pendingAttachments: StateFlow<List<PendingAttachment>> = _pendingAttachments.asStateFlow()

    private val _fetchedMediaPaths = MutableStateFlow<Map<String, String>>(emptyMap())
    val fetchedMediaPaths: StateFlow<Map<String, String>> = _fetchedMediaPaths.asStateFlow()

    private val _loadingMediaIds = MutableStateFlow<Set<String>>(emptySet())
    val loadingMediaIds: StateFlow<Set<String>> = _loadingMediaIds.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _fileSuggestions = MutableStateFlow<List<WorkspaceFileSuggestion>>(emptyList())
    val fileSuggestions: StateFlow<List<WorkspaceFileSuggestion>> = _fileSuggestions.asStateFlow()

    private val _slashCommands = MutableStateFlow<List<SlashCommandSuggestion>>(emptyList())
    val slashCommands: StateFlow<List<SlashCommandSuggestion>> = _slashCommands.asStateFlow()

    private var bridgeId: String = ""
    private var sid: String = ""
    private var tool: String = ""
    private var convId: String = ""
    private var structured: Boolean = true
    private var activeJob: Job? = null
    private var fileSearchJob: Job? = null
    private var lastPresenceSentMs = 0L

    fun attach(bridgeId: String, sid: String, tool: String, convId: String, structuredHistory: Boolean) {
        if (this.bridgeId == bridgeId && this.sid == sid) return
        this.bridgeId = bridgeId
        this.sid = sid
        this.tool = tool
        this.convId = convId
        this.structured = structuredHistory
        _slashCommands.value = coreBuiltinSlashCommands(tool)

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            launch { refreshCommands() }
            if (structuredHistory && convId.isNotBlank()) {
                launch { repo.observeMessages(bridgeId, convId).collect { _messages.value = it } }
                runCatching {
                    if (repo.fetchMessages(bridgeId, tool, convId)) {
                        _toast.value = "历史已刷新"
                    }
                }
            }
            launch {
                repo.events.collect { ev ->
                    if (ev.bridgeId != bridgeId) return@collect
                    ev.frame.presence?.let { p ->
                        if (p.session_id == sid && p.source == PresenceSource.PRESENCE_SOURCE_LOCAL) {
                            _localTyping.value = p.typing
                        }
                    }
                    val se = ev.frame.event ?: return@collect
                    if (se.session_id != sid) return@collect
                    when {
                        se.screen != null -> {
                            val scr = se.screen!!
                            _screenRevision.value = scr.revision
                            _screenLines.value = AnsiParser.parse(scr.ansi.utf8())
                        }
                        se.message != null && structuredHistory -> {
                            repo.saveIncomingMessage(bridgeId, convId, se.message!!)
                        }
                        se.reset == true && structuredHistory && convId.isNotBlank() -> {
                            repo.resetMessages(bridgeId, convId)
                            _toast.value = "历史已刷新"
                            repo.fetchMessages(bridgeId, tool, convId)
                        }
                        se.closed != null -> {
                            _toast.value = "会话已关闭"
                        }
                    }
                }
            }
            val streams = if (structuredHistory) {
                listOf(Stream.STREAM_SCREEN, Stream.STREAM_MESSAGES)
            } else {
                listOf(Stream.STREAM_SCREEN)
            }
            val afterSeq = if (structuredHistory && convId.isNotBlank()) {
                _messages.value.lastOrNull()?.seq ?: 0L
            } else {
                0L
            }
            runCatching { repo.subscribe(bridgeId, sid, streams, afterSeq) }
        }
    }

    fun decodeParts(partsJson: String): List<UiPart> = repo.decodeParts(partsJson)

    fun sendInput(text: String, submit: Boolean) {
        if (bridgeId.isBlank() || sid.isBlank()) return
        viewModelScope.launch {
            runCatching { repo.sendInput(bridgeId, sid, text, submit) }
                .onFailure { _toast.value = it.message ?: "发送失败" }
        }
    }

    fun addAttachment(att: PendingAttachment) {
        _pendingAttachments.value = _pendingAttachments.value + att
    }

    fun removeAttachment(id: String) {
        _pendingAttachments.value = _pendingAttachments.value.filterNot { it.id == id }
    }

    fun sendWithAttachments(text: String, submit: Boolean) {
        val atts = _pendingAttachments.value
        if (atts.isNotEmpty()) {
            val tierLabel = if (atts.any { it.usesBlobTier }) "加密 Blob (>2MiB)" else "Noise 直传 (≤2MiB)"
            _toast.value = "已暂存 ${atts.size} 个媒体附件（$tierLabel），待 P6 媒体传输通道启用后自动加密上传"
            _pendingAttachments.value = emptyList()
            if (text.isNotBlank()) {
                sendInput(text, submit)
            }
            return
        }
        sendInput(text, submit)
    }

    fun refreshMessages() {
        if (!structured || bridgeId.isBlank() || convId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val reset = repo.fetchMessages(bridgeId, tool, convId)
                _toast.value = if (reset) "历史已刷新" else "历史已同步"
            }.onFailure { _toast.value = it.message ?: "刷新失败" }
        }
    }

    fun stopSession(onStopped: () -> Unit) {
        if (bridgeId.isBlank() || sid.isBlank()) return
        viewModelScope.launch {
            runCatching { repo.stopSession(bridgeId, sid) }
                .onSuccess { onStopped() }
                .onFailure { _toast.value = it.message ?: "停止会话失败" }
        }
    }

    fun fetchMediaIfNeeded(media: UiPart.Media) {
        if (bridgeId.isBlank() || media.mediaId.isBlank()) return
        if (_fetchedMediaPaths.value.containsKey(media.mediaId)) return
        if (_loadingMediaIds.value.contains(media.mediaId)) return
        _loadingMediaIds.value = _loadingMediaIds.value + media.mediaId
        viewModelScope.launch {
            runCatching {
                repo.fetchMedia(bridgeId, media.mediaId, media.mimeType, media.name)
            }.onSuccess { file ->
                _fetchedMediaPaths.value = _fetchedMediaPaths.value + (media.mediaId to file.absolutePath)
            }.onFailure { e ->
                _toast.value = e.message ?: "加载原图失败"
            }
            _loadingMediaIds.value = _loadingMediaIds.value - media.mediaId
        }
    }

    fun sendKey(key: Key) {
        if (bridgeId.isBlank() || sid.isBlank()) return
        viewModelScope.launch {
            runCatching { repo.sendKeys(bridgeId, sid, listOf(key)) }
                .onFailure { _toast.value = it.message ?: "按键发送失败" }
        }
    }

    fun onFocusChanged(focused: Boolean) {
        if (bridgeId.isBlank() || sid.isBlank()) return
        val now = System.currentTimeMillis()
        if (focused && now - lastPresenceSentMs < 1_000L) return
        lastPresenceSentMs = now
        viewModelScope.launch {
            repo.sendPresence(bridgeId, sid, focused)
        }
    }

    fun clearToast() {
        _toast.value = null
    }

    fun searchWorkspaceFiles(query: String) {
        fileSearchJob?.cancel()
        if (bridgeId.isBlank() || sid.isBlank()) return
        fileSearchJob = viewModelScope.launch {
            delay(160)
            runCatching { repo.listFiles(bridgeId, sid, query.trim(), limit = 40) }
                .onSuccess { list ->
                    _fileSuggestions.value = list.map {
                        WorkspaceFileSuggestion(
                            relPath = it.rel_path,
                            isDir = it.is_dir,
                            sizeBytes = it.size_bytes,
                        )
                    }
                }
        }
    }

    private suspend fun refreshCommands() {
        if (bridgeId.isBlank() || sid.isBlank()) return
        val remote = runCatching { repo.listCommands(bridgeId, sid, tool) }.getOrNull().orEmpty()
        if (remote.isEmpty()) return
        val mapped = remote.map { cmd ->
            val src = when (cmd.source) {
                SlashCommandSource.SLASH_COMMAND_SOURCE_SKILL -> SlashCommandSourceUi.SKILL
                SlashCommandSource.SLASH_COMMAND_SOURCE_PROJECT -> SlashCommandSourceUi.PROJECT
                SlashCommandSource.SLASH_COMMAND_SOURCE_USER -> SlashCommandSourceUi.USER
                SlashCommandSource.SLASH_COMMAND_SOURCE_PLUGIN -> SlashCommandSourceUi.PLUGIN
                else -> SlashCommandSourceUi.BUILTIN
            }
            SlashCommandSuggestion(
                name = cmd.name,
                description = cmd.description,
                argsHint = cmd.args_hint,
                source = src,
            )
        }
        val merged = LinkedHashMap<String, SlashCommandSuggestion>()
        for (c in coreBuiltinSlashCommands(tool)) merged[c.name.lowercase()] = c
        for (c in mapped) merged.putIfAbsent(c.name.lowercase(), c)
        _slashCommands.value = merged.values.toList()
    }

    override fun onCleared() {
        val bid = bridgeId
        val s = sid
        if (bid.isNotBlank() && s.isNotBlank()) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                repo.unsubscribe(bid, s)
            }
        }
        super.onCleared()
    }
}

@HiltViewModel
class PairViewModel @Inject constructor(
    private val api: RelayApi,
    private val tokenManager: TokenManager,
    private val secretStore: SecretStore,
    private val socketManager: RelaySocketManager,
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    private val _pairedSuccess = MutableStateFlow(false)
    val pairedSuccess: StateFlow<Boolean> = _pairedSuccess.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun pairWithQrJson(rawJson: String) {
        val qr = runCatching { json.decodeFromString<QrPairingPayload>(rawJson) }.getOrNull()
        if (qr == null || qr.v != 1 || qr.eid.isBlank()) {
            _isError.value = true
            _status.value = "无效的配对二维码"
            return
        }
        if (qr.psk.isBlank()) {
            _isError.value = true
            _status.value = "二维码缺少 PSK，禁止降级为无 PSK 握手，请重新生成"
            return
        }
        approveAndHandshake(enrollId = qr.eid, userCode = null, expectedPk = qr.pk, psk = qr.psk)
    }

    fun pairWithManualCode(userCode: String, psk: String) {
        if (psk.isBlank()) {
            _isError.value = true
            _status.value = "缺少一次性 PSK，禁止降级为无 PSK 握手，请重新扫码或输入完整 PSK"
            return
        }
        approveAndHandshake(enrollId = null, userCode = userCode.trim(), expectedPk = null, psk = psk.trim())
    }

    private fun approveAndHandshake(enrollId: String?, userCode: String?, expectedPk: String?, psk: String) {
        viewModelScope.launch {
            _busy.value = true
            _isError.value = false
            _status.value = "正在配对…"
            runCatching {
                var deviceId = secretStore.current().deviceId
                if (deviceId.isBlank()) {
                    val kp = secretStore.ensureNoiseKeyPair()
                    val pubB64 = java.util.Base64.getEncoder().encodeToString(kp.publicKey)
                    val reg = tokenManager.withValidToken { url, tok ->
                        api.registerDevice(url, tok, "${Build.MANUFACTURER} ${Build.MODEL}".trim(), pubB64)
                    }
                    secretStore.updateTokens(reg.access_token, reg.expires_at_ms, reg.refresh_token, deviceId = reg.device_id)
                    deviceId = reg.device_id
                }
                val approved = tokenManager.withValidToken { url, tok ->
                    api.approveEnroll(url, tok, enrollId, userCode, deviceId)
                }
                socketManager.pairWithBridge(
                    bridgeId = approved.bridge_device_id,
                    pskB64 = psk,
                    expectedBridgePubB64 = expectedPk ?: approved.bridge_pubkey,
                )
                _isError.value = false
                _status.value = "已成功配对 ${approved.bridge_name}"
                _pairedSuccess.value = true
            }.onFailure { e ->
                _isError.value = true
                _status.value = e.message ?: "配对失败"
            }
            _busy.value = false
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: RelayApi,
    private val tokenManager: TokenManager,
    private val secretStore: SecretStore,
    private val socketManager: RelaySocketManager,
) : ViewModel() {
    val prefs = secretStore.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, dev.ddgrcf.sidekick.core.store.SecretPrefs())

    private val _devices = MutableStateFlow<List<RelayDevice>>(emptyList())
    val devices: StateFlow<List<RelayDevice>> = _devices.asStateFlow()

    private val _otpauthUri = MutableStateFlow<String?>(null)
    val otpauthUri: StateFlow<String?> = _otpauthUri.asStateFlow()

    private val _recoveryCodes = MutableStateFlow<List<String>>(emptyList())
    val recoveryCodes: StateFlow<List<String>> = _recoveryCodes.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshDevices()
    }

    fun refreshDevices() {
        viewModelScope.launch {
            runCatching {
                _devices.value = tokenManager.withValidToken { url, tok -> api.listDevices(url, tok) }
            }
        }
    }

    fun enrollTotp() {
        viewModelScope.launch {
            runCatching {
                val res = tokenManager.withValidToken { url, tok -> api.enrollTotp(url, tok) }
                _otpauthUri.value = res.otpauth_uri
            }.onFailure { _message.value = it.message }
        }
    }

    fun confirmTotp(code: String) {
        viewModelScope.launch {
            runCatching {
                val res = tokenManager.withValidToken { url, tok -> api.confirmTotp(url, tok, code.trim()) }
                _otpauthUri.value = null
                _recoveryCodes.value = res.recovery_codes
            }.onFailure { _message.value = it.message ?: "TOTP 验证失败" }
        }
    }

    fun dismissRecoveryCodes() {
        _recoveryCodes.value = emptyList()
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            secretStore.updateThemeMode(mode)
        }
    }

    fun revokeDevice(deviceId: String) {
        viewModelScope.launch {
            runCatching {
                tokenManager.withValidToken { url, tok -> api.revokeDevice(url, tok, deviceId) }
                refreshDevices()
            }.onFailure { _message.value = it.message }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            val cur = secretStore.current()
            api.logout(cur.relayUrl, cur.refreshToken)
            socketManager.stop()
            secretStore.clearAuth()
            onDone()
        }
    }
}
