package dev.ddgrcf.sidekick.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object ToolVisuals {
    fun color(tool: String): Color = color(tool, isDark = true)

    fun color(tool: String, isDark: Boolean): Color = when (tool.lowercase()) {
        "codex" -> if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
        "claude" -> if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
        "pi" -> if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE)
        "devin" -> if (isDark) Color(0xFF22D3EE) else Color(0xFF0E7490)
        "antigravity", "agy" -> if (isDark) Color(0xFF34D399) else Color(0xFF047857)
        else -> if (isDark) Color(0xFF8B949E) else Color(0xFF475569)
    }

    fun icon(tool: String): ImageVector = when (tool.lowercase()) {
        "codex" -> Icons.Rounded.Code
        "claude" -> Icons.Rounded.Psychology
        "pi" -> Icons.Rounded.Bolt
        "devin" -> Icons.Rounded.SmartToy
        "antigravity", "agy" -> Icons.Rounded.Terminal
        else -> Icons.Rounded.Terminal
    }

    fun label(tool: String): String = when (tool.lowercase()) {
        "antigravity" -> "agy"
        else -> tool
    }
}
