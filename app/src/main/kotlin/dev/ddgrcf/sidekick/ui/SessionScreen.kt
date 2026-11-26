package dev.ddgrcf.sidekick.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.WrapText
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ddgrcf.sidekick.core.ansi.AnsiLine
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.core.store.SessionEntity
import dev.ddgrcf.sidekick.data.UiPart
import dev.ddgrcf.sidekick.proto.v1.Key
import dev.ddgrcf.sidekick.proto.v1.ToolInfo
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras
import dev.ddgrcf.sidekick.vm.MediaKind
import dev.ddgrcf.sidekick.vm.PendingAttachment
import dev.ddgrcf.sidekick.vm.QuotedMessage
import dev.ddgrcf.sidekick.vm.SessionViewModel
import dev.ddgrcf.sidekick.vm.SlashCommandSuggestion
import dev.ddgrcf.sidekick.vm.WorkspaceFileSuggestion
import dev.ddgrcf.sidekick.vm.coreBuiltinSlashCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Base64
import java.util.Locale
import java.util.UUID

@Composable
fun SessionScreen(
    bridgeId: String,
    sid: String,
    tool: String,
    title: String,
    convId: String,
    structuredHistory: Boolean,
    vm: SessionViewModel,
    onBack: () -> Unit,
    onOpenSession: (sid: String, tool: String, title: String, convId: String, structured: Boolean) -> Unit = { _, _, _, _, _ -> },
) {
    LaunchedEffect(bridgeId, sid) {
        vm.attach(bridgeId, sid, tool, convId, structuredHistory)
    }
    val connState by vm.connectionState.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val screenLines by vm.screenLines.collectAsStateWithLifecycle()
    val screenRev by vm.screenRevision.collectAsStateWithLifecycle()
    val localTyping by vm.localTyping.collectAsStateWithLifecycle()
    val pendingAttachments by vm.pendingAttachments.collectAsStateWithLifecycle()
    val fetchedMediaPaths by vm.fetchedMediaPaths.collectAsStateWithLifecycle()
    val loadingMediaIds by vm.loadingMediaIds.collectAsStateWithLifecycle()
    val fileSuggestions by vm.fileSuggestions.collectAsStateWithLifecycle()
    val slashCommands by vm.slashCommands.collectAsStateWithLifecycle()
    val availableTools by vm.availableTools.collectAsStateWithLifecycle()
    val otherSessions by vm.otherSessions.collectAsStateWithLifecycle()
    val handoverBusy by vm.handoverBusy.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()

    SessionContent(
        sessionId = sid,
        title = title,
        tool = tool,
        structuredHistory = structuredHistory,
        connectionState = connState,
        messages = messages,
        decodeParts = vm::decodeParts,
        screenLines = screenLines,
        screenRevision = screenRev,
        localTyping = localTyping,
        pendingAttachments = pendingAttachments,
        fetchedMediaPaths = fetchedMediaPaths,
        loadingMediaIds = loadingMediaIds,
        fileSuggestions = fileSuggestions,
        slashCommands = slashCommands,
        availableTools = availableTools,
        otherSessions = otherSessions,
        handoverBusy = handoverBusy,
        toast = toast,
        onClearToast = vm::clearToast,
        onBack = onBack,
        onSendInput = vm::sendWithAttachments,
        onSendKey = vm::sendKey,
        onFocusChanged = vm::onFocusChanged,
        onSearchFiles = vm::searchWorkspaceFiles,
        onAddAttachment = vm::addAttachment,
        onRemoveAttachment = vm::removeAttachment,
        onRefreshMessages = vm::refreshMessages,
        onStopSession = { vm.stopSession(onBack) },
        onRequestMedia = vm::fetchMediaIfNeeded,
        onHandoverSession = { targetTool, targetSid, note, includeCtx ->
            vm.handover(targetTool, targetSid, note, includeCtx, onOpenSession)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionContent(
    title: String,
    tool: String,
    structuredHistory: Boolean,
    connectionState: ConnectionState,
    messages: List<MessageEntity>,
    decodeParts: (String) -> List<UiPart>,
    screenLines: List<AnsiLine>,
    screenRevision: Long,
    localTyping: Boolean,
    toast: String?,
    onClearToast: () -> Unit,
    onBack: () -> Unit,
    onSendInput: (String, Boolean) -> Unit,
    onSendKey: (Key) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    sessionId: String = "",
    isDemo: Boolean = false,
    pendingAttachments: List<PendingAttachment> = emptyList(),
    fetchedMediaPaths: Map<String, String> = emptyMap(),
    loadingMediaIds: Set<String> = emptySet(),
    fileSuggestions: List<WorkspaceFileSuggestion> = emptyList(),
    slashCommands: List<SlashCommandSuggestion> = remember(tool) { coreBuiltinSlashCommands(tool) },
    availableTools: List<ToolInfo> = emptyList(),
    otherSessions: List<SessionEntity> = emptyList(),
    handoverBusy: Boolean = false,
    onSearchFiles: (String) -> Unit = {},
    onAddAttachment: (PendingAttachment) -> Unit = {},
    onRemoveAttachment: (String) -> Unit = {},
    onRefreshMessages: () -> Unit = {},
    onStopSession: () -> Unit = {},
    onRequestMedia: (UiPart.Media) -> Unit = {},
    onHandoverSession: (targetTool: String, targetSid: String?, note: String, includeContext: Boolean) -> Unit = { _, _, _, _ -> },
) {
    var tabIdx by rememberSaveable(structuredHistory) { mutableIntStateOf(0) }
    var terminalFontSizeSp by rememberSaveable { mutableIntStateOf(12) }
    var terminalSoftWrap by rememberSaveable { mutableStateOf(true) }
    var headerExpanded by rememberSaveable(sessionId) { mutableStateOf(true) }
    var headerPinned by rememberSaveable(sessionId) { mutableStateOf(false) }
    var peekTrigger by rememberSaveable { mutableIntStateOf(0) }
    var showOverflow by remember { mutableStateOf(false) }
    var confirmStop by rememberSaveable { mutableStateOf(false) }
    var showHandoverSheet by rememberSaveable { mutableStateOf(false) }
    val stateJson = remember { Json { ignoreUnknownKeys = true } }
    var lightboxMedia by rememberSaveable(
        stateSaver = Saver<UiPart.Media?, String>(
            save = { m -> m?.let { stateJson.encodeToString(it) } ?: "" },
            restore = { raw -> if (raw.isBlank()) null else runCatching { stateJson.decodeFromString<UiPart.Media>(raw) }.getOrNull() },
        ),
    ) { mutableStateOf<UiPart.Media?>(null) }
    var videoPlayerMedia by rememberSaveable(
        stateSaver = Saver<UiPart.Media?, String>(
            save = { m -> m?.let { stateJson.encodeToString(it) } ?: "" },
            restore = { raw -> if (raw.isBlank()) null else runCatching { stateJson.decodeFromString<UiPart.Media>(raw) }.getOrNull() },
        ),
    ) { mutableStateOf<UiPart.Media?>(null) }
    var quotedMessage by rememberSaveable(
        stateSaver = Saver<QuotedMessage?, String>(
            save = { q -> q?.let { stateJson.encodeToString(it) } ?: "" },
            restore = { raw -> if (raw.isBlank()) null else runCatching { stateJson.decodeFromString<QuotedMessage>(raw) }.getOrNull() },
        ),
    ) { mutableStateOf<QuotedMessage?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    fun notify(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    LaunchedEffect(toast) {
        if (!toast.isNullOrBlank()) {
            snackbarHostState.showSnackbar(toast)
            onClearToast()
        }
    }

    val isTerminalTab = !structuredHistory || tabIdx == 1
    val imeVisible = WindowInsets.isImeVisible
    val connectionAttention = connectionState != ConnectionState.CONNECTED
    val showExpandedHeader = headerExpanded || headerPinned || connectionAttention

    LaunchedEffect(peekTrigger, headerExpanded, headerPinned, connectionAttention) {
        if (headerExpanded && !headerPinned && !connectionAttention) {
            delay(2400L)
            headerExpanded = false
        }
    }

    LaunchedEffect(imeVisible) {
        if (imeVisible && !headerPinned && !connectionAttention) {
            headerExpanded = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                headerExpanded = true
                peekTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val density = LocalDensity.current
    val showThresholdPx = with(density) { 18.dp.toPx() }
    val hideThresholdPx = with(density) { 14.dp.toPx() }
    val headerNestedScroll = remember(showThresholdPx, hideThresholdPx, connectionAttention) {
        var upAccum = 0f
        var downAccum = 0f
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val dy = available.y
                    if (dy > 0f) {
                        upAccum += dy
                        downAccum = 0f
                        if (upAccum > showThresholdPx) {
                            upAccum = 0f
                            headerExpanded = true
                            peekTrigger++
                        }
                    } else if (dy < 0f) {
                        downAccum += -dy
                        upAccum = 0f
                        if (downAccum > hideThresholdPx && !headerPinned && !connectionAttention) {
                            downAccum = 0f
                            headerExpanded = false
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    fun selectTab(nextIdx: Int) {
        if (tabIdx != nextIdx) {
            tabIdx = nextIdx
            headerExpanded = true
            peekTrigger++
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var dismissSignal by rememberSaveable { mutableIntStateOf(0) }

    fun dismissConversationChrome() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        dismissSignal++
        if (headerPinned || headerExpanded) {
            headerPinned = false
            if (!connectionAttention) {
                headerExpanded = false
            }
        }
    }

    fun copyTerminalScreen() {
        val plain = terminalPlainText(screenLines)
        if (plain.isNotBlank()) {
            clipboard.setText(AnnotatedString(plain))
            notify("已复制终端屏幕")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SessionAdaptiveHeader(
                tool = tool,
                sessionId = sessionId,
                structuredHistory = structuredHistory,
                tabIdx = tabIdx,
                expanded = showExpandedHeader,
                showOverflow = showOverflow,
                isTerminalTab = isTerminalTab,
                terminalFontSizeSp = terminalFontSizeSp,
                terminalSoftWrap = terminalSoftWrap,
                canHandover = availableTools.isNotEmpty() || otherSessions.isNotEmpty(),
                onToggleExpanded = {
                    if (showExpandedHeader) {
                        headerPinned = false
                        headerExpanded = false
                    } else {
                        headerPinned = true
                        headerExpanded = true
                    }
                },
                onSelectTab = ::selectTab,
                onBack = onBack,
                onOpenOverflow = { showOverflow = true },
                onDismissOverflow = { showOverflow = false },
                onRefreshMessages = onRefreshMessages,
                onOpenHandover = { showHandoverSheet = true },
                onCopySessionId = {
                    clipboard.setText(AnnotatedString(sessionId))
                    notify("已复制会话 ID")
                },
                onTerminalFontDelta = { delta ->
                    terminalFontSizeSp = (terminalFontSizeSp + delta).coerceIn(10, 16)
                },
                onToggleTerminalWrap = { terminalSoftWrap = !terminalSoftWrap },
                onCopyTerminalScreen = ::copyTerminalScreen,
                onRequestStopSession = { confirmStop = true },
            )
        },
        bottomBar = {
            SessionComposer(
                tool = tool,
                localTyping = localTyping,
                defaultShowKeycaps = isTerminalTab,
                dismissSignal = dismissSignal,
                pendingAttachments = pendingAttachments,
                quotedMessage = quotedMessage,
                fileSuggestions = fileSuggestions,
                slashCommands = slashCommands,
                screenLines = screenLines,
                onSearchFiles = onSearchFiles,
                onClearQuote = { quotedMessage = null },
                onAddAttachment = onAddAttachment,
                onRemoveAttachment = onRemoveAttachment,
                onSendInput = onSendInput,
                onSendKey = onSendKey,
                onFocusChanged = onFocusChanged,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("session_canvas")
                    .nestedScroll(headerNestedScroll)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val slop = viewConfiguration.touchSlop
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    val dist = (change.position - down.position).getDistance()
                                    val elapsed = change.uptimeMillis - down.uptimeMillis
                                    if (!change.isConsumed &&
                                        dist <= slop &&
                                        elapsed < viewConfiguration.longPressTimeoutMillis
                                    ) {
                                        dismissConversationChrome()
                                    }
                                    break
                                }
                                if ((change.position - down.position).getDistance() > slop) {
                                    break
                                }
                            }
                        }
                    },
            ) {
                if (structuredHistory && tabIdx == 0) {
                    ChatView(
                        tool = tool,
                        messages = messages,
                        decodeParts = decodeParts,
                        fetchedMediaPaths = fetchedMediaPaths,
                        onCopyText = { txt, label ->
                            clipboard.setText(AnnotatedString(txt))
                            notify("已复制$label")
                        },
                        onQuoteMessage = { author, excerpt ->
                            quotedMessage = QuotedMessage(author, excerpt)
                        },
                        onAutoFetchMedia = onRequestMedia,
                        onOpenImage = { media ->
                            onRequestMedia(media)
                            lightboxMedia = media
                        },
                        onOpenVideo = { media ->
                            onRequestMedia(media)
                            videoPlayerMedia = media
                        },
                        onSwitchToTerminal = { selectTab(1) },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    TerminalView(
                        lines = screenLines,
                        revision = screenRevision,
                        fontSizeSp = terminalFontSizeSp,
                        softWrap = terminalSoftWrap,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                SessionTitleDetailsOverlay(
                    visible = showExpandedHeader,
                    title = title,
                    tool = tool,
                    sessionId = sessionId,
                    connectionState = connectionState,
                    isDemo = isDemo,
                    isTerminalTab = isTerminalTab,
                    terminalLineCount = screenLines.size,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("停止当前会话？") },
            text = { Text("停止后远程与本地 mux 会话将被终止。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmStop = false
                        onStopSession()
                    },
                ) {
                    Text("停止会话", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) { Text("取消") }
            },
        )
    }

    lightboxMedia?.let { media ->
        MediaLightboxDialog(
            media = media,
            resolvedPath = fetchedMediaPaths[media.mediaId],
            isLoading = loadingMediaIds.contains(media.mediaId),
            onDismiss = { lightboxMedia = null },
        )
    }

    videoPlayerMedia?.let { media ->
        VideoPlayerDialog(
            media = media,
            resolvedPath = fetchedMediaPaths[media.mediaId],
            isLoading = loadingMediaIds.contains(media.mediaId),
            isDemo = isDemo,
            onDismiss = { videoPlayerMedia = null },
        )
    }

    if (showHandoverSheet) {
        HandoverBottomSheet(
            sourceTool = tool,
            sourceSessionId = sessionId,
            availableTools = availableTools,
            otherSessions = otherSessions,
            busy = handoverBusy,
            onDismiss = { showHandoverSheet = false },
            onSubmit = { targetTool, targetSid, note, includeCtx ->
                showHandoverSheet = false
                onHandoverSession(targetTool, targetSid, note, includeCtx)
            },
        )
    }
}

@Composable
private fun SessionAdaptiveHeader(
    tool: String,
    sessionId: String,
    structuredHistory: Boolean,
    tabIdx: Int,
    expanded: Boolean,
    showOverflow: Boolean,
    isTerminalTab: Boolean,
    terminalFontSizeSp: Int,
    terminalSoftWrap: Boolean,
    canHandover: Boolean = false,
    onToggleExpanded: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenOverflow: () -> Unit,
    onDismissOverflow: () -> Unit,
    onRefreshMessages: () -> Unit,
    onOpenHandover: () -> Unit = {},
    onCopySessionId: () -> Unit,
    onTerminalFontDelta: (Int) -> Unit,
    onToggleTerminalWrap: () -> Unit,
    onCopyTerminalScreen: () -> Unit,
    onRequestStopSession: () -> Unit,
) {
    val largeFontMode = LocalDensity.current.fontScale >= 1.45f

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            onClickLabel = if (expanded) "收起标题详情" else "展开完整标题",
                            onClick = onToggleExpanded,
                        )
                        .semantics(mergeDescendants = true) {
                            stateDescription = if (expanded) "已展开会话标题" else "已收起会话标题"
                        }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = ToolVisuals.label(tool),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (structuredHistory && !largeFontMode) {
                    SessionViewTabs(
                        selectedIndex = tabIdx,
                        onSelectTab = onSelectTab,
                    )
                }

                Box {
                    IconButton(onClick = onOpenOverflow) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(
                        expanded = showOverflow,
                        onDismissRequest = onDismissOverflow,
                    ) {
                        if (canHandover) {
                            DropdownMenuItem(
                                text = { Text("移交给其他 Agent") },
                                leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
                                onClick = {
                                    onDismissOverflow()
                                    onOpenHandover()
                                },
                            )
                        }
                        if (structuredHistory) {
                            DropdownMenuItem(
                                text = { Text("同步消息历史") },
                                leadingIcon = { Icon(Icons.Rounded.Sync, contentDescription = null) },
                                onClick = {
                                    onDismissOverflow()
                                    onRefreshMessages()
                                },
                            )
                        }
                        if (isTerminalTab) {
                            DropdownMenuItem(
                                text = { Text("增大终端字号 (${terminalFontSizeSp}sp)") },
                                leadingIcon = { Icon(Icons.Rounded.TextIncrease, contentDescription = null) },
                                enabled = terminalFontSizeSp < 16,
                                onClick = {
                                    onDismissOverflow()
                                    onTerminalFontDelta(1)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("减小终端字号 (${terminalFontSizeSp}sp)") },
                                leadingIcon = { Icon(Icons.Rounded.TextDecrease, contentDescription = null) },
                                enabled = terminalFontSizeSp > 10,
                                onClick = {
                                    onDismissOverflow()
                                    onTerminalFontDelta(-1)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (terminalSoftWrap) "关闭自动换行" else "开启自动换行") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.WrapText, contentDescription = null) },
                                onClick = {
                                    onDismissOverflow()
                                    onToggleTerminalWrap()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("复制终端屏幕") },
                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                                onClick = {
                                    onDismissOverflow()
                                    onCopyTerminalScreen()
                                },
                            )
                        }
                        if (sessionId.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("复制会话 ID") },
                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                                onClick = {
                                    onDismissOverflow()
                                    onCopySessionId()
                                },
                            )
                        }
                        if (structuredHistory || isTerminalTab || sessionId.isNotBlank()) {
                            HairlineDivider()
                        }
                        DropdownMenuItem(
                            text = { Text("停止会话", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.StopCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                onDismissOverflow()
                                onRequestStopSession()
                            },
                        )
                    }
                }
            }

            if (structuredHistory && largeFontMode) {
                SessionViewTabs(
                    selectedIndex = tabIdx,
                    onSelectTab = onSelectTab,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }

            HairlineDivider()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionTitleDetailsOverlay(
    visible: Boolean,
    title: String,
    tool: String,
    sessionId: String,
    connectionState: ConnectionState,
    isDemo: Boolean,
    isTerminalTab: Boolean,
    terminalLineCount: Int,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(200)) + fadeIn(tween(160)),
        exit = shrinkVertically(tween(180)) + fadeOut(tween(140)),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title.ifBlank { ToolVisuals.label(tool) },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        ConnectionStatusPill(connectionState, compact = false)
                        if (sessionId.isNotBlank()) {
                            Text(
                                text = sessionId.take(14),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (isTerminalTab) {
                            Text(
                                text = "ANSI · $terminalLineCount 行",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (isDemo) {
                            DemoTag()
                        }
                    }
                }
                HairlineDivider()
            }
        }
    }
}

@Composable
private fun SessionViewTabs(
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tabCentersPx = remember { mutableStateMapOf<Int, Float>() }
    val indicatorCenterPx = remember { Animatable(0f) }
    var indicatorInitialized by remember { mutableStateOf(false) }

    val targetCenter = tabCentersPx[selectedIndex]
    LaunchedEffect(selectedIndex, targetCenter) {
        if (targetCenter != null) {
            if (!indicatorInitialized) {
                indicatorCenterPx.snapTo(targetCenter)
                indicatorInitialized = true
            } else {
                indicatorCenterPx.animateTo(
                    targetValue = targetCenter,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    val indicatorWidthPx = with(density) { 20.dp.toPx() }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("Chat", "Terminal").forEachIndexed { idx, label ->
                val isSelected = selectedIndex == idx
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            tabCentersPx[idx] = coords.positionInParent().x + coords.size.width / 2f
                        }
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelectTab(idx) },
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
        if (indicatorInitialized) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset {
                        IntOffset((indicatorCenterPx.value - indicatorWidthPx / 2f).roundToInt(), 0)
                    }
                    .width(20.dp)
                    .height(2.5.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

@Composable
private fun ScrollToBottomButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = SidekickThemeExtras.colors
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 1.dp,
            border = BorderStroke(0.5.dp, extras.hairline),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = "回到底部",
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatView(
    tool: String,
    messages: List<MessageEntity>,
    decodeParts: (String) -> List<UiPart>,
    fetchedMediaPaths: Map<String, String>,
    onCopyText: (String, String) -> Unit,
    onQuoteMessage: (String, String) -> Unit,
    onAutoFetchMedia: (UiPart.Media) -> Unit,
    onOpenImage: (UiPart.Media) -> Unit,
    onOpenVideo: (UiPart.Media) -> Unit,
    onSwitchToTerminal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (messages.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ToolIcon(tool = tool, size = 36.dp, contentDescription = null)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "已连接到 ${ToolVisuals.label(tool)} 会话",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "可发送指令到远程会话，或切换到 Terminal 查看实时 PTY 输出",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onSwitchToTerminal,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("查看 Terminal")
                }
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showJumpToBottom by remember {
        derivedStateOf { listState.canScrollForward }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val nearBottom = info.totalItemsCount <= 1 || lastVisible >= info.totalItemsCount - 2
            val lastIsUser = messages.last().role.contains("USER", ignoreCase = true)
            if (lastIsUser || nearBottom) {
                listState.scrollToItem(messages.lastIndex)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("chat_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            items(messages, key = { it.seq }) { msg ->
                val parts = remember(msg.partsJson) { decodeParts(msg.partsJson) }
                val isUser = msg.role.contains("USER", ignoreCase = true)
                Box(modifier = Modifier.animateItem()) {
                    if (isUser) {
                        UserPromptTurn(
                            parts = parts,
                            fetchedMediaPaths = fetchedMediaPaths,
                            onCopyText = onCopyText,
                            onQuote = { excerpt -> onQuoteMessage("你", excerpt) },
                            onAutoFetchMedia = onAutoFetchMedia,
                            onOpenImage = onOpenImage,
                            onOpenVideo = onOpenVideo,
                        )
                    } else {
                        AssistantTurn(
                            tool = tool,
                            model = msg.model,
                            parts = parts,
                            fetchedMediaPaths = fetchedMediaPaths,
                            onCopyText = onCopyText,
                            onQuote = { author, excerpt -> onQuoteMessage(author, excerpt) },
                            onAutoFetchMedia = onAutoFetchMedia,
                            onOpenImage = onOpenImage,
                            onOpenVideo = onOpenVideo,
                        )
                    }
                }
            }
        }

        if (showJumpToBottom) {
            ScrollToBottomButton(
                onClick = { scope.launch { listState.scrollToItem(messages.lastIndex) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun UserPromptTurn(
    parts: List<UiPart>,
    fetchedMediaPaths: Map<String, String>,
    onCopyText: (String, String) -> Unit,
    onQuote: (String) -> Unit,
    onAutoFetchMedia: (UiPart.Media) -> Unit,
    onOpenImage: (UiPart.Media) -> Unit,
    onOpenVideo: (UiPart.Media) -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    val textParts = parts.filterIsInstance<UiPart.Text>()
    val mediaParts = parts.filterIsInstance<UiPart.Media>()
    val userPlainText = remember(textParts) { textParts.joinToString("\n") { it.text }.trim() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        mediaParts.forEach { media ->
            Box(modifier = Modifier.widthIn(max = 280.dp)) {
                MediaPartCard(
                    media = media,
                    resolvedPath = fetchedMediaPaths[media.mediaId],
                    onAutoFetch = { onAutoFetchMedia(media) },
                    onOpenImage = { onOpenImage(media) },
                    onOpenVideo = { onOpenVideo(media) },
                )
            }
        }
        if (textParts.isNotEmpty()) {
            Surface(
                modifier = Modifier.widthIn(max = 320.dp),
                shape = RoundedCornerShape(22.dp),
                color = extras.userBubble,
            ) {
                SelectionContainer {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        textParts.forEach { tp ->
                            Text(
                                text = tp.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    lineHeight = 23.sp,
                                ),
                                color = extras.onUserBubble,
                            )
                        }
                    }
                }
            }
            if (userPlainText.isNotEmpty()) {
                MessageActionRow(
                    copyContentDescription = "复制用户消息",
                    quoteContentDescription = "引用用户消息",
                    onCopy = { onCopyText(userPlainText, "消息") },
                    onQuote = { onQuote(summarizeExcerpt(userPlainText)) },
                )
            }
        }
    }
}

@Composable
private fun AssistantTurn(
    tool: String,
    model: String?,
    parts: List<UiPart>,
    fetchedMediaPaths: Map<String, String>,
    onCopyText: (String, String) -> Unit,
    onQuote: (String, String) -> Unit,
    onAutoFetchMedia: (UiPart.Media) -> Unit,
    onOpenImage: (UiPart.Media) -> Unit,
    onOpenVideo: (UiPart.Media) -> Unit,
) {
    val plainText = remember(parts) {
        parts.filterIsInstance<UiPart.Text>().joinToString("\n\n") { it.text }
    }
    val authorLabel = model?.takeIf { it.isNotBlank() } ?: ToolVisuals.label(tool)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToolIcon(tool = tool, size = 22.dp, contentDescription = null)
            Text(
                text = authorLabel,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        parts.forEach { p ->
            when (p) {
                is UiPart.Text -> MarkdownContent(
                    md = p.text,
                    textColor = MaterialTheme.colorScheme.onBackground,
                    onCopyCode = { code -> onCopyText(code, "代码") },
                )
                is UiPart.Thinking -> InlineThinkingDisclosure(p.text)
                is UiPart.ToolCall -> InlineToolDisclosure(
                    title = "${p.name} (${p.status.removePrefix("TOOL_CALL_STATUS_")})",
                    body = p.inputJson,
                    isError = p.status.contains("ERROR", ignoreCase = true) ||
                        p.status.contains("FAILED", ignoreCase = true),
                    isResult = false,
                    onCopyBody = { onCopyText(it, "参数") },
                )
                is UiPart.ToolResult -> InlineToolDisclosure(
                    title = if (p.isError) "工具执行出错" else "工具输出",
                    body = p.output,
                    isError = p.isError,
                    isResult = true,
                    onCopyBody = { onCopyText(it, "输出") },
                )
                is UiPart.Media -> MediaPartCard(
                    media = p,
                    resolvedPath = fetchedMediaPaths[p.mediaId],
                    onAutoFetch = { onAutoFetchMedia(p) },
                    onOpenImage = { onOpenImage(p) },
                    onOpenVideo = { onOpenVideo(p) },
                )
            }
        }

        if (plainText.isNotBlank()) {
            MessageActionRow(
                copyContentDescription = "复制回复",
                quoteContentDescription = "引用回复",
                onCopy = { onCopyText(plainText, "回复") },
                onQuote = { onQuote(authorLabel, summarizeExcerpt(plainText)) },
            )
        }
    }
}

@Composable
private fun MessageActionRow(
    copyContentDescription: String,
    quoteContentDescription: String,
    onCopy: () -> Unit,
    onQuote: () -> Unit,
) {
    var copiedRecently by remember { mutableStateOf(false) }
    LaunchedEffect(copiedRecently) {
        if (copiedRecently) {
            delay(1500)
            copiedRecently = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MessageActionItem(
            icon = if (copiedRecently) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
            label = if (copiedRecently) "已复制" else "复制",
            contentDescription = copyContentDescription,
            highlighted = copiedRecently,
            onClick = {
                onCopy()
                copiedRecently = true
            },
        )
        MessageActionItem(
            icon = Icons.Rounded.FormatQuote,
            label = "引用",
            contentDescription = quoteContentDescription,
            highlighted = false,
            onClick = onQuote,
        )
    }
}

@Composable
private fun MessageActionItem(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (highlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
        )
    }
}

private fun summarizeExcerpt(raw: String, maxChars: Int = 160): String {
    val clean = raw
        .replace(Regex("```[\\s\\S]*?```"), " [代码块] ")
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
    return if (clean.length <= maxChars) clean else clean.take(maxChars - 1) + "…"
}

@Composable
private fun InlineThinkingDisclosure(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = "思考过程",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
        AnimatedVisibility(visible = expanded && text.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 6.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InlineToolDisclosure(
    title: String,
    body: String,
    isError: Boolean,
    isResult: Boolean,
    onCopyBody: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val extras = SidekickThemeExtras.colors
    val accent = when {
        isError -> MaterialTheme.colorScheme.error
        isResult -> extras.success
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when {
        isError -> Icons.Rounded.ErrorOutline
        isResult -> Icons.Rounded.CheckCircle
        else -> Icons.Rounded.Build
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { if (body.isNotBlank()) expanded = !expanded },
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (body.isNotBlank()) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        AnimatedVisibility(visible = expanded && body.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(10.dp),
                color = extras.codeBg,
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)) {
                    Text(
                        text = body,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = extras.codeText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(end = 44.dp, top = 4.dp, bottom = 4.dp),
                    )
                    IconButton(
                        onClick = { onCopyBody(body) },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "复制",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaPartCard(
    media: UiPart.Media,
    resolvedPath: String?,
    onAutoFetch: () -> Unit,
    onOpenImage: () -> Unit,
    onOpenVideo: () -> Unit,
) {
    val extras = SidekickThemeExtras.colors

    LaunchedEffect(media.mediaId) {
        if (media.kind.equals("image", ignoreCase = true) &&
            resolvedPath == null &&
            media.localUri == null &&
            media.mediaId.isNotBlank()
        ) {
            onAutoFetch()
        }
    }

    when (media.kind.lowercase()) {
        "image" -> {
            val previewBmp by produceState<ImageBitmap?>(
                initialValue = null,
                resolvedPath,
                media.localUri,
                media.thumbnailB64,
            ) {
                value = withContext(Dispatchers.IO) {
                    decodeMediaBitmap(resolvedPath, media.localUri, media.thumbnailB64, maxDim = 1080)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (previewBmp != null) {
                    Surface(
                        onClick = onOpenImage,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Image(
                            bitmap = previewBmp!!,
                            contentDescription = media.name,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable(onClick = onOpenImage)
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = media.name.ifBlank { "图片" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (media.sizeBytes > 0) {
                            Text(
                                text = formatByteSize(media.sizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = onOpenImage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 64.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = media.name.ifBlank { null },
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = media.name.ifBlank { "图片" },
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val meta = buildList {
                                    if (media.width > 0 && media.height > 0) add("${media.width}×${media.height}")
                                    if (media.sizeBytes > 0) add(formatByteSize(media.sizeBytes))
                                }.joinToString(" · ")
                                if (meta.isNotBlank()) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        "video" -> {
            val context = LocalContext.current
            val videoThumb by produceState<ImageBitmap?>(
                initialValue = null,
                resolvedPath,
                media.localUri,
                media.thumbnailB64,
            ) {
                value = withContext(Dispatchers.IO) {
                    decodeVideoPosterBitmap(context, resolvedPath, media.localUri, media.thumbnailB64)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    onClick = onOpenVideo,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = extras.terminalBg,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (videoThumb != null) {
                            Image(
                                bitmap = videoThumb!!,
                                contentDescription = media.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.58f),
                            modifier = Modifier.size(48.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "视频预览",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        if (media.durationMs > 0L) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp),
                            ) {
                                Text(
                                    text = formatDuration(media.durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = media.name.ifBlank { "视频" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (media.sizeBytes > 0) {
                        Text(
                            text = formatByteSize(media.sizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        else -> {
            val context = LocalContext.current
            val audioPath = resolvedPath ?: media.localUri?.takeIf { it.startsWith("/") }
            val audioUri = media.localUri?.takeIf { it.startsWith("content://") }?.let { Uri.parse(it) }
            val hasSource = !audioPath.isNullOrBlank() || audioUri != null
            var isPlaying by remember(media.mediaId) { mutableStateOf(false) }
            var pendingPlay by remember(media.mediaId) { mutableStateOf(false) }
            var totalMs by remember(media.mediaId) { mutableStateOf(media.durationMs) }
            var positionMs by remember(media.mediaId) { mutableStateOf(0L) }
            var player by remember(media.mediaId) { mutableStateOf<MediaPlayer?>(null) }

            DisposableEffect(audioPath, audioUri) {
                var created: MediaPlayer? = null
                if (hasSource) {
                    runCatching {
                        val mp = MediaPlayer().apply {
                            when {
                                !audioPath.isNullOrBlank() -> setDataSource(audioPath)
                                audioUri != null -> setDataSource(context, audioUri)
                            }
                            setOnPreparedListener { ready ->
                                val d = ready.duration.toLong()
                                if (d > 0L) totalMs = d
                                if (pendingPlay) {
                                    pendingPlay = false
                                    isPlaying = true
                                    runCatching { ready.start() }
                                }
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                positionMs = 0L
                            }
                            setOnErrorListener { _, _, _ ->
                                isPlaying = false
                                pendingPlay = false
                                true
                            }
                            prepareAsync()
                        }
                        created = mp
                        player = mp
                    }.onFailure {
                        pendingPlay = false
                        isPlaying = false
                    }
                }
                onDispose {
                    isPlaying = false
                    player = null
                    runCatching {
                        created?.stop()
                    }
                    runCatching {
                        created?.release()
                    }
                }
            }

            LaunchedEffect(isPlaying, player) {
                while (isPlaying) {
                    delay(150L)
                    val mp = player ?: break
                    val active = runCatching { mp.isPlaying }.getOrDefault(false)
                    val cur = runCatching { mp.currentPosition.toLong() }.getOrDefault(positionMs)
                    positionMs = cur
                    if (!active && cur >= totalMs && totalMs > 0L) {
                        isPlaying = false
                    }
                }
            }

            val progress = if (totalMs > 0L) (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (!hasSource) {
                                    pendingPlay = true
                                    onAutoFetch()
                                    return@clickable
                                }
                                val mp = player ?: return@clickable
                                if (isPlaying) {
                                    runCatching { mp.pause() }
                                    isPlaying = false
                                } else {
                                    runCatching { mp.start() }
                                    isPlaying = true
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (pendingPlay && !hasSource) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "暂停音频" else "播放音频",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = media.name.ifBlank { "语音" },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AudioWaveformBars(
                            accent = MaterialTheme.colorScheme.primary,
                            track = MaterialTheme.colorScheme.surfaceContainerHighest,
                            active = isPlaying,
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp),
                        )
                    }
                    Text(
                        text = when {
                            isPlaying && totalMs > 0L -> "${formatDuration(positionMs)} / ${formatDuration(totalMs)}"
                            totalMs > 0L -> formatDuration(totalMs)
                            else -> formatByteSize(media.sizeBytes)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioWaveformBars(
    accent: Color,
    track: Color,
    active: Boolean,
    progress: Float = 0.5f,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val heights = floatArrayOf(0.35f, 0.7f, 0.5f, 0.95f, 0.6f, 0.8f, 0.4f, 0.9f, 0.55f, 0.75f, 0.45f, 0.65f, 0.35f)
        val count = heights.size
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2.dp.toPx())
        heights.forEachIndexed { idx, ratio ->
            val h = size.height * ratio
            val x = idx * (barWidth + gap)
            val y = (size.height - h) / 2f
            val threshold = (idx + 1).toFloat() / count
            val barColor = if (active) {
                if (threshold <= progress + 0.04f) accent else track
            } else {
                if (progress > 0.01f && threshold <= progress + 0.04f) accent else track
            }
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
private fun VideoPlayerDialog(
    media: UiPart.Media,
    resolvedPath: String?,
    isLoading: Boolean,
    isDemo: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val localPath = resolvedPath ?: media.localUri?.takeIf { it.startsWith("/") }
    val contentUri = media.localUri?.takeIf { it.startsWith("content://") }?.let { Uri.parse(it) }
    val hasPlayableSource = !localPath.isNullOrBlank() || contentUri != null
    var isPrepared by remember(localPath, contentUri) { mutableStateOf(false) }
    var playbackError by remember(localPath, contentUri) { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val posterBmp by produceState<ImageBitmap?>(
        initialValue = null,
        localPath,
        media.localUri,
        media.thumbnailB64,
    ) {
        value = withContext(Dispatchers.IO) {
            decodeVideoPosterBitmap(context, localPath, media.localUri, media.thumbnailB64)
        }
    }

    DisposableEffect(videoViewRef) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 72.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (hasPlayableSource && !playbackError) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setOnPreparedListener { mp ->
                                    isPrepared = true
                                    mp.isLooping = false
                                    runCatching { start() }
                                }
                                setOnErrorListener { _, _, _ ->
                                    playbackError = true
                                    true
                                }
                                runCatching {
                                    when {
                                        !localPath.isNullOrBlank() -> setVideoPath(localPath)
                                        contentUri != null -> setVideoURI(contentUri)
                                    }
                                }
                                videoViewRef = this
                            }
                        },
                        update = { view ->
                            videoViewRef = view
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }

                if (!hasPlayableSource || !isPrepared || playbackError) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (posterBmp != null) {
                            Image(
                                bitmap = posterBmp!!,
                                contentDescription = media.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 20.dp),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(32.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Videocam,
                                    contentDescription = media.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp),
                                )
                            }
                            Text(
                                text = when {
                                    playbackError -> "暂无法解码该视频格式"
                                    isLoading -> "正在加密拉取视频…"
                                    hasPlayableSource -> "正在缓冲视频…"
                                    isDemo -> "演示视频预览（真机连接或本地选取视频可直接播放）"
                                    else -> "等待从主机加密拉取视频…"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = media.name.ifBlank { "视频播放" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = listOfNotNull(
                        media.mimeType.ifBlank { null },
                        if (media.durationMs > 0) formatDuration(media.durationMs) else null,
                        if (media.sizeBytes > 0) formatByteSize(media.sizeBytes) else null,
                        if (hasPlayableSource && !playbackError) "已解密播放" else null,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭视频", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HandoverBottomSheet(
    sourceTool: String,
    sourceSessionId: String,
    availableTools: List<ToolInfo>,
    otherSessions: List<SessionEntity>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (targetTool: String, targetSid: String?, note: String, includeContext: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectableTools = remember(availableTools, otherSessions) {
        if (availableTools.isNotEmpty()) availableTools
        else otherSessions.map { it.tool }.distinct().map { ToolInfo(name = it, structured_history = true) }
    }
    var selectedTool by remember(sourceTool, selectableTools) {
        mutableStateOf(
            selectableTools.firstOrNull { !it.name.equals(sourceTool, ignoreCase = true) }?.name
                ?: selectableTools.firstOrNull()?.name.orEmpty(),
        )
    }
    var selectedTargetSid by remember { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var includeContext by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("移交给同机其他 Agent", style = MaterialTheme.typography.titleLarge)
                val srcDesc = listOfNotNull(
                    ToolVisuals.label(sourceTool),
                    sourceSessionId.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                Text(
                    text = "来源：$srcDesc（默认自动向目标 Agent 传入来源 Agent、Session ID 与工作区）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (selectableTools.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "目标 Agent（新建同目录会话）",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(selectableTools, key = { it.name }) { t ->
                            val isSelected = selectedTargetSid == null && selectedTool.equals(t.name, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTargetSid = null
                                    selectedTool = t.name
                                },
                                label = { Text(ToolVisuals.label(t.name)) },
                                leadingIcon = { ToolIcon(tool = t.name, size = 18.dp, contentDescription = null) },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            )
                        }
                    }
                }
            }

            if (otherSessions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "或注入到同机运行中会话",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(otherSessions, key = { it.sid }) { sess ->
                            val isSelected = selectedTargetSid == sess.sid
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        selectedTargetSid = null
                                    } else {
                                        selectedTargetSid = sess.sid
                                        selectedTool = sess.tool
                                    }
                                },
                                label = {
                                    val desc = sess.title?.take(12) ?: sess.sid.take(8)
                                    Text("${ToolVisuals.label(sess.tool)} · $desc")
                                },
                                leadingIcon = { ToolIcon(tool = sess.tool, size = 18.dp, contentDescription = null) },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("移交内容与指令") },
                placeholder = { Text("说明当前已完成工作、关键约束、待办事项或需要继续处理的文件…") },
                minLines = 2,
                maxLines = 5,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { includeContext = !includeContext },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("附带最近对话 / 终端上下文（可选）", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "默认关闭；开启后会额外提取最近对话或终端输出一并发送",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = includeContext,
                    onCheckedChange = { includeContext = it },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("取消")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSubmit(selectedTool, selectedTargetSid, note.trim(), includeContext) },
                    enabled = !busy && selectedTool.isNotBlank() && (note.isNotBlank() || includeContext),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("确认移交")
                }
            }
        }
    }
}

@Composable
private fun MediaLightboxDialog(
    media: UiPart.Media,
    resolvedPath: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }
    val fullBmp by produceState<ImageBitmap?>(
        initialValue = null,
        resolvedPath,
        media.localUri,
        media.thumbnailB64,
    ) {
        value = withContext(Dispatchers.IO) {
            decodeMediaBitmap(resolvedPath, media.localUri, media.thumbnailB64, maxDim = 2048)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 72.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(state = transformState),
                contentAlignment = Alignment.Center,
            ) {
                if (fullBmp != null) {
                    Image(
                        bitmap = fullBmp!!,
                        contentDescription = media.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = media.name,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(72.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = media.name.ifBlank { "图片预览" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = listOfNotNull(
                        media.mimeType.ifBlank { null },
                        if (media.width > 0 && media.height > 0) "${media.width}×${media.height}" else null,
                        if (media.sizeBytes > 0) formatByteSize(media.sizeBytes) else null,
                        if (resolvedPath != null) "原图已解密" else if (isLoading) "正在拉取原图…" else "双指缩放",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(20.dp)
                        .size(22.dp),
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭预览", tint = Color.White)
            }
        }
    }
}

private fun decodeMediaBitmap(
    localPath: String?,
    localUri: String?,
    thumbnailB64: String?,
    maxDim: Int,
): ImageBitmap? {
    val filePath = localPath ?: localUri?.takeIf { it.startsWith("/") }
    if (!filePath.isNullOrBlank()) {
        runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, opts)
            var sample = 1
            while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(filePath, decodeOpts)?.asImageBitmap()
        }.getOrNull()?.let { return it }
    }
    if (!thumbnailB64.isNullOrBlank()) {
        return runCatching {
            val raw = Base64.getDecoder().decode(thumbnailB64)
            BitmapFactory.decodeByteArray(raw, 0, raw.size)?.asImageBitmap()
        }.getOrNull()
    }
    return null
}

private fun decodeVideoPosterBitmap(
    context: Context,
    localPath: String?,
    localUri: String?,
    thumbnailB64: String?,
): ImageBitmap? {
    decodeMediaBitmap(localPath = null, localUri = null, thumbnailB64 = thumbnailB64, maxDim = 960)?.let { return it }
    val path = localPath ?: localUri?.takeIf { it.startsWith("/") }
    if (!path.isNullOrBlank()) {
        runCatching {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(path)
                r.getFrameAtTime(0)?.asImageBitmap()
            } finally {
                r.release()
            }
        }.getOrNull()?.let { return it }
    }
    if (!localUri.isNullOrBlank() && localUri.startsWith("content://")) {
        runCatching {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(context, Uri.parse(localUri))
                r.getFrameAtTime(0)?.asImageBitmap()
            } finally {
                r.release()
            }
        }.getOrNull()?.let { return it }
    }
    return null
}

private sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Code(val lang: String, val code: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MdBlock
    data object Divider : MdBlock
}

@Composable
private fun MarkdownContent(
    md: String,
    textColor: Color,
    onCopyCode: (String) -> Unit,
) {
    val blocks = remember(md) { splitMarkdownBlocks(md) }
    val extras = SidekickThemeExtras.colors
    val linkColor = MaterialTheme.colorScheme.secondary
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> {
                    val styled = remember(block.text, extras.codeBg, extras.codeText, linkColor) {
                        renderInlineAndHeadings(block.text, extras.codeBg, extras.codeText, linkColor)
                    }
                    SelectionContainer {
                        Text(
                            text = styled,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 26.sp,
                                letterSpacing = 0.1.sp,
                            ),
                            color = textColor,
                        )
                    }
                }
                is MdBlock.Code -> CodeBlockCard(
                    lang = block.lang,
                    code = block.code,
                    onCopy = { onCopyCode(block.code) },
                )
                is MdBlock.Quote -> {
                    val styledQuote = remember(block.text, extras.codeBg, extras.codeText, linkColor) {
                        renderInlineAndHeadings(block.text, extras.codeBg, extras.codeText, linkColor)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(vertical = 2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        SelectionContainer {
                            Text(
                                text = styledQuote,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                is MdBlock.Table -> MarkdownTableBlock(
                    table = block,
                    codeBg = extras.codeBg,
                    codeText = extras.codeText,
                    linkColor = linkColor,
                )
                MdBlock.Divider -> HairlineDivider()
            }
        }
    }
}

@Composable
private fun MarkdownTableBlock(
    table: MdBlock.Table,
    codeBg: Color,
    codeText: Color,
    linkColor: Color,
) {
    val extras = SidekickThemeExtras.colors
    val colCount = table.headers.size.coerceAtLeast(1)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, extras.hairline),
    ) {
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    table.headers.forEach { cell ->
                        Text(
                            text = remember(cell, codeBg, codeText, linkColor) {
                                renderInlineAndHeadings(cell, codeBg, codeText, linkColor)
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .width(156.dp)
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                        )
                    }
                }
                table.rows.forEach { row ->
                    HairlineDivider()
                    Row {
                        for (c in 0 until colCount) {
                            val cellText = row.getOrElse(c) { "" }
                            Text(
                                text = remember(cellText, codeBg, codeText, linkColor) {
                                    renderInlineAndHeadings(cellText, codeBg, codeText, linkColor)
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .width(156.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(
    lang: String,
    code: String,
    onCopy: () -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    var copiedRecently by remember { mutableStateOf(false) }
    LaunchedEffect(copiedRecently) {
        if (copiedRecently) {
            delay(1500)
            copiedRecently = false
        }
    }
    val highlighted = remember(code, lang, extras.isDark, extras.codeText) {
        highlightCode(code, lang, extras.isDark, extras.codeText)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = extras.codeBg,
        border = if (extras.isDark) null else BorderStroke(0.5.dp, extras.codeBorder),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extras.codeHeaderBg)
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = lang.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onCopy()
                            copiedRecently = true
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (copiedRecently) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = "复制代码",
                        tint = if (copiedRecently) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = if (copiedRecently) "已复制" else "复制",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (copiedRecently) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SelectionContainer {
                Text(
                    text = highlighted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }
}

private val CODE_KEYWORDS = setOf(
    "fun", "val", "var", "class", "interface", "object", "data", "sealed", "enum",
    "if", "else", "when", "for", "while", "do", "return", "break", "continue",
    "suspend", "inline", "override", "private", "internal", "public", "open",
    "import", "package", "try", "catch", "finally", "throw", "true", "false", "null",
    "func", "type", "struct", "chan", "go", "defer", "select", "range", "nil", "map",
    "const", "let", "async", "await", "export", "from", "function", "typeof", "undefined",
    "def", "lambda", "with", "yield", "None", "True", "False", "pass", "raise", "and", "or", "not",
    "fn", "impl", "trait", "pub", "mut", "match", "use", "mod", "local", "then", "end",
    "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "CREATE", "TABLE", "DELETE", "UPDATE",
)

private fun highlightCode(
    code: String,
    lang: String,
    isDark: Boolean,
    defaultColor: Color,
): AnnotatedString {
    val kwColor = if (isDark) Color(0xFFFF7B72) else Color(0xFFCF222E)
    val strColor = if (isDark) Color(0xFFA5D6FF) else Color(0xFF0A3069)
    val commentColor = if (isDark) Color(0xFF8B949E) else Color(0xFF57606A)
    val numColor = if (isDark) Color(0xFF79C0FF) else Color(0xFF0550AE)
    val annColor = if (isDark) Color(0xFFD2A8FF) else Color(0xFF6639BA)
    val l = lang.lowercase()
    val hashComment = l in setOf("py", "python", "sh", "bash", "zsh", "yaml", "yml", "toml", "rb", "ruby", "conf")
    val dashComment = l in setOf("sql", "lua")

    return buildAnnotatedString {
        var i = 0
        val n = code.length
        while (i < n) {
            val c = code[i]
            // Line comments
            if ((c == '/' && i + 1 < n && code[i + 1] == '/') ||
                (hashComment && c == '#') ||
                (dashComment && c == '-' && i + 1 < n && code[i + 1] == '-')
            ) {
                val end = code.indexOf('\n', i).let { if (it < 0) n else it }
                withStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) {
                    append(code.substring(i, end))
                }
                i = end
                continue
            }
            // String literals
            if (c == '"' || c == '\'' || c == '`') {
                var j = i + 1
                while (j < n) {
                    if (code[j] == '\\' && j + 1 < n) {
                        j += 2
                        continue
                    }
                    if (code[j] == c) {
                        j++
                        break
                    }
                    if (code[j] == '\n' && c != '`') break
                    j++
                }
                withStyle(SpanStyle(color = strColor)) { append(code.substring(i, j)) }
                i = j
                continue
            }
            // Annotations (@...)
            if (c == '@' && i + 1 < n && code[i + 1].isLetter()) {
                var j = i + 1
                while (j < n && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                withStyle(SpanStyle(color = annColor)) { append(code.substring(i, j)) }
                i = j
                continue
            }
            // Numbers
            if (c.isDigit() && (i == 0 || !code[i - 1].isLetterOrDigit())) {
                var j = i + 1
                while (j < n && (code[j].isLetterOrDigit() || code[j] == '_' || code[j] == '.')) j++
                withStyle(SpanStyle(color = numColor)) { append(code.substring(i, j)) }
                i = j
                continue
            }
            // Identifiers & keywords
            if (c.isLetter() || c == '_') {
                var j = i + 1
                while (j < n && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                val word = code.substring(i, j)
                when {
                    word in CODE_KEYWORDS -> withStyle(SpanStyle(color = kwColor, fontWeight = FontWeight.Medium)) {
                        append(word)
                    }
                    word.first().isUpperCase() -> withStyle(SpanStyle(color = numColor)) {
                        append(word)
                    }
                    else -> withStyle(SpanStyle(color = defaultColor)) {
                        append(word)
                    }
                }
                i = j
                continue
            }
            withStyle(SpanStyle(color = defaultColor)) { append(c) }
            i++
        }
    }
}

private fun parsePipeRow(line: String): List<String>? {
    val trimmed = line.trim()
    if (!trimmed.contains('|')) return null
    val core = trimmed.removePrefix("|").removeSuffix("|")
    val cells = core.split('|').map { it.trim() }
    return if (cells.size >= 2) cells else null
}

private fun isTableSeparatorRow(line: String, expectedCols: Int): Boolean {
    val cells = parsePipeRow(line) ?: return false
    if (cells.size != expectedCols) return false
    return cells.all { cell ->
        val s = cell.trim()
        s.isNotEmpty() && s.all { it == '-' || it == ':' } && s.contains('-')
    }
}

private fun splitMarkdownBlocks(md: String): List<MdBlock> {
    val result = mutableListOf<MdBlock>()
    val lines = md.lines()
    val textBuf = StringBuilder()
    val quoteBuf = StringBuilder()
    val codeBuf = StringBuilder()
    var inFence = false
    var fenceLang = ""

    fun flushText() {
        if (textBuf.isNotEmpty()) {
            result += MdBlock.Paragraph(textBuf.toString().trimEnd('\n'))
            textBuf.clear()
        }
    }

    fun flushQuote() {
        if (quoteBuf.isNotEmpty()) {
            result += MdBlock.Quote(quoteBuf.toString().trimEnd('\n'))
            quoteBuf.clear()
        }
    }

    var idx = 0
    while (idx < lines.size) {
        val line = lines[idx]
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("```") -> {
                flushQuote()
                if (!inFence) {
                    flushText()
                    inFence = true
                    fenceLang = trimmed.removePrefix("```").trim()
                    codeBuf.clear()
                } else {
                    result += MdBlock.Code(fenceLang, codeBuf.toString().trimEnd('\n'))
                    inFence = false
                    fenceLang = ""
                    codeBuf.clear()
                }
            }
            inFence -> {
                if (codeBuf.isNotEmpty()) codeBuf.append('\n')
                codeBuf.append(line)
            }
            trimmed.startsWith(">") -> {
                flushText()
                val quoteLine = trimmed.removePrefix(">").trimStart()
                if (quoteBuf.isNotEmpty()) quoteBuf.append('\n')
                quoteBuf.append(quoteLine)
            }
            else -> {
                flushQuote()
                val headers = parsePipeRow(trimmed)
                if (headers != null && idx + 1 < lines.size && isTableSeparatorRow(lines[idx + 1], headers.size)) {
                    flushText()
                    val rows = mutableListOf<List<String>>()
                    idx += 2
                    while (idx < lines.size) {
                        val row = parsePipeRow(lines[idx]) ?: break
                        rows += row
                        idx++
                    }
                    result += MdBlock.Table(headers, rows)
                    continue
                }
                when {
                    trimmed == "---" || trimmed == "***" -> {
                        flushText()
                        result += MdBlock.Divider
                    }
                    else -> {
                        if (textBuf.isNotEmpty()) textBuf.append('\n')
                        textBuf.append(line)
                    }
                }
            }
        }
        idx++
    }
    flushQuote()
    if (inFence && codeBuf.isNotEmpty()) {
        result += MdBlock.Code(fenceLang, codeBuf.toString())
    } else {
        flushText()
    }
    return result
}

private val ORDERED_LIST_REGEX = Regex("^(\\d+)\\.\\s+(.*)$")

private fun renderInlineAndHeadings(
    md: String,
    inlineCodeBg: Color,
    inlineCodeFg: Color,
    linkColor: Color,
): AnnotatedString =
    buildAnnotatedString {
        val lines = md.lines()
        lines.forEachIndexed { idx, line ->
            val trimmed = line.trimStart()
            val indent = line.takeWhile { it == ' ' }
            when {
                trimmed.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp)) {
                        appendInlineMarkdown(trimmed.removePrefix("### "), inlineCodeBg, inlineCodeFg, linkColor)
                    }
                }
                trimmed.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp)) {
                        appendInlineMarkdown(trimmed.removePrefix("## "), inlineCodeBg, inlineCodeFg, linkColor)
                    }
                }
                trimmed.startsWith("# ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 23.sp)) {
                        appendInlineMarkdown(trimmed.removePrefix("# "), inlineCodeBg, inlineCodeFg, linkColor)
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    append(indent)
                    append("• ")
                    appendInlineMarkdown(trimmed.drop(2), inlineCodeBg, inlineCodeFg, linkColor)
                }
                ORDERED_LIST_REGEX.matches(trimmed) -> {
                    val match = ORDERED_LIST_REGEX.matchEntire(trimmed)!!
                    append(indent)
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)) {
                        append("${match.groupValues[1]}. ")
                    }
                    appendInlineMarkdown(match.groupValues[2], inlineCodeBg, inlineCodeFg, linkColor)
                }
                else -> appendInlineMarkdown(line, inlineCodeBg, inlineCodeFg, linkColor)
            }
            if (idx < lines.lastIndex) append('\n')
        }
    }

private fun AnnotatedString.Builder.appendInlineMarkdown(
    line: String,
    codeBg: Color,
    codeFg: Color,
    linkColor: Color,
) {
    var i = 0
    while (i < line.length) {
        if (line[i] == '[' ) {
            val closeBracket = line.indexOf(']', i + 1)
            if (closeBracket > i + 1 && closeBracket + 1 < line.length && line[closeBracket + 1] == '(') {
                val closeParen = line.indexOf(')', closeBracket + 2)
                if (closeParen > closeBracket + 2) {
                    val label = line.substring(i + 1, closeBracket)
                    val url = line.substring(closeBracket + 2, closeParen).trim()
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        withLink(
                            LinkAnnotation.Url(
                                url = url,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                ),
                            ),
                        ) {
                            append(label)
                        }
                        i = closeParen + 1
                        continue
                    }
                }
            }
        }
        if (line.startsWith("***", i)) {
            val end = line.indexOf("***", i + 3)
            if (end > i + 3) {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic)) {
                    append(line.substring(i + 3, end))
                }
                i = end + 3
                continue
            }
        }
        if (line.startsWith("**", i)) {
            val end = line.indexOf("**", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(line.substring(i + 2, end)) }
                i = end + 2
                continue
            }
        }
        if (line.startsWith("~~", i)) {
            val end = line.indexOf("~~", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(line.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        if (line[i] == '*' && (i + 1 >= line.length || line[i + 1] != ' ')) {
            val end = line.indexOf('*', i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(line.substring(i + 1, end)) }
                i = end + 1
                continue
            }
        }
        if (line[i] == '`') {
            val end = line.indexOf('`', i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, background = codeBg, color = codeFg)) {
                    append(" ")
                    append(line.substring(i + 1, end))
                    append(" ")
                }
                i = end + 1
                continue
            }
        }
        append(line[i])
        i++
    }
}

private fun terminalPlainText(lines: List<AnsiLine>): String =
    lines.joinToString("\n") { l -> l.spans.joinToString("") { it.text } }

@Composable
private fun TerminalView(
    lines: List<AnsiLine>,
    revision: Long,
    fontSizeSp: Int,
    softWrap: Boolean,
    modifier: Modifier = Modifier,
) {
    val extras = SidekickThemeExtras.colors

    // Chunk every 50 lines per remote-sync-android.md §6 to keep 2000-line scrollback smooth.
    val syncSmallChunks = remember(lines, revision, extras.terminalFg) {
        if (lines.size <= 200) buildTerminalChunks(lines, extras.terminalFg) else null
    }
    val asyncLargeChunks by produceState<List<AnnotatedString>>(
        initialValue = emptyList(),
        lines,
        revision,
        extras.terminalFg,
    ) {
        if (lines.size > 200) {
            value = withContext(Dispatchers.Default) { buildTerminalChunks(lines, extras.terminalFg) }
        }
    }
    val chunks = syncSmallChunks ?: asyncLargeChunks
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showJumpToBottom by remember {
        derivedStateOf { listState.canScrollForward }
    }

    LaunchedEffect(revision, chunks.size) {
        if (chunks.isNotEmpty()) {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val nearBottom = info.totalItemsCount <= 1 || lastVisible >= info.totalItemsCount - 2
            if (nearBottom) {
                listState.scrollToItem(chunks.lastIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (extras.isDark) 0.dp else 12.dp, vertical = if (extras.isDark) 0.dp else 4.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = if (extras.isDark) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
            color = extras.terminalBg,
            border = if (extras.isDark) null else BorderStroke(1.dp, extras.terminalBorder),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (chunks.isEmpty()) {
                    Text(
                        text = "等待 PTY 屏幕输出…",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = extras.terminalFg.copy(alpha = 0.65f),
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    val hScrollModifier = if (softWrap) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState())
                            .widthIn(min = maxWidth)
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        modifier = hScrollModifier,
                    ) {
                        items(chunks.size, key = { idx -> idx }) { idx ->
                            Text(
                                text = chunks[idx],
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp + 4).sp,
                                softWrap = softWrap,
                            )
                        }
                    }
                }
                if (showJumpToBottom && chunks.isNotEmpty()) {
                    ScrollToBottomButton(
                        onClick = { scope.launch { listState.scrollToItem(chunks.lastIndex) } },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                    )
                }
            }
        }
    }
}

private fun buildTerminalChunks(lines: List<AnsiLine>, defaultFg: Color): List<AnnotatedString> =
    lines.chunked(50).map { chunkLines ->
        buildAnnotatedString {
            chunkLines.forEachIndexed { idx, line ->
                line.spans.forEach { sp ->
                    withStyle(
                        SpanStyle(
                            color = sp.style.fgArgb?.let { Color(it) } ?: defaultFg,
                            background = sp.style.bgArgb?.let { Color(it) } ?: Color.Unspecified,
                            fontWeight = if (sp.style.bold) FontWeight.Bold else null,
                            fontStyle = if (sp.style.italic) FontStyle.Italic else null,
                            textDecoration = if (sp.style.underline) TextDecoration.Underline else null,
                        ),
                    ) {
                        append(sp.text)
                    }
                }
                if (idx < chunkLines.lastIndex) append('\n')
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionComposer(
    tool: String,
    localTyping: Boolean,
    defaultShowKeycaps: Boolean,
    dismissSignal: Int,
    pendingAttachments: List<PendingAttachment>,
    quotedMessage: QuotedMessage?,
    fileSuggestions: List<WorkspaceFileSuggestion>,
    slashCommands: List<SlashCommandSuggestion>,
    screenLines: List<AnsiLine>,
    onSearchFiles: (String) -> Unit,
    onClearQuote: () -> Unit,
    onAddAttachment: (PendingAttachment) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSendInput: (String, Boolean) -> Unit,
    onSendKey: (Key) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    var fieldState by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var autocompleteDismissed by rememberSaveable { mutableStateOf(false) }
    val text = fieldState.text
    val slashQuery = remember(text) { extractLeadingSlashQuery(text) }
    val atMatch = remember(text, slashQuery) {
        if (slashQuery == null) Regex("(?:^|\\s)@([^\\s]*)$").find(text) else null
    }
    val atQuery = atMatch?.groupValues?.getOrNull(1)

    fun updateText(newText: String) {
        autocompleteDismissed = false
        fieldState = TextFieldValue(text = newText, selection = TextRange(newText.length))
    }

    var showKeycaps by rememberSaveable(defaultShowKeycaps) { mutableStateOf(defaultShowKeycaps) }
    var showMediaSheet by rememberSaveable { mutableStateOf(false) }
    var livePtySlashMode by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val extras = SidekickThemeExtras.colors

    LaunchedEffect(dismissSignal) {
        if (dismissSignal > 0) {
            livePtySlashMode = false
            autocompleteDismissed = true
        }
    }
    LaunchedEffect(atQuery) {
        if (atQuery != null) {
            onSearchFiles(atQuery)
        }
    }
    LaunchedEffect(slashQuery) {
        if (slashQuery == null && livePtySlashMode) {
            livePtySlashMode = false
        }
    }

    val filteredCommands = remember(slashCommands, slashQuery) {
        if (slashQuery == null) {
            emptyList()
        } else {
            val q = slashQuery.trim().lowercase()
            if (q.isEmpty()) {
                slashCommands
            } else {
                val prefix = slashCommands.filter { it.name.lowercase().startsWith(q) }
                val rest = slashCommands.filter {
                    !it.name.lowercase().startsWith(q) &&
                        (it.name.lowercase().contains(q) || it.description.lowercase().contains(q))
                }
                prefix + rest
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val att = withContext(Dispatchers.IO) { resolvePendingAttachment(context, uri, MediaKind.IMAGE, "image/*") }
                onAddAttachment(att)
            }
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val att = withContext(Dispatchers.IO) { resolvePendingAttachment(context, uri, MediaKind.VIDEO, "video/*") }
                onAddAttachment(att)
            }
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val att = withContext(Dispatchers.IO) { resolvePendingAttachment(context, uri, MediaKind.AUDIO, "audio/*") }
                onAddAttachment(att)
            }
        }
    }

    fun dispatchSend() {
        val payload = formatMessageWithQuote(quotedMessage, text)
        when {
            payload.isNotEmpty() || pendingAttachments.isNotEmpty() -> {
                onSendInput(payload, true)
                updateText("")
                livePtySlashMode = false
                onClearQuote()
            }
            else -> onSendKey(Key.KEY_ENTER)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(visible = localTyping) {
            Surface(
                color = extras.warningContainer,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = extras.onWarningContainer,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "本地正在输入…",
                        style = MaterialTheme.typography.labelSmall,
                        color = extras.onWarningContainer,
                    )
                }
            }
        }

        AnimatedVisibility(visible = !autocompleteDismissed && (slashQuery != null || livePtySlashMode)) {
            SlashCommandAutocompleteSheet(
                tool = tool,
                commands = filteredCommands,
                livePtyMode = livePtySlashMode,
                screenLines = screenLines,
                onToggleLivePty = {
                    val next = !livePtySlashMode
                    livePtySlashMode = next
                    if (next) {
                        onSendInput("/", false)
                    } else {
                        onSendKey(Key.KEY_ESCAPE)
                    }
                },
                onSelectCommand = { cmd ->
                    livePtySlashMode = false
                    updateText(if (cmd.argsHint.isNotBlank()) "/${cmd.name} " else "/${cmd.name}")
                },
                onSendKey = onSendKey,
            )
        }

        AnimatedVisibility(visible = !autocompleteDismissed && slashQuery == null && !livePtySlashMode && atQuery != null && fileSuggestions.isNotEmpty()) {
            FileMentionAutocompleteSheet(
                files = fileSuggestions,
                onSelectFile = { file ->
                    val range = atMatch?.range
                    if (range != null) {
                        val leadingSpace = if (text.substring(range).startsWith(" ")) " " else ""
                        updateText(text.replaceRange(range, "$leadingSpace@${file.relPath} "))
                    } else {
                        updateText("$text@${file.relPath} ")
                    }
                },
            )
        }

        AnimatedVisibility(visible = quotedMessage != null) {
            quotedMessage?.let { quote ->
                QuotedMessageBanner(
                    quote = quote,
                    onClear = onClearQuote,
                )
            }
        }

        AnimatedVisibility(visible = pendingAttachments.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(pendingAttachments, key = { it.id }) { att ->
                    PendingAttachmentChip(att = att, onRemove = { onRemoveAttachment(att.id) })
                }
            }
        }

        AnimatedVisibility(visible = showKeycaps) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val chips = listOf(
                    "⏎" to Key.KEY_ENTER,
                    "Esc" to Key.KEY_ESCAPE,
                    "Tab" to Key.KEY_TAB,
                    "S-Tab" to Key.KEY_SHIFT_TAB,
                    "⌫" to Key.KEY_BACKSPACE,
                    "↑" to Key.KEY_UP,
                    "↓" to Key.KEY_DOWN,
                    "←" to Key.KEY_LEFT,
                    "→" to Key.KEY_RIGHT,
                    "Ctrl-C" to Key.KEY_CTRL_C,
                    "Ctrl-D" to Key.KEY_CTRL_D,
                )
                chips.forEach { (label, key) ->
                    KeycapChip(
                        label = label,
                        danger = key == Key.KEY_CTRL_C,
                        onClick = { onSendKey(key) },
                    )
                }
                KeycapChip(
                    label = "粘贴",
                    danger = false,
                    onClick = {
                        val clip = clipboard.getText()?.text
                        if (!clip.isNullOrEmpty()) updateText(text + clip)
                    },
                )
            }
        }

        // Unified floating composer capsule
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.5.dp, extras.hairline),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = fieldState,
                    onValueChange = { next ->
                        autocompleteDismissed = false
                        fieldState = next
                        val nextSlash = extractLeadingSlashQuery(next.text)
                        val nextAt = if (nextSlash == null) {
                            Regex("(?:^|\\s)@([^\\s]*)$").find(next.text)?.groupValues?.getOrNull(1)
                        } else {
                            null
                        }
                        if (nextAt != null && nextAt != atQuery) {
                            onSearchFiles(nextAt)
                        }
                    },
                    placeholder = {
                        Text(
                            text = "输入指令…",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp),
                    singleLine = false,
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { dispatchSend() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .onFocusChanged {
                            if (it.isFocused) autocompleteDismissed = false
                            onFocusChanged(it.isFocused)
                        },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { showMediaSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "添加附件",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            updateText(
                                when {
                                    text.isEmpty() || text.last().isWhitespace() -> "$text@"
                                    else -> "$text @"
                                },
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AlternateEmail,
                            contentDescription = "引用文件",
                            tint = if (atQuery != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            updateText(if (slashQuery != null) "" else "/")
                        },
                        modifier = Modifier.semantics { contentDescription = "斜杠命令" },
                    ) {
                        Text(
                            text = "/",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (slashQuery != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { showKeycaps = !showKeycaps }) {
                        Icon(
                            imageVector = Icons.Rounded.Keyboard,
                            contentDescription = "快捷键栏",
                            tint = if (showKeycaps) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(1f))

                    val hasPayload = text.isNotEmpty() || quotedMessage != null || pendingAttachments.isNotEmpty()
                    val sendBg = if (hasPayload) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val sendFg = if (hasPayload) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = ::dispatchSend),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = sendBg,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = "发送",
                                    tint = sendFg,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMediaSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showMediaSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text("添加到 ${ToolVisuals.label(tool)}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                HairlineDivider()
                MediaPickerTile(
                    icon = Icons.Rounded.Image,
                    title = "图片",
                    subtitle = "选择本地图片并端到端加密发送（≤ 16 MiB）",
                    onClick = {
                        showMediaSheet = false
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
                HairlineDivider(Modifier.padding(start = 34.dp))
                MediaPickerTile(
                    icon = Icons.Rounded.Videocam,
                    title = "视频",
                    subtitle = "选择本地视频并端到端加密发送（≤ 16 MiB）",
                    onClick = {
                        showMediaSheet = false
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                )
                HairlineDivider(Modifier.padding(start = 34.dp))
                MediaPickerTile(
                    icon = Icons.Rounded.AudioFile,
                    title = "音频",
                    subtitle = "选择本地音频并端到端加密发送（≤ 16 MiB）",
                    onClick = {
                        showMediaSheet = false
                        audioPicker.launch("audio/*")
                    },
                )
                HairlineDivider(Modifier.padding(start = 34.dp))
                MediaPickerTile(
                    icon = Icons.Rounded.ContentPaste,
                    title = "粘贴剪贴板",
                    subtitle = "插入当前系统剪贴板文本",
                    onClick = {
                        showMediaSheet = false
                        val clip = clipboard.getText()?.text
                        if (!clip.isNullOrEmpty()) updateText(text + clip)
                    },
                )
            }
        }
    }
}

@Composable
private fun PendingAttachmentChip(
    att: PendingAttachment,
    onRemove: () -> Unit,
) {
    val icon = when (att.kind) {
        MediaKind.IMAGE -> Icons.Rounded.Image
        MediaKind.VIDEO -> Icons.Rounded.Videocam
        MediaKind.AUDIO -> Icons.Rounded.AudioFile
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = att.kind.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Column(
                modifier = Modifier
                    .widthIn(max = 140.dp)
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    text = att.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        if (att.sizeBytes > 0) append(formatByteSize(att.sizeBytes)).append(" · ")
                        append(if (att.usesBlobTier) "Blob" else "Noise")
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = "移除附件", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MediaPickerTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun KeycapChip(
    label: String,
    danger: Boolean,
    onClick: () -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (danger) extras.keycapDangerBg else extras.keycapBg,
            border = BorderStroke(
                width = 1.dp,
                color = if (danger) MaterialTheme.colorScheme.error.copy(alpha = 0.45f) else extras.keycapBorder,
            ),
        ) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 30.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (danger) extras.keycapDangerText else extras.keycapText,
                )
            }
        }
    }
}

private fun extractLeadingSlashQuery(text: String): String? {
    if (text.contains('\n') || !text.startsWith("/")) return null
    val rest = text.removePrefix("/")
    return if (rest.none { it.isWhitespace() }) rest else null
}

private fun formatMessageWithQuote(quote: QuotedMessage?, text: String): String {
    val body = text.trim()
    if (quote == null) return text
    val line = "> [${quote.author}]: ${quote.excerpt.replace('\n', ' ').trim()}"
    return if (body.isEmpty()) line else "$line\n\n$text"
}

@Composable
private fun QuotedMessageBanner(
    quote: QuotedMessage,
    onClear: () -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, extras.hairline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "引用 ${quote.author}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = quote.excerpt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "取消引用",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SlashCommandAutocompleteSheet(
    tool: String,
    commands: List<SlashCommandSuggestion>,
    livePtyMode: Boolean,
    screenLines: List<AnsiLine>,
    onToggleLivePty: () -> Unit,
    onSelectCommand: (SlashCommandSuggestion) -> Unit,
    onSendKey: (Key) -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, extras.hairline),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = if (livePtyMode) "终端实时菜单 · ${ToolVisuals.label(tool)}"
                        else "命令 · ${ToolVisuals.label(tool)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onToggleLivePty,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(
                        text = if (livePtyMode) "返回列表" else "终端实时 >_",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            HairlineDivider()
            if (livePtyMode) {
                val tailLines = remember(screenLines) {
                    screenLines
                        .map { l -> l.spans.joinToString("") { it.text }.trimEnd() }
                        .filter { it.isNotBlank() }
                        .takeLast(8)
                }
                Surface(
                    color = extras.terminalBg,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (tailLines.isEmpty()) "已向终端输入 /，等待 PTY 补全菜单输出…"
                        else tailLines.joinToString("\n"),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = extras.terminalFg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf(
                        "↑" to Key.KEY_UP,
                        "↓" to Key.KEY_DOWN,
                        "Tab" to Key.KEY_TAB,
                        "⏎" to Key.KEY_ENTER,
                        "Esc" to Key.KEY_ESCAPE,
                    ).forEach { (label, key) ->
                        KeycapChip(label = label, danger = false, onClick = { onSendKey(key) })
                    }
                }
            } else if (commands.isEmpty()) {
                Text(
                    text = "没有匹配的命令，可点右上角「终端实时 >_」直连 CLI 菜单",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp),
                ) {
                    itemsIndexed(commands, key = { _, c -> "${c.source}_${c.name}" }) { idx, cmd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { onSelectCommand(cmd) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = "/${cmd.name}",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (cmd.argsHint.isNotBlank()) {
                                        Text(
                                            text = cmd.argsHint,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                if (cmd.description.isNotBlank()) {
                                    Text(
                                        text = cmd.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Text(
                                text = cmd.source.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (idx < commands.lastIndex) {
                            HairlineDivider(Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileMentionAutocompleteSheet(
    files: List<WorkspaceFileSuggestion>,
    onSelectFile: (WorkspaceFileSuggestion) -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, extras.hairline),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "引用工作区文件",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            HairlineDivider()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp),
            ) {
                itemsIndexed(files, key = { _, f -> f.relPath }) { idx, file ->
                    val name = file.relPath.substringAfterLast('/')
                    val parent = file.relPath.substringBeforeLast('/', "")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onSelectFile(file) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (parent.isNotBlank()) {
                                Text(
                                    text = parent,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (file.sizeBytes > 0L) {
                            Text(
                                text = formatByteSize(file.sizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (idx < files.lastIndex) {
                        HairlineDivider(Modifier.padding(start = 40.dp))
                    }
                }
            }
        }
    }
}

private fun resolvePendingAttachment(
    context: Context,
    uri: Uri,
    kind: MediaKind,
    defaultMime: String,
): PendingAttachment {
    var name = uri.lastPathSegment ?: "attachment"
    var size = 0L
    val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull() ?: defaultMime
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) cursor.getString(nameIdx)?.let { if (it.isNotBlank()) name = it }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }
    }
    return PendingAttachment(
        id = UUID.randomUUID().toString(),
        uri = uri.toString(),
        name = name,
        mimeType = mime,
        sizeBytes = size,
        kind = kind,
    )
}

private fun formatByteSize(bytes: Long): String = when {
    bytes <= 0L -> "0 B"
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KiB"
    else -> String.format(Locale.US, "%.1f MiB", bytes / (1024.0 * 1024.0))
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", min, sec)
}
