package dev.ddgrcf.sidekick.core.ansi

/**
 * Pure-JVM ANSI SGR parser supporting:
 * 0 (reset), 1 (bold), 3 (italic), 4 (underline), 7 (reverse),
 * 30-37, 90-97, 38;5;n, 38;2;r;g;b, 39 (reset fg),
 * 40-47, 100-107, 48;5;n, 48;2;r;g;b, 49 (reset bg).
 * All other CSI/OSC/ESC sequences are cleanly stripped.
 */
data class AnsiStyle(
    val fgArgb: Long? = null,
    val bgArgb: Long? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val reverse: Boolean = false,
)

data class AnsiSpan(
    val text: String,
    val style: AnsiStyle,
)

data class AnsiLine(
    val spans: List<AnsiSpan>,
)

object AnsiParser {
    private val basicFg = longArrayOf(
        0xFF1E1E2E, 0xFFF38BA8, 0xFFA6E3A1, 0xFFF9E2AF,
        0xFF89B4FA, 0xFFCBA6F7, 0xFF94E2D5, 0xFFCDD6F4,
    )
    private val brightFg = longArrayOf(
        0xFF585B70, 0xFFF38BA8, 0xFFA6E3A1, 0xFFF9E2AF,
        0xFF89B4FA, 0xFFCBA6F7, 0xFF94E2D5, 0xFFFFFFFF,
    )

    fun parse(input: String): List<AnsiLine> {
        val normalized = input.replace("\r\n", "\n").replace("\r", "")
        val lines = ArrayList<AnsiLine>()
        var currentSpans = ArrayList<AnsiSpan>()
        var style = AnsiStyle()
        val buf = StringBuilder()

        fun flushBuf() {
            if (buf.isNotEmpty()) {
                currentSpans.add(AnsiSpan(buf.toString(), style))
                buf.setLength(0)
            }
        }

        fun flushLine() {
            flushBuf()
            lines.add(AnsiLine(currentSpans))
            currentSpans = ArrayList()
        }

        var i = 0
        val n = normalized.length
        while (i < n) {
            val ch = normalized[i]
            if (ch == '\n') {
                flushLine()
                i++
                continue
            }
            if (ch == '\u001B') {
                if (i + 1 < n && normalized[i + 1] == '[') {
                    // CSI sequence: ESC [ ... final_byte (@-~, 0x40..0x7E)
                    var j = i + 2
                    while (j < n && normalized[j].code !in 0x40..0x7E) {
                        j++
                    }
                    if (j < n) {
                        val finalByte = normalized[j]
                        if (finalByte == 'm') {
                            flushBuf()
                            val paramsStr = normalized.substring(i + 2, j)
                            style = applySgr(style, paramsStr)
                        }
                        i = j + 1
                        continue
                    }
                } else if (i + 1 < n && normalized[i + 1] == ']') {
                    // OSC sequence terminated by BEL (\u0007) or ST (ESC \)
                    var j = i + 2
                    while (j < n) {
                        if (normalized[j] == '\u0007') {
                            j++
                            break
                        }
                        if (normalized[j] == '\u001B' && j + 1 < n && normalized[j + 1] == '\\') {
                            j += 2
                            break
                        }
                        j++
                    }
                    i = j
                    continue
                } else if (i + 1 < n) {
                    i += 2
                    continue
                }
            }
            buf.append(ch)
            i++
        }
        flushLine()
        return lines
    }

    private fun applySgr(initial: AnsiStyle, paramsStr: String): AnsiStyle {
        if (paramsStr.isEmpty()) return AnsiStyle()
        val parts = paramsStr.split(';')
        var s = initial
        var idx = 0
        while (idx < parts.size) {
            val code = parts[idx].toIntOrNull() ?: 0
            when (code) {
                0 -> s = AnsiStyle()
                1 -> s = s.copy(bold = true)
                3 -> s = s.copy(italic = true)
                4 -> s = s.copy(underline = true)
                7 -> s = s.copy(reverse = true)
                22 -> s = s.copy(bold = false)
                23 -> s = s.copy(italic = false)
                24 -> s = s.copy(underline = false)
                27 -> s = s.copy(reverse = false)
                in 30..37 -> s = s.copy(fgArgb = basicFg[code - 30])
                39 -> s = s.copy(fgArgb = null)
                in 40..47 -> s = s.copy(bgArgb = basicFg[code - 40])
                49 -> s = s.copy(bgArgb = null)
                in 90..97 -> s = s.copy(fgArgb = brightFg[code - 90])
                in 100..107 -> s = s.copy(bgArgb = brightFg[code - 100])
                38, 48 -> {
                    val isFg = code == 38
                    val mode = parts.getOrNull(idx + 1)?.toIntOrNull()
                    if (mode == 5 && idx + 2 < parts.size) {
                        val colorIdx = parts[idx + 2].toIntOrNull() ?: 0
                        val argb = xterm256(colorIdx)
                        s = if (isFg) s.copy(fgArgb = argb) else s.copy(bgArgb = argb)
                        idx += 2
                    } else if (mode == 2 && idx + 4 < parts.size) {
                        val r = (parts[idx + 2].toIntOrNull() ?: 0).coerceIn(0, 255)
                        val g = (parts[idx + 3].toIntOrNull() ?: 0).coerceIn(0, 255)
                        val b = (parts[idx + 4].toIntOrNull() ?: 0).coerceIn(0, 255)
                        val argb = (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
                        s = if (isFg) s.copy(fgArgb = argb) else s.copy(bgArgb = argb)
                        idx += 4
                    }
                }
            }
            idx++
        }
        return s
    }

    private fun xterm256(index: Int): Long {
        val c = index.coerceIn(0, 255)
        if (c < 8) return basicFg[c]
        if (c < 16) return brightFg[c - 8]
        if (c < 232) {
            val n = c - 16
            val r = n / 36
            val g = (n % 36) / 6
            val b = n % 6
            fun level(v: Int): Long = if (v == 0) 0L else (55 + v * 40).toLong()
            return (0xFFL shl 24) or (level(r) shl 16) or (level(g) shl 8) or level(b)
        }
        val gray = (8 + (c - 232) * 10).toLong()
        return (0xFFL shl 24) or (gray shl 16) or (gray shl 8) or gray
    }
}
