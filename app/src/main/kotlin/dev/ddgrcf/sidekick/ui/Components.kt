package dev.ddgrcf.sidekick.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras

@Composable
fun SidekickCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = SidekickThemeExtras.colors.cardBorder,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun ConnectionStatusPill(
    state: ConnectionState,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val extras = SidekickThemeExtras.colors
    val dotColor: Color
    val bgColor: Color
    val textColor: Color
    val label: String
    when (state) {
        ConnectionState.CONNECTED -> {
            dotColor = extras.success
            bgColor = extras.successContainer
            textColor = extras.onSuccessContainer
            label = "已连接"
        }
        ConnectionState.CONNECTING -> {
            dotColor = extras.warning
            bgColor = extras.warningContainer
            textColor = extras.onWarningContainer
            label = "连接中"
        }
        ConnectionState.OFFLINE -> {
            dotColor = MaterialTheme.colorScheme.error
            bgColor = MaterialTheme.colorScheme.errorContainer
            textColor = MaterialTheme.colorScheme.onErrorContainer
            label = "离线"
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusPulseDot(color = dotColor, pulse = state == ConnectionState.CONNECTING)
            if (!compact) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun StatusPulseDot(color: Color, pulse: Boolean) {
    if (pulse) {
        val transition = rememberInfiniteTransition(label = "conn_pulse")
        val alpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
            label = "conn_alpha",
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer { this.alpha = alpha }
                .clip(CircleShape)
                .background(color),
        )
    } else {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun ConnectionDot(state: ConnectionState) {
    val extras = SidekickThemeExtras.colors
    val color = when (state) {
        ConnectionState.CONNECTED -> extras.success
        ConnectionState.CONNECTING -> extras.warning
        ConnectionState.OFFLINE -> MaterialTheme.colorScheme.error
    }
    StatusPulseDot(color = color, pulse = state == ConnectionState.CONNECTING)
}

@Composable
fun ToolBadge(
    tool: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val isDark = SidekickThemeExtras.colors.isDark
    val fg = ToolVisuals.color(tool, isDark)
    val bg = ToolVisuals.containerColor(tool, isDark)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ToolVisuals.icon(tool),
            contentDescription = tool,
            tint = fg,
            modifier = Modifier.size(size * 0.54f),
        )
    }
}

@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    val extras = SidekickThemeExtras.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = extras.successContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(extras.success),
            )
            Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = extras.onSuccessContainer,
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!trailing.isNullOrBlank()) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SidekickCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
