package dev.ddgrcf.sidekick.ui

import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.data.UiPart
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.IdentityHashMap

enum class ToolActivityKind {
    COMMAND,
    READ,
    EDIT,
    SEARCH,
    WEB,
    AGENT,
    OTHER,
}

data class TranslatedToolActivity(
    val callId: String,
    val kind: ToolActivityKind,
    val verb: String,
    val target: String,
    val statusLabel: String,
    val isRunning: Boolean,
    val isError: Boolean,
    val addedLines: Int = 0,
    val removedLines: Int = 0,
    val detailInput: String = "",
    val detailDiff: String = "",
    val detailOutput: String = "",
    val rawToolName: String = "",
)

data class ChatQuestionOption(
    val label: String,
    val description: String = "",
)

data class ChatQuestionPrompt(
    val callId: String,
    val header: String,
    val question: String,
    val options: List<ChatQuestionOption>,
    val multiSelect: Boolean = false,
    val answered: Boolean = false,
    val answerSummary: String = "",
)

sealed interface TurnWorkStep {
    data class Thinking(val text: String) : TurnWorkStep
    data class Note(val text: String) : TurnWorkStep
    data class Activity(val activity: TranslatedToolActivity) : TurnWorkStep
}

sealed interface ChatTurn {
    val key: String

    data class User(
        override val key: String,
        val textParts: List<UiPart.Text>,
        val mediaParts: List<UiPart.Media>,
    ) : ChatTurn

    data class Assistant(
        override val key: String,
        val model: String?,
        val thinkingBlocks: List<String>,
        val activities: List<TranslatedToolActivity>,
        val timeline: List<TurnWorkStep> = emptyList(),
        val questions: List<ChatQuestionPrompt> = emptyList(),
        val textBlocks: List<String>,
        val mediaParts: List<UiPart.Media>,
    ) : ChatTurn
}

fun hasNonQuestionToolCall(parts: List<UiPart>): Boolean =
    parts.filterIsInstance<UiPart.ToolCall>()
        .any { !isInteractiveQuestionTool(normalizeToolName(it.name)) }

private val harnessBlockRegex = Regex(
    """<(environment_context|permissions\s+instructions|collaboration_mode|skills_instructions|multi_agent_role|multi_agent_mode|user_instructions|system-reminder|ide_opened_file|ide_selection|t3_code_[a-zA-Z0-9_]+|ADDITIONAL_METADATA|USER_SETTINGS_CHANGE|INSTRUCTIONS)\b[^>]*>[\s\S]*?</(?:environment_context|permissions\s+instructions|collaboration_mode|skills_instructions|multi_agent_role|multi_agent_mode|user_instructions|system-reminder|ide_opened_file|ide_selection|t3_code_[a-zA-Z0-9_]+|ADDITIONAL_METADATA|USER_SETTINGS_CHANGE|INSTRUCTIONS)>""",
    setOf(RegexOption.IGNORE_CASE),
)

private val userRequestRegex = Regex("""<USER_REQUEST>\s*([\s\S]*?)\s*</USER_REQUEST>""", setOf(RegexOption.IGNORE_CASE))
private val lenientJson = Json { ignoreUnknownKeys = true }

fun cleanUserPromptText(raw: String): String {
    var s = raw.trim()
    if (s.isEmpty()) return ""
    val match = userRequestRegex.find(s)
    if (match != null) {
        s = match.groupValues[1].trim()
    }
    s = harnessBlockRegex.replace(s, "")
    return s.trim()
}

/**
 * Translates raw sequential protocol/JSONL messages into modern T3Code-style semantic turns:
 * - Drops internal system/developer prompts and empty injected harness envelopes.
 * - Merges consecutive assistant/tool steps between user prompts into a single coherent Assistant turn.
 * - Pairs ToolCall + ToolResult into human-readable work-log entries with diff/command summaries.
 */
fun groupMessagesIntoChatTurns(
    messages: List<MessageEntity>,
    decodeParts: (String) -> List<UiPart>,
): List<ChatTurn> {
    val turns = mutableListOf<ChatTurn>()

    var pendingStartSeq: Long? = null
    var pendingModel: String? = null
    val pendingThinking = mutableListOf<String>()
    val pendingCalls = mutableListOf<UiPart.ToolCall>()
    val pendingResultsById = LinkedHashMap<String, UiPart.ToolResult>()
    val orphanResults = mutableListOf<UiPart.ToolResult>()
    val pendingTexts = mutableListOf<String>()
    val pendingMedia = mutableListOf<UiPart.Media>()
    // Chronological raw entries inside the current continuous Assistant turn:
    // ("thinking" -> text, "text" -> text, "call" -> ToolCall)
    val rawOrder = mutableListOf<Any>()

    fun flushAssistant() {
        val startSeq = pendingStartSeq ?: return
        val activities = mutableListOf<TranslatedToolActivity>()
        val questions = mutableListOf<ChatQuestionPrompt>()
        val usedResultIds = HashSet<String>()
        val translatedByCall = IdentityHashMap<UiPart.ToolCall, TranslatedToolActivity>()
        for (call in pendingCalls) {
            val res = if (call.callId.isNotBlank()) pendingResultsById[call.callId] else null
            if (res != null) usedResultIds += call.callId
            val extractedQuestions = extractInteractiveQuestions(call, res)
            if (extractedQuestions.isNotEmpty()) {
                questions += extractedQuestions
            } else {
                val translated = translateToolActivity(call, res)
                activities += translated
                translatedByCall[call] = translated
            }
        }
        val orphanActivities = mutableListOf<TranslatedToolActivity>()
        for ((cid, res) in pendingResultsById) {
            if (cid !in usedResultIds) {
                val a = translateOrphanToolResult(res)
                activities += a
                orphanActivities += a
            }
        }
        for (res in orphanResults) {
            val a = translateOrphanToolResult(res)
            activities += a
            orphanActivities += a
        }

        // Find index of the last non-question tool call in rawOrder:
        // any brief prose emitted BEFORE a tool call inside the same continuous turn
        // is folded into the chronological work timeline as a Note, keeping the final
        // answer below the work-log card clean (T3Code / Claude Agent pattern).
        val lastToolOrderIdx = rawOrder.indexOfLast { entry ->
            entry is UiPart.ToolCall && translatedByCall.containsKey(entry)
        }
        val hasPostToolText = lastToolOrderIdx >= 0 && rawOrder.drop(lastToolOrderIdx + 1)
            .any { it is UiPart.Text && it.text.trim().isNotEmpty() }
        val timeline = mutableListOf<TurnWorkStep>()
        val finalTexts = mutableListOf<String>()
        rawOrder.forEachIndexed { idx, entry ->
            when (entry) {
                is UiPart.Thinking -> timeline += TurnWorkStep.Thinking(entry.text.trim())
                is UiPart.ToolCall -> translatedByCall[entry]?.let { timeline += TurnWorkStep.Activity(it) }
                is UiPart.Text -> {
                    val t = entry.text.trim()
                    if (t.isNotEmpty()) {
                        val isIntermediateNarration = lastToolOrderIdx >= 0 &&
                            idx < lastToolOrderIdx &&
                            (hasPostToolText || !t.contains("```"))
                        if (isIntermediateNarration) {
                            timeline += TurnWorkStep.Note(t)
                        } else {
                            finalTexts += t
                        }
                    }
                }
            }
        }
        for (oa in orphanActivities) {
            timeline += TurnWorkStep.Activity(oa)
        }

        val hasContent = pendingThinking.isNotEmpty() ||
            activities.isNotEmpty() ||
            timeline.isNotEmpty() ||
            questions.isNotEmpty() ||
            finalTexts.isNotEmpty() ||
            pendingMedia.isNotEmpty()
        if (hasContent) {
            turns += ChatTurn.Assistant(
                key = "asst-$startSeq",
                model = pendingModel,
                thinkingBlocks = pendingThinking.toList(),
                activities = activities,
                timeline = timeline,
                questions = questions,
                textBlocks = finalTexts,
                mediaParts = pendingMedia.toList(),
            )
        }
        pendingStartSeq = null
        pendingModel = null
        pendingThinking.clear()
        pendingCalls.clear()
        pendingResultsById.clear()
        orphanResults.clear()
        pendingTexts.clear()
        pendingMedia.clear()
        rawOrder.clear()
    }

    for (msg in messages) {
        val role = msg.role.uppercase()
        if (role.contains("SYSTEM") || role.contains("DEVELOPER")) {
            continue
        }
        val parts = decodeParts(msg.partsJson)
        if (parts.isEmpty()) continue

        if (role.contains("USER")) {
            // Backward compatibility: some transcripts (e.g. older Claude logs) store
            // tool_result blocks under ROLE_USER. Fold tool-result-only user records
            // into the active Assistant turn instead of splitting or dropping them!
            val toolResults = parts.filterIsInstance<UiPart.ToolResult>()
            val cleanedTexts = parts.filterIsInstance<UiPart.Text>()
                .mapNotNull { tp ->
                    val cleaned = cleanUserPromptText(tp.text)
                    if (cleaned.isNotEmpty()) UiPart.Text(cleaned) else null
                }
            val media = parts.filterIsInstance<UiPart.Media>()
            if (toolResults.isNotEmpty() && cleanedTexts.isEmpty() && media.isEmpty()) {
                if (pendingStartSeq == null) pendingStartSeq = msg.seq
                for (tr in toolResults) {
                    if (tr.callId.isNotBlank()) pendingResultsById[tr.callId] = tr else orphanResults += tr
                }
                continue
            }
            if (cleanedTexts.isEmpty() && media.isEmpty()) {
                continue
            }
            flushAssistant()
            turns += ChatTurn.User(
                key = "user-${msg.seq}",
                textParts = cleanedTexts,
                mediaParts = media,
            )
            continue
        }

        if (pendingStartSeq == null) {
            pendingStartSeq = msg.seq
        }
        if (!msg.model.isNullOrBlank()) {
            pendingModel = msg.model
        }
        for (p in parts) {
            when (p) {
                is UiPart.Thinking -> {
                    val t = p.text.trim()
                    if (t.isNotEmpty()) {
                        pendingThinking += t
                        rawOrder += UiPart.Thinking(t)
                    }
                }
                is UiPart.ToolCall -> {
                    pendingCalls += p
                    rawOrder += p
                }
                is UiPart.ToolResult -> {
                    if (p.callId.isNotBlank()) {
                        pendingResultsById[p.callId] = p
                    } else {
                        orphanResults += p
                    }
                }
                is UiPart.Text -> {
                    val t = p.text.trim()
                    if (t.isNotEmpty()) {
                        pendingTexts += t
                        rawOrder += UiPart.Text(t)
                    }
                }
                is UiPart.Media -> pendingMedia += p
            }
        }
    }
    flushAssistant()
    return turns
}

fun summarizeTurnWorkLog(thinkingCount: Int, activities: List<TranslatedToolActivity>): String {
    val parts = mutableListOf<String>()
    val reads = activities.count { it.kind == ToolActivityKind.READ }
    val edits = activities.count { it.kind == ToolActivityKind.EDIT }
    val cmds = activities.count { it.kind == ToolActivityKind.COMMAND }
    val searches = activities.count { it.kind == ToolActivityKind.SEARCH }
    val webs = activities.count { it.kind == ToolActivityKind.WEB }
    val agents = activities.count { it.kind == ToolActivityKind.AGENT }
    val others = activities.count { it.kind == ToolActivityKind.OTHER }

    if (reads > 0) parts += "读取 $reads 个文件"
    if (edits > 0) parts += "修改 $edits 个文件"
    if (cmds > 0) parts += "运行 $cmds 条命令"
    if (searches > 0) parts += "搜索 $searches 次"
    if (webs > 0) parts += "检索网页 $webs 次"
    if (agents > 0) parts += "委派 $agents 个子任务"
    if (others > 0) parts += "$others 项工具操作"

    return when {
        activities.isNotEmpty() && parts.isNotEmpty() -> {
            val prefix = if (activities.any { it.isRunning }) "正在执行" else "已执行 ${activities.size} 步"
            "$prefix · ${parts.take(3).joinToString("、")}"
        }
        thinkingCount > 0 -> "已思考（点击展开摘要）"
        else -> "执行过程"
    }
}

private fun translateToolActivity(call: UiPart.ToolCall, result: UiPart.ToolResult?): TranslatedToolActivity {
    val normName = normalizeToolName(call.name)
    val obj = parseJsonObject(call.inputJson)
    val statusRaw = call.status.uppercase()
    val isRunning = statusRaw.contains("RUNNING") || statusRaw.contains("PENDING")
    val isError = (!isRunning && result?.isError == true) || statusRaw.contains("ERROR") || statusRaw.contains("FAILED")
    val statusLabel = when {
        isError -> "失败"
        isRunning -> "执行中"
        else -> "完成"
    }

    val kind = classifyToolKind(normName)
    val verb = when (kind) {
        ToolActivityKind.COMMAND -> "运行命令"
        ToolActivityKind.READ -> "读取文件"
        ToolActivityKind.EDIT -> if (normName.contains("write") || normName.contains("create")) "写入文件" else "修改文件"
        ToolActivityKind.SEARCH -> if (normName.contains("list") || normName.contains("dir")) "查看目录" else "搜索代码"
        ToolActivityKind.WEB -> "检索网页"
        ToolActivityKind.AGENT -> "委派子任务"
        ToolActivityKind.OTHER -> normName.ifBlank { "工具调用" }
    }

    val target = extractToolTarget(kind, normName, obj, call.inputJson)
    val (diffText, added, removed) = if (kind == ToolActivityKind.EDIT) {
        extractDiffInfo(obj, call.inputJson)
    } else {
        Triple("", 0, 0)
    }

    val detailInput = when (kind) {
        ToolActivityKind.COMMAND -> extractStringArg(obj, "command", "cmd", "CommandLine", "script") ?: call.inputJson
        ToolActivityKind.EDIT -> if (diffText.isNotEmpty()) "" else call.inputJson
        else -> call.inputJson
    }.trim()

    return TranslatedToolActivity(
        callId = call.callId,
        kind = kind,
        verb = verb,
        target = target,
        statusLabel = statusLabel,
        isRunning = isRunning,
        isError = isError,
        addedLines = added,
        removedLines = removed,
        detailInput = detailInput,
        detailDiff = diffText,
        detailOutput = result?.output?.trim().orEmpty(),
        rawToolName = call.name,
    )
}

private fun translateOrphanToolResult(res: UiPart.ToolResult): TranslatedToolActivity {
    val firstLine = res.output.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
    return TranslatedToolActivity(
        callId = res.callId,
        kind = ToolActivityKind.OTHER,
        verb = if (res.isError) "执行出错" else "工具输出",
        target = firstLine.take(64),
        statusLabel = if (res.isError) "失败" else "完成",
        isRunning = false,
        isError = res.isError,
        detailOutput = res.output.trim(),
    )
}

private fun normalizeToolName(raw: String): String {
    var name = raw.trim().lowercase()
    if (name.startsWith("mcp_")) {
        name = name.substringAfterLast("__").substringAfterLast("_fast_")
    }
    return name.substringAfterLast('.')
}

private fun classifyToolKind(normName: String): ToolActivityKind = when {
    normName in setOf("bash", "sh", "shell", "exec", "exec_command", "run_command", "run_checks", "terminal") ||
        normName.contains("command") || normName.contains("shell") || normName.contains("bash") -> ToolActivityKind.COMMAND

    normName in setOf("read", "read_file", "view_file", "view_file_outline", "read_slices", "quick_outline", "cat") ||
        normName.startsWith("read_") || normName.startsWith("view_") -> ToolActivityKind.READ

    normName in setOf("edit", "edit_file", "write", "write_file", "apply_patch", "apply_edits", "replace_file_content", "multi_replace_file_content", "patch") ||
        normName.contains("edit") || normName.contains("write") || normName.contains("patch") || normName.contains("replace") -> ToolActivityKind.EDIT

    normName in setOf("grep", "rg", "glob", "find", "search", "grep_search", "find_by_name", "list_dir", "batch_search", "ls") ||
        normName.contains("search") || normName.contains("grep") || normName.contains("glob") || normName.contains("find") || normName.contains("list_") -> ToolActivityKind.SEARCH

    normName.contains("web") || normName.contains("fetch") || normName.contains("browser") || normName.contains("url") -> ToolActivityKind.WEB

    normName.contains("subagent") || normName == "task" || normName == "agent" -> ToolActivityKind.AGENT

    else -> ToolActivityKind.OTHER
}

private fun parseJsonObject(raw: String): JsonObject? {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("{")) return null
    return runCatching { lenientJson.parseToJsonElement(trimmed) as? JsonObject }.getOrNull()
}

private fun extractStringArg(obj: JsonObject?, vararg keys: String): String? {
    if (obj == null) return null
    for (k in keys) {
        val v = obj[k] ?: obj.entries.firstOrNull { it.key.equals(k, ignoreCase = true) }?.value
        val s = (v as? JsonPrimitive)?.contentOrNull?.trim()
        if (!s.isNullOrEmpty()) return s
    }
    return null
}

private fun extractIntArg(obj: JsonObject?, vararg keys: String): Int? {
    if (obj == null) return null
    for (k in keys) {
        val v = obj[k] ?: obj.entries.firstOrNull { it.key.equals(k, ignoreCase = true) }?.value
        val n = (v as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
        if (n != null) return n
    }
    return null
}

private fun extractToolTarget(kind: ToolActivityKind, normName: String, obj: JsonObject?, rawInput: String): String {
    val candidate = when (kind) {
        ToolActivityKind.COMMAND -> {
            extractStringArg(obj, "toolSummary", "command", "cmd", "CommandLine", "script")
                ?: (obj?.get("commands") as? JsonArray)?.firstOrNull()?.let { el ->
                    extractStringArg(el as? JsonObject, "name", "command")
                }
        }
        ToolActivityKind.READ -> {
            val p = extractStringArg(obj, "path", "Path", "file_path", "AbsolutePath", "file")
                ?: (obj?.get("paths") as? JsonArray)?.firstOrNull()?.let { (it as? JsonPrimitive)?.contentOrNull }
            val start = extractIntArg(obj, "StartLine", "start_line", "offset")
            val end = extractIntArg(obj, "EndLine", "end_line")
            val base = p?.substringAfterLast('/')
            when {
                base != null && start != null && end != null -> "$base · L$start-$end"
                base != null && start != null -> "$base · L$start"
                base != null -> base
                else -> extractStringArg(obj, "toolSummary")
            }
        }
        ToolActivityKind.EDIT -> {
            val p = extractStringArg(obj, "path", "Path", "file_path", "TargetFile", "file")
                ?: (obj?.get("edits") as? JsonArray)?.mapNotNull { el ->
                    extractStringArg(el as? JsonObject, "path")?.substringAfterLast('/')
                }?.distinct()?.take(2)?.joinToString(", ")
            p?.substringAfterLast('/')
        }
        ToolActivityKind.SEARCH -> {
            extractStringArg(obj, "pattern", "query", "Query", "Pattern", "glob", "toolSummary")
                ?: (obj?.get("queries") as? JsonArray)?.firstOrNull()?.let { el ->
                    extractStringArg(el as? JsonObject, "pattern")
                }
                ?: extractStringArg(obj, "path", "SearchPath", "DirectoryPath")?.substringAfterLast('/')
        }
        ToolActivityKind.WEB -> extractStringArg(obj, "query", "url", "Url")
        ToolActivityKind.AGENT -> extractStringArg(obj, "Role", "role", "description", "Prompt")
        ToolActivityKind.OTHER -> extractStringArg(obj, "toolSummary", "path", "query", "command", "name")
    }

    val cleaned = candidate
        ?.lineSequence()
        ?.map { it.trim() }
        ?.firstOrNull { it.isNotEmpty() }
        ?: rawInput.trim().takeIf { it.isNotEmpty() && !it.startsWith("{") }
        ?: normName
    return if (cleaned.length <= 72) cleaned else cleaned.take(71) + "…"
}

private fun extractDiffInfo(obj: JsonObject?, rawInput: String): Triple<String, Int, Int> {
    val patch = extractStringArg(obj, "patch", "diff")
        ?: rawInput.takeIf { it.contains("*** Update File:") || it.contains("@@ ") }
    if (!patch.isNullOrBlank()) {
        var added = 0
        var removed = 0
        patch.lineSequence().forEach { ln ->
            when {
                ln.startsWith("+") && !ln.startsWith("+++") -> added++
                ln.startsWith("-") && !ln.startsWith("---") -> removed++
            }
        }
        return Triple(patch.trim(), added, removed)
    }

    val oldText = extractStringArg(obj, "Target", "old_string", "TargetContent", "old_str")
    val newText = extractStringArg(obj, "Replacement", "new_string", "ReplacementContent", "new_str")
    if (oldText != null || newText != null) {
        val oldLines = oldText?.lines().orEmpty()
        val newLines = newText?.lines().orEmpty()
        val diff = buildString {
            oldLines.take(40).forEach { appendLine("- $it") }
            if (oldLines.size > 40) appendLine("- … (${oldLines.size - 40} more lines)")
            newLines.take(40).forEach { appendLine("+ $it") }
            if (newLines.size > 40) appendLine("+ … (${newLines.size - 40} more lines)")
        }.trim()
        return Triple(diff, newLines.size, oldLines.size)
    }

    val content = extractStringArg(obj, "Content", "content", "CodeContent")
    if (content != null) {
        val lines = content.lines()
        val preview = lines.take(40).joinToString("\n") { "+ $it" } +
            if (lines.size > 40) "\n+ … (${lines.size - 40} more lines)" else ""
        return Triple(preview, lines.size, 0)
    }
    return Triple("", 0, 0)
}

private fun isInteractiveQuestionTool(normName: String): Boolean =
    normName in setOf("ask_user_question", "askuserquestion", "ask_question")

private fun extractInteractiveQuestions(call: UiPart.ToolCall, result: UiPart.ToolResult?): List<ChatQuestionPrompt> {
    val normName = normalizeToolName(call.name)
    if (!isInteractiveQuestionTool(normName)) return emptyList()
    val obj = parseJsonObject(call.inputJson) ?: return emptyList()
    val arr = obj["questions"] as? JsonArray ?: return emptyList()
    val answerSummary = result?.output?.trim().orEmpty()
    val answered = result != null
    val out = mutableListOf<ChatQuestionPrompt>()
    for (el in arr) {
        val qObj = el as? JsonObject ?: continue
        val qText = extractStringArg(qObj, "question") ?: continue
        val header = extractStringArg(qObj, "header").orEmpty()
        val multi = (qObj["multiSelect"] as? JsonPrimitive)?.contentOrNull?.equals("true", ignoreCase = true) == true ||
            (qObj["is_multi_select"] as? JsonPrimitive)?.contentOrNull?.equals("true", ignoreCase = true) == true
        val opts = (qObj["options"] as? JsonArray)?.mapNotNull { optEl ->
            when (optEl) {
                is JsonPrimitive -> optEl.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.let { ChatQuestionOption(it) }
                is JsonObject -> {
                    val label = extractStringArg(optEl, "label", "title", "text") ?: return@mapNotNull null
                    val desc = extractStringArg(optEl, "description", "detail").orEmpty()
                    ChatQuestionOption(label = label, description = desc)
                }
                else -> null
            }
        }.orEmpty()
        out += ChatQuestionPrompt(
            callId = call.callId,
            header = header,
            question = qText,
            options = opts,
            multiSelect = multi,
            answered = answered,
            answerSummary = answerSummary,
        )
    }
    return out
}

private val borderDecorChars = " \t─━═-_~·•●◦○◌◆▸⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏✶✽✻✢✳◐◓◑◒╭╮╰╯┌┐└┘│├┤┬┴┼*=".toSet()
private val statusKeywords = listOf(
    "working",
    "thinking",
    "generating",
    "pondering",
    "brewing",
    "reasoning",
    "compacting",
    "auto-compacting",
    "retrying",
    "summarizing",
    "loading",
)

/**
 * Defensive client-side sanitizer for live StreamingDraft text: strips residual
 * terminal spinner borders (e.g. `----working-----`, `── ⠋ Working... ───`)
 * and pure horizontal box rules before rendering in Chat.
 */
fun sanitizeStreamingDraftText(raw: String): String {
    if (raw.isBlank()) return ""
    val kept = mutableListOf<String>()
    for (line in raw.lines()) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            if (kept.isNotEmpty() && kept.last().isNotEmpty()) kept += ""
            continue
        }
        val stripped = trimmed.trim { it in borderDecorChars }
        if (stripped.isEmpty()) continue
        val low = stripped.lowercase()
        val hasRuleEdge = trimmed.startsWith("--") || trimmed.startsWith("──") ||
            trimmed.startsWith("━━") || trimmed.endsWith("--") || trimmed.endsWith("──")
        val isSpinnerLead = trimmed.first() in "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏✶✽✻✢✳◦◌◐◓◑◒"
        val isStatusText = statusKeywords.any { low.startsWith(it) } ||
            low.contains("esc to interrupt") || low.contains("esc to cancel") || low.contains("ctrl+c to")
        if (isStatusText && (hasRuleEdge || isSpinnerLead || stripped.length <= 48)) {
            continue
        }
        kept += line.trimEnd()
    }
    while (kept.isNotEmpty() && kept.last().isBlank()) {
        kept.removeAt(kept.lastIndex)
    }
    return kept.joinToString("\n").trim()
}
