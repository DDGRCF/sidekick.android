package dev.ddgrcf.sidekick.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras
import dev.ddgrcf.sidekick.vm.AuthNavEvent
import dev.ddgrcf.sidekick.vm.AuthViewModel
import kotlin.math.roundToInt

@Composable
fun LoginScreen(
    vm: AuthViewModel,
    onLoggedIn: () -> Unit,
    onRequireTotp: (ticket: String, enrolled: Boolean) -> Unit,
    onDevLogin: (() -> Unit)? = null,
    onDemoPreview: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val relayUrl by vm.relayUrl.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val navEvent by vm.navEvent.collectAsStateWithLifecycle()

    LaunchedEffect(navEvent) {
        when (val ev = navEvent) {
            is AuthNavEvent.LoggedIn -> {
                vm.consumeNavEvent()
                onLoggedIn()
            }
            is AuthNavEvent.RequireTotp -> {
                vm.consumeNavEvent()
                onRequireTotp(ev.mfaTicket, ev.totpEnrolled)
            }
            AuthNavEvent.Idle -> Unit
        }
    }

    LoginContent(
        relayUrl = relayUrl,
        loading = loading,
        error = error,
        onRelayUrlChange = vm::setRelayUrl,
        onGitHubLoginClick = {
            val url = vm.buildGitHubStartUrl()
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
        },
        onDevLoginClick = onDevLogin,
        onDemoPreviewClick = onDemoPreview,
    )
}

@Composable
fun LoginContent(
    relayUrl: String,
    loading: Boolean,
    error: String?,
    onRelayUrlChange: (String) -> Unit,
    onGitHubLoginClick: () -> Unit,
    onDevLoginClick: (() -> Unit)? = null,
    onDemoPreviewClick: (() -> Unit)? = null,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(
                imageVector = Icons.Rounded.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Sidekick Remote",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "端到端加密控制本地 AI 编程终端会话",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = relayUrl,
                onValueChange = onRelayUrlChange,
                label = { Text("Relay URL") },
                leadingIcon = {
                    Icon(Icons.Rounded.Language, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            if (onDevLoginClick != null) {
                TextButton(
                    onClick = { onRelayUrlChange("http://127.0.0.1:18090") },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("填入本机调试地址 (127.0.0.1:18090)")
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onGitHubLoginClick,
                enabled = !loading && relayUrl.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("使用 GitHub 登录")
                }
            }
            if (onDevLoginClick != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDevLoginClick,
                    enabled = !loading && relayUrl.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("本地开发登录 (Debug)")
                }
            }
            if (onDemoPreviewClick != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDemoPreviewClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("进入本地演示预览 (Debug)")
                }
            }
            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                ErrorBanner(error)
            }
        }
    }
}

@Composable
fun TotpScreen(
    mfaTicket: String,
    totpEnrolled: Boolean,
    vm: AuthViewModel,
    onLoggedIn: () -> Unit,
) {
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val navEvent by vm.navEvent.collectAsStateWithLifecycle()

    LaunchedEffect(navEvent) {
        if (navEvent is AuthNavEvent.LoggedIn) {
            vm.consumeNavEvent()
            onLoggedIn()
        }
    }

    TotpContent(
        totpEnrolled = totpEnrolled,
        loading = loading,
        error = error,
        onVerify = { code -> vm.verifyTotp(mfaTicket, code) },
    )
}

@Composable
fun TotpContent(
    totpEnrolled: Boolean,
    loading: Boolean,
    error: String?,
    onVerify: (String) -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var useRecovery by rememberSaveable { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 320
                    (-18f) at 50
                    18f at 110
                    (-12f) at 170
                    12f at 230
                    0f at 320
                },
            )
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text("两步验证", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    !totpEnrolled -> "请先在网页端绑定 TOTP 或输入 6 位验证码"
                    useRecovery -> "输入 10 位一次性恢复码"
                    else -> "输入验证器 App 中的 6 位验证码"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shake.value.roundToInt(), 0) },
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!useRecovery) {
                    OtpDigitsInput(
                        value = code,
                        onValueChange = { raw ->
                            code = raw.filter { it.isDigit() }.take(6)
                        },
                    )
                } else {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("恢复码") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Button(
                    onClick = { onVerify(code) },
                    enabled = !loading && code.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("验证")
                    }
                }

                TextButton(
                    onClick = {
                        useRecovery = !useRecovery
                        code = ""
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(if (useRecovery) "改用 6 位 TOTP 验证码" else "使用恢复码")
                }

                if (!error.isNullOrBlank()) {
                    ErrorBanner(error)
                }
            }
        }
    }
}

@Composable
private fun OtpDigitsInput(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val extras = SidekickThemeExtras.colors

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(6) { idx ->
                    val char = value.getOrNull(idx)?.toString() ?: ""
                    val isCurrent = focused && (idx == value.length || (value.length == 6 && idx == 5))
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 54.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                char.isNotEmpty() -> MaterialTheme.colorScheme.outline
                                else -> extras.hairline
                            },
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
            }
        },
    )
}
