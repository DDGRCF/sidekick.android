package dev.ddgrcf.sidekick.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ddgrcf.sidekick.core.store.HistoryEntity
import dev.ddgrcf.sidekick.core.store.SessionEntity
import dev.ddgrcf.sidekick.proto.v1.ToolInfo
import dev.ddgrcf.sidekick.proto.v1.Workspace
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras
import dev.ddgrcf.sidekick.vm.BridgeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BridgeScreen(
    bridgeId: String,
    bridgeName: String,
    vm: BridgeViewModel,
    onBack: () -> Unit,
    onOpenSession: (sid: String, tool: String, title: String, convId: String, structured: Boolean) -> Unit,
) {
    LaunchedEffect(bridgeId) { vm.load(bridgeId) }
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val tools by vm.tools.collectAsStateWithLifecycle()
    val workspaces by vm.workspaces.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val syncingHistory by vm.syncingHistory.collectAsStateWithLifecycle()
    val pullRefreshing by vm.pullRefreshing.collectAsStateWithLifecycle()
    val startingSession by vm.startingSession.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val hiddenHistoryCount by vm.hiddenHistoryCount.collectAsStateWithLifecycle()

    BridgeContent(
        bridgeName = bridgeName,
        sessions = sessions,
        history = history,
        tools = tools,
        workspaces = workspaces,
        loading = loading,
        syncingHistory = syncingHistory,
        pullRefreshing = pullRefreshing,
        startingSession = startingSession,
        error = error,
        hiddenHistoryCount = hiddenHistoryCount,
        onBack = onBack,
        onRefresh = { vm.refresh(userInitiated = true) },
        onClickSession = { s ->
            onOpenSession(s.sid, s.tool, s.title ?: s.sid, s.convId ?: "", vm.isStructured(s.tool))
        },
        onClickHistory = { h ->
            if (!h.liveSid.isNullOrBlank()) {
                onOpenSession(h.liveSid!!, h.tool, h.title ?: h.convId, h.convId, vm.isStructured(h.tool))
            } else {
                vm.startOrResume(h.tool, h.cwd, h.convId) { sid, convId, structured ->
                    onOpenSession(sid, h.tool, h.title ?: sid, convId, structured)
                }
            }
        },
        onStartNew = { tool, cwd ->
            vm.startOrResume(tool, cwd) { sid, convId, structured ->
                onOpenSession(sid, tool, sid, convId, structured)
            }
        },
        onDeleteSession = vm::deleteSession,
        onDeleteHistory = vm::deleteHistory,
        onRestoreHiddenHistory = vm::restoreHiddenHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgeContent(
    bridgeName: String,
    sessions: List<SessionEntity>,
    history: List<HistoryEntity>,
    tools: List<ToolInfo>,
    workspaces: List<Workspace>,
    error: String?,
    loading: Boolean = false,
    syncingHistory: Boolean = false,
    pullRefreshing: Boolean = false,
    startingSession: Boolean = false,
    isDemo: Boolean = false,
    hiddenHistoryCount: Int = 0,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onClickSession: (SessionEntity) -> Unit,
    onClickHistory: (HistoryEntity) -> Unit,
    onStartNew: (tool: String, cwd: String) -> Unit,
    onDeleteSession: (SessionEntity) -> Unit = {},
    onDeleteHistory: (HistoryEntity) -> Unit = {},
    onRestoreHiddenHistory: () -> Unit = {},
) {
    var tabIdx by rememberSaveable { mutableIntStateOf(0) }
    var showStartSheet by rememberSaveable { mutableStateOf(false) }
    var revealedKey by remember { mutableStateOf<String?>(null) }
    var pendingStopSession by remember { mutableStateOf<SessionEntity?>(null) }
    var sheetSession by remember { mutableStateOf<SessionEntity?>(null) }
    var sheetHistory by remember { mutableStateOf<HistoryEntity?>(null) }
    val sessionsListState = rememberLazyListState()
    val historyListState = rememberLazyListState()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(tabIdx) { revealedKey = null }
    LaunchedEffect(sessionsListState.isScrollInProgress) {
        if (sessionsListState.isScrollInProgress) revealedKey = null
    }
    LaunchedEffect(historyListState.isScrollInProgress) {
        if (historyListState.isScrollInProgress) revealedKey = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(bridgeName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isDemo) DemoTag()
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
        floatingActionButton = {
            if (tools.isNotEmpty() && workspaces.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showStartSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 1.dp,
                        pressedElevation = 2.dp,
                        focusedElevation = 1.dp,
                        hoveredElevation = 2.dp,
                    ),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "新建会话")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(
                selectedTabIndex = tabIdx,
                containerColor = MaterialTheme.colorScheme.background,
                divider = { HairlineDivider() },
            ) {
                Tab(
                    selected = tabIdx == 0,
                    onClick = { tabIdx = 0 },
                    text = { Text("会话 (${sessions.size})") },
                )
                Tab(
                    selected = tabIdx == 1,
                    onClick = { tabIdx = 1 },
                    text = {
                        Text(
                            if (syncingHistory && history.isEmpty()) "全部历史 (同步中…)" else "全部历史 (${history.size})",
                        )
                    },
                )
            }
            val showTopProgress = !pullRefreshing && (loading || syncingHistory || startingSession)
            if (showTopProgress) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                )
            }
            if (startingSession) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "正在工作站启动会话，稍候将自动进入…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            PullToRefreshBox(
                isRefreshing = pullRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                if (tabIdx == 0) {
                    LazyColumn(
                        state = sessionsListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                    ) {
                        if (!error.isNullOrBlank()) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    ErrorBanner(error, actionLabel = "重试", onAction = onRefresh)
                                }
                            }
                        }
                        when {
                            loading && sessions.isEmpty() -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 48.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "正在同步运行中的会话…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            sessions.isEmpty() && error.isNullOrBlank() -> {
                                item {
                                    EmptyStateView(
                                        icon = Icons.Rounded.Terminal,
                                        title = "暂无运行中会话",
                                        subtitle = if (tools.isNotEmpty() && workspaces.isNotEmpty()) {
                                            "点击右下角 + 启动一个新会话，或切换到“全部历史”继续之前的对话"
                                        } else {
                                            "当前未检测到运行中的会话"
                                        },
                                        actionLabel = if (tools.isNotEmpty() && workspaces.isNotEmpty()) "新建会话" else null,
                                        onAction = if (tools.isNotEmpty() && workspaces.isNotEmpty()) {
                                            { showStartSheet = true }
                                        } else null,
                                    )
                                }
                            }
                            sessions.isNotEmpty() -> {
                                itemsIndexed(sessions, key = { _, it -> it.sid }) { idx, s ->
                                    val rowKey = "s::${s.sid}"
                                    SwipeRevealActionRow(
                                        revealed = revealedKey == rowKey,
                                        actionLabel = if (s.live) "停止" else "移除",
                                        actionContentDescription = if (s.live) "停止会话 ${s.sid}" else "移除会话 ${s.sid}",
                                        onRevealChange = { open ->
                                            revealedKey = if (open) rowKey else if (revealedKey == rowKey) null else revealedKey
                                        },
                                        onClick = { onClickSession(s) },
                                        onLongClick = { sheetSession = s },
                                        onAction = {
                                            revealedKey = null
                                            if (s.live) {
                                                pendingStopSession = s
                                            } else {
                                                onDeleteSession(s)
                                            }
                                        },
                                    ) {
                                        SessionItemRow(session = s)
                                    }
                                    if (idx < sessions.lastIndex) {
                                        HairlineDivider(Modifier.padding(start = 52.dp))
                                    } else {
                                        HairlineDivider()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = historyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                    ) {
                        if (!error.isNullOrBlank()) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    ErrorBanner(error, actionLabel = "重试", onAction = onRefresh)
                                }
                            }
                        }
                        if (hiddenHistoryCount > 0) {
                            item(key = "hidden-history-banner") {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "已隐藏 $hiddenHistoryCount 条历史",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                        )
                                        TextButton(
                                            onClick = onRestoreHiddenHistory,
                                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                        ) {
                                            Text("恢复全部")
                                        }
                                    }
                                }
                                HairlineDivider()
                            }
                        }
                        when {
                            (loading || syncingHistory) && history.isEmpty() -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 48.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "正在后台索引工作站历史记录…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            history.isEmpty() && error.isNullOrBlank() -> {
                                item {
                                    EmptyStateView(
                                        icon = Icons.Rounded.History,
                                        title = "暂无历史记录",
                                        subtitle = "该电脑上尚未发现结构化 AI 会话历史",
                                    )
                                }
                            }
                            history.isNotEmpty() -> {
                                itemsIndexed(history, key = { _, it -> it.convId }) { idx, h ->
                                    val rowKey = "h::${h.convId}"
                                    SwipeRevealActionRow(
                                        revealed = revealedKey == rowKey,
                                        actionLabel = "移除",
                                        actionContentDescription = "移除历史 ${h.convId}",
                                        onRevealChange = { open ->
                                            revealedKey = if (open) rowKey else if (revealedKey == rowKey) null else revealedKey
                                        },
                                        onClick = { onClickHistory(h) },
                                        onLongClick = { sheetHistory = h },
                                        onAction = {
                                            revealedKey = null
                                            onDeleteHistory(h)
                                        },
                                    ) {
                                        HistoryItemRow(entry = h)
                                    }
                                    if (idx < history.lastIndex) {
                                        HairlineDivider(Modifier.padding(start = 16.dp))
                                    } else {
                                        HairlineDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingStopSession?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingStopSession = null },
            title = { Text("停止并移除会话？") },
            text = { Text("将终止本地运行的「${target.title ?: target.sid}」进程并从列表中移除（历史记录仍会保留）。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingStopSession = null
                        onDeleteSession(target)
                    },
                ) {
                    Text("停止并移除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStopSession = null }) { Text("取消") }
            },
        )
    }

    sheetSession?.let { s ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { sheetSession = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = s.title ?: s.sid,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = s.cwd,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                SheetActionRow(
                    icon = Icons.Rounded.Terminal,
                    label = "进入会话",
                    onClick = {
                        sheetSession = null
                        onClickSession(s)
                    },
                )
                SheetActionRow(
                    icon = Icons.Rounded.ContentCopy,
                    label = "复制会话 ID",
                    onClick = {
                        clipboard.setText(AnnotatedString(s.sid))
                        sheetSession = null
                    },
                )
                SheetActionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = if (s.live) "停止并移除会话" else "从列表移除",
                    destructive = true,
                    onClick = {
                        sheetSession = null
                        if (s.live) {
                            pendingStopSession = s
                        } else {
                            onDeleteSession(s)
                        }
                    },
                )
            }
        }
    }

    sheetHistory?.let { h ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { sheetHistory = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = h.title ?: h.convId,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = h.cwd,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                SheetActionRow(
                    icon = Icons.Rounded.History,
                    label = "继续对话",
                    onClick = {
                        sheetHistory = null
                        onClickHistory(h)
                    },
                )
                SheetActionRow(
                    icon = Icons.Rounded.ContentCopy,
                    label = "复制对话 ID",
                    onClick = {
                        clipboard.setText(AnnotatedString(h.convId))
                        sheetHistory = null
                    },
                )
                SheetActionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "从列表移除（保留电脑原文件）",
                    destructive = true,
                    onClick = {
                        sheetHistory = null
                        onDeleteHistory(h)
                    },
                )
            }
        }
    }

    if (showStartSheet) {
        StartSessionSheet(
            tools = tools,
            workspaces = workspaces,
            onDismiss = { showStartSheet = false },
            onStart = { tool, cwd ->
                showStartSheet = false
                onStartNew(tool, cwd)
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeRevealActionRow(
    revealed: Boolean,
    actionLabel: String,
    actionContentDescription: String,
    onRevealChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAction: () -> Unit,
    content: @Composable () -> Unit,
) {
    val actionWidthDp = 84.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidthDp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(revealed) {
        val target = if (revealed) -actionWidthPx else 0f
        if (abs(offsetX.value - target) > 0.5f) {
            offsetX.animateTo(target, tween(180))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clipToBounds(),
    ) {
        val actionVisible = revealed || offsetX.value < -8f
        if (actionVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(actionWidthDp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(enabled = revealed || offsetX.value <= -actionWidthPx * 0.45f) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAction()
                    }
                    .testTag("swipe_action_$actionContentDescription"),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = actionContentDescription,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.background)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val next = (offsetX.value + delta).coerceIn(-actionWidthPx, 0f)
                        scope.launch { offsetX.snapTo(next) }
                    },
                    onDragStarted = { onRevealChange(true) },
                    onDragStopped = {
                        val open = offsetX.value <= -actionWidthPx * 0.45f
                        onRevealChange(open)
                        scope.launch { offsetX.animateTo(if (open) -actionWidthPx else 0f, tween(180)) }
                    },
                )
                .combinedClickable(
                    onClick = {
                        if (revealed) {
                            onRevealChange(false)
                        } else {
                            onClick()
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                ),
        ) {
            content()
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
        }
    }
}

@Composable
private fun SessionItemRow(
    session: SessionEntity,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolIcon(tool = session.tool, size = 22.dp)
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = session.title ?: session.sid,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = session.cwd,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (session.live) {
            LiveBadge()
            Spacer(Modifier.width(6.dp))
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HistoryItemRow(
    entry: HistoryEntity,
) {
    val isDark = SidekickThemeExtras.colors.isDark
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolIcon(tool = entry.tool, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = ToolVisuals.label(entry.tool),
                color = ToolVisuals.color(entry.tool, isDark),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.weight(1f))
            if (!entry.liveSid.isNullOrBlank()) {
                LiveBadge()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = formatTime(entry.updatedMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = entry.title ?: entry.convId,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!entry.preview.isNullOrBlank()) {
            Text(
                text = entry.preview!!,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartSessionSheet(
    tools: List<ToolInfo>,
    workspaces: List<Workspace>,
    onDismiss: () -> Unit,
    onStart: (String, String) -> Unit,
) {
    var selectedTool by remember { mutableStateOf(tools.firstOrNull()?.name ?: "") }
    var selectedCwd by remember { mutableStateOf(workspaces.firstOrNull()?.cwd ?: "") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            item {
                Text("新建会话", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(14.dp))
                SectionHeader("选择 Agent", modifier = Modifier.padding(bottom = 6.dp))
                HairlineDivider()
            }
            itemsIndexed(tools, key = { _, t -> t.name }) { idx, t ->
                val selected = selectedTool == t.name
                Surface(
                    onClick = { selectedTool = t.name },
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToolIcon(tool = t.name, size = 22.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ToolVisuals.label(t.name), style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (t.structured_history) "Chat + Terminal 双视图" else "Terminal 实时交互",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "已选中",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (idx < tools.lastIndex) {
                    HairlineDivider(Modifier.padding(start = 34.dp))
                } else {
                    HairlineDivider()
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("选择工作区", modifier = Modifier.padding(bottom = 6.dp))
                HairlineDivider()
            }
            itemsIndexed(workspaces, key = { _, ws -> ws.cwd }) { idx, ws ->
                val selected = selectedCwd == ws.cwd
                val folderName = ws.cwd.trimEnd('/').substringAfterLast('/').ifBlank { ws.cwd }
                Surface(
                    onClick = { selectedCwd = ws.cwd },
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folderName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                ws.cwd,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "已选中",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (idx < workspaces.lastIndex) {
                    HairlineDivider(Modifier.padding(start = 34.dp))
                } else {
                    HairlineDivider()
                }
            }
            item {
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onStart(selectedTool, selectedCwd) },
                    enabled = selectedTool.isNotBlank() && selectedCwd.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .defaultMinSize(minHeight = 52.dp),
                ) {
                    Text("启动会话")
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return ""
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
}
