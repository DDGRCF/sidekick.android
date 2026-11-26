package dev.ddgrcf.sidekick.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.core.relay.RelayDevice
import dev.ddgrcf.sidekick.core.store.SecretStore
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras
import dev.ddgrcf.sidekick.vm.HomeViewModel

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onOpenBridge: (RelayDevice) -> Unit,
    onOpenPair: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val connState by vm.connectionState.collectAsStateWithLifecycle()
    val bridges by vm.bridges.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    HomeContent(
        connectionState = connState,
        bridges = bridges,
        refreshing = refreshing,
        error = error,
        onRefresh = vm::refresh,
        onOpenBridge = onOpenBridge,
        onOpenPair = onOpenPair,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    connectionState: ConnectionState,
    bridges: List<RelayDevice>,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onOpenBridge: (RelayDevice) -> Unit,
    onOpenPair: () -> Unit,
    onOpenSettings: () -> Unit,
    isDemo: Boolean = false,
) {
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
                        Text("我的电脑")
                        ConnectionStatusPill(connectionState)
                        if (isDemo) {
                            DemoTag()
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "设置")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenPair,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 1.dp,
                    pressedElevation = 2.dp,
                    focusedElevation = 1.dp,
                    hoveredElevation = 2.dp,
                ),
                icon = {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = "添加电脑")
                },
                text = { Text("添加电脑") },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
            ) {
                if (!error.isNullOrBlank()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            ErrorBanner(
                                message = error,
                                actionLabel = "重试",
                                onAction = onRefresh,
                            )
                        }
                    }
                }
                when {
                    refreshing && bridges.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            }
                        }
                    }
                    bridges.isEmpty() && error.isNullOrBlank() -> {
                        item {
                            Spacer(Modifier.height(16.dp))
                            EmptyStateView(
                                icon = Icons.Rounded.Computer,
                                title = "尚未配对电脑",
                                subtitle = "在电脑端运行 sidekick-bridge enroll 后，点击下方按钮扫描二维码完成加密配对",
                                actionLabel = "扫码添加电脑",
                                onAction = onOpenPair,
                            )
                        }
                    }
                    bridges.isNotEmpty() -> {
                        item {
                            SectionHeader(
                                title = "已配对工作站",
                                trailing = "${bridges.size} 台",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            HairlineDivider()
                        }
                        itemsIndexed(bridges, key = { _, it -> it.id }) { idx, dev ->
                            BridgeDeviceRow(
                                dev = dev,
                                onClick = { onOpenBridge(dev) },
                            )
                            val nextOnline = bridges.getOrNull(idx + 1)?.online
                            if (idx < bridges.lastIndex && dev.online && nextOnline == true) {
                                HairlineDivider(Modifier.padding(start = 54.dp))
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

@Composable
internal fun DemoTag(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = "DEMO",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun BridgeDeviceRow(
    dev: RelayDevice,
    onClick: () -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    val fp = SecretStore.fingerprintFromBase64(dev.pubkey)
    val rowBg = if (dev.online) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val mutedVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        enabled = dev.online,
        color = rowBg,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 68.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Computer,
                contentDescription = null,
                tint = if (dev.online) MaterialTheme.colorScheme.primary else mutedVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(22.dp),
            )

            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = dev.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (dev.online) MaterialTheme.colorScheme.onSurface else mutedVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (fp.isNotBlank()) {
                    Text(
                        text = "指纹 $fp",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (dev.online) mutedVariant else mutedVariant.copy(alpha = 0.72f),
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            Text(
                text = if (dev.online) "在线" else "离线",
                style = MaterialTheme.typography.labelMedium,
                color = if (dev.online) extras.success else mutedVariant.copy(alpha = 0.78f),
            )

            if (dev.online) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = mutedVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
