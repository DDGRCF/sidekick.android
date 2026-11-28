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
import dev.ddgrcf.sidekick.ui.hasNonQuestionToolCall
import dev.ddgrcf.sidekick.proto.v1.Key
import dev.ddgrcf.sidekick.proto.v1.PresenceSource
import dev.ddgrcf.sidekick.proto.v1.RuntimeAnswer
import dev.ddgrcf.sidekick.proto.v1.RuntimeMode
import dev.ddgrcf.sidekick.proto.v1.RuntimeRequest
import dev.ddgrcf.sidekick.proto.v1.RuntimeRequestKind
import dev.ddgrcf.sidekick.proto.v1.SessionConfig
import dev.ddgrcf.sidekick.proto.v1.SlashCommandSource
import dev.ddgrcf.sidekick.proto.v1.Stream
import dev.ddgrcf.sidekick.proto.v1.ToolInfo
import dev.ddgrcf.sidekick.proto.v1.Workspace
import dev.ddgrcf.sidekick.proto.v1.WorkspaceContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@Serializable
data class QuotedMessage(
    val author: String,
    val excerpt: String,
)

data class StreamingMessageState(
    val active: Boolean = false,
    val text: String = "",
    val thinking: String = "",
    val status: String = "",
    val model: String? = null,
)

data class InteractiveDialogOptionUi(
    val label: String,
    val description: String = "",
    val selected: Boolean = false,
    val current: Boolean = false,
)

data class InteractiveDialogState(
    val open: Boolean = false,
    val kind: String = "select",
    val title: String = "",
    val subtitle: String = "",
    val query: String = "",
    val options: List<InteractiveDialogOptionUi> = emptyList(),
    val selectedIndex: Int = 0,
    val hint: String = "",
    val searchable: Boolean = false,
)

data class ModelOptionUi(
    val id: String,
    val provider: String = "",
    val name: String = "",
    val reasoning: Boolean = true,
    val thinkingLevels: List<String> = emptyList(),
    val current: Boolean = false,
)

enum class RuntimeModeUi(val label: String, val description: String) {
    UNSPECIFIED("默认", ""),
    APPROVAL_REQUIRED("受控审批", "修改与命令执行前弹窗确认"),
    AUTO_ACCEPT_EDITS("自动接受编辑", "自动允许读写编辑，高危命令仍需确认"),
    AUTO("智能自动", "常规操作自动放行，敏感动作确认"),
    FULL_ACCESS("完全访问", "全部工具与命令免确认执行"),
}

data class SessionConfigUi(
    val sessionId: String = "",
    val model: String = "",
    val thinkingLevel: String = "",
    val runtimeMode: RuntimeModeUi = RuntimeModeUi.APPROVAL_REQUIRED,
    val availableModels: List<ModelOptionUi> = emptyList(),
    val availableThinkingLevels: List<String> = emptyList(),
    val supportedRuntimeModes: List<RuntimeModeUi> = emptyList(),
    val supportsMidTurnConfig: Boolean = false,
    val supportsSteer: Boolean = false,
    val supportsInterrupt: Boolean = true,
    val isRunning: Boolean = false,
)

enum class RuntimeRequestKindUi {
    APPROVAL,
    USER_INPUT,
}

data class RuntimeRequestOptionUi(
    val id: String,
    val label: String,
    val description: String = "",
    val preview: String = "",
)

data class RuntimeQuestionUi(
    val id: String,
    val header: String = "",
    val question: String = "",
    val options: List<RuntimeRequestOptionUi> = emptyList(),
    val multiSelect: Boolean = false,
    val allowCustomAnswer: Boolean = true,
)

data class ChangedFileUi(
    val path: String,
    val status: String,
    val added: Int = 0,
    val removed: Int = 0,
)

data class DiagnosticItemUi(
    val file: String,
    val line: Int,
    val col: Int,
    val severity: String,
    val message: String,
    val source: String = "",
)

data class WorkspaceContextUi(
    val cwd: String = "",
    val gitBranch: String = "",
    val changedFiles: List<ChangedFileUi> = emptyList(),
    val gitDiff: String = "",
    val nvimConnected: Boolean = false,
    val nvimServer: String = "",
    val nvimFile: String = "",
    val nvimLine: Int = 0,
    val diagnostics: List<DiagnosticItemUi> = emptyList(),
    val openBuffers: List<String> = emptyList(),
)

data class RuntimeRequestUi(
    val requestId: String,
    val kind: RuntimeRequestKindUi,
    val title: String = "",
    val detail: String = "",
    val toolName: String = "",
    val approvalOptions: List<RuntimeRequestOptionUi> = emptyList(),
    val questions: List<RuntimeQuestionUi> = emptyList(),
    val dismissible: Boolean = true,
)

data class RuntimeAnswerInput(
    val questionId: String,
    val selectedValues: List<String> = emptyList(),
    val customText: String = "",
)

private fun RuntimeMode.toUi(): RuntimeModeUi = when (this) {
    RuntimeMode.RUNTIME_MODE_APPROVAL_REQUIRED -> RuntimeModeUi.APPROVAL_REQUIRED
    RuntimeMode.RUNTIME_MODE_AUTO_ACCEPT_EDITS -> RuntimeModeUi.AUTO_ACCEPT_EDITS
    RuntimeMode.RUNTIME_MODE_AUTO -> RuntimeModeUi.AUTO
    RuntimeMode.RUNTIME_MODE_FULL_ACCESS -> RuntimeModeUi.FULL_ACCESS
    else -> RuntimeModeUi.UNSPECIFIED
}

private fun RuntimeModeUi.toProto(): RuntimeMode = when (this) {
    RuntimeModeUi.APPROVAL_REQUIRED -> RuntimeMode.RUNTIME_MODE_APPROVAL_REQUIRED
    RuntimeModeUi.AUTO_ACCEPT_EDITS -> RuntimeMode.RUNTIME_MODE_AUTO_ACCEPT_EDITS
    RuntimeModeUi.AUTO -> RuntimeMode.RUNTIME_MODE_AUTO
    RuntimeModeUi.FULL_ACCESS -> RuntimeMode.RUNTIME_MODE_FULL_ACCESS
    RuntimeModeUi.UNSPECIFIED -> RuntimeMode.RUNTIME_MODE_UNSPECIFIED
}

private fun SessionConfig.toUi(): SessionConfigUi = SessionConfigUi(
    sessionId = session_id,
    model = model,
    thinkingLevel = thinking_level,
    runtimeMode = runtime_mode.toUi(),
    availableModels = available_models.map {
        ModelOptionUi(
            id = it.id,
            provider = it.provider,
            name = it.name,
            reasoning = it.reasoning,
            thinkingLevels = it.thinking_levels,
            current = it.current,
        )
    },
    availableThinkingLevels = available_thinking_levels,
    supportedRuntimeModes = supported_runtime_modes.map { it.toUi() }.filter { it != RuntimeModeUi.UNSPECIFIED },
    supportsMidTurnConfig = supports_mid_turn_config,
    supportsSteer = supports_steer,
    supportsInterrupt = supports_interrupt,
    isRunning = is_running,
)

private fun RuntimeRequest.toUi(): RuntimeRequestUi = RuntimeRequestUi(
    requestId = request_id,
    kind = if (kind == RuntimeRequestKind.RUNTIME_REQUEST_KIND_USER_INPUT) {
        RuntimeRequestKindUi.USER_INPUT
    } else {
        RuntimeRequestKindUi.APPROVAL
    },
    title = title,
    detail = detail,
    toolName = tool_name,
    approvalOptions = approval_options.map {
        RuntimeRequestOptionUi(id = it.id, label = it.label, description = it.description, preview = it.preview)
    },
    questions = questions.map { q ->
        RuntimeQuestionUi(
            id = q.id,
            header = q.header_,
            question = q.question,
            options = q.options.map {
                RuntimeRequestOptionUi(id = it.id, label = it.label, description = it.description, preview = it.preview)
            },
            multiSelect = q.multi_select,
            allowCustomAnswer = q.allow_custom_answer,
        )
    },
    dismissible = dismissible,
)

data class OptimisticUserMessage(
    val id: String,
    val text: String,
    val afterSeq: Long,
    val createdMs: Long = System.currentTimeMillis(),
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
            b("model", "切换当前对话模型"),
            b("context", "查看上下文窗口占用情况"),
            b("diff", "查看当前工作区变更"),
            b("review", "代码变更审查", "[target]"),
        )
        "codex" -> listOf(
            b("compact", "压缩会话上下文摘要", "[focus]"),
            b("model", "切换模型或推理强度"),
            b("diff", "查看工作区 Git diff"),
            b("approvals", "调整审批与权限策略"),
            b("status", "显示当前会话状态"),
            b("review", "发起代码审查", "[target]"),
        )
        "pi" -> listOf(
            b("model", "打开模型选择弹窗"),
            b("thinking", "调整思考强度"),
            b("settings", "打开 Pi 会话设置"),
            b("theme", "切换界面主题"),
            b("compact", "压缩上下文并保留关键状态", "[focus]"),
            b("clear", "开启新会话 / 清空上下文"),
            b("tree", "浏览对话分支树"),
            b("reload", "重载扩展与技能配置"),
        )
        else -> listOf(
            b("model", "切换当前对话模型"),
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
                switchRelayIfNeeded(base)
                secretStore.updateRelayUrl(base)
                when (val res = api.exchangeAuthCode(base, code)) {
                    is AuthResult.Authenticated -> {
                        completeAuthenticatedSession(base, res.tokens)
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

    fun loginDev() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                val base = RelayApi.normalizeHttp(_relayUrl.value)
                switchRelayIfNeeded(base)
                secretStore.updateRelayUrl(base)
                val tok = api.devLogin(base)
                completeAuthenticatedSession(base, tok)
            }.onFailure { e ->
                _error.value = e.message ?: "本地开发登录失败"
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
                completeAuthenticatedSession(base, tok)
            }.onFailure { e ->
                _error.value = e.message ?: "验证码错误"
            }
            _loading.value = false
        }
    }

    private suspend fun switchRelayIfNeeded(newBase: String) {
        val prev = secretStore.current().relayUrl
        if (prev.isNotBlank() && prev != newBase) {
            secretStore.clearAuth()
        }
    }

    private suspend fun completeAuthenticatedSession(base: String, tok: dev.ddgrcf.sidekick.core.relay.TokenPair) {
        secretStore.updateTokens(
            tok.access_token,
            tok.expires_at_ms,
            tok.refresh_token,
            tok.user_id,
            tok.device_id,
        )
        ensurePhoneDeviceRegistered(base, tok.access_token)
        _navEvent.value = AuthNavEvent.LoggedIn
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
    private val secretStore: SecretStore,
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = socketManager.connectionState

    val pairedBridgeIds: StateFlow<Set<String>> = secretStore.prefs
        .map { it.peerPins.keys }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _bridges = MutableStateFlow<List<RelayDevice>>(emptyList())
    val bridges: StateFlow<List<RelayDevice>> = _bridges.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _pullRefreshing = MutableStateFlow(false)
    val pullRefreshing: StateFlow<Boolean> = _pullRefreshing.asStateFlow()

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
        refresh(userInitiated = false)
    }

    fun refresh(userInitiated: Boolean = false) {
        viewModelScope.launch {
            _refreshing.value = true
            if (userInitiated) _pullRefreshing.value = true
            _error.value = null
            runCatching {
                val devs = tokenManager.withValidToken { url, tok -> api.listDevices(url, tok) }
                _bridges.value = devs.filter { it.kind == "bridge" }
            }.onFailure { e ->
                _error.value = e.message
            }
            _refreshing.value = false
            _pullRefreshing.value = false
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

    private val _syncingHistory = MutableStateFlow(false)
    val syncingHistory: StateFlow<Boolean> = _syncingHistory.asStateFlow()

    private val _pullRefreshing = MutableStateFlow(false)
    val pullRefreshing: StateFlow<Boolean> = _pullRefreshing.asStateFlow()

    private val _startingSession = MutableStateFlow(false)
    val startingSession: StateFlow<Boolean> = _startingSession.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var observeJob: Job? = null
    private var refreshJob: Job? = null

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
        refresh(userInitiated = false)
    }

    fun refresh(userInitiated: Boolean = false) {
        val bid = _bridgeId.value
        if (bid.isBlank()) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _loading.value = true
            _syncingHistory.value = true
            if (userInitiated) _pullRefreshing.value = true
            _error.value = null

            val fastJob = launch {
                runCatching {
                    val (toolsList, wsList) = repo.hello(bid)
                    _tools.value = toolsList
                    _workspaces.value = wsList
                    repo.refreshSessions(bid)
                }.onFailure { e ->
                    _error.value = e.message ?: "连接工作站失败"
                }
                _loading.value = false
                _pullRefreshing.value = false
            }

            val historyJob = launch {
                runCatching {
                    repo.refreshHistory(bid)
                }.onFailure { e ->
                    if (_error.value == null) {
                        _error.value = e.message ?: "历史同步失败"
                    }
                }
                _syncingHistory.value = false
            }

            fastJob.join()
            historyJob.join()
        }
    }

    fun isStructured(tool: String): Boolean {
        val info = _tools.value.firstOrNull { it.name == tool }
        return info?.structured_history ?: true
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
            _startingSession.value = true
            _error.value = null
            runCatching {
                val sess = repo.startSession(bid, tool, cwd, resumeConvId)
                if (sess != null) {
                    onStarted(sess.id, sess.conversation?.id ?: resumeConvId, isStructured(tool))
                }
            }.onFailure { e ->
                _error.value = e.message ?: "启动会话失败"
            }
            _startingSession.value = false
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repo: SessionRepository,
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = repo.connectionState

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _streamingDraft = MutableStateFlow(StreamingMessageState())
    val streamingDraft: StateFlow<StreamingMessageState> = _streamingDraft.asStateFlow()

    private val _interactiveDialog = MutableStateFlow<InteractiveDialogState?>(null)
    val interactiveDialog: StateFlow<InteractiveDialogState?> = _interactiveDialog.asStateFlow()

    private val _sessionConfig = MutableStateFlow<SessionConfigUi?>(null)
    val sessionConfig: StateFlow<SessionConfigUi?> = _sessionConfig.asStateFlow()

    private val _pendingRuntimeRequests = MutableStateFlow<List<RuntimeRequestUi>>(emptyList())
    val pendingRuntimeRequests: StateFlow<List<RuntimeRequestUi>> = _pendingRuntimeRequests.asStateFlow()

    private val _isRpcSession = MutableStateFlow(false)
    val isRpcSession: StateFlow<Boolean> = _isRpcSession.asStateFlow()

    private val _optimisticUserMessages = MutableStateFlow<List<OptimisticUserMessage>>(emptyList())
    val optimisticUserMessages: StateFlow<List<OptimisticUserMessage>> = _optimisticUserMessages.asStateFlow()

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

    private val _availableTools = MutableStateFlow<List<ToolInfo>>(emptyList())
    val availableTools: StateFlow<List<ToolInfo>> = _availableTools.asStateFlow()

    private val _otherSessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val otherSessions: StateFlow<List<SessionEntity>> = _otherSessions.asStateFlow()

    private val _handoverBusy = MutableStateFlow(false)
    val handoverBusy: StateFlow<Boolean> = _handoverBusy.asStateFlow()

    private val _syncingHistory = MutableStateFlow(false)
    val syncingHistory: StateFlow<Boolean> = _syncingHistory.asStateFlow()

    private val _workspaceContext = MutableStateFlow<WorkspaceContextUi?>(null)
    val workspaceContext: StateFlow<WorkspaceContextUi?> = _workspaceContext.asStateFlow()

    private val _workspaceContextLoading = MutableStateFlow(false)
    val workspaceContextLoading: StateFlow<Boolean> = _workspaceContextLoading.asStateFlow()

    private val _turnStartedMs = MutableStateFlow<Long?>(null)
    val turnStartedMs: StateFlow<Long?> = _turnStartedMs.asStateFlow()

    private val _lastResumeCommand = MutableStateFlow<String?>(null)
    val lastResumeCommand: StateFlow<String?> = _lastResumeCommand.asStateFlow()

    private var bridgeId: String = ""
    private var sid: String = ""
    private var tool: String = ""
    private var convId: String = ""
    private var sessionCwd: String = ""
    private var structured: Boolean = true
    private val _activeConvId = MutableStateFlow("")
    private var lastPromptMaxSeq: Long = 0L
    private var lastCommittedTurnAssistantText: String? = null
    private var activeJob: Job? = null
    private var fileSearchJob: Job? = null
    private var lastPresenceSentMs = 0L

    fun attach(bridgeId: String, sid: String, tool: String, convId: String, structuredHistory: Boolean) {
        if (this.bridgeId == bridgeId && this.sid == sid) return
        this.bridgeId = bridgeId
        this.sid = sid
        this.tool = tool
        this.convId = convId
        this.sessionCwd = ""
        this.structured = structuredHistory
        this.lastPromptMaxSeq = 0L
        this.lastCommittedTurnAssistantText = null
        _messages.value = emptyList()
        _optimisticUserMessages.value = emptyList()
        _streamingDraft.value = StreamingMessageState()
        _interactiveDialog.value = null
        _sessionConfig.value = null
        _pendingRuntimeRequests.value = emptyList()
        _workspaceContext.value = null
        _workspaceContextLoading.value = false
        _turnStartedMs.value = null
        _lastResumeCommand.value = null
        _isRpcSession.value = repo.isRpcSession(bridgeId, sid)
        _syncingHistory.value = structuredHistory && convId.isNotBlank()
        _activeConvId.value = convId.ifBlank { "_pending_$sid" }
        _slashCommands.value = coreBuiltinSlashCommands(tool)

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            launch { refreshCommands() }
            launch {
                runCatching { repo.hello(bridgeId) }
                    .onSuccess { (toolsList, _) -> _availableTools.value = toolsList }
            }
            launch {
                repo.observeSessions(bridgeId).collect { list ->
                    _otherSessions.value = list.filter { it.live && it.sid != sid }
                    list.firstOrNull { it.sid == sid }?.cwd?.takeIf { it.isNotBlank() }?.let { cwd ->
                        sessionCwd = cwd
                    }
                }
            }
            if (structuredHistory) {
                launch {
                    _activeConvId
                        .flatMapLatest { cid -> repo.observeMessages(bridgeId, cid) }
                        .collect { msgs ->
                            _messages.value = msgs
                            reconcileStreamingWithMessages(msgs)
                        }
                }
                if (convId.isNotBlank()) {
                    launch {
                        _syncingHistory.value = true
                        runCatching {
                            if (repo.fetchMessages(bridgeId, tool, convId)) {
                                _toast.value = "历史已刷新"
                            }
                        }
                        _syncingHistory.value = false
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
                        se.updated != null && structuredHistory -> {
                            val updatedCid = se.updated!!.conversation?.id.orEmpty()
                            if (updatedCid.isNotBlank() && this@SessionViewModel.convId != updatedCid) {
                                val prevCid = _activeConvId.value
                                this@SessionViewModel.convId = updatedCid
                                if (prevCid.startsWith("_pending_") && prevCid != updatedCid) {
                                    repo.migratePendingConvMessages(bridgeId, prevCid, updatedCid)
                                }
                                _activeConvId.value = updatedCid
                                runCatching { repo.fetchMessages(bridgeId, tool, updatedCid) }
                            }
                        }
                        se.draft != null && structuredHistory -> {
                            val d = se.draft!!
                            onStreamingDraftEvent(d.active, d.text, d.thinking, d.status, d.model)
                        }
                        se.config_updated != null -> {
                            val cfg = se.config_updated!!.toUi()
                            _sessionConfig.value = cfg
                            if (!cfg.isRunning && _streamingDraft.value.active) {
                                _streamingDraft.value = _streamingDraft.value.copy(active = false, text = "", thinking = "", status = "")
                            }
                            updateTurnClockAndContext()
                        }
                        se.runtime_request != null -> {
                            val req = se.runtime_request!!
                            if (req.resolved) {
                                _pendingRuntimeRequests.value = _pendingRuntimeRequests.value.filterNot { it.requestId == req.request_id }
                            } else {
                                val mapped = req.toUi()
                                _pendingRuntimeRequests.value = _pendingRuntimeRequests.value.filterNot { it.requestId == mapped.requestId } + mapped
                            }
                        }
                        se.dialog != null && structuredHistory -> {
                            val dlg = se.dialog!!
                            _interactiveDialog.value = if (dlg.open_) {
                                InteractiveDialogState(
                                    open = true,
                                    kind = dlg.kind.ifBlank { "select" },
                                    title = dlg.title,
                                    subtitle = dlg.subtitle,
                                    query = dlg.query,
                                    options = dlg.options.map { o ->
                                        InteractiveDialogOptionUi(
                                            label = o.label,
                                            description = o.description,
                                            selected = o.selected,
                                            current = o.current,
                                        )
                                    },
                                    selectedIndex = dlg.selected_index,
                                    hint = dlg.hint,
                                    searchable = dlg.searchable,
                                )
                            } else {
                                null
                            }
                        }
                        se.message != null && structuredHistory -> {
                            val targetCid = _activeConvId.value.ifBlank { "_pending_$sid" }
                            repo.saveIncomingMessage(bridgeId, targetCid, se.message!!)
                        }
                        se.reset == true && structuredHistory -> {
                            val realCid = this@SessionViewModel.convId
                            if (realCid.isNotBlank()) {
                                repo.resetMessages(bridgeId, realCid)
                                _toast.value = "历史已刷新"
                                repo.fetchMessages(bridgeId, tool, realCid)
                            }
                        }
                        se.closed != null -> {
                            _streamingDraft.value = StreamingMessageState()
                            _interactiveDialog.value = null
                            _pendingRuntimeRequests.value = emptyList()
                            updateTurnClockAndContext()
                            _toast.value = "会话已关闭"
                        }
                    }
                }
            }
            val streams = when {
                repo.isRpcSession(bridgeId, sid) -> listOf(Stream.STREAM_MESSAGES)
                structuredHistory -> listOf(Stream.STREAM_SCREEN, Stream.STREAM_MESSAGES)
                else -> listOf(Stream.STREAM_SCREEN)
            }
            // Subscribe immediately when connected AND automatically re-subscribe whenever
            // the Noise KK connection drops and reconnects (e.g. after bridge restart or network switch),
            // so Screen / InteractiveDialog / StreamingDraft / RuntimeRequest streams never stall.
            launch {
                var hadConnectedOnce = false
                repo.connectionState.collect { state ->
                    if (state == ConnectionState.CONNECTED) {
                        val currentCid = this@SessionViewModel.convId
                        val afterSeq = if (structuredHistory && currentCid.isNotBlank()) {
                            _messages.value.lastOrNull()?.seq ?: 0L
                        } else {
                            0L
                        }
                        runCatching { repo.subscribe(bridgeId, sid, streams, afterSeq) }
                        runCatching { repo.getSessionConfig(bridgeId, sid) }.getOrNull()?.let { cfg ->
                            val ui = cfg.toUi()
                            _sessionConfig.value = ui
                            if (ui.supportedRuntimeModes.isNotEmpty()) {
                                _isRpcSession.value = true
                            }
                        }
                        refreshWorkspaceContext(includeDiff = false)
                        if (hadConnectedOnce && structuredHistory && currentCid.isNotBlank()) {
                            runCatching { repo.fetchMessages(bridgeId, tool, currentCid) }
                        }
                        hadConnectedOnce = true
                    }
                }
            }
        }
    }

    fun decodeParts(partsJson: String): List<UiPart> = repo.decodeParts(partsJson)

    fun sendInput(text: String, submit: Boolean) {
        if (bridgeId.isBlank() || sid.isBlank()) return
        val normalized = if (submit) {
            val trimmed = text.trim()
            if (trimmed.startsWith("/") && !trimmed.any { it.isWhitespace() }) trimmed else text
        } else {
            text
        }
        if (submit && normalized.isNotBlank()) {
            val pendingApproval = _pendingRuntimeRequests.value.firstOrNull { it.kind == RuntimeRequestKindUi.APPROVAL }
            if (pendingApproval != null) {
                respondApproval(pendingApproval.requestId, "decline", normalized)
                return
            }
        }
        val optId = enqueueOptimisticTurnIfSubmitted(normalized, submit)
        viewModelScope.launch {
            runCatching { repo.sendInput(bridgeId, sid, normalized, submit) }
                .onFailure {
                    rollbackOptimisticTurn(optId)
                    _toast.value = it.message ?: "发送失败"
                }
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
        if (atts.isEmpty()) {
            sendInput(text, submit)
            return
        }
        val currentBridgeId = bridgeId
        val currentSid = sid
        if (currentBridgeId.isBlank() || currentSid.isBlank()) {
            _toast.value = "当前会话未连接，无法上传附件"
            return
        }
        viewModelScope.launch {
            _toast.value = "正在加密上传 ${atts.size} 个媒体附件…"
            val uploadedPaths = mutableListOf<String>()
            for (att in atts) {
                val res = runCatching {
                    repo.uploadMedia(
                        bridgeId = currentBridgeId,
                        sid = currentSid,
                        kind = att.kind.name.lowercase(),
                        mimeType = att.mimeType,
                        filename = att.name,
                        uriString = att.uri,
                    )
                }
                val relPath = res.getOrNull()
                if (relPath == null) {
                    val err = res.exceptionOrNull()
                    _toast.value = "附件上传失败 (${att.name}): ${err?.message ?: "未知错误"}"
                    return@launch
                }
                uploadedPaths += relPath
            }
            val uploadedIds = atts.mapTo(HashSet()) { it.id }
            _pendingAttachments.value = _pendingAttachments.value.filterNot { it.id in uploadedIds }
            val refs = uploadedPaths.joinToString("\n") { "@$it" }
            val payload = listOf(text.trim(), refs).filter { it.isNotBlank() }.joinToString("\n")
            if (payload.isNotBlank()) {
                val optId = enqueueOptimisticTurnIfSubmitted(text.trim().ifBlank { payload }, submit)
                runCatching { repo.sendInput(currentBridgeId, currentSid, payload, submit) }
                    .onFailure { err ->
                        rollbackOptimisticTurn(optId)
                        _toast.value = "附件已上传，但发送指令失败: ${err.message ?: err}"
                        return@launch
                    }
            }
            _toast.value = "已加密上传 ${uploadedPaths.size} 个媒体附件"
        }
    }

    private fun isInteractiveSlashCommand(text: String): Boolean {
        val trimmed = text.trim()
        if (!trimmed.startsWith("/")) return false
        val cmd = trimmed.substringBefore(' ').lowercase()
        return cmd in setOf(
            "/model", "/models", "/thinking", "/settings", "/config", "/theme",
            "/resume", "/session", "/sessions", "/tree", "/login", "/logout",
            "/permissions", "/approvals", "/mcp", "/skills", "/agents",
            "/help", "/clear", "/compact", "/status", "/cost", "/fork", "/quit", "/exit",
        )
    }

    private fun enqueueOptimisticTurnIfSubmitted(text: String, submit: Boolean): String? {
        if (!submit || !structured || text.isBlank() || isInteractiveSlashCommand(text)) return null
        val afterSeq = maxOf(lastPromptMaxSeq, _messages.value.maxOfOrNull { it.seq } ?: 0L)
        lastPromptMaxSeq = afterSeq
        lastCommittedTurnAssistantText = null
        val optId = UUID.randomUUID().toString()
        _optimisticUserMessages.value = _optimisticUserMessages.value + OptimisticUserMessage(
            id = optId,
            text = text.trim(),
            afterSeq = afterSeq,
        )
        val speakerModel = _messages.value
            .lastOrNull { it.role.contains("ASSISTANT", ignoreCase = true) && !it.model.isNullOrBlank() }
            ?.model
            ?: _streamingDraft.value.model
        _streamingDraft.value = StreamingMessageState(active = true, text = "", status = "思考中…", model = speakerModel)
        return optId
    }

    private fun rollbackOptimisticTurn(optId: String?) {
        if (optId == null) return
        val remaining = _optimisticUserMessages.value.filterNot { it.id == optId }
        _optimisticUserMessages.value = remaining
        if (remaining.isEmpty() && _streamingDraft.value.text.isBlank()) {
            _streamingDraft.value = StreamingMessageState()
        }
    }

    private fun extractPlainText(partsJson: String): String =
        repo.decodeParts(partsJson)
            .filterIsInstance<UiPart.Text>()
            .joinToString("\n") { it.text }
            .trim()

    private fun reconcileStreamingWithMessages(msgs: List<MessageEntity>) {
        if (_optimisticUserMessages.value.isNotEmpty()) {
            val committedUsers = msgs.filter { it.role.contains("USER", ignoreCase = true) }
            val userTexts = committedUsers.mapTo(HashSet()) { extractPlainText(it.partsJson) }
            _optimisticUserMessages.value = _optimisticUserMessages.value.filterNot { opt ->
                committedUsers.any { it.seq > opt.afterSeq } || opt.text in userTexts
            }
        }
        val latestSeq = msgs.maxOfOrNull { it.seq } ?: 0L
        if (!_streamingDraft.value.active) {
            if (latestSeq > lastPromptMaxSeq) {
                lastPromptMaxSeq = latestSeq
            }
            return
        }
        // Native RPC sessions report exact turn completion via StreamingDraft.active / config_updated.isRunning;
        // never prematurely dismiss live streaming when intermediate assistant text/tools commit.
        if (_isRpcSession.value) {
            return
        }
        val tail = msgs.lastOrNull() ?: return
        if (tail.seq <= lastPromptMaxSeq || !tail.role.contains("ASSISTANT", ignoreCase = true)) {
            return
        }
        val parts = repo.decodeParts(tail.partsJson)
        val hasFinalText = parts.filterIsInstance<UiPart.Text>().any { it.text.isNotBlank() }
        if (hasFinalText && !hasNonQuestionToolCall(parts)) {
            val committedText = extractPlainText(tail.partsJson)
            lastCommittedTurnAssistantText = committedText
            _streamingDraft.value = StreamingMessageState(
                active = false,
                text = "",
                thinking = "",
                status = "",
                model = tail.model ?: _streamingDraft.value.model,
            )
            updateTurnClockAndContext()
        }
    }

    private fun onStreamingDraftEvent(active: Boolean, text: String, thinking: String, status: String, model: String) {
        if (!active) {
            _streamingDraft.value = StreamingMessageState(
                active = false,
                text = "",
                thinking = "",
                status = "",
                model = model.ifBlank { _streamingDraft.value.model },
            )
            updateTurnClockAndContext()
            return
        }
        if (!_isRpcSession.value) {
            val trimmed = text.trim()
            val committedText = lastCommittedTurnAssistantText
            if (committedText != null && thinking.isBlank() && (trimmed.isEmpty() || committedText == trimmed || committedText.endsWith(trimmed))) {
                return
            }
        }
        val effectiveModel = model.ifBlank {
            _streamingDraft.value.model ?: _messages.value
                .lastOrNull { it.role.contains("ASSISTANT", ignoreCase = true) && !it.model.isNullOrBlank() }
                ?.model
        }
        _streamingDraft.value = StreamingMessageState(
            active = true,
            text = text,
            thinking = thinking,
            status = status,
            model = effectiveModel,
        )
        updateTurnClockAndContext()
    }

    private fun updateTurnClockAndContext() {
        val nowRunning = _streamingDraft.value.active || (_sessionConfig.value?.isRunning == true)
        val wasRunning = _turnStartedMs.value != null
        if (nowRunning && !wasRunning) {
            _turnStartedMs.value = System.currentTimeMillis()
        } else if (!nowRunning && wasRunning) {
            _turnStartedMs.value = null
            refreshWorkspaceContext(includeDiff = false)
        }
    }

    fun refreshWorkspaceContext(includeDiff: Boolean = false) {
        val currentBridgeId = bridgeId
        val currentSid = sid
        if (currentBridgeId.isBlank() || currentSid.isBlank()) return
        viewModelScope.launch {
            _workspaceContextLoading.value = true
            runCatching {
                repo.getWorkspaceContext(currentBridgeId, currentSid, sessionCwd, includeDiff)
            }.onSuccess { wc ->
                if (wc != null) {
                    val prevDiff = _workspaceContext.value?.gitDiff.orEmpty()
                    val ui = wc.toUi()
                    _workspaceContext.value = if (!includeDiff && ui.gitDiff.isBlank() && prevDiff.isNotBlank() && ui.changedFiles.isNotEmpty()) {
                        ui.copy(gitDiff = prevDiff)
                    } else {
                        ui
                    }
                    if (sessionCwd.isBlank() && ui.cwd.isNotBlank()) {
                        sessionCwd = ui.cwd
                    }
                }
            }
            _workspaceContextLoading.value = false
        }
    }

    fun openInNvim(stopRemoteRpc: Boolean = true) {
        val currentBridgeId = bridgeId
        val currentSid = sid
        if (currentBridgeId.isBlank() || currentSid.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repo.openInNvim(
                    bridgeId = currentBridgeId,
                    sid = currentSid,
                    tool = tool,
                    cwd = sessionCwd.ifBlank { _workspaceContext.value?.cwd.orEmpty() },
                    convId = convId,
                    stopRemoteRpc = stopRemoteRpc,
                )
            }.onSuccess { resp ->
                if (resp != null) {
                    _lastResumeCommand.value = if (!resp.opened && resp.resume_command.isNotBlank()) {
                        resp.resume_command
                    } else {
                        null
                    }
                    _toast.value = resp.message.ifBlank {
                        if (resp.opened) "已在本地 Neovim 打开" else "未检测到运行中的 Neovim"
                    }
                }
            }.onFailure { err ->
                _toast.value = "在 Neovim 打开失败: ${err.message ?: err}"
            }
        }
    }

    fun clearResumeCommand() {
        _lastResumeCommand.value = null
    }

    fun takeOverAsRpc(onReady: (sid: String, tool: String, title: String, convId: String, structured: Boolean) -> Unit) {
        val currentBridgeId = bridgeId
        val currentConvId = convId
        val cwd = sessionCwd.ifBlank { _workspaceContext.value?.cwd.orEmpty() }
        if (currentBridgeId.isBlank() || currentConvId.isBlank() || cwd.isBlank()) {
            _toast.value = "当前会话尚无完整上下文或工作目录，无法切换接管"
            return
        }
        viewModelScope.launch {
            runCatching { repo.startSession(currentBridgeId, tool, cwd, currentConvId) }
                .onSuccess { sess ->
                    if (sess != null) {
                        _toast.value = "已切换为原生 RPC 接管"
                        onReady(
                            sess.id,
                            sess.tool,
                            sess.title.ifBlank { "${sess.tool} · rpc" },
                            sess.conversation?.id ?: currentConvId,
                            true,
                        )
                    } else {
                        _toast.value = "未返回 RPC 会话"
                    }
                }
                .onFailure { err ->
                    _toast.value = "切换 RPC 接管失败: ${err.message ?: err}"
                }
        }
    }

    fun formatContextInjection(kind: String): String {
        val wc = _workspaceContext.value ?: return ""
        return when (kind) {
            "file" -> if (wc.nvimFile.isNotBlank()) {
                if (wc.nvimLine > 0) "@${wc.nvimFile}:${wc.nvimLine} " else "@${wc.nvimFile} "
            } else {
                ""
            }
            "diagnostics" -> {
                if (wc.diagnostics.isEmpty()) return ""
                buildString {
                    appendLine("当前 Neovim 诊断错误：")
                    wc.diagnostics.take(8).forEach { d ->
                        val src = if (d.source.isNotBlank()) " [${d.source}]" else ""
                        appendLine("- ${d.file}:${d.line}:${d.col} (${d.severity})$src: ${d.message}")
                    }
                }
            }
            "git_diff" -> {
                if (wc.changedFiles.isEmpty()) return ""
                buildString {
                    val branchLabel = wc.gitBranch.ifBlank { "HEAD" }
                    appendLine("工作区 ($branchLabel) 改动文件：")
                    wc.changedFiles.take(12).forEach { f ->
                        appendLine("- [${f.status}] ${f.path} (+${f.added} -${f.removed})")
                    }
                }
            }
            else -> ""
        }
    }

    fun selectDialogOption(targetIndex: Int) {
        val dlg = _interactiveDialog.value ?: return
        val delta = targetIndex - dlg.selectedIndex
        val keys = buildList {
            when {
                delta > 0 -> repeat(delta) { add(Key.KEY_DOWN) }
                delta < 0 -> repeat(-delta) { add(Key.KEY_UP) }
            }
            add(Key.KEY_ENTER)
        }
        sendKeys(keys)
    }

    fun updateDialogSearch(newQuery: String) {
        val current = _interactiveDialog.value ?: return
        val oldQuery = current.query
        if (newQuery == oldQuery || bridgeId.isBlank() || sid.isBlank()) return
        _interactiveDialog.value = current.copy(query = newQuery)
        viewModelScope.launch {
            runCatching {
                when {
                    newQuery.startsWith(oldQuery) -> {
                        val suffix = newQuery.substring(oldQuery.length)
                        if (suffix.isNotEmpty()) {
                            repo.sendInput(bridgeId, sid, suffix, submit = false)
                        }
                    }
                    oldQuery.startsWith(newQuery) -> {
                        val delCount = oldQuery.length - newQuery.length
                        if (delCount > 0) {
                            repo.sendKeys(bridgeId, sid, List(delCount) { Key.KEY_BACKSPACE })
                        }
                    }
                    else -> {
                        if (oldQuery.isNotEmpty()) {
                            repo.sendKeys(bridgeId, sid, List(oldQuery.length) { Key.KEY_BACKSPACE })
                        }
                        if (newQuery.isNotEmpty()) {
                            repo.sendInput(bridgeId, sid, newQuery, submit = false)
                        }
                    }
                }
            }
        }
    }

    fun dismissDialog() {
        _interactiveDialog.value = null
        sendKey(Key.KEY_ESCAPE)
    }

    fun updateModel(modelId: String) {
        if (bridgeId.isBlank() || sid.isBlank() || modelId.isBlank()) return
        val prev = _sessionConfig.value
        if (prev != null) {
            val pickedLevels = prev.availableModels.firstOrNull { it.id == modelId }?.thinkingLevels.orEmpty()
            _sessionConfig.value = prev.copy(
                model = modelId,
                availableModels = prev.availableModels.map { it.copy(current = it.id == modelId) },
                availableThinkingLevels = pickedLevels.ifEmpty { prev.availableThinkingLevels },
            )
        }
        viewModelScope.launch {
            runCatching { repo.setSessionConfig(bridgeId, sid, model = modelId) }
                .onSuccess { cfg -> if (cfg != null) _sessionConfig.value = cfg.toUi() }
                .onFailure { _toast.value = it.message ?: "切换模型失败" }
        }
    }

    fun updateThinkingLevel(level: String) {
        if (bridgeId.isBlank() || sid.isBlank() || level.isBlank()) return
        val prev = _sessionConfig.value
        if (prev != null) {
            _sessionConfig.value = prev.copy(thinkingLevel = level)
        }
        viewModelScope.launch {
            runCatching { repo.setSessionConfig(bridgeId, sid, thinkingLevel = level) }
                .onSuccess { cfg -> if (cfg != null) _sessionConfig.value = cfg.toUi() }
                .onFailure { _toast.value = it.message ?: "切换思考强度失败" }
        }
    }

    fun updateRuntimeMode(mode: RuntimeModeUi) {
        if (bridgeId.isBlank() || sid.isBlank() || mode == RuntimeModeUi.UNSPECIFIED) return
        val prev = _sessionConfig.value
        if (prev != null) {
            _sessionConfig.value = prev.copy(runtimeMode = mode)
        }
        viewModelScope.launch {
            runCatching { repo.setSessionConfig(bridgeId, sid, runtimeMode = mode.toProto()) }
                .onSuccess { cfg -> if (cfg != null) _sessionConfig.value = cfg.toUi() }
                .onFailure { _toast.value = it.message ?: "切换权限模式失败" }
        }
    }

    fun respondApproval(requestId: String, decision: String, feedback: String = "") {
        if (bridgeId.isBlank() || sid.isBlank() || requestId.isBlank()) return
        _pendingRuntimeRequests.value = _pendingRuntimeRequests.value.filterNot { it.requestId == requestId }
        viewModelScope.launch {
            runCatching {
                repo.respondRuntimeRequest(
                    bridgeId = bridgeId,
                    sid = sid,
                    requestId = requestId,
                    decision = decision,
                    feedback = feedback,
                )
            }.onFailure { _toast.value = it.message ?: "提交审批失败" }
        }
    }

    fun respondUserInput(requestId: String, answers: List<RuntimeAnswerInput>, cancelled: Boolean = false) {
        if (bridgeId.isBlank() || sid.isBlank() || requestId.isBlank()) return
        _pendingRuntimeRequests.value = _pendingRuntimeRequests.value.filterNot { it.requestId == requestId }
        viewModelScope.launch {
            runCatching {
                repo.respondRuntimeRequest(
                    bridgeId = bridgeId,
                    sid = sid,
                    requestId = requestId,
                    answers = answers.map {
                        RuntimeAnswer(
                            question_id = it.questionId,
                            selected_values = it.selectedValues,
                            custom_text = it.customText,
                        )
                    },
                    cancelled = cancelled,
                )
            }.onFailure { _toast.value = it.message ?: "提交回答失败" }
        }
    }

    fun interruptTurn() {
        if (bridgeId.isBlank() || sid.isBlank()) return
        viewModelScope.launch {
            runCatching { repo.interruptTurn(bridgeId, sid) }
                .onFailure { _toast.value = it.message ?: "中断失败" }
        }
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
                _toast.value = e.message ?: "加载媒体失败"
            }
            _loadingMediaIds.value = _loadingMediaIds.value - media.mediaId
        }
    }

    fun sendKey(key: Key) {
        sendKeys(listOf(key))
    }

    fun sendKeys(keys: List<Key>) {
        if (bridgeId.isBlank() || sid.isBlank() || keys.isEmpty()) return
        viewModelScope.launch {
            runCatching { repo.sendKeys(bridgeId, sid, keys) }
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

    fun handover(
        targetTool: String,
        targetSid: String?,
        note: String,
        includeContext: Boolean,
        onNavigated: (sid: String, tool: String, title: String, convId: String, structured: Boolean) -> Unit,
    ) {
        val currentBridgeId = bridgeId
        val currentSid = sid
        if (currentBridgeId.isBlank() || currentSid.isBlank() || _handoverBusy.value) return
        viewModelScope.launch {
            _handoverBusy.value = true
            runCatching {
                repo.handoverSession(
                    bridgeId = currentBridgeId,
                    sourceSid = currentSid,
                    targetTool = targetTool,
                    targetSid = targetSid,
                    handoverNote = note,
                    includeContext = includeContext,
                )
            }.onSuccess { target ->
                if (target != null) {
                    val targetStructured = _availableTools.value
                        .firstOrNull { it.name.equals(target.tool, ignoreCase = true) }
                        ?.structured_history ?: true
                    _toast.value = "已移交工作至 ${target.tool}"
                    onNavigated(
                        target.id,
                        target.tool,
                        target.title.ifBlank { "${target.tool} · handover" },
                        target.conversation?.id.orEmpty(),
                        targetStructured,
                    )
                } else {
                    _toast.value = "移交已完成，未返回目标会话"
                }
            }.onFailure { err ->
                _toast.value = "移交失败: ${err.message ?: err}"
            }
            _handoverBusy.value = false
        }
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

private fun WorkspaceContext.toUi(): WorkspaceContextUi = WorkspaceContextUi(
    cwd = cwd,
    gitBranch = git_branch,
    changedFiles = changed_files.map { f ->
        ChangedFileUi(path = f.path, status = f.status, added = f.added, removed = f.removed)
    },
    gitDiff = git_diff,
    nvimConnected = nvim_connected,
    nvimServer = nvim_server,
    nvimFile = nvim_file,
    nvimLine = nvim_line,
    diagnostics = diagnostics.map { d ->
        DiagnosticItemUi(
            file = d.file_,
            line = d.line,
            col = d.col,
            severity = d.severity,
            message = d.message,
            source = d.source,
        )
    },
    openBuffers = open_buffers,
)

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
