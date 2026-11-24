package dev.ddgrcf.sidekick.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ddgrcf.sidekick.core.store.HistoryEntity
import dev.ddgrcf.sidekick.core.store.SessionEntity
import dev.ddgrcf.sidekick.proto.v1.ToolInfo
import dev.ddgrcf.sidekick.proto.v1.Workspace
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras
import dev.ddgrcf.sidekick.vm.BridgeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val error by vm.error.collectAsStateWithLifecycle()

    BridgeContent(
        bridgeName = bridgeName,
        sessions = sessions,
        history = history,
        tools = tools,
        workspaces = workspaces,
        error = error,
        onBack = onBack,
        onRefresh = vm::refresh,
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
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onClickSession: (SessionEntity) -> Unit,
    onClickHistory: (HistoryEntity) -> Unit,
    onStartNew: (tool: String, cwd: String) -> Unit,
) {
    var tabIdx by remember { mutableIntStateOf(0) }
    var showStartSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = { Text(bridgeName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "新建会话")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SecondaryTabRow(
                selectedTabIndex = tabIdx,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                Tab(selected = tabIdx == 0, onClick = { tabIdx = 0 }, text = { Text("会话 (${sessions.size})") })
                Tab(selected = tabIdx == 1, onClick = { tabIdx = 1 }, text = { Text("全部历史 (${history.size})") })
            }
            if (!error.isNullOrBlank()) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    ErrorBanner(error)
                }
            }
            if (tabIdx == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (sessions.isEmpty()) {
                        item {
                            EmptyStateCard(
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
                    } else {
                        items(sessions, key = { it.sid }) { s ->
                            SessionItemCard(session = s, onClick = { onClickSession(s) })
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (history.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.History,
                                title = "暂无历史记录",
                                subtitle = "该电脑上尚未发现结构化 AI 会话历史",
                            )
                        }
                    } else {
                        items(history, key = { it.convId }) { h ->
                            HistoryItemCard(entry = h, onClick = { onClickHistory(h) })
                        }
                    }
                }
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

@Composable
private fun SessionItemCard(
    session: SessionEntity,
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
        ) {
            ToolBadge(tool = session.tool, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
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
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    entry: HistoryEntity,
    onClick: () -> Unit,
) {
    val isDark = SidekickThemeExtras.colors.isDark
    SidekickCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToolBadge(tool = entry.tool, size = 28.dp)
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
    val extras = SidekickThemeExtras.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("新建会话", style = MaterialTheme.typography.titleLarge)

            SectionHeader("选择 Tool")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tools.forEach { t ->
                    val selected = selectedTool == t.name
                    Surface(
                        onClick = { selectedTool = t.name },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else extras.cardBorder,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ToolBadge(tool = t.name, size = 36.dp)
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
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            SectionHeader("选择工作区")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                workspaces.forEach { ws ->
                    val selected = selectedCwd == ws.cwd
                    val folderName = ws.cwd.trimEnd('/').substringAfterLast('/').ifBlank { ws.cwd }
                    Surface(
                        onClick = { selectedCwd = ws.cwd },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else extras.cardBorder,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
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
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onStart(selectedTool, selectedCwd) },
                enabled = selectedTool.isNotBlank() && selectedCwd.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("启动会话")
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return ""
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
}
