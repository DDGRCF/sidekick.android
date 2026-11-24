package dev.ddgrcf.sidekick

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dev.ddgrcf.sidekick.core.ansi.AnsiParser
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.core.relay.RelayDevice
import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.core.store.SessionEntity
import dev.ddgrcf.sidekick.data.UiPart
import dev.ddgrcf.sidekick.proto.v1.Key
import dev.ddgrcf.sidekick.ui.BridgeContent
import dev.ddgrcf.sidekick.ui.HomeContent
import dev.ddgrcf.sidekick.ui.LoginContent
import dev.ddgrcf.sidekick.ui.SessionContent
import dev.ddgrcf.sidekick.ui.theme.SidekickTheme
import dev.ddgrcf.sidekick.vm.MediaKind
import dev.ddgrcf.sidekick.vm.PendingAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginScreenTriggersGitHubLogin() {
        var clicked = false
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                LoginContent(
                    relayUrl = "https://relay.example.com",
                    loading = false,
                    error = null,
                    onRelayUrlChange = {},
                    onGitHubLoginClick = { clicked = true },
                )
            }
        }
        composeRule.onNodeWithText("Sidekick Remote").assertIsDisplayed()
        composeRule.onNodeWithText("使用 GitHub 登录").performClick()
        assertTrue(clicked)
    }

    @Test
    fun lightThemeRendersHomeAndBridgeEmptyStates() {
        val bridge = RelayDevice(id = "b1", kind = "bridge", name = "mac-mini", pubkey = "AAAA", online = true)
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                HomeContent(
                    connectionState = ConnectionState.CONNECTED,
                    bridges = listOf(bridge),
                    refreshing = false,
                    error = null,
                    onRefresh = {},
                    onOpenBridge = {},
                    onOpenPair = {},
                    onOpenSettings = {},
                )
            }
        }
        composeRule.onNodeWithText("我的电脑").assertIsDisplayed()
        composeRule.onNodeWithText("已连接").assertIsDisplayed()
        composeRule.onNodeWithText("mac-mini").assertIsDisplayed()
    }

    @Test
    fun homeEmptyStateShowsPairCtaInLightTheme() {
        var pairClicked = false
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                HomeContent(
                    connectionState = ConnectionState.OFFLINE,
                    bridges = emptyList(),
                    refreshing = false,
                    error = null,
                    onRefresh = {},
                    onOpenBridge = {},
                    onOpenPair = { pairClicked = true },
                    onOpenSettings = {},
                )
            }
        }
        composeRule.onNodeWithText("尚未配对电脑").assertIsDisplayed()
        composeRule.onNodeWithText("扫码添加电脑").performClick()
        assertTrue(pairClicked)
    }

    @Test
    fun bridgeScreenRendersSessionsAndHistory() {
        val sessions = listOf(
            SessionEntity("b1", "codex 59bb842411d", "codex", "/repo", "Fix bug", "c1", true, 1000L, 1000L),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                BridgeContent(
                    bridgeName = "workstation",
                    sessions = sessions,
                    history = emptyList(),
                    tools = emptyList(),
                    workspaces = emptyList(),
                    error = null,
                    onBack = {},
                    onRefresh = {},
                    onClickSession = {},
                    onClickHistory = {},
                    onStartNew = { _, _ -> },
                )
            }
        }
        composeRule.onNodeWithText("Fix bug").assertIsDisplayed()
        composeRule.onNodeWithText("LIVE").assertIsDisplayed()
    }

    @Test
    fun sessionInputBarDispatchesInputAndKeysAndHidesChatForAgy() {
        var sentInput: Pair<String, Boolean>? = null
        var sentKey: Key? = null

        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                SessionContent(
                    title = "agy session",
                    tool = "antigravity",
                    structuredHistory = false,
                    connectionState = ConnectionState.CONNECTED,
                    messages = listOf(MessageEntity("b1", "c1", 1, "USER", "[]", 1L, null)),
                    decodeParts = { listOf(UiPart.Text("hidden")) },
                    screenLines = AnsiParser.parse("\u001B[32magy ready\u001B[0m"),
                    screenRevision = 1L,
                    localTyping = true,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { t, s -> sentInput = t to s },
                    onSendKey = { k -> sentKey = k },
                    onFocusChanged = {},
                )
            }
        }

        // structuredHistory=false hides Chat tab and shows Terminal + local typing banner.
        composeRule.onNodeWithText("Chat").assertDoesNotExist()
        composeRule.onNodeWithText("本地正在输入…").assertIsDisplayed()
        composeRule.onNodeWithText("agy ready").assertIsDisplayed()

        // Shortcut chip emits KeysRequest.
        composeRule.onNodeWithText("Ctrl-C").performScrollTo().performClick()
        assertEquals(Key.KEY_CTRL_C, sentKey)

        // Input field + Send emits InputRequest(submit=true).
        composeRule.onNodeWithText("输入指令…").performTextInput("hello")
        composeRule.onNodeWithContentDescription("发送").performClick()
        assertEquals("hello" to true, sentInput)
    }

    @Test
    fun sessionChatRendersMarkdownCodeBlockAndToolCallInLightTheme() {
        val parts = listOf(
            UiPart.Text("Here is the patch:\n```kotlin\nval x = 1\n```"),
            UiPart.ToolCall("t1", "bash", """{"cmd":"ls"}""", "TOOL_CALL_STATUS_DONE"),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "codex session",
                    tool = "codex",
                    structuredHistory = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = listOf(MessageEntity("b1", "c1", 1, "ROLE_ASSISTANT", "[]", 1L, "gpt-5")),
                    decodeParts = { parts },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { _, _ -> },
                    onSendKey = {},
                    onFocusChanged = {},
                )
            }
        }
        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithText("Terminal").assertIsDisplayed()
        composeRule.onNodeWithText("kotlin").assertIsDisplayed()
        composeRule.onNodeWithText("val x = 1").assertIsDisplayed()
        composeRule.onNodeWithText("bash (DONE)").assertIsDisplayed()
    }

    @Test
    fun sessionChatRendersMediaCardsAndPendingAttachmentTray() {
        val parts = listOf(
            UiPart.Media("m1", "image", "image/webp", "screenshot.webp", sizeBytes = 320_000L, width = 1280, height = 720),
            UiPart.Media("m2", "video", "video/mp4", "repro.mp4", sizeBytes = 4_200_000L, durationMs = 15_000L),
            UiPart.Media("m3", "audio", "audio/ogg", "voice.ogg", sizeBytes = 90_000L, durationMs = 12_000L),
        )
        val pending = listOf(
            PendingAttachment(
                id = "p1",
                uri = "content://media/video/1",
                name = "clip.mp4",
                mimeType = "video/mp4",
                sizeBytes = 3L * 1024 * 1024,
                kind = MediaKind.VIDEO,
            ),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                SessionContent(
                    title = "claude session",
                    tool = "claude",
                    structuredHistory = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = listOf(MessageEntity("b1", "c1", 1, "ROLE_USER", "[]", 1L, null)),
                    decodeParts = { parts },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    pendingAttachments = pending,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { _, _ -> },
                    onSendKey = {},
                    onFocusChanged = {},
                )
            }
        }
        composeRule.onNodeWithText("screenshot.webp").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("repro.mp4").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("voice.ogg").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("clip.mp4").assertIsDisplayed()
        composeRule.onNodeWithText("3.0 MiB · Blob").assertIsDisplayed()
    }
}
