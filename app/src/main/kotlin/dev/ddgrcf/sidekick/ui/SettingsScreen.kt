package dev.ddgrcf.sidekick.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PermDeviceInformation
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dev.ddgrcf.sidekick.core.relay.RelayDevice
import dev.ddgrcf.sidekick.core.store.SecretStore
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras
import dev.ddgrcf.sidekick.ui.theme.ThemeMode
import dev.ddgrcf.sidekick.vm.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val otpauthUri by vm.otpauthUri.collectAsStateWithLifecycle()
    val recoveryCodes by vm.recoveryCodes.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    SettingsContent(
        themeModeKey = prefs.themeMode,
        relayUrl = prefs.relayUrl,
        deviceId = prefs.deviceId,
        devices = devices,
        otpauthUri = otpauthUri,
        recoveryCodes = recoveryCodes,
        message = message,
        onBack = onBack,
        onSetThemeMode = vm::setThemeMode,
        onEnrollTotp = vm::enrollTotp,
        onConfirmTotp = vm::confirmTotp,
        onDismissRecoveryCodes = vm::dismissRecoveryCodes,
        onRevokeDevice = vm::revokeDevice,
        onLogout = { vm.logout(onLoggedOut) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    themeModeKey: String,
    relayUrl: String,
    deviceId: String,
    devices: List<RelayDevice>,
    otpauthUri: String?,
    recoveryCodes: List<String>,
    message: String?,
    onBack: () -> Unit,
    onSetThemeMode: (String) -> Unit,
    onEnrollTotp: () -> Unit,
    onConfirmTotp: (String) -> Unit,
    onDismissRecoveryCodes: () -> Unit,
    onRevokeDevice: (String) -> Unit,
    onLogout: () -> Unit,
) {
    var totpCode by rememberSaveable { mutableStateOf("") }
    var deviceToRevoke by remember { mutableStateOf<RelayDevice?>(null) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val currentTheme = ThemeMode.fromKey(themeModeKey)

    fun copyWithToast(label: String, value: String) {
        if (value.isBlank()) return
        clipboard.setText(AnnotatedString(value))
        scope.launch { snackbarHostState.showSnackbar("已复制 $label") }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        ) {
            item {
                SectionHeader(
                    title = "外观与主题",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text("主题模式", style = MaterialTheme.typography.titleMedium)
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { idx, mode ->
                            val icon = when (mode) {
                                ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                            }
                            SegmentedButton(
                                selected = currentTheme == mode,
                                onClick = { onSetThemeMode(mode.key) },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = ThemeMode.entries.size),
                                icon = {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                            ) {
                                Text(mode.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                HairlineDivider()
            }

            item {
                SectionHeader(
                    title = "账号与中继",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    InfoCopyRow(
                        icon = Icons.Rounded.Language,
                        label = "Relay URL",
                        value = relayUrl.ifBlank { "未设置" },
                        canCopy = relayUrl.isNotBlank(),
                        onCopy = { copyWithToast("Relay URL", relayUrl) },
                    )
                    HairlineDivider(Modifier.padding(start = 46.dp))
                    InfoCopyRow(
                        icon = Icons.Rounded.PermDeviceInformation,
                        label = "Device ID",
                        value = deviceId.ifBlank { "未注册" },
                        canCopy = deviceId.isNotBlank(),
                        onCopy = { copyWithToast("Device ID", deviceId) },
                    )
                    HairlineDivider()
                }
            }

            item {
                SectionHeader(
                    title = "安全验证",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text("两步验证 (TOTP)", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "保护账号免受未授权 OAuth 登录，重新绑定会生成新密钥与恢复码。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (otpauthUri == null) {
                        FilledTonalButton(
                            onClick = onEnrollTotp,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text("绑定 / 重置 TOTP")
                        }
                    } else {
                        val bmp by produceState<Bitmap?>(initialValue = null, otpauthUri) {
                            value = withContext(Dispatchers.IO) { renderQrBitmap(otpauthUri) }
                        }
                        if (bmp != null) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Image(
                                    bitmap = bmp!!.asImageBitmap(),
                                    contentDescription = "TOTP QR",
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(188.dp),
                                )
                            }
                        }
                        OutlinedTextField(
                            value = totpCode,
                            onValueChange = { raw -> totpCode = raw.filter { it.isDigit() }.take(6) },
                            label = { Text("输入验证器 6 位验证码确认") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                val submitted = totpCode
                                totpCode = ""
                                onConfirmTotp(submitted)
                            },
                            enabled = totpCode.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text("激活 TOTP")
                        }
                    }
                }
                HairlineDivider()
            }

            item {
                SectionHeader(
                    title = "已注册设备",
                    trailing = "${devices.size}",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
                )
                if (devices.isNotEmpty()) {
                    HairlineDivider()
                } else {
                    Text(
                        text = "暂无已注册设备",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    HairlineDivider()
                }
            }

            itemsIndexed(devices, key = { _, it -> it.id }) { idx, dev ->
                val isBridge = dev.kind.equals("bridge", ignoreCase = true)
                val isSelf = deviceId.isNotBlank() && dev.id == deviceId
                val extras = SidekickThemeExtras.colors
                val mutedVariant = MaterialTheme.colorScheme.onSurfaceVariant
                val rowBg = if (dev.online) {
                    MaterialTheme.colorScheme.background
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
                Surface(
                    color = rowBg,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 60.dp)
                            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isBridge) Icons.Rounded.Computer else Icons.Rounded.Smartphone,
                            contentDescription = null,
                            tint = if (dev.online) MaterialTheme.colorScheme.primary else mutedVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = dev.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (dev.online) MaterialTheme.colorScheme.onSurface else mutedVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = buildString {
                                        append(dev.kind)
                                        if (isSelf) append(" · 本机")
                                        if (!dev.online) append(" · 离线")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dev.online) extras.success else mutedVariant.copy(alpha = 0.78f),
                                )
                            }
                            Text(
                                text = "指纹 ${SecretStore.fingerprintFromBase64(dev.pubkey)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (dev.online) mutedVariant else mutedVariant.copy(alpha = 0.72f),
                            )
                        }
                        IconButton(onClick = { deviceToRevoke = dev }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "吊销", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                val nextOnline = devices.getOrNull(idx + 1)?.online
                if (idx < devices.lastIndex && dev.online && nextOnline == true) {
                    HairlineDivider(Modifier.padding(start = 48.dp))
                } else {
                    HairlineDivider()
                }
            }

            if (!message.isNullOrBlank()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        ErrorBanner(message)
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { confirmLogout = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .defaultMinSize(minHeight = 50.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("退出登录")
                }
            }
        }
    }

    deviceToRevoke?.let { target ->
        AlertDialog(
            onDismissRequest = { deviceToRevoke = null },
            title = { Text("吊销设备？") },
            text = { Text("确定吊销「${target.name}」吗？该设备将立即断开并无法再访问中继。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deviceToRevoke = null
                        onRevokeDevice(target.id)
                    },
                ) {
                    Text("确认吊销", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToRevoke = null }) { Text("取消") }
            },
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("退出登录？") },
            text = { Text("退出后将清除本机访问令牌与设备 pin 记录（主题偏好保留）。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        onLogout()
                    },
                ) {
                    Text("退出登录", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("取消") }
            },
        )
    }

    if (recoveryCodes.isNotEmpty()) {
        val extras = SidekickThemeExtras.colors
        AlertDialog(
            onDismissRequest = onDismissRecoveryCodes,
            title = { Text("一次性恢复码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("仅显示这一次，请离线妥善保存：", color = MaterialTheme.colorScheme.error)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = extras.codeBg,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(recoveryCodes.chunked(2)) { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    pair.forEach { rc ->
                                        Text(
                                            text = rc,
                                            fontFamily = FontFamily.Monospace,
                                            color = extras.codeText,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissRecoveryCodes) { Text("我已保存") }
            },
        )
    }
}

@Composable
private fun InfoCopyRow(
    icon: ImageVector,
    label: String,
    value: String,
    canCopy: Boolean,
    onCopy: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (canCopy) {
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "复制 $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun renderQrBitmap(content: String): Bitmap? {
    return runCatching {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 400, 400)
        val bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        for (x in 0 until 400) {
            for (y in 0 until 400) {
                bmp.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bmp
    }.getOrNull()
}
