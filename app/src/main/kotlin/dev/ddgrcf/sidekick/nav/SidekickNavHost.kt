package dev.ddgrcf.sidekick.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.ddgrcf.sidekick.service.RelayForegroundService
import dev.ddgrcf.sidekick.ui.BridgeScreen
import dev.ddgrcf.sidekick.ui.HomeScreen
import dev.ddgrcf.sidekick.ui.LoginScreen
import dev.ddgrcf.sidekick.ui.PairScreen
import dev.ddgrcf.sidekick.ui.SessionScreen
import dev.ddgrcf.sidekick.ui.SettingsScreen
import dev.ddgrcf.sidekick.ui.TotpScreen
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras
import dev.ddgrcf.sidekick.vm.AuthViewModel

@Composable
fun SidekickNavHost(authViewModel: AuthViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()

    if (isLoggedIn == null) {
        val extras = SidekickThemeExtras.colors
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(extras.brandGradientStart, extras.brandGradientEnd),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
            }
        }
        return
    }

    val backStack = remember(isLoggedIn) {
        mutableStateListOf<AppRoute>(
            if (isLoggedIn == true) AppRoute.HomeRoute else AppRoute.LoginRoute,
        )
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == true) {
            RelayForegroundService.start(context)
            RelayForegroundService.scheduleReconnectWorker(context)
        } else {
            RelayForegroundService.stop(context)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        },
        entryProvider = entryProvider {
            entry<AppRoute.LoginRoute> {
                LoginScreen(
                    vm = authViewModel,
                    onLoggedIn = {
                        backStack.clear()
                        backStack.add(AppRoute.HomeRoute)
                    },
                    onRequireTotp = { ticket, enrolled ->
                        backStack.add(AppRoute.TotpRoute(ticket, enrolled))
                    },
                )
            }
            entry<AppRoute.TotpRoute> { route ->
                TotpScreen(
                    mfaTicket = route.mfaTicket,
                    totpEnrolled = route.totpEnrolled,
                    vm = authViewModel,
                    onLoggedIn = {
                        backStack.clear()
                        backStack.add(AppRoute.HomeRoute)
                    },
                )
            }
            entry<AppRoute.HomeRoute> {
                HomeScreen(
                    vm = hiltViewModel(),
                    onOpenBridge = { dev ->
                        backStack.add(AppRoute.BridgeRoute(dev.id, dev.name))
                    },
                    onOpenPair = {
                        backStack.add(AppRoute.PairRoute)
                    },
                    onOpenSettings = {
                        backStack.add(AppRoute.SettingsRoute)
                    },
                )
            }
            entry<AppRoute.BridgeRoute> { route ->
                BridgeScreen(
                    bridgeId = route.bridgeId,
                    bridgeName = route.bridgeName,
                    vm = hiltViewModel(),
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onOpenSession = { sid, tool, title, convId, structured ->
                        backStack.add(
                            AppRoute.SessionRoute(
                                bridgeId = route.bridgeId,
                                sid = sid,
                                tool = tool,
                                title = title,
                                convId = convId,
                                structuredHistory = structured,
                            ),
                        )
                    },
                )
            }
            entry<AppRoute.SessionRoute> { route ->
                SessionScreen(
                    bridgeId = route.bridgeId,
                    sid = route.sid,
                    tool = route.tool,
                    title = route.title,
                    convId = route.convId,
                    structuredHistory = route.structuredHistory,
                    vm = hiltViewModel(),
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<AppRoute.PairRoute> {
                PairScreen(
                    vm = hiltViewModel(),
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onPaired = {
                        backStack.clear()
                        backStack.add(AppRoute.HomeRoute)
                    },
                )
            }
            entry<AppRoute.SettingsRoute> {
                SettingsScreen(
                    vm = hiltViewModel(),
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onLoggedOut = {
                        backStack.clear()
                        backStack.add(AppRoute.LoginRoute)
                    },
                )
            }
        },
    )
}
