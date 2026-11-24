package dev.ddgrcf.sidekick.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.WrapText
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
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
    pendingAttachments: List<PendingAttachment> = emptyList(),
    onAddAttachment: (PendingAttachment) -> Unit = {},
    onRemoveAttachment: (String) -> Unit = {},
    onRefreshMessages: () -> Unit = {},
    onStopSession: () -> Unit = {},
) {
    var tabIdx by remember(structuredHistory) { mutableIntStateOf(0) }
    var showOverflow by remember { mutableStateOf(false) }
    var confirmStop by remember { mutableStateOf(false) }
    var lightboxMedia by remember { mutableStateOf<UiPart.Media?>(null) }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ToolBadge(tool = tool, size = 32.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = ToolVisuals.label(tool),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        ConnectionStatusPill(connectionState, compact = true)
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
            SessionInputBar(
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
                SessionModeSwitcher(
                    selectedIndex = tabIdx,
                    onSelect = { tabIdx = it },
                )
            }
            if (structuredHistory && tabIdx == 0) {
                ChatView(
                    messages = messages,
                    decodeParts = decodeParts,
                    onCopyText = { txt, label ->
                        clipboard.setText(AnnotatedString(txt))
                        notify("已复制$label")
                    },
                    onOpenImage = { media -> lightboxMedia = media },
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
            onDismiss = { lightboxMedia = null },
        )
    }
}

@Composable
private fun SessionModeSwitcher(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, extras.cardBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ModePillItem(
                selected = selectedIndex == 0,
                icon = Icons.AutoMirrored.Rounded.Chat,
                label = "Chat",
                onClick = { onSelect(0) },
                modifier = Modifier.weight(1f),
            )
            ModePillItem(
                selected = selectedIndex == 1,
                icon = Icons.Rounded.Terminal,
                label = "Terminal",
                onClick = { onSelect(1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModePillItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatView(
    messages: List<MessageEntity>,
    decodeParts: (String) -> List<UiPart>,
    onCopyText: (String, String) -> Unit,
    onOpenImage: (UiPart.Media) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (messages.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateCard(
                icon = Icons.AutoMirrored.Rounded.Chat,
                title = "暂无对话消息",
                subtitle = "在下方输入指令或添加图片/音视频附件，或切换到 Terminal 查看实时画面",
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            messages.isEmpty() || lastVisible >= messages.lastIndex
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && atBottom) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages, key = { it.seq }) { msg ->
                val parts = remember(msg.partsJson) { decodeParts(msg.partsJson) }
                val isUser = msg.role.contains("USER", ignoreCase = true)
                ChatMessageBubble(
                    role = msg.role.removePrefix("ROLE_"),
                    model = msg.model,
                    createdMs = msg.createdMs,
                    isUser = isUser,
                    parts = parts,
                    onCopyText = onCopyText,
                    onOpenImage = onOpenImage,
                )
            }
        }

        if (!atBottom) {
            SmallFloatingActionButton(
                onClick = { scope.launch { listState.scrollToItem(messages.lastIndex) } },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
private fun ChatMessageBubble(
    role: String,
    model: String?,
    createdMs: Long,
    isUser: Boolean,
    parts: List<UiPart>,
    onCopyText: (String, String) -> Unit,
    onOpenImage: (UiPart.Media) -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp)
    }
    val bgColor = if (isUser) extras.userBubble else MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor = if (isUser) extras.userBubbleBorder else extras.cardBorder
    val fgColor = if (isUser) extras.onUserBubble else MaterialTheme.colorScheme.onSurface
    val plainText = remember(parts) {
        parts.filterIsInstance<UiPart.Text>().joinToString("\n\n") { it.text }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = if (isUser) Modifier.widthIn(max = 320.dp) else Modifier.fillMaxWidth(),
            shape = bubbleShape,
            color = bgColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = role,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (!model.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = model,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (createdMs > 0L) {
                        Text(
                            text = formatClockTime(createdMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!isUser && plainText.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { onCopyText(plainText, "消息") },
                            modifier = Modifier.size(22.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "复制消息",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }
                parts.forEach { p ->
                    when (p) {
                        is UiPart.Text -> MarkdownContent(
                            md = p.text,
                            textColor = fgColor,
                            onCopyCode = { code -> onCopyText(code, "代码") },
                        )
                        is UiPart.Thinking -> CollapsibleThinking(p.text)
                        is UiPart.ToolCall -> ExpandableToolCard(
                            title = "${p.name} (${p.status.removePrefix("TOOL_CALL_STATUS_")})",
                            body = p.inputJson,
                            isError = p.status.contains("ERROR", ignoreCase = true) ||
                                p.status.contains("FAILED", ignoreCase = true),
                            isResult = false,
                            onCopyBody = { onCopyText(it, "参数") },
                        )
                        is UiPart.ToolResult -> ExpandableToolCard(
                            title = if (p.isError) "工具执行出错" else "工具输出",
                            body = p.output,
                            isError = p.isError,
                            isResult = true,
                            onCopyBody = { onCopyText(it, "输出") },
                        )
                        is UiPart.Media -> MediaPartCard(
                            media = p,
                            onOpenImage = { onOpenImage(p) },
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
    onOpenImage: () -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    var playingAudio by remember(media.mediaId) { mutableStateOf(false) }

    when (media.kind.lowercase()) {
        "image" -> {
            Surface(
                onClick = onOpenImage,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, extras.cardBorder),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = media.name.ifBlank { "图片附件" },
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
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    MediaE2eBadge(tier = if (media.sizeBytes > 2L * 1024 * 1024) "Blob" else "E2E")
                    Icon(
                        imageVector = Icons.Rounded.ZoomIn,
                        contentDescription = "查看大图",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        "video" -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = extras.terminalBg,
                border = BorderStroke(1.dp, extras.cardBorder),
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .background(extras.terminalBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "播放视频",
                                    tint = MaterialTheme.colorScheme.onPrimary,
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
                                    .padding(8.dp),
                            ) {
                                Text(
                                    text = formatDuration(media.durationMs),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = media.name.ifBlank { "视频附件" },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (media.sizeBytes > 0) {
                            Text(
                                text = formatByteSize(media.sizeBytes),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MediaE2eBadge("加密 Blob")
                    }
                }
            }
        }
        else -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, extras.cardBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledIconButton(
                        onClick = { playingAudio = !playingAudio },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (playingAudio) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playingAudio) "暂停音频" else "播放音频",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = media.name.ifBlank { "音频消息" },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AudioWaveformBars(
                            accent = MaterialTheme.colorScheme.primary,
                            track = MaterialTheme.colorScheme.surfaceContainerHighest,
                            active = playingAudio,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp),
                        )
                    }
                    Text(
                        text = if (media.durationMs > 0L) formatDuration(media.durationMs)
                        else formatByteSize(media.sizeBytes),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MediaE2eBadge("E2E")
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
private fun MediaE2eBadge(tier: String) {
    val extras = SidekickThemeExtras.colors
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = extras.successContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = extras.onSuccessContainer,
                modifier = Modifier.size(10.dp),
            )
            Text(
                text = tier,
                style = MaterialTheme.typography.labelSmall,
                color = extras.onSuccessContainer,
            )
        }
    }
}

@Composable
private fun MediaLightboxDialog(
    media: UiPart.Media,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f)),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(state = transformState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF141A26),
                    border = BorderStroke(1.dp, Color(0xFF2B384F)),
                    modifier = Modifier.size(220.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = media.name,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(72.dp),
                        )
                    }
                }
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
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFF94A3B8),
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

@Composable
private fun CollapsibleThinking(text: String) {
    var expanded by remember { mutableStateOf(false) }
    val extras = SidekickThemeExtras.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, extras.cardBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(
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
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            AnimatedVisibility(visible = expanded && text.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableToolCard(
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
        else -> MaterialTheme.colorScheme.secondary
    }
    val icon = when {
        isError -> Icons.Rounded.ErrorOutline
        isResult -> Icons.Rounded.CheckCircle
        else -> Icons.Rounded.Build
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = extras.codeBg,
        border = BorderStroke(1.dp, extras.codeBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (body.isNotBlank()) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            AnimatedVisibility(visible = expanded && body.isNotBlank()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = extras.codeBorder)
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = body,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = extras.codeText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(end = 28.dp),
                        )
                        IconButton(
                            onClick = { onCopyBody(body) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "复制",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> {
                    val styled = remember(block.text, extras.codeBg, extras.codeText) {
                        renderInlineAndHeadings(block.text, extras.codeBg, extras.codeText)
                    }
                    Text(
                        text = styled,
                        style = MaterialTheme.typography.bodyMedium,
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
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                MdBlock.Divider -> HorizontalDivider(color = extras.cardBorder)
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
        shape = RoundedCornerShape(10.dp),
        color = extras.codeBg,
        border = BorderStroke(1.dp, extras.codeBorder),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extras.codeHeaderBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = lang.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(26.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "复制代码",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = extras.codeText,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp),
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
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
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
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(line.substring(i + 2, end)) }
                i = end + 2
                continue
            }
        }
        if (line[i] == '`') {
            val end = line.indexOf('`', i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg, color = codeFg)) {
                    append(line.substring(i + 1, end))
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
    var fontSizeSp by remember { mutableIntStateOf(12) }
    var softWrap by remember { mutableStateOf(true) }

    // Chunk every 50 lines per remote-sync-android.md §6 to keep 2000-line scrollback smooth.
    val chunks = remember(lines, revision) {
        lines.chunked(50).map { chunkLines ->
            buildAnnotatedString {
                chunkLines.forEachIndexed { idx, line ->
                    line.spans.forEach { sp ->
                        withStyle(
                            SpanStyle(
                                color = sp.style.fgArgb?.let { Color(it) } ?: extras.terminalFg,
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
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            chunks.isEmpty() || lastVisible >= chunks.lastIndex
        }
    }

    LaunchedEffect(revision, chunks.size) {
        if (chunks.isNotEmpty() && atBottom) {
            listState.scrollToItem(chunks.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Compact terminal controls bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ANSI · ${lines.size} 行",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { if (fontSizeSp > 10) fontSizeSp-- },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(Icons.Rounded.TextDecrease, contentDescription = "减小字号", modifier = Modifier.size(16.dp))
            }
            Text(
                text = "${fontSizeSp}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = { if (fontSizeSp < 16) fontSizeSp++ },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(Icons.Rounded.TextIncrease, contentDescription = "增大字号", modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = { softWrap = !softWrap },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.WrapText,
                    contentDescription = if (softWrap) "关闭自动换行" else "开启自动换行",
                    tint = if (softWrap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(
                onClick = {
                    val plain = lines.joinToString("\n") { l -> l.spans.joinToString("") { it.text } }
                    if (plain.isNotBlank()) onCopyScreen(plain)
                },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "复制屏幕", modifier = Modifier.size(16.dp))
            }
        }

        val outerPad = if (extras.isDark) 0.dp else 10.dp
        val termShape = if (extras.isDark) RoundedCornerShape(0.dp) else RoundedCornerShape(14.dp)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = outerPad, vertical = if (extras.isDark) 0.dp else 4.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = termShape,
                color = extras.terminalBg,
                border = if (extras.isDark) null else BorderStroke(1.dp, extras.terminalBorder),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val hScrollModifier = if (softWrap) Modifier else Modifier.horizontalScroll(rememberScrollState())
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(hScrollModifier)
                            .padding(10.dp),
                    ) {
                        items(chunks.size, key = { idx -> "${revision}_$idx" }) { idx ->
                            Text(
                                text = chunks[idx],
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp + 4).sp,
                                softWrap = softWrap,
                            )
                        }
                    }
                    if (!atBottom && chunks.isNotEmpty()) {
                        SmallFloatingActionButton(
                            onClick = { scope.launch { listState.scrollToItem(chunks.lastIndex) } },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionInputBar(
    localTyping: Boolean,
    defaultShowKeycaps: Boolean,
    pendingAttachments: List<PendingAttachment>,
    onAddAttachment: (PendingAttachment) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSendInput: (String, Boolean) -> Unit,
    onSendKey: (Key) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var showKeycaps by remember(defaultShowKeycaps) { mutableStateOf(defaultShowKeycaps) }
    var showMediaSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val extras = SidekickThemeExtras.colors

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAddAttachment(resolvePendingAttachment(context, uri, MediaKind.IMAGE, "image/*"))
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAddAttachment(resolvePendingAttachment(context, uri, MediaKind.VIDEO, "video/*"))
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onAddAttachment(resolvePendingAttachment(context, uri, MediaKind.AUDIO, "audio/*"))
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

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            HorizontalDivider(color = extras.cardBorder)

            AnimatedVisibility(visible = localTyping) {
                Surface(
                    color = extras.warningContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(extras.warning),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                        .padding(start = 10.dp, end = 10.dp, top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilledTonalIconButton(
                    onClick = { showMediaSheet = true },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "添加附件")
                }

                IconButton(
                    onClick = { showKeycaps = !showKeycaps },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Keyboard,
                        contentDescription = "快捷键栏",
                        tint = if (showKeycaps) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("输入指令…") },
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedBorderColor = extras.cardBorder,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { dispatchSend() }),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { onFocusChanged(it.isFocused) },
                )

                val hasPayload = text.isNotEmpty() || pendingAttachments.isNotEmpty()
                FilledIconButton(
                    onClick = ::dispatchSend,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (hasPayload) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (hasPayload) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.size(46.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "发送")
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("发送多媒体 / 快捷动作", style = MaterialTheme.typography.titleLarge)
                Text(
                    "≤ 2 MiB 走 Noise 隧道低优先级分片；> 2 MiB 走客户端流式 AEAD 加密 Blob（详见协议 §3.5）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                MediaPickerTile(
                    icon = Icons.Rounded.Image,
                    title = "选择图片",
                    subtitle = "默认剥离 EXIF 并压缩至 2048px WebP",
                    onClick = {
                        showMediaSheet = false
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
                MediaPickerTile(
                    icon = Icons.Rounded.Videocam,
                    title = "选择视频",
                    subtitle = "流式加密 Blob 中转 + 断点续传",
                    onClick = {
                        showMediaSheet = false
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                )
                MediaPickerTile(
                    icon = Icons.Rounded.AudioFile,
                    title = "选择音频",
                    subtitle = "Opus/M4A 低码率 E2E 加密传输",
                    onClick = {
                        showMediaSheet = false
                        audioPicker.launch("audio/*")
                    },
                )
                MediaPickerTile(
                    icon = Icons.Rounded.ContentPaste,
                    title = "粘贴剪贴板文本",
                    subtitle = "插入当前系统剪贴板内容到输入框",
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
    val extras = SidekickThemeExtras.colors
    val icon = when (att.kind) {
        MediaKind.IMAGE -> Icons.Rounded.Image
        MediaKind.VIDEO -> Icons.Rounded.Videocam
        MediaKind.AUDIO -> Icons.Rounded.AudioFile
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, extras.cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = att.kind.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Column(modifier = Modifier.widthIn(max = 140.dp)) {
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
            IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "移除附件", modifier = Modifier.size(14.dp))
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
    SidekickCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (danger) extras.keycapDangerBg else extras.keycapBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (danger) MaterialTheme.colorScheme.error.copy(alpha = 0.45f) else extras.keycapBorder,
        ),
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .padding(horizontal = 10.dp),
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

private fun formatClockTime(ms: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
}
