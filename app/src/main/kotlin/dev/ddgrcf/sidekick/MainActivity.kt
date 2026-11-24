package dev.ddgrcf.sidekick

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.ddgrcf.sidekick.nav.SidekickNavHost
import dev.ddgrcf.sidekick.ui.theme.SidekickTheme
import dev.ddgrcf.sidekick.ui.theme.ThemeMode
import dev.ddgrcf.sidekick.vm.AuthViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        setContent {
            val modeKey by authViewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (ThemeMode.fromKey(modeKey)) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            LaunchedEffect(darkTheme) {
                if (darkTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
                    )
                }
            }

            SidekickTheme(darkTheme = darkTheme) {
                SidekickNavHost(authViewModel = authViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "sidekick" && uri.host == "auth" && uri.path == "/callback") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                authViewModel.handleOAuthCallback(code)
            }
        }
    }
}
