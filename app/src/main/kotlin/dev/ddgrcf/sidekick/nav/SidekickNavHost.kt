package dev.ddgrcf.sidekick.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.ddgrcf.sidekick.BuildConfig
import dev.ddgrcf.sidekick.service.RelayForegroundService
import dev.ddgrcf.sidekick.ui.BridgeScreen
import dev.ddgrcf.sidekick.ui.DemoPreviewFlow
import dev.ddgrcf.sidekick.ui.HomeScreen
import dev.ddgrcf.sidekick.ui.LoginScreen
import dev.ddgrcf.sidekick.ui.PairScreen
import dev.ddgrcf.sidekick.ui.SessionScreen
import dev.ddgrcf.sidekick.ui.SettingsScreen
import dev.ddgrcf.sidekick.ui.TotpScreen
import dev.ddgrcf.sidekick.vm.AuthViewModel
import kotlinx.serialization.json.Json

@Composable
fun SidekickNavHost(authViewModel: AuthViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val themeMode by authViewModel.themeMode.collectAsStateWithLifecycle()

    if (isLoggedIn == null) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
            }
        }
        return
    }

    val routeJson = remember { Json { ignoreUnknownKeys = true } }
    val backStack = rememberSaveable(
        saver = listSaver<SnapshotStateList<AppRoute>, String>(
            save = { list -> list.map { routeJson.encodeToString(AppRoute.serializer(), it) } },
            restore = { saved ->
                mutableStateListOf<AppRoute>().apply {
                    saved.forEach { raw ->
                        runCatching { routeJson.decodeFromString(AppRoute.serializer(), raw) }
                            .getOrNull()
                            ?.let(::add)
                    }
                    if (isEmpty()) {
                        add(if (isLoggedIn == true) AppRoute.HomeRoute else AppRoute.LoginRoute)
                    }
                }
            },
        ),
    ) {
        mutableStateListOf<AppRoute>(
            if (isLoggedIn == true) AppRoute.HomeRoute else AppRoute.LoginRoute,
        )
    }

    var lastLoggedIn by rememberSaveable { mutableStateOf(isLoggedIn) }

    val popBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    LaunchedEffect(isLoggedIn) {
        val loggedIn = isLoggedIn
        if (loggedIn == true) {
            RelayForegroundService.start(context)
            RelayForegroundService.scheduleReconnectWorker(context)
        } else {
            RelayForegroundService.stop(context)
        }
        if (lastLoggedIn != loggedIn && loggedIn != null) {
            lastLoggedIn = loggedIn
            val cur = backStack.lastOrNull()
            if (loggedIn) {
                if (cur is AppRoute.LoginRoute || cur is AppRoute.TotpRoute) {
                    backStack.clear()
                    backStack.add(AppRoute.HomeRoute)
                }
            } else {
                if (cur !is AppRoute.LoginRoute && cur !is AppRoute.TotpRoute && cur !is AppRoute.DemoRoute) {
                    backStack.clear()
                    backStack.add(AppRoute.LoginRoute)
                }
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = popBack,
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
                    onDemoPreview = if (BuildConfig.DEBUG) {
                        { backStack.add(AppRoute.DemoRoute) }
                    } else null,
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
                    onBack = popBack,
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
                    onBack = popBack,
                )
            }
            entry<AppRoute.PairRoute> {
                PairScreen(
                    vm = hiltViewModel(),
                    onBack = popBack,
                    onPaired = {
                        backStack.clear()
                        backStack.add(AppRoute.HomeRoute)
                    },
                )
            }
            entry<AppRoute.SettingsRoute> {
                SettingsScreen(
                    vm = hiltViewModel(),
                    onBack = popBack,
                    onLoggedOut = {
                        backStack.clear()
                        backStack.add(AppRoute.LoginRoute)
                    },
                )
            }
            entry<AppRoute.DemoRoute> {
                DemoPreviewFlow(
                    themeModeKey = themeMode,
                    onSetThemeMode = authViewModel::setThemeMode,
                    onExit = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    },
                )
            }
        },
    )
}
