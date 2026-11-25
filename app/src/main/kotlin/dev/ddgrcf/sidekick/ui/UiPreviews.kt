package dev.ddgrcf.sidekick.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.io.ByteArrayOutputStream
import java.util.Base64
import dev.ddgrcf.sidekick.core.ansi.AnsiLine
import dev.ddgrcf.sidekick.core.ansi.AnsiParser
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.core.relay.RelayDevice
import dev.ddgrcf.sidekick.core.store.HistoryEntity
import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.core.store.SessionEntity
import dev.ddgrcf.sidekick.data.UiPart
import dev.ddgrcf.sidekick.proto.v1.ToolInfo
import dev.ddgrcf.sidekick.proto.v1.Workspace
import dev.ddgrcf.sidekick.ui.theme.SidekickTheme
import dev.ddgrcf.sidekick.vm.MediaKind
import dev.ddgrcf.sidekick.vm.PendingAttachment
import dev.ddgrcf.sidekick.vm.SlashCommandSourceUi
import dev.ddgrcf.sidekick.vm.SlashCommandSuggestion
import dev.ddgrcf.sidekick.vm.WorkspaceFileSuggestion
import dev.ddgrcf.sidekick.vm.coreBuiltinSlashCommands
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object DemoSampleData {
    private val json = Json { ignoreUnknownKeys = true }

    val bridges = listOf(
        RelayDevice(
            id = "bridge-mac-studio",
            kind = "bridge",
            name = "mac-studio",
            pubkey = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=",
            online = true,
        ),
        RelayDevice(
            id = "bridge-linux-box",
            kind = "bridge",
            name = "linux-workstation",
            pubkey = "ISAfHh0cGxoZGBcWFRQTEhEQDw4NDAsKCQgHBgUEAwI=",
            online = false,
        ),
    )

    val allDevices = bridges + listOf(
        RelayDevice(
            id = "phone-pixel",
            kind = "phone",
            name = "Pixel 9 Pro",
            pubkey = "MDAxMTIyMzM0NDU1NjY3Nzg4OTlhYWJiY2NkZGVlZmY=",
            online = true,
        ),
    )

    val tools = listOf(
        ToolInfo(name = "codex", structured_history = true),
        ToolInfo(name = "claude", structured_history = true),
        ToolInfo(name = "pi", structured_history = true),
        ToolInfo(name = "devin", structured_history = true),
        ToolInfo(name = "antigravity", structured_history = false),
    )

    val workspaces = listOf(
        Workspace(cwd = "/home/dev/sidekick.nvim"),
        Workspace(cwd = "/home/dev/sidekick-backend"),
    )

    val sessions = listOf(
        SessionEntity(
            bridgeId = "bridge-mac-studio",
            sid = "codex-59bb8424",
            tool = "codex",
            cwd = "/home/dev/sidekick.nvim",
            title = "Refactor Noise chunking & E2E media",
            convId = "conv-codex-1",
            live = true,
            lastActivityMs = 1764000000000L,
            updatedMs = 1764000000000L,
        ),
        SessionEntity(
            bridgeId = "bridge-mac-studio",
            sid = "claude-8a21f010",
            tool = "claude",
            cwd = "/home/dev/sidekick-backend",
            title = "Inspect relay WSS heartbeat",
            convId = "conv-claude-1",
            live = true,
            lastActivityMs = 1763998200000L,
            updatedMs = 1763998200000L,
        ),
        SessionEntity(
            bridgeId = "bridge-mac-studio",
            sid = "antigravity-01de",
            tool = "antigravity",
            cwd = "/home/dev/sidekick.nvim",
            title = "Live TUI terminal",
            convId = null,
            live = true,
            lastActivityMs = 1763997000000L,
            updatedMs = 1763997000000L,
        ),
    )

    val history = listOf(
        HistoryEntity(
            bridgeId = "bridge-mac-studio",
            convId = "conv-codex-1",
            tool = "codex",
            cwd = "/home/dev/sidekick.nvim",
            title = "Refactor Noise chunking & E2E media",
            preview = "Implemented Tier A/B E2E encrypted media architecture and Dual Theme UI.",
            updatedMs = 1764000000000L,
            liveSid = "codex-59bb8424",
        ),
        HistoryEntity(
            bridgeId = "bridge-mac-studio",
            convId = "conv-pi-99",
            tool = "pi",
            cwd = "/home/dev/sidekick-backend",
            title = "SQLite refresh token replay tests",
            preview = "All family revocation checks passed with -race enabled.",
            updatedMs = 1763950000000L,
            liveSid = null,
        ),
    )

    val samplePreviewPngB64: String by lazy { createSamplePreviewPngBase64() }

    val initialMessages: List<MessageEntity> by lazy {
        val now = 1764000000000L
        listOf(
            MessageEntity(
                bridgeId = "bridge-mac-studio",
                convId = "conv-codex-1",
                seq = 1L,
                role = "ROLE_USER",
                partsJson = json.encodeToString(
                    listOf<UiPart>(
                        UiPart.Text("帮我看下协议分片上限，并顺便生成一张效果预览图"),
                    ),
                ),
                createdMs = now - 120_000L,
                model = null,
            ),
            MessageEntity(
                bridgeId = "bridge-mac-studio",
                convId = "conv-codex-1",
                seq = 2L,
                role = "ROLE_ASSISTANT",
                partsJson = json.encodeToString(
                    listOf<UiPart>(
                        UiPart.ToolCall(
                            callId = "tc-1",
                            name = "read_file",
                            inputJson = """{"path":"core/protocol/Chunking.kt"}""",
                            status = "TOOL_CALL_STATUS_COMPLETED",
                        ),
                        UiPart.Text(
                            """
                            ### 分片与媒体传输策略

                            分片头固定 **9 bytes**（`u48 | u8 | u16`），单帧最大 **8 MiB**；支持 *Noise 直传* 与 *加密 Blob* 自动分流：

                            | 通道 | 适用体积 | 加密方式 |
                            | --- | --- | --- |
                            | Tier A · Noise | `<= 2 MiB` | `ChaChaPoly` |
                            | Tier B · Blob | `> 2 MiB` | 端到端加密分块 |

                            ```kotlin
                            const val MAX_CHUNK_PAYLOAD = 60_000
                            const val MAX_FRAME_BYTES = 8 * 1024 * 1024
                            ```

                            > 图片在传输前自动剥离 EXIF 元数据，参见 [设计规范](https://github.com/DDGRCF/sidekick.nvim)。
                            """.trimIndent(),
                        ),
                        UiPart.Media(
                            mediaId = "m-img-1",
                            kind = "image",
                            mimeType = "image/png",
                            name = "ui_preview_render.png",
                            sizeBytes = 64_200L,
                            width = 640,
                            height = 360,
                            thumbnailB64 = samplePreviewPngB64,
                        ),
                    ),
                ),
                createdMs = now - 60_000L,
                model = "gpt-5.1-codex",
            ),
            MessageEntity(
                bridgeId = "bridge-mac-studio",
                convId = "conv-codex-1",
                seq = 3L,
                role = "ROLE_USER",
                partsJson = json.encodeToString(
                    listOf<UiPart>(
                        UiPart.Text("清晰了！再把录屏演示和语音说明也放进来我看下"),
                    ),
                ),
                createdMs = now - 30_000L,
                model = null,
            ),
            MessageEntity(
                bridgeId = "bridge-mac-studio",
                convId = "conv-codex-1",
                seq = 4L,
                role = "ROLE_ASSISTANT",
                partsJson = json.encodeToString(
                    listOf<UiPart>(
                        UiPart.Text("已附上操作录屏和语音说明："),
                        UiPart.Media(
                            mediaId = "m-vid-1",
                            kind = "video",
                            mimeType = "video/mp4",
                            name = "demo_walkthrough.mp4",
                            sizeBytes = 4_850_000L,
                            durationMs = 28_000L,
                            thumbnailB64 = samplePreviewPngB64,
                        ),
                        UiPart.Media(
                            mediaId = "m-aud-1",
                            kind = "audio",
                            mimeType = "audio/ogg",
                            name = "voice_note.ogg",
                            sizeBytes = 124_000L,
                            durationMs = 19_000L,
                        ),
                    ),
                ),
                createdMs = now,
                model = "gpt-5.1-codex",
            ),
        )
    }

    val pendingSample = listOf(
        PendingAttachment(
            id = "pending-1",
            uri = "content://demo/clip.mp4",
            name = "bug_repro.mp4",
            mimeType = "video/mp4",
            sizeBytes = 3_400_000L,
            kind = MediaKind.VIDEO,
        ),
    )

    val sampleFiles = listOf(
        WorkspaceFileSuggestion("app/android/app/src/main/kotlin/dev/ddgrcf/sidekick/ui/SessionScreen.kt", sizeBytes = 84_200L),
        WorkspaceFileSuggestion("app/server/internal/bridge/workspace_index.go", sizeBytes = 12_400L),
        WorkspaceFileSuggestion("app/server/proto/sidekick/v1/remote.proto", sizeBytes = 11_800L),
        WorkspaceFileSuggestion("lua/sidekick/cli/remote.lua", sizeBytes = 6_200L),
        WorkspaceFileSuggestion("README.md", sizeBytes = 18_900L),
    )

    fun sampleCommands(tool: String): List<SlashCommandSuggestion> {
        val extras = listOf(
            SlashCommandSuggestion("unslop", "清理 AI 模板化视觉并优化交互", "[screen]", SlashCommandSourceUi.SKILL),
            SlashCommandSuggestion("unslop-anything", "跨端 UI 诊断、重构与走查", "[treat|review]", SlashCommandSourceUi.SKILL),
            SlashCommandSuggestion("frontend:audit", "核查移动端热区与对比度", "", SlashCommandSourceUi.PROJECT),
        )
        return coreBuiltinSlashCommands(tool) + extras
    }

    val terminalLines: List<AnsiLine> by lazy {
        AnsiParser.parse(
            "\u001B[1;36m~/Scripts/sidekick.nvim\u001B[0m \u001B[32m(main)\u001B[0m\n" +
                "\u001B[1;34m>\u001B[0m go test -race -p 2 ./...\n" +
                "\u001B[32mok\u001B[0m  github.com/DDGRCF/sidekick.server/internal/noise  \u001B[90m1.164s\u001B[0m\n" +
                "\u001B[32mok\u001B[0m  github.com/DDGRCF/sidekick.server/internal/e2etest \u001B[90m1.346s\u001B[0m\n" +
                "\u001B[33mcodex>\u001B[0m \u001B[1mReady for instructions (Noise KK pinned)\u001B[0m █",
        )
    }

    fun decode(partsJson: String): List<UiPart> =
        runCatching { json.decodeFromString<List<UiPart>>(partsJson) }.getOrDefault(emptyList())

    private fun createSamplePreviewPngBase64(): String = runCatching {
        val w = 480
        val h = 270
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0xFF0B0E14.toInt()
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)

        // Window chrome
        p.color = 0xFF141925.toInt()
        canvas.drawRoundRect(RectF(20f, 20f, w - 20f, h - 20f), 20f, 20f, p)
        p.color = 0xFFEF4444.toInt()
        canvas.drawCircle(46f, 44f, 6f, p)
        p.color = 0xFFF59E0B.toInt()
        canvas.drawCircle(66f, 44f, 6f, p)
        p.color = 0xFF22C55E.toInt()
        canvas.drawCircle(86f, 44f, 6f, p)

        // Chart bars preview returned by local agent
        val barColors = intArrayOf(0xFF60A5FA.toInt(), 0xFF2DD4BF.toInt(), 0xFFA855F7.toInt(), 0xFF22C55E.toInt())
        val barHeights = floatArrayOf(85f, 130f, 105f, 155f)
        for (i in barHeights.indices) {
            p.color = barColors[i]
            val left = 54f + i * 96f
            canvas.drawRoundRect(RectF(left, h - 42f - barHeights[i], left + 62f, h - 42f), 10f, 10f, p)
        }

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        Base64.getEncoder().encodeToString(out.toByteArray())
    }.getOrDefault("")
}

@Preview(name = "Login - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun PreviewLoginDark() {
    SidekickTheme(darkTheme = true) {
        LoginContent("http://127.0.0.1:8080", false, null, {}, {}, {})
    }
}

@Preview(name = "Login - Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun PreviewLoginLight() {
    SidekickTheme(darkTheme = false) {
        LoginContent("http://127.0.0.1:8080", false, null, {}, {}, {})
    }
}

@Preview(name = "TOTP - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun PreviewTotpDark() {
    SidekickTheme(darkTheme = true) {
        TotpContent(totpEnrolled = true, loading = false, error = null, onVerify = {})
    }
}

@Preview(name = "Home - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun PreviewHomeDark() {
    SidekickTheme(darkTheme = true) {
        HomeContent(ConnectionState.CONNECTED, DemoSampleData.bridges, false, null, {}, {}, {}, {})
    }
}

@Preview(name = "Home - Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun PreviewHomeLight() {
    SidekickTheme(darkTheme = false) {
        HomeContent(ConnectionState.CONNECTING, DemoSampleData.bridges, false, null, {}, {}, {}, {})
    }
}

@Preview(name = "Bridge - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun PreviewBridgeDark() {
    SidekickTheme(darkTheme = true) {
        BridgeContent(
            bridgeName = "mac-studio",
            sessions = DemoSampleData.sessions,
            history = DemoSampleData.history,
            tools = DemoSampleData.tools,
            workspaces = DemoSampleData.workspaces,
            error = null,
            onBack = {},
            onRefresh = {},
            onClickSession = {},
            onClickHistory = {},
            onStartNew = { _, _ -> },
        )
    }
}

@Preview(name = "Session Chat - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun PreviewSessionChatDark() {
    SidekickTheme(darkTheme = true) {
        SessionContent(
            sessionId = "codex-59bb8424",
            title = "Refactor Noise chunking",
            tool = "codex",
            structuredHistory = true,
            connectionState = ConnectionState.CONNECTED,
            messages = DemoSampleData.initialMessages,
            decodeParts = DemoSampleData::decode,
            screenLines = DemoSampleData.terminalLines,
            screenRevision = 1L,
            localTyping = true,
            pendingAttachments = DemoSampleData.pendingSample,
            toast = null,
            onClearToast = {},
            onBack = {},
            onSendInput = { _, _ -> },
            onSendKey = {},
            onFocusChanged = {},
        )
    }
}

@Preview(name = "Session Chat - Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun PreviewSessionChatLight() {
    SidekickTheme(darkTheme = false) {
        SessionContent(
            sessionId = "codex-59bb8424",
            title = "Refactor Noise chunking",
            tool = "codex",
            structuredHistory = true,
            connectionState = ConnectionState.CONNECTED,
            messages = DemoSampleData.initialMessages,
            decodeParts = DemoSampleData::decode,
            screenLines = DemoSampleData.terminalLines,
            screenRevision = 1L,
            localTyping = false,
            pendingAttachments = DemoSampleData.pendingSample,
            toast = null,
            onClearToast = {},
            onBack = {},
            onSendInput = { _, _ -> },
            onSendKey = {},
            onFocusChanged = {},
        )
    }
}

@Preview(name = "Session Terminal - Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun PreviewSessionTerminalLight() {
    SidekickTheme(darkTheme = false) {
        SessionContent(
            sessionId = "antigravity-01de",
            title = "Live TUI terminal",
            tool = "antigravity",
            structuredHistory = false,
            connectionState = ConnectionState.CONNECTED,
            messages = emptyList(),
            decodeParts = DemoSampleData::decode,
            screenLines = DemoSampleData.terminalLines,
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

@Preview(name = "Settings - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun PreviewSettingsDark() {
    SidekickTheme(darkTheme = true) {
        SettingsContent(
            themeModeKey = "dark",
            relayUrl = "http://127.0.0.1:8080",
            deviceId = "phone-pixel-9-pro",
            devices = DemoSampleData.allDevices,
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

@Preview(name = "Settings - Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun PreviewSettingsLight() {
    SidekickTheme(darkTheme = false) {
        SettingsContent(
            themeModeKey = "light",
            relayUrl = "http://127.0.0.1:8080",
            deviceId = "phone-pixel-9-pro",
            devices = DemoSampleData.allDevices,
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
