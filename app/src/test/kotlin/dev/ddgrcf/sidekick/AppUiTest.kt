package dev.ddgrcf.sidekick

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import dev.ddgrcf.sidekick.core.ansi.AnsiParser
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.core.relay.RelayDevice
import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.core.store.SessionEntity
import dev.ddgrcf.sidekick.data.UiPart
import dev.ddgrcf.sidekick.proto.v1.Key
import dev.ddgrcf.sidekick.ui.BridgeContent
import dev.ddgrcf.sidekick.ui.DemoPreviewFlow
import dev.ddgrcf.sidekick.ui.HomeContent
import dev.ddgrcf.sidekick.ui.LoginContent
import dev.ddgrcf.sidekick.ui.PairContent
import dev.ddgrcf.sidekick.ui.SessionContent
import dev.ddgrcf.sidekick.ui.SettingsContent
import dev.ddgrcf.sidekick.ui.theme.SidekickTheme
import dev.ddgrcf.sidekick.vm.MediaKind
import dev.ddgrcf.sidekick.vm.PendingAttachment
import dev.ddgrcf.sidekick.vm.SlashCommandSourceUi
import dev.ddgrcf.sidekick.vm.SlashCommandSuggestion
import dev.ddgrcf.sidekick.vm.WorkspaceFileSuggestion
import dev.ddgrcf.sidekick.vm.coreBuiltinSlashCommands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
        composeRule.onNodeWithText("kotlin").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("val x = 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("bash (DONE)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sessionChatRendersMarkdownTableQuoteAndCopyCheckmark() {
        val parts = listOf(
            UiPart.Text(
                """
                | 字段 | 说明 |
                | --- | --- |
                | ack_seq | 增量续传游标 |

                > 断线重连后自动从断点恢复
                """.trimIndent(),
            ),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
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
        composeRule.onNodeWithText("ack_seq").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("增量续传游标").assertIsDisplayed()
        composeRule.onNodeWithText("断线重连后自动从断点恢复").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("复制回复").performScrollTo().performClick()
        composeRule.onNodeWithText("已复制").assertIsDisplayed()
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
        composeRule.onNodeWithContentDescription("播放音频").assertIsDisplayed()
        composeRule.onNodeWithText("clip.mp4").assertIsDisplayed()
        composeRule.onNodeWithText("3.0 MiB · Blob").assertIsDisplayed()

        // Tapping video card opens fullscreen VideoPlayerDialog
        composeRule.onNodeWithContentDescription("视频预览").performClick()
        composeRule.onNodeWithContentDescription("关闭视频").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("关闭视频").assertDoesNotExist()
    }

    @Test
    fun demoPreviewFlowNavigatesToSessionAndSendsMessage() {
        var currentTheme = "dark"
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                DemoPreviewFlow(
                    themeModeKey = currentTheme,
                    onSetThemeMode = { currentTheme = it },
                    onExit = {},
                )
            }
        }
        composeRule.onNodeWithText("mac-studio").performClick()
        composeRule.onNodeWithText("Refactor Noise chunking & E2E media").performClick()
        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_list").performScrollToNode(hasText("ui_preview_render.png"))
        composeRule.onNodeWithContentDescription("ui_preview_render.png").performClick()
        composeRule.onNodeWithContentDescription("关闭预览").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("输入指令…").performTextInput("test demo")
        composeRule.onNodeWithContentDescription("发送").performClick()
        composeRule.onNodeWithText("test demo").assertIsDisplayed()
    }

    @Test
    fun demoPreviewFlowPreservesNavigationAndDraftAcrossStateRestoration() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            SidekickTheme(darkTheme = true) {
                DemoPreviewFlow(
                    themeModeKey = "dark",
                    onSetThemeMode = {},
                    onExit = {},
                )
            }
        }
        composeRule.onNodeWithText("mac-studio").performClick()
        composeRule.onNodeWithText("Refactor Noise chunking & E2E media").performClick()
        composeRule.onNodeWithText("Terminal").performClick()
        composeRule.onNodeWithText("ANSI · 5 行").assertIsDisplayed()
        composeRule.onNodeWithText("输入指令…").performTextInput("draft kept")

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Terminal").assertIsDisplayed()
        composeRule.onNodeWithText("ANSI · 5 行").assertIsDisplayed()
        composeRule.onNodeWithText("draft kept").assertIsDisplayed()
    }

    @Test
    fun demoPreviewFlowSupportsHandoverToAnotherAgentWithNote() {
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                DemoPreviewFlow(
                    themeModeKey = "dark",
                    onSetThemeMode = {},
                    onExit = {},
                )
            }
        }
        composeRule.onNodeWithText("mac-studio").performClick()
        composeRule.onNodeWithText("Refactor Noise chunking & E2E media").performClick()
        composeRule.onNodeWithContentDescription("更多操作").performClick()
        composeRule.onNodeWithText("移交给其他 Agent").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("移交给同机其他 Agent").assertIsDisplayed()
        composeRule.onNodeWithText("移交内容与指令").performTextInput("继续完成剩余单测并回归")
        composeRule.onNodeWithText("确认移交").performScrollTo().performClick()
        composeRule.onNodeWithText("确认移交").assertDoesNotExist()
        composeRule.onNodeWithText("claude").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_list").performScrollToNode(hasText("继续完成剩余单测并回归", substring = true))
        composeRule.onNodeWithText("继续完成剩余单测并回归", substring = true).assertIsDisplayed()
    }

    @Test
    fun sessionComposerSupportsQuoteSlashCommandsWithCompactAndFileMentions() {
        val sentMessages = mutableListOf<String>()
        var lastFileQuery: String? = null
        val files = listOf(
            WorkspaceFileSuggestion("app/android/app/src/main/kotlin/dev/ddgrcf/sidekick/ui/SessionScreen.kt"),
        )
        val commands = coreBuiltinSlashCommands("claude") + listOf(
            SlashCommandSuggestion("unslop", "UI polish skill", "[target]", SlashCommandSourceUi.SKILL),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                SessionContent(
                    title = "claude session",
                    tool = "claude",
                    structuredHistory = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = listOf(
                        MessageEntity(
                            "b1",
                            "c1",
                            1,
                            "ROLE_ASSISTANT",
                            """[{"type":"text","text":"Check SessionScreen composer logic"}]""",
                            1L,
                            "claude-sonnet",
                        ),
                    ),
                    decodeParts = { listOf(UiPart.Text("Check SessionScreen composer logic")) },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    fileSuggestions = files,
                    slashCommands = commands,
                    onSearchFiles = { lastFileQuery = it },
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { t, _ -> sentMessages += t },
                    onSendKey = {},
                    onFocusChanged = {},
                )
            }
        }

        // 1. Slash command popup displays /compact + discovered skill via filtering.
        composeRule.onNodeWithContentDescription("斜杠命令").performClick()
        composeRule.onNodeWithText("/compact").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("/un")
        composeRule.onNodeWithText("/unslop").assertIsDisplayed()

        // Re-open "/" popup cleanly and pick /compact.
        composeRule.onNodeWithContentDescription("斜杠命令").performClick()
        composeRule.onNodeWithContentDescription("斜杠命令").performClick()
        composeRule.onNodeWithText("/compact").performClick()
        composeRule.onNodeWithText("/compact ").assertIsDisplayed()

        // Clear slash text, quote assistant message, trigger @file mention, and send.
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNodeWithContentDescription("引用回复").performScrollTo().performClick()
        composeRule.onNodeWithText("引用 claude-sonnet").assertIsDisplayed()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Inspect @Sess")
        composeRule.waitForIdle()
        assertEquals("Sess", lastFileQuery)
        composeRule.onNodeWithText("SessionScreen.kt").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("发送").performClick()

        val sent = sentMessages.last()
        assertTrue(sent.startsWith("> [claude-sonnet]: Check SessionScreen composer logic"))
        assertTrue(sent.contains("@app/android/app/src/main/kotlin/dev/ddgrcf/sidekick/ui/SessionScreen.kt"))
    }

    @Test
    fun pairContentValidatesManualPairingAndShowsPermissionAction() {
        var manualCalled: Pair<String, String>? = null
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                PairContent(
                    status = null,
                    isError = false,
                    busy = false,
                    paired = false,
                    onBack = {},
                    onPairQrJson = {},
                    onPairManualCode = { code, psk -> manualCalled = code to psk },
                )
            }
        }
        composeRule.onNodeWithText("添加电脑").assertIsDisplayed()
        composeRule.onNodeWithText("6 位配对码 (user_code)").performTextInput("123456")
        composeRule.onNodeWithText("一次性 PSK (base64)").performTextInput("AAAA")
        composeRule.onNodeWithText("确认配对").performClick()
        assertEquals("123456" to "AAAA", manualCalled)
    }

    @Test
    fun sessionAdaptiveHeaderTogglesTitleAndKeepsOfflineVisible() {
        val connState = androidx.compose.runtime.mutableStateOf(ConnectionState.CONNECTED)
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                SessionContent(
                    title = "Refactor Noise chunking",
                    tool = "claude",
                    sessionId = "sid-adaptive-99",
                    structuredHistory = true,
                    connectionState = connState.value,
                    messages = emptyList(),
                    decodeParts = { emptyList() },
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

        // Initial peek shows full title strip cleanly without squeezing the chat canvas.
        val expandedCanvasBounds = composeRule.onNodeWithTag("session_canvas").fetchSemanticsNode().boundsInRoot
        composeRule.onNode(hasStateDescription("已展开会话标题")).assertIsDisplayed()
        composeRule.onNodeWithText("Refactor Noise chunking").assertIsDisplayed()
        composeRule.onNodeWithText("已连接").assertIsDisplayed()

        // Tapping the compact tool anchor collapses the overlay without shifting the canvas bounds.
        composeRule.onNode(hasStateDescription("已展开会话标题")).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasStateDescription("已收起会话标题")).assertIsDisplayed()
        composeRule.onNodeWithText("Refactor Noise chunking").assertDoesNotExist()
        val collapsedCanvasBounds = composeRule.onNodeWithTag("session_canvas").fetchSemanticsNode().boundsInRoot
        assertEquals(expandedCanvasBounds.top, collapsedCanvasBounds.top)
        assertEquals(expandedCanvasBounds.bottom, collapsedCanvasBounds.bottom)

        // Tapping again re-summons and pins the full title strip.
        composeRule.onNode(hasStateDescription("已收起会话标题")).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasStateDescription("已展开会话标题")).assertIsDisplayed()
        composeRule.onNodeWithText("Refactor Noise chunking").assertIsDisplayed()

        // Offline status keeps the full strip visible even after clicking to collapse.
        connState.value = ConnectionState.OFFLINE
        composeRule.waitForIdle()
        composeRule.onNode(hasStateDescription("已展开会话标题")).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("离线").assertIsDisplayed()
        composeRule.onNodeWithText("Refactor Noise chunking").assertIsDisplayed()
    }

    @Config(qualifiers = "w390dp-h844dp")
    @Test
    fun sessionContentTapBlankAreaDismissesPopupAndHeader() {
        val focusChanges = mutableListOf<Boolean>()
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                SessionContent(
                    title = "Tap dismiss session",
                    tool = "claude",
                    sessionId = "sid-tap-dismiss",
                    structuredHistory = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = listOf(
                        MessageEntity(
                            "b1",
                            "c1",
                            1,
                            "ROLE_ASSISTANT",
                            """[{"type":"text","text":"Hello world"}]""",
                            1L,
                            "claude-sonnet",
                        ),
                    ),
                    decodeParts = { listOf(UiPart.Text("Hello world")) },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    slashCommands = coreBuiltinSlashCommands("claude"),
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { _, _ -> },
                    onSendKey = {},
                    onFocusChanged = { focusChanges += it },
                )
            }
        }

        // Open slash popup by tapping "/" quick button and focus text field.
        composeRule.onNodeWithContentDescription("斜杠命令").performClick()
        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("/compact").assertIsDisplayed()
        composeRule.onNodeWithText("Tap dismiss session").assertIsDisplayed()

        // Tap blank area inside the conversation canvas -> dismisses popup, header strip, and focus while preserving draft "/".
        val canvasBounds = composeRule.onNodeWithTag("session_canvas").fetchSemanticsNode().boundsInRoot
        assertTrue(canvasBounds.height > 50f)
        composeRule.onNodeWithTag("session_canvas").performTouchInput { click(center) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("/compact").assertDoesNotExist()
        composeRule.onNode(hasStateDescription("已收起会话标题")).assertIsDisplayed()
        composeRule.onNode(hasSetTextAction() and hasText("/")).assertIsDisplayed()
        assertTrue(focusChanges.lastOrNull() == false)
    }

    @Test
    fun demoPreviewFlowEnablesChatForAntigravitySession() {
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                DemoPreviewFlow(
                    themeModeKey = "dark",
                    onSetThemeMode = {},
                    onExit = {},
                )
            }
        }
        composeRule.onNodeWithText("mac-studio").performClick()
        composeRule.onNodeWithText("Live TUI & Chat sync").performClick()
        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithText("Terminal").assertIsDisplayed()
    }

    @Test
    fun homeAndSettingsMarkOfflineDeviceRowsDistinctlyAndDisableHomeRow() {
        val bridges = listOf(
            RelayDevice(id = "b-on", kind = "bridge", name = "mac-studio", pubkey = "AAAA", online = true),
            RelayDevice(id = "b-off", kind = "bridge", name = "ubuntu-workstation", pubkey = "BBBB", online = false),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                HomeContent(
                    connectionState = ConnectionState.CONNECTED,
                    bridges = bridges,
                    refreshing = false,
                    error = null,
                    onRefresh = {},
                    onOpenBridge = {},
                    onOpenPair = {},
                    onOpenSettings = {},
                )
            }
        }
        composeRule.onNodeWithText("mac-studio").assertIsDisplayed()
        composeRule.onNodeWithText("ubuntu-workstation").assertIsNotEnabled()
        composeRule.onNodeWithText("离线").assertIsDisplayed()
    }

    @Test
    fun settingsContentShowsOfflineTagOnOfflineDevices() {
        val devices = listOf(
            RelayDevice(id = "p1", kind = "phone", name = "Pixel", pubkey = "AAAA", online = true),
            RelayDevice(id = "b-off", kind = "bridge", name = "ubuntu-workstation", pubkey = "BBBB", online = false),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                SettingsContent(
                    themeModeKey = "dark",
                    relayUrl = "http://127.0.0.1:8080",
                    deviceId = "p1",
                    devices = devices,
                    otpauthUri = null,
                    recoveryCodes = emptyList(),
                    message = null,
                    onBack = {},
                    onSetThemeMode = {},
                    onEnrollTotp = {},
                    onConfirmTotp = {},
                    onDismissRecoveryCodes = {},
                    onRevokeDevice = {},
                    onLogout = {},
                )
            }
        }
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("bridge · 离线"))
        composeRule.onNodeWithText("bridge · 离线").assertIsDisplayed()
    }
}
