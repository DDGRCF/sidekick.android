package dev.ddgrcf.sidekick

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
import dev.ddgrcf.sidekick.core.store.HistoryEntity
import dev.ddgrcf.sidekick.core.ansi.AnsiParser
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.core.relay.RelayDevice
import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.core.store.SessionEntity
import dev.ddgrcf.sidekick.data.UiPart
import dev.ddgrcf.sidekick.proto.v1.Backend
import dev.ddgrcf.sidekick.proto.v1.Conversation
import dev.ddgrcf.sidekick.proto.v1.Key
import dev.ddgrcf.sidekick.proto.v1.Session
import dev.ddgrcf.sidekick.ui.BridgeContent
import dev.ddgrcf.sidekick.ui.DemoPreviewFlow
import dev.ddgrcf.sidekick.ui.HomeContent
import dev.ddgrcf.sidekick.ui.LoginContent
import dev.ddgrcf.sidekick.ui.PairContent
import dev.ddgrcf.sidekick.ui.SessionContent
import dev.ddgrcf.sidekick.ui.SessionSettingsBottomSheet
import dev.ddgrcf.sidekick.ui.SettingsContent
import dev.ddgrcf.sidekick.ui.WorkspaceDiffSheet
import dev.ddgrcf.sidekick.ui.theme.SidekickTheme
import dev.ddgrcf.sidekick.vm.MediaKind
import dev.ddgrcf.sidekick.vm.ModelOptionUi
import dev.ddgrcf.sidekick.vm.InteractiveDialogOptionUi
import dev.ddgrcf.sidekick.vm.InteractiveDialogState
import dev.ddgrcf.sidekick.vm.NvimHandoverState
import dev.ddgrcf.sidekick.vm.OptimisticUserMessage
import dev.ddgrcf.sidekick.vm.PendingAttachment
import dev.ddgrcf.sidekick.vm.RuntimeAnswerInput
import dev.ddgrcf.sidekick.vm.RuntimeModeUi
import dev.ddgrcf.sidekick.vm.RuntimeQuestionUi
import dev.ddgrcf.sidekick.vm.RuntimeRequestKindUi
import dev.ddgrcf.sidekick.vm.RuntimeRequestOptionUi
import dev.ddgrcf.sidekick.vm.RuntimeRequestUi
import dev.ddgrcf.sidekick.vm.SessionConfigUi
import dev.ddgrcf.sidekick.vm.SlashCommandSourceUi
import dev.ddgrcf.sidekick.vm.SlashCommandSuggestion
import dev.ddgrcf.sidekick.vm.StreamingMessageState
import dev.ddgrcf.sidekick.vm.WorkspaceContextUi
import dev.ddgrcf.sidekick.vm.ChangedFileUi
import dev.ddgrcf.sidekick.vm.DiagnosticItemUi
import dev.ddgrcf.sidekick.vm.WorkspaceFileSuggestion
import dev.ddgrcf.sidekick.vm.coreBuiltinSlashCommands
import dev.ddgrcf.sidekick.vm.findNvimMuxSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun loginScreenSupportsDebugDevUrlFillAndDevLogin() {
        var currentUrl = "https://relay.example.com"
        var devClicked = false
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                LoginContent(
                    relayUrl = currentUrl,
                    loading = false,
                    error = null,
                    onRelayUrlChange = { currentUrl = it },
                    onGitHubLoginClick = {},
                    onDevLoginClick = { devClicked = true },
                )
            }
        }
        composeRule.onNodeWithText("填入本机调试地址 (127.0.0.1:18090)").performClick()
        assertEquals("http://127.0.0.1:18090", currentUrl)
        composeRule.onNodeWithText("本地开发登录 (Debug)").performClick()
        assertTrue(devClicked)
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
        composeRule.mainClock.advanceTimeBy(2600)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithText("Terminal").assertIsDisplayed()
        composeRule.onNodeWithText("kotlin").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("val x = 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("已执行 1 步 · 运行 1 条命令").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("运行命令").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ls").performScrollTo().assertIsDisplayed()
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
        composeRule.onNodeWithText("source_agent: codex", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("source_session_id: codex-59bb8424", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("继续完成剩余单测并回归", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("已选附带最近对话上下文", substring = true).assertDoesNotExist()
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
        var qrJsonCalled: String? = null
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                PairContent(
                    status = null,
                    isError = false,
                    busy = false,
                    paired = false,
                    onBack = {},
                    onPairQrJson = { qrJsonCalled = it },
                    onPairManualCode = { code, psk -> manualCalled = code to psk },
                )
            }
        }
        composeRule.onNodeWithText("添加电脑").assertIsDisplayed()
        composeRule.onNodeWithText("6 位配对码 (user_code)").performTextInput("123456")
        composeRule.onNodeWithText("一次性 PSK (base64)").performTextInput("AAAA")
        composeRule.onNodeWithText("确认配对").performClick()
        assertEquals("123456" to "AAAA", manualCalled)

        composeRule.onNodeWithText("6 位配对码 (user_code)").performTextReplacement("""{"v":1,"eid":"e1","pk":"pk1","psk":"psk1","relay":"ws://127.0.0.1:18090"}""")
        composeRule.onNodeWithText("确认配对").performClick()
        assertEquals("""{"v":1,"eid":"e1","pk":"pk1","psk":"psk1","relay":"ws://127.0.0.1:18090"}""", qrJsonCalled)
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
    fun homeSeparatesUnpairedBridgesAndRoutesClickToPair() {
        val bridges = listOf(
            RelayDevice(id = "b-unpaired", kind = "bridge", name = "fedora", pubkey = "AAAA", online = true),
        )
        var pairOpened = false
        var bridgeOpened = false
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                HomeContent(
                    connectionState = ConnectionState.CONNECTED,
                    bridges = bridges,
                    pairedBridgeIds = emptySet(),
                    refreshing = false,
                    error = null,
                    onRefresh = {},
                    onOpenBridge = { bridgeOpened = true },
                    onOpenPair = { pairOpened = true },
                    onOpenSettings = {},
                )
            }
        }
        composeRule.onNodeWithText("账号下待配对电脑").assertIsDisplayed()
        composeRule.onNodeWithText("待配对").assertIsDisplayed()
        composeRule.onNodeWithText("fedora").performClick()
        assertTrue(pairOpened)
        assertTrue(!bridgeOpened)
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

    @Test
    fun sessionChatRendersOptimisticUserAndLiveStreamingDraft() {
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                SessionContent(
                    title = "codex session",
                    tool = "codex",
                    structuredHistory = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    optimisticUserMessages = listOf(
                        OptimisticUserMessage(id = "opt-1", text = "Explain streaming draft", afterSeq = 0L),
                    ),
                    streamingDraft = StreamingMessageState(
                        active = true,
                        text = "",
                        model = "gpt-5.1-codex",
                    ),
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
        composeRule.onNodeWithText("Explain streaming draft").assertIsDisplayed()
        composeRule.onNodeWithText("gpt-5.1-codex").assertIsDisplayed()
        composeRule.onNodeWithText("思考中…").assertIsDisplayed()
    }

    @Test
    fun sessionChatStreamsMarkdownDraftProgressively() {
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "claude session",
                    tool = "claude",
                    structuredHistory = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    streamingDraft = StreamingMessageState(
                        active = true,
                        text = "----working-----\nPatch applied cleanly.",
                        model = "claude-sonnet-4",
                    ),
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
        composeRule.mainClock.advanceTimeBy(1500)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("streaming_assistant_turn").assertIsDisplayed()
        composeRule.onNodeWithText("claude-sonnet-4").assertIsDisplayed()
        composeRule.onNodeWithText("Patch applied cleanly. ▍").assertIsDisplayed()
    }

    @Test
    fun sessionChatRendersInteractiveQuestionCardAndDispatchesOptionKeys() {
        var sentKeys: List<Key> = emptyList()
        val qJson = """{"questions":[{"header":"意图","question":"你想让我做什么？","options":[{"label":"生成图片","description":"画一张图"},{"label":"写一段文字","description":"创作文案"}]}]}"""
        val parts = listOf(
            UiPart.Text("请先选择方向："),
            UiPart.ToolCall("q1", "ask_user_question", qJson, "TOOL_CALL_STATUS_COMPLETED"),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "pi session",
                    tool = "pi",
                    structuredHistory = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = listOf(MessageEntity("b1", "c1", 1, "ROLE_ASSISTANT", "[]", 1L, "deepseek-v4")),
                    decodeParts = { parts },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { _, _ -> },
                    onSendKey = {},
                    onSendKeys = { sentKeys = it },
                    onFocusChanged = {},
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(2600)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("interactive_question_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("你想让我做什么？").assertIsDisplayed()
        composeRule.onNodeWithText("写一段文字").performScrollTo().performClick()
        assertEquals(listOf(Key.KEY_DOWN, Key.KEY_ENTER), sentKeys)
    }

    @Test
    fun sessionChatRendersLiveThinkingAndInteractiveModelDialogOverlay() {
        var sentKeys: List<Key> = emptyList()
        var dismissed = false
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "pi session",
                    tool = "pi",
                    structuredHistory = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    streamingDraft = StreamingMessageState(
                        active = true,
                        thinking = "先检查可用模型列表",
                        text = "实时输出正文",
                        status = "正在生成回复…",
                        model = "deepseek/deepseek-v4.1-flash",
                    ),
                    interactiveDialog = InteractiveDialogState(
                        open = true,
                        kind = "model",
                        title = "选择模型",
                        subtitle = "(1/2)",
                        searchable = true,
                        selectedIndex = 0,
                        options = listOf(
                            InteractiveDialogOptionUi("deepseek/deepseek-v4.1-flash", "[commandcode] · default", selected = true, current = true),
                            InteractiveDialogOptionUi("claude-sonnet-4", "[anthropic]", selected = false, current = false),
                        ),
                    ),
                    decodeParts = { emptyList() },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { _, _ -> },
                    onSendKey = {},
                    onSendKeys = { sentKeys = it },
                    onDismissDialog = { dismissed = true },
                    onFocusChanged = {},
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(2600)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("streaming_thinking_box").assertIsDisplayed()
        composeRule.onNodeWithText("实时输出正文 ▍").assertIsDisplayed()
        composeRule.onNodeWithTag("interactive_dialog_overlay").assertIsDisplayed()
        composeRule.onNodeWithText("选择模型").assertIsDisplayed()
        composeRule.onNodeWithText("claude-sonnet-4").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(Key.KEY_DOWN, Key.KEY_ENTER), sentKeys)
        composeRule.onNodeWithText("取消 (Esc)").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun sessionChatMergesLiveStreamingIntoActiveAssistantTurnWithoutDuplicateHeader() {
        val partsByKey = mapOf(
            "u1" to listOf(UiPart.Text("排查构建失败原因")),
            "a1" to listOf(
                UiPart.Thinking("先运行单元测试"),
                UiPart.Text("我先执行测试定位问题。"),
                UiPart.ToolCall("c1", "bash", """{"command":"go test ./..."}""", "TOOL_CALL_STATUS_COMPLETED"),
            ),
            "t1" to listOf(UiPart.ToolResult("c1", "PASS", isError = false, truncated = false)),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "pi session",
                    tool = "pi",
                    structuredHistory = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = listOf(
                        MessageEntity("b1", "c1", 1, "ROLE_USER", "u1", 1L, null),
                        MessageEntity("b1", "c1", 2, "ROLE_ASSISTANT", "a1", 2L, "deepseek/deepseek-v4.1-flash"),
                        MessageEntity("b1", "c1", 3, "ROLE_TOOL", "t1", 3L, null),
                    ),
                    streamingDraft = StreamingMessageState(
                        active = true,
                        thinking = "正在总结测试结果",
                        text = "结论已整理完成",
                        status = "正在生成回复…",
                        model = "deepseek/deepseek-v4.1-flash",
                    ),
                    decodeParts = { partsByKey[it].orEmpty() },
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
        composeRule.mainClock.advanceTimeBy(2600)
        composeRule.waitForIdle()
        // Live streaming merges into the single active Assistant turn — no duplicate header or second bubble.
        composeRule.onAllNodesWithText("deepseek/deepseek-v4.1-flash").assertCountEquals(1)
        composeRule.onNodeWithTag("streaming_assistant_turn").assertDoesNotExist()
        composeRule.onNodeWithTag("streaming_thinking_box").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("结论已整理完成 ▍").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sessionSlashModelItemImmediatelySubmitsBareModelCommand() {
        var sentInput: Pair<String, Boolean>? = null
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "pi session",
                    tool = "pi",
                    structuredHistory = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    decodeParts = { emptyList() },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { t, submit -> sentInput = t to submit },
                    onSendKey = {},
                    onFocusChanged = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("斜杠命令").performClick()
        composeRule.onNodeWithText("/model").assertIsDisplayed().performClick()
        assertEquals("/model" to true, sentInput)
    }

    @Test
    fun sessionComposerConfigPillSwitchesModelAndThinkingWhileTypingAndStreaming() {
        var pickedModel: String? = null
        var pickedThinking: String? = null
        val cfg = SessionConfigUi(
            sessionId = "pi-rpc-1",
            model = "deepseek/deepseek-v4.1-flash",
            thinkingLevel = "high",
            runtimeMode = RuntimeModeUi.APPROVAL_REQUIRED,
            availableModels = listOf(
                ModelOptionUi(
                    id = "deepseek/deepseek-v4.1-flash",
                    provider = "commandcode",
                    name = "DeepSeek V4.1 Flash",
                    reasoning = true,
                    thinkingLevels = listOf("off", "medium", "high", "xhigh"),
                    current = true,
                ),
                ModelOptionUi(
                    id = "gpt-5.6-luna",
                    provider = "openai-codex",
                    name = "GPT-5.6 Luna",
                    reasoning = true,
                    thinkingLevels = listOf("off", "medium", "high", "xhigh"),
                ),
            ),
            availableThinkingLevels = listOf("off", "medium", "high", "xhigh"),
            supportedRuntimeModes = listOf(RuntimeModeUi.APPROVAL_REQUIRED, RuntimeModeUi.AUTO_ACCEPT_EDITS),
            isRunning = true,
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "pi rpc",
                    tool = "pi",
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    streamingDraft = StreamingMessageState(
                        active = true,
                        thinking = "分析依赖",
                        text = "正在生成部分内容",
                        model = "deepseek/deepseek-v4.1-flash",
                    ),
                    sessionConfig = cfg,
                    decodeParts = { emptyList() },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { _, _ -> },
                    onSendKey = {},
                    onSelectModel = { pickedModel = it },
                    onSelectThinkingLevel = { pickedThinking = it },
                    onFocusChanged = {},
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(1200)
        composeRule.waitForIdle()
        // Native RPC session does not expose a broken Terminal tab.
        composeRule.onNodeWithText("Terminal").assertDoesNotExist()
        // Type a partial prompt in the composer first.
        composeRule.onNode(hasSetTextAction()).performTextInput("正在输入一半的问题")
        composeRule.onNodeWithTag("composer_config_pill").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("session_settings_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("session_settings_sheet").performScrollToNode(hasText("xhigh"))
        composeRule.onNodeWithText("xhigh").performClick()
        assertEquals("xhigh", pickedThinking)
        composeRule.onNodeWithTag("session_settings_sheet").performScrollToNode(hasText("GPT-5.6 Luna"))
        composeRule.onNodeWithText("GPT-5.6 Luna").performClick()
        assertEquals("gpt-5.6-luna", pickedModel)
        composeRule.onNodeWithContentDescription("关闭设置").performClick()
        composeRule.waitForIdle()
        // Partial typed text remains completely undisturbed.
        composeRule.onNodeWithText("正在输入一半的问题").assertIsDisplayed()
    }

    @Test
    fun sessionDockedApprovalCardHandlesDecisionsAndReplyToRedirect() {
        val calls = mutableListOf<Triple<String, String, String>>()
        val approval = RuntimeRequestUi(
            requestId = "req-app-1",
            kind = RuntimeRequestKindUi.APPROVAL,
            title = "允许执行 bash？",
            detail = "rm -rf build && ./gradlew test",
            toolName = "bash",
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                SessionContent(
                    title = "codex rpc",
                    tool = "codex",
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    pendingRuntimeRequests = listOf(approval),
                    decodeParts = { emptyList() },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { _, _ -> },
                    onSendKey = {},
                    onRespondApproval = { id, dec, fb -> calls += Triple(id, dec, fb) },
                    onFocusChanged = {},
                )
            }
        }
        composeRule.onNodeWithTag("pending_approval_card").assertIsDisplayed()
        composeRule.onNodeWithText("输入反馈以拒绝并引导 Agent…").assertIsDisplayed()
        composeRule.onNodeWithText("本会话允许").performScrollTo().performClick()
        assertEquals(Triple("req-app-1", "acceptForSession", ""), calls.last())

        // Sending composer text while an approval is pending declines and redirects the agent.
        composeRule.onNode(hasSetTextAction()).performTextInput("改用单测命令")
        composeRule.onNodeWithContentDescription("发送").performClick()
        assertEquals(Triple("req-app-1", "decline", "改用单测命令"), calls.last())
    }

    @Test
    fun sessionDockedUserInputCardSupportsSelectionCollapseAndSubmit() {
        var submitted: Triple<String, List<RuntimeAnswerInput>, Boolean>? = null
        val userReq = RuntimeRequestUi(
            requestId = "req-ui-1",
            kind = RuntimeRequestKindUi.USER_INPUT,
            title = "请确认实现细节",
            questions = listOf(
                RuntimeQuestionUi(
                    id = "q1",
                    header = "架构",
                    question = "选择运行时通道",
                    options = listOf(
                        RuntimeRequestOptionUi("rpc", "Native RPC", "直接结构化流"),
                        RuntimeRequestOptionUi("tmux", "Legacy Tmux"),
                    ),
                    multiSelect = false,
                    allowCustomAnswer = true,
                ),
                RuntimeQuestionUi(
                    id = "q2",
                    header = "范围",
                    question = "覆盖哪些端",
                    options = listOf(
                        RuntimeRequestOptionUi("go", "Go Bridge"),
                        RuntimeRequestOptionUi("android", "Android App"),
                    ),
                    multiSelect = true,
                    allowCustomAnswer = false,
                ),
            ),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "claude rpc",
                    tool = "claude",
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    pendingRuntimeRequests = listOf(userReq),
                    decodeParts = { emptyList() },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { _, _ -> },
                    onSendKey = {},
                    onRespondUserInput = { id, ans, c -> submitted = Triple(id, ans, c) },
                    onFocusChanged = {},
                )
            }
        }
        composeRule.onNodeWithTag("pending_user_input_card").assertIsDisplayed()
        // Collapse to pill and expand back.
        composeRule.onNodeWithContentDescription("收起提问卡片").performClick()
        composeRule.onNodeWithTag("pending_user_input_pill").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("pending_user_input_card").assertIsDisplayed()

        composeRule.onNodeWithTag("user_input_questions_list").performScrollToNode(hasText("Native RPC"))
        composeRule.onNodeWithText("Native RPC").performClick()
        composeRule.onNodeWithTag("user_input_questions_list").performScrollToNode(hasText("Go Bridge"))
        composeRule.onNodeWithText("Go Bridge").performClick()
        composeRule.onNodeWithTag("user_input_questions_list").performScrollToNode(hasText("Android App"))
        composeRule.onNodeWithText("Android App").performClick()
        composeRule.onNodeWithText("提交回答").performClick()

        val res = submitted!!
        assertEquals("req-ui-1", res.first)
        assertEquals(false, res.third)
        assertEquals(listOf("rpc"), res.second[0].selectedValues)
        assertEquals(listOf("go", "android"), res.second[1].selectedValues)
    }

    @Test
    fun sessionStreamingShowsStopButtonWhenComposerEmpty() {
        var interrupted = false
        composeRule.setContent {
            SidekickTheme(darkTheme = true) {
                SessionContent(
                    title = "pi rpc",
                    tool = "pi",
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    streamingDraft = StreamingMessageState(active = true, text = "生成中"),
                    decodeParts = { emptyList() },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { _, _ -> },
                    onSendKey = {},
                    onInterruptTurn = { interrupted = true },
                    onFocusChanged = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("停止生成").assertIsDisplayed().performClick()
        assertTrue(interrupted)
    }

    @Test
    fun turnWorkLogAutoExpandsLiveAndShowsRunningToolOutputThenCollapsesOnDone() {
        val runningParts = mapOf(
            "u1" to listOf(UiPart.Text("跑构建")),
            "a1" to listOf(
                UiPart.Thinking("先检查测试输出"),
                UiPart.ToolCall("c1", "bash", """{"command":"./scripts/test"}""", "TOOL_CALL_STATUS_RUNNING"),
            ),
            "t1" to listOf(UiPart.ToolResult("c1", "running test case 1\nstreaming live line", isError = false, truncated = false)),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "codex rpc",
                    tool = "codex",
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = listOf(
                        MessageEntity("b1", "c1", 1, "ROLE_USER", "u1", 1L, null),
                        MessageEntity("b1", "c1", 2, "ROLE_ASSISTANT", "a1", 2L, "gpt-6.1-terra"),
                        MessageEntity("b1", "c1", 3, "ROLE_TOOL", "t1", 3L, null),
                    ),
                    streamingDraft = StreamingMessageState(
                        active = true,
                        thinking = "正在分析命令结果",
                        status = "运行 ./scripts/test",
                        model = "gpt-6.1-terra",
                    ),
                    decodeParts = { runningParts[it].orEmpty() },
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
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        // Auto-expanded while running: thinking + live stdout are visible without tapping.
        composeRule.onNodeWithText("先检查测试输出").assertIsDisplayed()
        composeRule.onNodeWithText("running test case 1\nstreaming live line").assertIsDisplayed()
        composeRule.onNodeWithTag("streaming_thinking_box").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun completedTurnWorkLogExpandsThinkingDirectlyOnSingleTap() {
        val doneParts = mapOf(
            "u1" to listOf(UiPart.Text("总结情况")),
            "a1" to listOf(
                UiPart.Thinking("已完成全部推理过程"),
                UiPart.ToolCall("c1", "read_file", """{"Path":"/repo/Main.kt"}""", "TOOL_CALL_STATUS_COMPLETED"),
                UiPart.Text("处理完成。"),
            ),
            "t1" to listOf(UiPart.ToolResult("c1", "ok", isError = false, truncated = false)),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "pi rpc",
                    tool = "pi",
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = listOf(
                        MessageEntity("b1", "c1", 1, "ROLE_USER", "u1", 1L, null),
                        MessageEntity("b1", "c1", 2, "ROLE_ASSISTANT", "a1", 2L, "deepseek-v4.1"),
                        MessageEntity("b1", "c1", 3, "ROLE_TOOL", "t1", 3L, null),
                    ),
                    streamingDraft = StreamingMessageState(active = false),
                    decodeParts = { doneParts[it].orEmpty() },
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
        composeRule.waitForIdle()
        // Completed card starts collapsed, then reveals thinking directly on 1 tap without double collapse.
        composeRule.onNodeWithText("已完成全部推理过程").assertDoesNotExist()
        composeRule.onNodeWithTag("turn_work_log_card").performClick()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("已完成全部推理过程").assertIsDisplayed()
    }

    @Test
    fun sidekickContextBarShowsGitDiffNvimContextAndOpenInNvim() {
        var openedInNvim = false
        var sentPayload = ""
        val wc = WorkspaceContextUi(
            cwd = "/repo/sidekick.nvim",
            gitBranch = "feat-deep-bind",
            changedFiles = listOf(ChangedFileUi("lua/sidekick/init.lua", "M", added = 5, removed = 1)),
            gitDiff = "diff --git a/lua/sidekick/init.lua b/lua/sidekick/init.lua\n+setup()",
            nvimConnected = true,
            nvimServer = "/run/user/1000/nvim.1.0",
            nvimFile = "lua/sidekick/init.lua",
            nvimLine = 18,
            diagnostics = listOf(DiagnosticItemUi("lua/sidekick/init.lua", 18, 3, "error", "undefined field", "lua_ls")),
            openBuffers = listOf("lua/sidekick/init.lua"),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "codex rpc",
                    tool = "codex",
                    sessionId = "codex 1234 abcd",
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    workspaceContext = wc,
                    turnStartedMs = 1000L,
                    decodeParts = { emptyList() },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { txt, _ -> sentPayload = txt },
                    onSendKey = {},
                    onOpenInNvim = { openedInNvim = true },
                    onFocusChanged = {},
                )
            }
        }
        composeRule.onNodeWithTag("context_bar_git_pill").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("workspace_diff_sheet").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("关闭变更").performClick()

        composeRule.onNodeWithTag("context_bar_nvim_pill").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("nvim_context_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("插入当前文件").performClick()
        composeRule.onNodeWithContentDescription("发送").performClick()
        assertEquals("@lua/sidekick/init.lua:18 ", sentPayload)

        composeRule.onNodeWithContentDescription("更多操作").performClick()
        composeRule.onNodeWithText("在 Neovim 打开").performClick()
        assertTrue(openedInNvim)
    }

    @Test
    fun resumeCommandFallbackDialogCopiesAndDismisses() {
        var dismissed = false
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "codex rpc",
                    tool = "codex",
                    sessionId = "codex 1234 abcd",
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    lastResumeCommand = """cd "/repo/sidekick.nvim" && codex resume "thr-123"""",
                    onDismissResumeCommand = { dismissed = true },
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
        composeRule.onNodeWithText("在本地终端接续会话").assertIsDisplayed()
        composeRule.onNodeWithText("""cd "/repo/sidekick.nvim" && codex resume "thr-123"""").assertIsDisplayed()
        composeRule.onNodeWithText("复制命令").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun sessionComposerClearsFocusAfterSendingMessage() {
        var sentPayload = ""
        val focusChanges = mutableListOf<Boolean>()
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "codex rpc",
                    tool = "codex",
                    sessionId = "codex 1234 abcd",
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    decodeParts = { emptyList() },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { txt, submit ->
                        if (submit) sentPayload = txt
                    },
                    onSendKey = {},
                    onFocusChanged = { focusChanges += it },
                )
            }
        }
        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("hello agent")
        composeRule.waitForIdle()
        assertTrue(focusChanges.lastOrNull() == true)

        composeRule.onNodeWithContentDescription("发送").performClick()
        composeRule.waitForIdle()
        assertEquals("hello agent", sentPayload)
        assertTrue(focusChanges.lastOrNull() == false)
    }

    @Test
    fun sessionComposerDisablesEmptySendInRpcChatAndPersistsDraftPerSession() {
        var sentCount = 0
        val enterKeys = mutableListOf<Key>()
        val activeSid = androidx.compose.runtime.mutableStateOf("codex s1")
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "codex rpc",
                    tool = "codex",
                    sessionId = activeSid.value,
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    decodeParts = { emptyList() },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { _, _ -> sentCount++ },
                    onSendKey = { enterKeys += it },
                    onFocusChanged = {},
                )
            }
        }

        // In RPC Chat mode, empty send must not dispatch KEY_ENTER or message.
        composeRule.onNodeWithContentDescription("发送").performClick()
        composeRule.waitForIdle()
        assertEquals(0, sentCount)
        assertTrue(enterKeys.isEmpty())

        // Type draft in session s1, switch to s2 (clean), then switch back to s1 (restored).
        composeRule.onNode(hasSetTextAction()).performTextInput("draft in s1")
        composeRule.waitForIdle()
        composeRule.runOnIdle { activeSid.value = "codex s2" }
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction() and hasText("")).assertIsDisplayed()

        composeRule.runOnIdle { activeSid.value = "codex s1" }
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction() and hasText("draft in s1")).assertIsDisplayed()
    }

    @Test
    fun sessionSettingsSheetPinsCurrentModelFirstAndRanksSearchMatches() {
        val cfg = SessionConfigUi(
            sessionId = "codex 1",
            model = "claude-opus-4",
            thinkingLevel = "high",
            runtimeMode = RuntimeModeUi.AUTO_ACCEPT_EDITS,
            availableModels = listOf(
                ModelOptionUi(id = "gpt-5", provider = "openai", name = "GPT-5", reasoning = true),
                ModelOptionUi(id = "claude-sonnet-4", provider = "anthropic", name = "Claude Sonnet 4", reasoning = true),
                ModelOptionUi(id = "claude-opus-4", provider = "anthropic", name = "Claude Opus 4", reasoning = true, current = true),
            ),
            availableThinkingLevels = listOf("low", "medium", "high"),
            supportedRuntimeModes = listOf(RuntimeModeUi.AUTO_ACCEPT_EDITS, RuntimeModeUi.FULL_ACCESS),
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionSettingsBottomSheet(
                    config = cfg,
                    onSelectModel = {},
                    onSelectThinking = {},
                    onSelectRuntimeMode = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("settings_sheet_model_item")[0].assertTextContains("Claude Opus 4")

        composeRule.onNodeWithTag("settings_sheet_model_search").performTextInput("gpt")
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("settings_sheet_model_item").assertCountEquals(1)
        composeRule.onAllNodesWithTag("settings_sheet_model_item")[0].assertTextContains("GPT-5")

        composeRule.onNodeWithContentDescription("清空搜索").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("模型选择 (3)").assertIsDisplayed()
        composeRule.onAllNodesWithTag("settings_sheet_model_item")[0].assertTextContains("Claude Opus 4")
    }

    @Test
    fun sessionComposerShowsEnterActionForNonRpcChatAndInteractiveDialogConfirmsEnter() {
        val sentKeys = mutableListOf<Key>()
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "codex tui",
                    tool = "codex",
                    sessionId = "codex 1",
                    structuredHistory = true,
                    isRpcSession = false,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    interactiveDialog = InteractiveDialogState(
                        open = true,
                        kind = "confirm",
                        title = "Trust directory?",
                        options = listOf(InteractiveDialogOptionUi(label = "Yes, proceed")),
                    ),
                    decodeParts = { emptyList() },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { _, _ -> },
                    onSendKey = { sentKeys += it },
                    onFocusChanged = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("发送回车").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("回车确认").assertIsDisplayed().performClick()
        assertEquals(listOf(Key.KEY_ENTER, Key.KEY_ENTER), sentKeys)
    }

    @Test
    fun bridgeScreenSupportsLongPressAndDeleteForSessionsAndHistory() {
        val deletedSessions = mutableListOf<String>()
        val deletedHistory = mutableListOf<String>()
        var restoredCount = 0
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                BridgeContent(
                    bridgeName = "workstation",
                    sessions = listOf(
                        SessionEntity("b1", "live-1", "codex", "/repo/a", "Live task", null, true, 100L, 100L),
                    ),
                    history = listOf(
                        HistoryEntity("b1", "conv-1", "codex", "/repo/a", "Old task", "preview", 100L, null),
                    ),
                    tools = emptyList(),
                    workspaces = emptyList(),
                    error = null,
                    hiddenHistoryCount = 1,
                    onBack = {},
                    onRefresh = {},
                    onClickSession = {},
                    onClickHistory = {},
                    onStartNew = { _, _ -> },
                    onDeleteSession = { deletedSessions += it.sid },
                    onDeleteHistory = { deletedHistory += it.convId },
                    onRestoreHiddenHistory = { restoredCount++ },
                )
            }
        }
        // Long-press live session -> bottom sheet -> stop & remove -> confirmation dialog
        composeRule.onNodeWithText("Live task").performTouchInput { longClick() }
        composeRule.onNodeWithText("停止并移除会话").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("停止并移除会话？").assertIsDisplayed()
        composeRule.onNodeWithText("停止并移除").performClick()
        assertEquals(listOf("live-1"), deletedSessions)

        // Switch to history tab -> verify restore banner + long-press remove
        composeRule.onNodeWithText("全部历史 (1)").performClick()
        composeRule.onNodeWithText("已隐藏 1 条历史").assertIsDisplayed()
        composeRule.onNodeWithText("恢复全部").performClick()
        assertEquals(1, restoredCount)

        composeRule.onNodeWithText("Old task").performTouchInput { longClick() }
        composeRule.onNodeWithText("从列表移除（保留电脑原文件）").assertIsDisplayed().performClick()
        assertEquals(listOf("conv-1"), deletedHistory)
    }

    @Test
    fun workspaceDiffSheetExpandsPerFileDiffAndOpensFullscreenPreview() {
        val rawDiff = """
            diff --git a/lua/one.lua b/lua/one.lua
            --- a/lua/one.lua
            +++ b/lua/one.lua
            @@ -1,2 +1,2 @@
             local a = 1
            -return 1
            +return 2
            diff --git a/new_untracked.lua b/new_untracked.lua
            new file mode 100644
            --- /dev/null
            +++ b/new_untracked.lua
            @@ -0,0 +1,1 @@
            +print("hello")
        """.trimIndent()
        val wc = WorkspaceContextUi(
            cwd = "/repo/sidekick.nvim",
            gitBranch = "feat-diff",
            changedFiles = listOf(
                ChangedFileUi("lua/one.lua", "M", added = 1, removed = 1),
                ChangedFileUi("new_untracked.lua", "??", added = 1, removed = 0),
            ),
            gitDiff = rawDiff,
        )
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                WorkspaceDiffSheet(
                    workspaceContext = wc,
                    loading = false,
                    onRefresh = {},
                    onAttachSummary = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("workspace_diff_sheet").assertIsDisplayed()
        // Multi-file list starts collapsed; tap second file row to inspect untracked diff
        composeRule.onNodeWithText("+print(\"hello\")").assertDoesNotExist()
        composeRule.onNodeWithTag("diff_file_row_new_untracked.lua").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("workspace_diff_sheet").performScrollToNode(hasText("+print(\"hello\")"))
        composeRule.onNodeWithText("+print(\"hello\")").assertIsDisplayed()

        // Open fullscreen preview dialog and close it
        composeRule.onNodeWithContentDescription("全屏查看 Diff").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("fullscreen_diff_dialog").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("关闭全屏 Diff").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("fullscreen_diff_dialog").assertDoesNotExist()
    }

    @Test
    fun userBubbleLongPressShowsCopyQuoteAndReeditActions() {
        var sentPayload = ""
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "codex rpc",
                    tool = "codex",
                    sessionId = "codex 1",
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = listOf(
                        MessageEntity("b1", "c1", 1, "ROLE_USER", """[{"type":"text","text":"explain main loop"}]""", 1L, null),
                    ),
                    decodeParts = { listOf(UiPart.Text("explain main loop")) },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = {},
                    onSendInput = { txt, submit -> if (submit) sentPayload = txt },
                    onSendKey = {},
                    onFocusChanged = {},
                )
            }
        }
        // Wait for the initial 2.4s SessionTitleDetailsOverlay peek to auto-collapse so the top bubble is unobstructed.
        composeRule.mainClock.advanceTimeBy(2600L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("explain main loop").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("复制消息").assertIsDisplayed()
        composeRule.onNodeWithText("引用回复").assertIsDisplayed()
        composeRule.onNodeWithText("填入输入框重编").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("发送").performClick()
        assertEquals("explain main loop", sentPayload)
    }

    @Test
    fun findNvimMuxSessionMatchesConversationOrNewMuxSession() {
        val oldMux = Session(
            id = "codex 1111 existing",
            tool = "codex",
            cwd = "/repo/sidekick.nvim",
            backend = Backend.BACKEND_TMUX,
            live = true,
            last_activity_unix_ms = 100L,
        )
        val sameConvMux = Session(
            id = "codex 1111 new-conv",
            tool = "codex",
            cwd = "/repo/sidekick.nvim",
            backend = Backend.BACKEND_TMUX,
            live = true,
            conversation = Conversation(id = "conv-123", provider = "codex", resumable = true),
            last_activity_unix_ms = 200L,
        )
        val rpcTwin = Session(
            id = "codex 1111 rpc-new",
            tool = "codex",
            cwd = "/repo/sidekick.nvim",
            backend = Backend.BACKEND_RPC,
            live = true,
            conversation = Conversation(id = "conv-123", provider = "codex", resumable = true),
            last_activity_unix_ms = 300L,
        )
        val matchedByConv = findNvimMuxSession(
            sessions = listOf(oldMux, rpcTwin, sameConvMux),
            previousSid = "codex 1111 rpc-prev",
            existingSidsBeforeHandover = setOf(oldMux.id, "codex 1111 rpc-prev"),
            tool = "codex",
            cwd = "/repo/sidekick.nvim",
            convId = "conv-123",
        )
        assertEquals(sameConvMux.id, matchedByConv?.id)

        val freshAnonMux = Session(
            id = "codex 1111 anon",
            tool = "codex",
            cwd = "/repo/sidekick.nvim/",
            backend = Backend.BACKEND_ZELLIJ,
            live = true,
            last_activity_unix_ms = 250L,
        )
        val matchedByNewMux = findNvimMuxSession(
            sessions = listOf(oldMux, freshAnonMux),
            previousSid = "codex 1111 rpc-prev",
            existingSidsBeforeHandover = setOf(oldMux.id, "codex 1111 rpc-prev"),
            tool = "codex",
            cwd = "/repo/sidekick.nvim",
            convId = "conv-999",
        )
        assertEquals(freshAnonMux.id, matchedByNewMux?.id)

        val noMatch = findNvimMuxSession(
            sessions = listOf(oldMux, rpcTwin),
            previousSid = "codex 1111 rpc-prev",
            existingSidsBeforeHandover = setOf(oldMux.id, "codex 1111 rpc-prev"),
            tool = "codex",
            cwd = "/repo/sidekick.nvim",
            convId = "conv-other",
        )
        assertNull(noMatch)
    }

    @Test
    fun sessionContentShowsNvimHandoverBannerWithTakeoverAndBackActions() {
        var tookOver = false
        var wentBack = false
        composeRule.setContent {
            SidekickTheme(darkTheme = false) {
                SessionContent(
                    title = "codex rpc",
                    tool = "codex",
                    sessionId = "codex 1",
                    convId = "conv-1",
                    structuredHistory = true,
                    isRpcSession = true,
                    live = true,
                    connectionState = ConnectionState.CONNECTED,
                    messages = emptyList(),
                    nvimHandover = NvimHandoverState(
                        active = true,
                        switching = false,
                        message = "会话已移交至本地 Neovim（开启 cli.mux 可双端实时同步）",
                        canTakeOverBack = true,
                    ),
                    decodeParts = { emptyList() },
                    screenLines = emptyList(),
                    screenRevision = 1L,
                    localTyping = false,
                    toast = null,
                    onClearToast = {},
                    onBack = { wentBack = true },
                    onSendInput = { _, _ -> },
                    onSendKey = {},
                    onTakeOverAsRpc = { tookOver = true },
                    onFocusChanged = {},
                )
            }
        }
        composeRule.onNodeWithTag("nvim_handover_banner").assertIsDisplayed()
        composeRule.onNodeWithText("会话已移交至本地 Neovim（开启 cli.mux 可双端实时同步）").assertIsDisplayed()
        composeRule.onNodeWithText("切回手机接管").assertIsDisplayed().performClick()
        assertTrue(tookOver)
        composeRule.onNodeWithText("返回列表").assertIsDisplayed().performClick()
        assertTrue(wentBack)
    }
}
