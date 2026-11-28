package dev.ddgrcf.sidekick.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras
import dev.ddgrcf.sidekick.vm.ModelOptionUi
import dev.ddgrcf.sidekick.vm.RuntimeAnswerInput
import dev.ddgrcf.sidekick.vm.RuntimeModeUi
import dev.ddgrcf.sidekick.vm.RuntimeRequestUi
import dev.ddgrcf.sidekick.vm.SessionConfigUi
import dev.ddgrcf.sidekick.vm.WorkspaceContextUi
import kotlinx.coroutines.delay

fun shortenModelLabel(raw: String): String {
    val t = raw.trim()
    if (t.isEmpty()) return "默认模型"
    return t.substringAfterLast('/').ifBlank { t }
}

@Composable
fun ComposerConfigPill(
    config: SessionConfigUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val modelLabel = shortenModelLabel(config.model)
    val thinking = config.thinkingLevel.trim()
    val summary = if (thinking.isNotEmpty() && thinking != "off") {
        "$modelLabel · $thinking"
    } else {
        modelLabel
    }
    val nonDefaultMode = config.runtimeMode != RuntimeModeUi.APPROVAL_REQUIRED &&
        config.runtimeMode != RuntimeModeUi.UNSPECIFIED
    Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .widthIn(max = 210.dp)
            .testTag("composer_config_pill"),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Psychology,
                contentDescription = null,
                tint = if (nonDefaultMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "模型与思考设置",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionSettingsBottomSheet(
    config: SessionConfigUi,
    onSelectModel: (String) -> Unit,
    onSelectThinking: (String) -> Unit,
    onSelectRuntimeMode: (RuntimeModeUi) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    fun isModelSelected(m: ModelOptionUi): Boolean =
        m.id == config.model || (config.model.isBlank() && m.current)

    val filteredModels = remember(config.availableModels, config.model, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            val selected = config.availableModels.filter(::isModelSelected)
            val rest = config.availableModels.filterNot(::isModelSelected)
            selected + rest
        } else {
            config.availableModels
                .withIndex()
                .filter { (_, m) ->
                    m.id.lowercase().contains(q) ||
                        m.name.lowercase().contains(q) ||
                        m.provider.lowercase().contains(q)
                }
                .sortedWith(
                    compareBy<IndexedValue<ModelOptionUi>>(
                        { (_, m) ->
                            val id = m.id.lowercase()
                            val name = m.name.lowercase()
                            val provider = m.provider.lowercase()
                            when {
                                name == q || id == q -> 0
                                name.startsWith(q) || id.startsWith(q) -> 1
                                provider.startsWith(q) -> 2
                                else -> 3
                            }
                        },
                        { (_, m) -> if (isModelSelected(m)) 0 else 1 },
                        { (idx, _) -> idx },
                    ),
                )
                .map { it.value }
        }
    }
    val thinkingLevels = remember(config.availableThinkingLevels, config.model, config.availableModels) {
        val fromActive = config.availableModels.firstOrNull { it.id == config.model || it.current }?.thinkingLevels.orEmpty()
        when {
            fromActive.isNotEmpty() -> fromActive
            config.availableThinkingLevels.isNotEmpty() -> config.availableThinkingLevels
            else -> listOf("off", "low", "medium", "high", "xhigh")
        }
    }
    val runtimeModes = remember(config.supportedRuntimeModes) {
        if (config.supportedRuntimeModes.isNotEmpty()) {
            config.supportedRuntimeModes
        } else {
            listOf(
                RuntimeModeUi.APPROVAL_REQUIRED,
                RuntimeModeUi.AUTO_ACCEPT_EDITS,
                RuntimeModeUi.AUTO,
                RuntimeModeUi.FULL_ACCESS,
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "模型与思考控制",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (config.isRunning) {
                        Text(
                            text = if (config.supportsMidTurnConfig) {
                                "运行中可实时切换参数"
                            } else {
                                "当前轮次执行中，更改将在下一轮自动生效"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭设置")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .testTag("session_settings_sheet"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (thinkingLevels.isNotEmpty()) {
                item(key = "thinking") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "思考强度 (Thinking / Effort)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_sheet_thinking"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            thinkingLevels.forEach { level ->
                                val selected = level.equals(config.thinkingLevel, ignoreCase = true)
                                Surface(
                                    onClick = { onSelectThinking(level) },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    border = BorderStroke(
                                        width = if (selected) 1.dp else 0.5.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = level,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                            ),
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (config.availableModels.isNotEmpty()) {
                item(key = "models-search") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "模型选择 (${filteredModels.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索模型或提供商…") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "清空搜索",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_sheet_model_search"),
                        )
                    }
                }
                if (filteredModels.isEmpty()) {
                    item(key = "models-empty") {
                        Text(
                            text = "未找到匹配模型",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                } else {
                    items(filteredModels, key = { "model-${it.id}" }) { m ->
                        val isSelected = isModelSelected(m)
                        Surface(
                            onClick = { onSelectModel(m.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("settings_sheet_model_item"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = m.name.ifBlank { m.id },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (m.provider.isNotBlank() || m.id != m.name) {
                                        Text(
                                            text = listOf(m.provider, m.id).filter { it.isNotBlank() }.joinToString(" · "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "当前模型",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "modes-header") {
                Row(
                    modifier = Modifier.testTag("settings_sheet_modes"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "权限与执行模式",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            items(runtimeModes.filter { it != RuntimeModeUi.UNSPECIFIED }, key = { "mode-${it.name}" }) { mode ->
                val isSelected = config.runtimeMode == mode
                Surface(
                    onClick = { onSelectRuntimeMode(mode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        width = if (isSelected) 1.dp else 0.5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "已选模式",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun DockedApprovalCard(
    request: RuntimeRequestUi,
    onDecision: (decision: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = SidekickThemeExtras.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pending_approval_card"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, extras.warning),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Build,
                    contentDescription = null,
                    tint = extras.warning,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = request.title.ifBlank { "请求执行工具" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (request.toolName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = extras.warningContainer,
                    ) {
                        Text(
                            text = request.toolName,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = extras.onWarningContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            if (request.detail.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp),
                ) {
                    Text(
                        text = request.detail,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onDecision("accept") },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("允许一次")
                }
                OutlinedButton(
                    onClick = { onDecision("acceptForSession") },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("本会话允许")
                }
                OutlinedButton(
                    onClick = { onDecision("decline") },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("拒绝", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun DockedUserInputCard(
    request: RuntimeRequestUi,
    onSubmit: (answers: List<RuntimeAnswerInput>, cancelled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var collapsed by rememberSaveable(request.requestId) { mutableStateOf(false) }
    val selectedMap = remember(request.requestId) { mutableStateMapOf<String, Set<String>>() }
    val customMap = remember(request.requestId) { mutableStateMapOf<String, String>() }

    if (collapsed) {
        Surface(
            onClick = { collapsed = false },
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("pending_user_input_pill"),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "等待你的选择 · ${request.questions.size} 个问题",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandLess,
                    contentDescription = "展开提问卡片",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pending_user_input_card"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = request.title.ifBlank { "等待你的回答" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { collapsed = true },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "收起提问卡片",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .testTag("user_input_questions_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(request.questions, key = { it.id }) { q ->
                    val chosen = selectedMap[q.id].orEmpty()
                    val customText = customMap[q.id].orEmpty()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = q.question,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        q.options.forEach { opt ->
                            val value = opt.id.ifBlank { opt.label }
                            val isPicked = value in chosen
                            Surface(
                                onClick = {
                                    selectedMap[q.id] = if (q.multiSelect) {
                                        if (isPicked) chosen - value else chosen + value
                                    } else {
                                        setOf(value)
                                    }
                                    if (!q.multiSelect) {
                                        customMap.remove(q.id)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isPicked) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    width = if (isPicked) 1.dp else 0.5.dp,
                                    color = if (isPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = opt.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (opt.description.isNotBlank()) {
                                            Text(
                                                text = opt.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    if (isPicked) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                        if (q.allowCustomAnswer || q.options.isEmpty()) {
                            OutlinedTextField(
                                value = customText,
                                onValueChange = {
                                    customMap[q.id] = it
                                    if (!q.multiSelect && it.isNotBlank()) {
                                        selectedMap.remove(q.id)
                                    }
                                },
                                placeholder = { Text("或输入自定义回答…") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (request.dismissible) {
                    TextButton(
                        onClick = { onSubmit(emptyList(), true) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text("取消")
                    }
                }
                Spacer(Modifier.weight(1f))
                    Button(
                    onClick = {
                        val answers = request.questions.map { q ->
                            RuntimeAnswerInput(
                                questionId = q.id,
                                selectedValues = selectedMap[q.id].orEmpty().toList(),
                                customText = customMap[q.id].orEmpty(),
                            )
                        }
                        onSubmit(answers, false)
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("提交回答")
                }
            }
        }
    }
}

@Composable
fun SidekickContextBar(
    workspaceContext: WorkspaceContextUi?,
    turnStartedMs: Long?,
    onOpenDiff: () -> Unit,
    onOpenNvim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (workspaceContext == null && turnStartedMs == null) return
    val extras = SidekickThemeExtras.colors
    var nowMs by remember(turnStartedMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(turnStartedMs) {
        if (turnStartedMs != null) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("sidekick_context_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (workspaceContext != null && (workspaceContext.gitBranch.isNotBlank() || workspaceContext.changedFiles.isNotEmpty() || workspaceContext.cwd.isNotBlank())) {
            val branch = workspaceContext.gitBranch.ifBlank { workspaceContext.cwd.substringAfterLast('/').ifBlank { "workspace" } }
            val count = workspaceContext.changedFiles.size
            val label = if (count > 0) "$branch · $count 改动" else branch
            Surface(
                onClick = onOpenDiff,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("context_bar_git_pill"),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (workspaceContext != null && workspaceContext.nvimConnected) {
            val filePart = workspaceContext.nvimFile.substringAfterLast('/').ifBlank { "已连接" }
            val linePart = if (workspaceContext.nvimLine > 0 && workspaceContext.nvimFile.isNotBlank()) ":${workspaceContext.nvimLine}" else ""
            val errCount = remember(workspaceContext.diagnostics) {
                workspaceContext.diagnostics.count { it.severity.equals("error", ignoreCase = true) }
            }
            val nvimLabel = buildString {
                append("Neovim · ")
                append(filePart)
                append(linePart)
                if (errCount > 0) append(" · $errCount 错误")
            }
            Surface(
                onClick = onOpenNvim,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("context_bar_nvim_pill"),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.5.dp, if (errCount > 0) MaterialTheme.colorScheme.error else extras.success),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (errCount > 0) Icons.Rounded.ErrorOutline else Icons.Rounded.Code,
                        contentDescription = null,
                        tint = if (errCount > 0) MaterialTheme.colorScheme.error else extras.success,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = nvimLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (turnStartedMs != null) {
            val secs = ((nowMs - turnStartedMs).coerceAtLeast(0L) / 1000L)
            Surface(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("context_bar_timer_pill"),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "${secs}s",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceDiffSheet(
    workspaceContext: WorkspaceContextUi,
    loading: Boolean,
    onRefresh: () -> Unit,
    onAttachSummary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extras = SidekickThemeExtras.colors
    val primaryColor = MaterialTheme.colorScheme.primary
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val parsedByFile = remember(workspaceContext.gitDiff) {
        parseUnifiedDiffByFile(workspaceContext.gitDiff)
    }
    var expandedFile by rememberSaveable(workspaceContext.cwd, workspaceContext.changedFiles.size) {
        mutableStateOf(
            if (workspaceContext.changedFiles.size == 1) workspaceContext.changedFiles.first().path else null,
        )
    }
    var fullscreenPayload by remember { mutableStateOf<Triple<String, String, List<DiffLine>>?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "工作区变更 (${workspaceContext.gitBranch.ifBlank { "HEAD" }})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (workspaceContext.cwd.isNotBlank()) {
                        Text(
                            text = workspaceContext.cwd,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新变更")
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭变更")
                }
            }

            if (workspaceContext.changedFiles.isNotEmpty() || workspaceContext.gitDiff.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (workspaceContext.changedFiles.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                onAttachSummary()
                                onDismiss()
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text("将改动摘要插入输入框")
                        }
                    }
                    if (workspaceContext.gitDiff.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val allLines = parseSinglePatchSnippet(workspaceContext.gitDiff)
                                fullscreenPayload = Triple(
                                    "全部变更 (${workspaceContext.gitBranch.ifBlank { "HEAD" }})",
                                    "${workspaceContext.changedFiles.size} 个文件",
                                    allLines,
                                )
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text("全屏查看全部")
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .testTag("workspace_diff_sheet"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (workspaceContext.changedFiles.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = "当前工作区干净，暂无未提交改动",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    for (f in workspaceContext.changedFiles) {
                        val fileDiff = parsedByFile[f.path]
                        val isExpanded = expandedFile == f.path
                        item(key = "file-${f.path}") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable {
                                        expandedFile = if (isExpanded) null else f.path
                                    }
                                    .testTag("diff_file_row_${f.path}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isExpanded) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                border = BorderStroke(
                                    width = if (isExpanded) 1.dp else 0.5.dp,
                                    color = if (isExpanded) primaryColor else MaterialTheme.colorScheme.outlineVariant,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = f.status,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = primaryColor,
                                    )
                                    Text(
                                        text = f.path,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (f.added > 0 || f.removed > 0) {
                                        Text(
                                            text = "+${f.added} -${f.removed}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = extras.success,
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = if (isExpanded) "收起文件 Diff" else "展开文件 Diff",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                        if (isExpanded) {
                            item(key = "diff-${f.path}") {
                                if (fileDiff != null && fileDiff.lines.isNotEmpty()) {
                                    DiffCodeBlock(
                                        lines = fileDiff.lines,
                                        maxLines = 80,
                                        onCopyDiff = {
                                            clipboard.setText(AnnotatedString(fileDiff.lines.joinToString("\n") { it.text }))
                                        },
                                        onOpenFullscreen = {
                                            fullscreenPayload = Triple(
                                                f.path,
                                                formatDiffStats(fileDiff.added, fileDiff.removed),
                                                fileDiff.lines,
                                            )
                                        },
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    ) {
                                        Text(
                                            text = when {
                                                loading -> "正在加载该文件的 Diff 预览…"
                                                f.status == "D" -> "该文件已删除"
                                                else -> "暂无可预览文本差异（可能是二进制文件、空文件或超过单次 Diff 上限）"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(12.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fullscreenPayload?.let { (title, subtitle, lines) ->
        FullscreenDiffDialog(
            title = title,
            subtitle = subtitle,
            lines = lines,
            onCopyDiff = {
                clipboard.setText(AnnotatedString(lines.joinToString("\n") { it.text }))
            },
            onDismiss = { fullscreenPayload = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NvimContextSheet(
    workspaceContext: WorkspaceContextUi,
    onRefresh: () -> Unit,
    onInsertSnippet: (kind: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Neovim 实时上下文",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (workspaceContext.nvimFile.isNotBlank()) {
                        Text(
                            text = "${workspaceContext.nvimFile}:${workspaceContext.nvimLine}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新编辑器上下文")
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭编辑器上下文")
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (workspaceContext.nvimFile.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            onInsertSnippet("file")
                            onDismiss()
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text("插入当前文件")
                    }
                }
                if (workspaceContext.diagnostics.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            onInsertSnippet("diagnostics")
                            onDismiss()
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text("插入诊断报错 (${workspaceContext.diagnostics.size})")
                    }
                }
                if (workspaceContext.changedFiles.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            onInsertSnippet("git_diff")
                            onDismiss()
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text("插入 Git 改动")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .testTag("nvim_context_sheet"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (workspaceContext.openBuffers.isNotEmpty()) {
                    item(key = "buffers-hdr") {
                        Text(
                            text = "已打开缓冲区 (${workspaceContext.openBuffers.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    items(workspaceContext.openBuffers, key = { "buf-$it" }) { b ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = b,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (workspaceContext.diagnostics.isNotEmpty()) {
                    item(key = "diag-hdr") {
                        Text(
                            text = "LSP 诊断 (${workspaceContext.diagnostics.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    items(workspaceContext.diagnostics, key = { "diag-${it.file}-${it.line}-${it.col}-${it.message.hashCode()}" }) { d ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "${d.file}:${d.line}:${d.col} · ${d.severity}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = if (d.severity.equals("error", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = d.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
