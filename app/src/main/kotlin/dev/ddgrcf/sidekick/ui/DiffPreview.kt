package dev.ddgrcf.sidekick.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras

enum class DiffLineKind {
    CONTEXT,
    ADD,
    REMOVE,
    HUNK,
    META,
}

data class DiffLine(
    val kind: DiffLineKind,
    val oldLine: Int?,
    val newLine: Int?,
    val text: String,
)

data class ParsedFileDiff(
    val path: String,
    val lines: List<DiffLine>,
    val added: Int,
    val removed: Int,
)

private val hunkHeaderRegex = Regex("""^@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@""")

fun parseUnifiedDiffByFile(rawDiff: String): Map<String, ParsedFileDiff> {
    if (rawDiff.isBlank()) return emptyMap()
    val rawSections = mutableListOf<List<String>>()
    var current = mutableListOf<String>()
    for (line in rawDiff.replace("\r\n", "\n").lines()) {
        if (line.startsWith("diff --git ")) {
            if (current.isNotEmpty()) rawSections += current
            current = mutableListOf(line)
        } else {
            current += line
        }
    }
    if (current.isNotEmpty()) rawSections += current

    val merged = LinkedHashMap<String, ParsedFileDiff>()
    for (sectionLines in rawSections) {
        val header = sectionLines.firstOrNull { it.startsWith("diff --git ") }.orEmpty()
        var pathFromPlus = ""
        var pathFromMinus = ""
        for (ln in sectionLines) {
            when {
                ln.startsWith("+++ b/") -> pathFromPlus = ln.removePrefix("+++ b/").trim()
                ln.startsWith("--- a/") -> pathFromMinus = ln.removePrefix("--- a/").trim()
            }
        }
        val headerFallback = if (header.isNotEmpty()) {
            header.removePrefix("diff --git ").split(" b/", limit = 2).lastOrNull()?.trim().orEmpty()
        } else {
            ""
        }
        val filePath = pathFromPlus.ifBlank { pathFromMinus }.ifBlank { headerFallback }
        if (filePath.isBlank()) continue

        val parsed = parseSectionLines(sectionLines)
        val existing = merged[filePath]
        if (existing == null) {
            merged[filePath] = ParsedFileDiff(
                path = filePath,
                lines = parsed.lines,
                added = parsed.added,
                removed = parsed.removed,
            )
        } else {
            merged[filePath] = ParsedFileDiff(
                path = filePath,
                lines = existing.lines + parsed.lines,
                added = existing.added + parsed.added,
                removed = existing.removed + parsed.removed,
            )
        }
    }
    return merged
}

fun parseSinglePatchSnippet(rawPatch: String): List<DiffLine> {
    if (rawPatch.isBlank()) return emptyList()
    val lines = rawPatch.replace("\r\n", "\n").lines()
    return parseSectionLines(lines).lines
}

private data class SectionParseResult(
    val lines: List<DiffLine>,
    val added: Int,
    val removed: Int,
)

private fun parseSectionLines(sectionLines: List<String>): SectionParseResult {
    val out = ArrayList<DiffLine>(sectionLines.size)
    var inHunk = false
    var oldCur = 0
    var newCur = 0
    var added = 0
    var removed = 0

    for (ln in sectionLines) {
        val hunkMatch = hunkHeaderRegex.find(ln)
        if (hunkMatch != null) {
            oldCur = hunkMatch.groupValues[1].toIntOrNull() ?: 1
            newCur = hunkMatch.groupValues[2].toIntOrNull() ?: 1
            inHunk = true
            out += DiffLine(DiffLineKind.HUNK, null, null, ln)
            continue
        }
        if (ln.startsWith("diff --git ") ||
            ln.startsWith("index ") ||
            ln.startsWith("new file mode ") ||
            ln.startsWith("deleted file mode ") ||
            ln.startsWith("--- ") ||
            ln.startsWith("+++ ")
        ) {
            continue
        }
        if (ln.startsWith("rename from ") ||
            ln.startsWith("rename to ") ||
            ln.startsWith("\\ No newline")
        ) {
            out += DiffLine(DiffLineKind.META, null, null, ln)
            continue
        }
        when {
            ln.startsWith("+") -> {
                val n = if (inHunk) newCur++ else null
                added++
                out += DiffLine(DiffLineKind.ADD, null, n, ln)
            }
            ln.startsWith("-") -> {
                val o = if (inHunk && oldCur > 0) oldCur++ else null
                removed++
                out += DiffLine(DiffLineKind.REMOVE, o, null, ln)
            }
            inHunk && (ln.startsWith(" ") || ln.isEmpty()) -> {
                val o = if (oldCur > 0) oldCur++ else null
                val n = if (newCur > 0) newCur++ else null
                out += DiffLine(DiffLineKind.CONTEXT, o, n, ln)
            }
            else -> {
                out += DiffLine(DiffLineKind.CONTEXT, null, null, ln)
            }
        }
    }
    return SectionParseResult(out, added, removed)
}

fun formatDiffStats(added: Int, removed: Int): String = "+$added -$removed"

@Composable
fun DiffStatText(
    added: Int,
    removed: Int,
    modifier: Modifier = Modifier,
) {
    if (added <= 0 && removed <= 0) return
    val extras = SidekickThemeExtras.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (added > 0) {
            Text(
                text = "+$added",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = extras.success,
            )
        }
        if (removed > 0) {
            Text(
                text = "-$removed",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
fun DiffCodeBlock(
    lines: List<DiffLine>,
    modifier: Modifier = Modifier,
    maxLines: Int = 100,
    framed: Boolean = true,
    onCopyDiff: (() -> Unit)? = null,
    onOpenFullscreen: (() -> Unit)? = null,
) {
    if (lines.isEmpty()) return
    val extras = SidekickThemeExtras.colors
    val visible = remember(lines, maxLines) { lines.take(maxLines) }
    val truncated = lines.size > maxLines
    val addedCount = remember(lines) { lines.count { it.kind == DiffLineKind.ADD } }
    val removedCount = remember(lines) { lines.count { it.kind == DiffLineKind.REMOVE } }
    val hasLineNums = remember(visible) { visible.any { it.oldLine != null || it.newLine != null } }
    val hScroll = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (framed) extras.codeBg else Color.Transparent,
        border = if (framed) BorderStroke(0.5.dp, extras.hairline) else null,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (onCopyDiff != null || onOpenFullscreen != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (framed) 10.dp else 0.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DiffStatText(
                        added = addedCount,
                        removed = removedCount,
                        modifier = Modifier.weight(1f),
                    )
                    if (onCopyDiff != null) {
                        IconButton(
                            onClick = onCopyDiff,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "复制 Diff",
                                tint = extras.codeText.copy(alpha = 0.78f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    if (onOpenFullscreen != null) {
                        IconButton(
                            onClick = onOpenFullscreen,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "全屏查看 Diff",
                                tint = extras.codeText.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(hScroll),
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 320.dp)
                            .padding(vertical = 6.dp),
                    ) {
                        visible.forEach { ln ->
                            DiffCodeRow(ln = ln, showGutter = hasLineNums)
                        }
                    }
                }
            }
            if (truncated) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "仅显示前 $maxLines 行（共 ${lines.size} 行）",
                        style = MaterialTheme.typography.labelSmall,
                        color = extras.codeText.copy(alpha = 0.65f),
                        modifier = Modifier.weight(1f),
                    )
                    if (onOpenFullscreen != null) {
                        TextButton(
                            onClick = onOpenFullscreen,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text("全屏查看全部")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffCodeRow(
    ln: DiffLine,
    showGutter: Boolean,
    hScroll: ScrollState? = null,
) {
    val extras = SidekickThemeExtras.colors
    val errorColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary
    val bg = when (ln.kind) {
        DiffLineKind.ADD -> extras.success.copy(alpha = 0.14f)
        DiffLineKind.REMOVE -> errorColor.copy(alpha = 0.14f)
        DiffLineKind.HUNK -> primaryColor.copy(alpha = 0.10f)
        DiffLineKind.CONTEXT, DiffLineKind.META -> Color.Transparent
    }
    val fg = when (ln.kind) {
        DiffLineKind.ADD -> extras.success
        DiffLineKind.REMOVE -> errorColor
        DiffLineKind.HUNK -> primaryColor
        DiffLineKind.META -> extras.codeText.copy(alpha = 0.58f)
        DiffLineKind.CONTEXT -> extras.codeText.copy(alpha = 0.88f)
    }

    val baseMod = if (hScroll != null) {
        Modifier
            .fillMaxWidth()
            .background(bg)
            .horizontalScroll(hScroll)
    } else {
        Modifier
            .widthIn(min = 320.dp)
            .background(bg)
    }

    Row(
        modifier = baseMod.padding(horizontal = 10.dp, vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showGutter) {
            val oldStr = ln.oldLine?.toString().orEmpty()
            val newStr = ln.newLine?.toString().orEmpty()
            Text(
                text = oldStr.padStart(4, ' '),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, lineHeight = 16.sp),
                color = extras.codeText.copy(alpha = 0.42f),
                modifier = Modifier.widthIn(min = 32.dp),
            )
            Text(
                text = newStr.padStart(4, ' '),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, lineHeight = 16.sp),
                color = extras.codeText.copy(alpha = 0.42f),
                modifier = Modifier.widthIn(min = 32.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = ln.text,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            color = fg,
            softWrap = false,
        )
    }
}

@Composable
fun FullscreenDiffDialog(
    title: String,
    subtitle: String,
    lines: List<DiffLine>,
    onCopyDiff: () -> Unit,
    onDismiss: () -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    val hasLineNums = remember(lines) { lines.any { it.oldLine != null || it.newLine != null } }
    val sharedHScroll = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("fullscreen_diff_dialog"),
            color = extras.codeBg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "关闭全屏 Diff",
                            tint = extras.codeText,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = extras.codeText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = extras.codeText.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(
                        onClick = onCopyDiff,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "复制 Diff",
                            tint = extras.codeText,
                        )
                    }
                }
                HairlineDivider()
                SelectionContainer(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(lines, key = { idx, _ -> idx }) { _, ln ->
                            DiffCodeRow(ln = ln, showGutter = hasLineNums, hScroll = sharedHScroll)
                        }
                    }
                }
            }
        }
    }
}
