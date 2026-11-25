package dev.ddgrcf.sidekick.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.WrapText
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ddgrcf.sidekick.core.ansi.AnsiLine
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.data.UiPart
import dev.ddgrcf.sidekick.proto.v1.Key
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras
import dev.ddgrcf.sidekick.vm.MediaKind
import dev.ddgrcf.sidekick.vm.PendingAttachment
import dev.ddgrcf.sidekick.vm.SessionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        toast = toast,
        onClearToast = vm::clearToast,
        onBack = onBack,
        onSendInput = vm::sendWithAttachments,
        onSendKey = vm::sendKey,
        onFocusChanged = vm::onFocusChanged,
        onAddAttachment = vm::addAttachment,
        onRemoveAttachment = vm::removeAttachment,
        onRefreshMessages = vm::refreshMessages,
        onStopSession = { vm.stopSession(onBack) },
        onRequestMedia = vm::fetchMediaIfNeeded,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onAddAttachment: (PendingAttachment) -> Unit = {},
    onRemoveAttachment: (String) -> Unit = {},
    onRefreshMessages: () -> Unit = {},
    onStopSession: () -> Unit = {},
    onRequestMedia: (UiPart.Media) -> Unit = {},
) {
    var tabIdx by rememberSaveable(structuredHistory) { mutableIntStateOf(0) }
    var showOverflow by remember { mutableStateOf(false) }
    var confirmStop by rememberSaveable { mutableStateOf(false) }
    val lightboxJson = remember { Json { ignoreUnknownKeys = true } }
    var lightboxMedia by rememberSaveable(
        stateSaver = Saver<UiPart.Media?, String>(
            save = { m -> m?.let { lightboxJson.encodeToString(it) } ?: "" },
            restore = { raw -> if (raw.isBlank()) null else runCatching { lightboxJson.decodeFromString<UiPart.Media>(raw) }.getOrNull() },
        ),
    ) { mutableStateOf<UiPart.Media?>(null) }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ConnectionStatusPill(connectionState, compact = true)
                            Text(
                                text = buildString {
                                    append(ToolVisuals.label(tool))
                                    if (sessionId.isNotBlank()) {
                                        append(" · ")
                                        append(sessionId.take(14))
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isDemo) {
                                DemoTag()
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "更多操作")
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false },
                        ) {
                            if (structuredHistory) {
                                DropdownMenuItem(
                                    text = { Text("同步消息历史") },
                                    leadingIcon = { Icon(Icons.Rounded.Sync, contentDescription = null) },
                                    onClick = {
                                        showOverflow = false
                                        onRefreshMessages()
                                    },
                                )
                            }
                            if (sessionId.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("复制会话 ID") },
                                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        showOverflow = false
                                        clipboard.setText(AnnotatedString(sessionId))
                                        notify("已复制会话 ID")
                                    },
                                )
                            }
                            if (structuredHistory || sessionId.isNotBlank()) {
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
                                    showOverflow = false
                                    confirmStop = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            SessionComposer(
                tool = tool,
                localTyping = localTyping,
                defaultShowKeycaps = !structuredHistory || tabIdx == 1,
                pendingAttachments = pendingAttachments,
                onAddAttachment = onAddAttachment,
                onRemoveAttachment = onRemoveAttachment,
                onSendInput = onSendInput,
                onSendKey = onSendKey,
                onFocusChanged = onFocusChanged,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (structuredHistory) {
                PrimaryTabRow(
                    selectedTabIndex = tabIdx,
                    containerColor = MaterialTheme.colorScheme.background,
                    divider = { HairlineDivider() },
                ) {
                    Tab(selected = tabIdx == 0, onClick = { tabIdx = 0 }, text = { Text("Chat") })
                    Tab(selected = tabIdx == 1, onClick = { tabIdx = 1 }, text = { Text("Terminal") })
                }
            }
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
                    onAutoFetchMedia = onRequestMedia,
                    onOpenImage = { media ->
                        onRequestMedia(media)
                        lightboxMedia = media
                    },
                    onUnsupportedMediaClick = { notify("音视频播放将在后续版本支持") },
                    onSwitchToTerminal = { tabIdx = 1 },
                    modifier = Modifier.weight(1f),
                )
            } else {
                TerminalView(
                    lines = screenLines,
                    revision = screenRevision,
                    onCopyScreen = { txt ->
                        clipboard.setText(AnnotatedString(txt))
                        notify("已复制终端屏幕")
                    },
                    modifier = Modifier.weight(1f),
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
}

@Composable
private fun ChatView(
    tool: String,
    messages: List<MessageEntity>,
    decodeParts: (String) -> List<UiPart>,
    fetchedMediaPaths: Map<String, String>,
    onCopyText: (String, String) -> Unit,
    onAutoFetchMedia: (UiPart.Media) -> Unit,
    onOpenImage: (UiPart.Media) -> Unit,
    onUnsupportedMediaClick: () -> Unit,
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
                ToolIcon(tool = tool, size = 36.dp)
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(messages, key = { it.seq }) { msg ->
                val parts = remember(msg.partsJson) { decodeParts(msg.partsJson) }
                val isUser = msg.role.contains("USER", ignoreCase = true)
                if (isUser) {
                    UserPromptTurn(
                        parts = parts,
                        fetchedMediaPaths = fetchedMediaPaths,
                        onAutoFetchMedia = onAutoFetchMedia,
                        onOpenImage = onOpenImage,
                        onUnsupportedMediaClick = onUnsupportedMediaClick,
                    )
                } else {
                    AssistantTurn(
                        tool = tool,
                        model = msg.model,
                        parts = parts,
                        fetchedMediaPaths = fetchedMediaPaths,
                        onCopyText = onCopyText,
                        onAutoFetchMedia = onAutoFetchMedia,
                        onOpenImage = onOpenImage,
                        onUnsupportedMediaClick = onUnsupportedMediaClick,
                    )
                }
            }
        }

        if (showJumpToBottom) {
            SmallFloatingActionButton(
                onClick = { scope.launch { listState.scrollToItem(messages.lastIndex) } },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Rounded.ArrowDownward, contentDescription = "回到底部")
            }
        }
    }
}

@Composable
private fun UserPromptTurn(
    parts: List<UiPart>,
    fetchedMediaPaths: Map<String, String>,
    onAutoFetchMedia: (UiPart.Media) -> Unit,
    onOpenImage: (UiPart.Media) -> Unit,
    onUnsupportedMediaClick: () -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    val textParts = parts.filterIsInstance<UiPart.Text>()
    val mediaParts = parts.filterIsInstance<UiPart.Media>()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        mediaParts.forEach { media ->
            Box(modifier = Modifier.widthIn(max = 280.dp)) {
                MediaPartCard(
                    media = media,
                    resolvedPath = fetchedMediaPaths[media.mediaId],
                    onAutoFetch = { onAutoFetchMedia(media) },
                    onOpenImage = { onOpenImage(media) },
                    onUnsupportedMediaClick = onUnsupportedMediaClick,
                )
            }
        }
        if (textParts.isNotEmpty()) {
            Surface(
                modifier = Modifier.widthIn(max = 300.dp),
                shape = RoundedCornerShape(20.dp),
                color = extras.userBubble,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    textParts.forEach { tp ->
                        Text(
                            text = tp.text,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
                            color = extras.onUserBubble,
                        )
                    }
                }
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
    onAutoFetchMedia: (UiPart.Media) -> Unit,
    onOpenImage: (UiPart.Media) -> Unit,
    onUnsupportedMediaClick: () -> Unit,
) {
    val plainText = remember(parts) {
        parts.filterIsInstance<UiPart.Text>().joinToString("\n\n") { it.text }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ToolIcon(tool = tool, size = 18.dp)
            Text(
                text = model?.takeIf { it.isNotBlank() } ?: ToolVisuals.label(tool),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    onUnsupportedMediaClick = onUnsupportedMediaClick,
                )
            }
        }

        if (plainText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCopyText(plainText, "回复") },
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "复制回复",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
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
    onUnsupportedMediaClick: () -> Unit,
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
                    onClick = onUnsupportedMediaClick,
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
                            .clickable(onClick = onUnsupportedMediaClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "音频预览",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
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
                            active = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp),
                        )
                    }
                    Text(
                        text = if (media.durationMs > 0L) formatDuration(media.durationMs)
                        else formatByteSize(media.sizeBytes),
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
            val barColor = if (active || idx < count / 2) accent else track
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> {
                    val styled = remember(block.text, extras.codeBg, extras.codeText) {
                        renderInlineAndHeadings(block.text, extras.codeBg, extras.codeText)
                    }
                    Text(
                        text = styled,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp, lineHeight = 24.sp),
                        color = textColor,
                    )
                }
                is MdBlock.Code -> CodeBlockCard(
                    lang = block.lang,
                    code = block.code,
                    onCopy = { onCopyCode(block.code) },
                )
                is MdBlock.Quote -> {
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
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                MdBlock.Divider -> HairlineDivider()
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = extras.codeBg,
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
                        .clickable(onClick = onCopy)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "复制代码",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "复制",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = extras.codeText,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp),
            )
        }
    }
}

private fun splitMarkdownBlocks(md: String): List<MdBlock> {
    val result = mutableListOf<MdBlock>()
    val textBuf = StringBuilder()
    val codeBuf = StringBuilder()
    var inFence = false
    var fenceLang = ""

    fun flushText() {
        if (textBuf.isNotEmpty()) {
            result += MdBlock.Paragraph(textBuf.toString().trimEnd('\n'))
            textBuf.clear()
        }
    }

    md.lines().forEach { line ->
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("```") -> {
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
            trimmed == "---" || trimmed == "***" -> {
                flushText()
                result += MdBlock.Divider
            }
            trimmed.startsWith("> ") -> {
                flushText()
                result += MdBlock.Quote(trimmed.removePrefix("> ").trim())
            }
            else -> {
                if (textBuf.isNotEmpty()) textBuf.append('\n')
                textBuf.append(line)
            }
        }
    }
    if (inFence && codeBuf.isNotEmpty()) {
        result += MdBlock.Code(fenceLang, codeBuf.toString())
    } else {
        flushText()
    }
    return result
}

private fun renderInlineAndHeadings(md: String, inlineCodeBg: Color, inlineCodeFg: Color): AnnotatedString =
    buildAnnotatedString {
        val lines = md.lines()
        lines.forEachIndexed { idx, line ->
            when {
                line.startsWith("#") -> {
                    val content = line.trimStart('#', ' ')
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp)) {
                        append(content)
                    }
                }
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                    val indent = line.takeWhile { it == ' ' }
                    append(indent)
                    append("• ")
                    appendInlineMarkdown(line.trimStart().drop(2), inlineCodeBg, inlineCodeFg)
                }
                else -> appendInlineMarkdown(line, inlineCodeBg, inlineCodeFg)
            }
            if (idx < lines.lastIndex) append('\n')
        }
    }

private fun AnnotatedString.Builder.appendInlineMarkdown(line: String, codeBg: Color, codeFg: Color) {
    var i = 0
    while (i < line.length) {
        if (line.startsWith("**", i)) {
            val end = line.indexOf("**", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(line.substring(i + 2, end)) }
                i = end + 2
                continue
            }
        }
        if (line[i] == '`') {
            val end = line.indexOf('`', i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.5.sp, background = codeBg, color = codeFg)) {
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

@Composable
private fun TerminalView(
    lines: List<AnsiLine>,
    revision: Long,
    onCopyScreen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = SidekickThemeExtras.colors
    var fontSizeSp by rememberSaveable { mutableIntStateOf(12) }
    var softWrap by rememberSaveable { mutableStateOf(true) }

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

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ANSI · ${lines.size} 行",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { if (fontSizeSp > 10) fontSizeSp-- }) {
                Icon(Icons.Rounded.TextDecrease, contentDescription = "减小字号", modifier = Modifier.size(18.dp))
            }
            Text(
                text = "$fontSizeSp",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = { if (fontSizeSp < 16) fontSizeSp++ }) {
                Icon(Icons.Rounded.TextIncrease, contentDescription = "增大字号", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { softWrap = !softWrap },
                modifier = Modifier.semantics {
                    stateDescription = if (softWrap) "已开启自动换行" else "已关闭自动换行"
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.WrapText,
                    contentDescription = if (softWrap) "关闭自动换行" else "开启自动换行",
                    tint = if (softWrap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(
                onClick = {
                    val plain = lines.joinToString("\n") { l -> l.spans.joinToString("") { it.text } }
                    if (plain.isNotBlank()) onCopyScreen(plain)
                },
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "复制屏幕", modifier = Modifier.size(18.dp))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                        SmallFloatingActionButton(
                            onClick = { scope.launch { listState.scrollToItem(chunks.lastIndex) } },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "回到底部")
                        }
                    }
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
    pendingAttachments: List<PendingAttachment>,
    onAddAttachment: (PendingAttachment) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSendInput: (String, Boolean) -> Unit,
    onSendKey: (Key) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var showKeycaps by rememberSaveable(defaultShowKeycaps) { mutableStateOf(defaultShowKeycaps) }
    var showMediaSheet by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val extras = SidekickThemeExtras.colors

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
        when {
            text.isNotEmpty() || pendingAttachments.isNotEmpty() -> {
                onSendInput(text, true)
                text = ""
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
                        if (!clip.isNullOrEmpty()) text += clip
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
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            text = "输入指令…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
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
                        .onFocusChanged { onFocusChanged(it.isFocused) },
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
                    IconButton(onClick = { showKeycaps = !showKeycaps }) {
                        Icon(
                            imageVector = Icons.Rounded.Keyboard,
                            contentDescription = "快捷键栏",
                            tint = if (showKeycaps) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(1f))

                    val hasPayload = text.isNotEmpty() || pendingAttachments.isNotEmpty()
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
                    subtitle = "选择本地图片附带到当前消息（预览）",
                    onClick = {
                        showMediaSheet = false
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
                HairlineDivider(Modifier.padding(start = 34.dp))
                MediaPickerTile(
                    icon = Icons.Rounded.Videocam,
                    title = "视频",
                    subtitle = "选择本地视频附带到当前消息（预览）",
                    onClick = {
                        showMediaSheet = false
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                )
                HairlineDivider(Modifier.padding(start = 34.dp))
                MediaPickerTile(
                    icon = Icons.Rounded.AudioFile,
                    title = "音频",
                    subtitle = "选择本地音频附带到当前消息（预览）",
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
                        if (!clip.isNullOrEmpty()) text += clip
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
