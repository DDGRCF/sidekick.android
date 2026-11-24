package dev.ddgrcf.sidekick.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("我的电脑")
                        ConnectionStatusPill(connectionState)
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!error.isNullOrBlank()) {
                    item {
                        ErrorBanner(error)
                    }
                }
                if (bridges.isEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        EmptyStateCard(
                            icon = Icons.Rounded.Computer,
                            title = "尚未配对电脑",
                            subtitle = "在电脑端运行 sidekick-bridge enroll 后，点击下方按钮扫描二维码完成加密配对",
                            actionLabel = "扫码添加电脑",
                            onAction = onOpenPair,
                        )
                    }
                } else {
                    item {
                        SectionHeader(title = "已配对工作站", trailing = "${bridges.size} 台")
                    }
                    items(bridges, key = { it.id }) { dev ->
                        BridgeDeviceCard(
                            dev = dev,
                            onClick = { onOpenBridge(dev) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BridgeDeviceCard(
    dev: RelayDevice,
    onClick: () -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    val fp = SecretStore.fingerprintFromBase64(dev.pubkey)

    SidekickCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dev.online) 1f else 0.6f),
        onClick = onClick,
        enabled = dev.online,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (dev.online) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Computer,
                    contentDescription = null,
                    tint = if (dev.online) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = dev.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (fp.isNotBlank()) {
                    Text(
                        text = "指纹 $fp",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (dev.online) extras.successContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (dev.online) extras.success else MaterialTheme.colorScheme.outline),
                    )
                    Text(
                        text = if (dev.online) "在线" else "离线",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dev.online) extras.onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (dev.online) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
