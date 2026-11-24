package dev.ddgrcf.sidekick.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
    )
}

@Composable
fun LoginContent(
    relayUrl: String,
    loading: Boolean,
    error: String?,
    onRelayUrlChange: (String) -> Unit,
    onGitHubLoginClick: () -> Unit,
) {
    val extras = SidekickThemeExtras.colors
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            extras.brandGradientStart.copy(alpha = if (extras.isDark) 0.55f else 0.7f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(extras.brandGradientStart, extras.brandGradientEnd),
                            ),
                        )
                        .border(1.dp, extras.cardBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp),
                    )
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Sidekick Remote",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "端到端加密控制本地 AI 编程会话",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroPillBadge(Icons.Rounded.Lock, "Noise E2E")
                    HeroPillBadge(Icons.Rounded.VerifiedUser, "GitHub OAuth")
                }

                Spacer(Modifier.height(28.dp))
                SidekickCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        OutlinedTextField(
                            value = relayUrl,
                            onValueChange = onRelayUrlChange,
                            label = { Text("Relay URL") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Language, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = onGitHubLoginClick,
                            enabled = !loading && relayUrl.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
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
                        if (!error.isNullOrBlank()) {
                            ErrorBanner(error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPillBadge(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, SidekickThemeExtras.colors.cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    var code by remember { mutableStateOf("") }
    var useRecovery by remember { mutableStateOf(false) }
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
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
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
            Spacer(Modifier.height(22.dp))

            SidekickCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shake.value.roundToInt(), 0) },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
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
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Button(
                        onClick = { onVerify(code) },
                        enabled = !loading && code.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
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
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                char.isNotEmpty() -> MaterialTheme.colorScheme.outline
                                else -> extras.cardBorder
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
