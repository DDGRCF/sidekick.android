package dev.ddgrcf.sidekick.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import dev.ddgrcf.sidekick.core.ansi.AnsiLine
import dev.ddgrcf.sidekick.core.ansi.AnsiParser
import dev.ddgrcf.sidekick.core.relay.ConnectionState
import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.data.UiPart
import dev.ddgrcf.sidekick.vm.MediaKind
import dev.ddgrcf.sidekick.vm.PendingAttachment
import dev.ddgrcf.sidekick.core.relay.QrPairingPayload
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64

@Serializable
private sealed interface DemoPage {
    @Serializable
    data object Home : DemoPage

    @Serializable
    data class Bridge(val name: String) : DemoPage

    @Serializable
    data object Pair : DemoPage

    @Serializable
    data class Session(
        val sid: String,
        val tool: String,
        val title: String,
        val structured: Boolean,
    ) : DemoPage

    @Serializable
    data object Settings : DemoPage
}

@Serializable
private data class SavedDemoMessage(
    val bridgeId: String,
    val convId: String,
    val seq: Long,
    val role: String,
    val partsJson: String,
    val createdMs: Long,
    val model: String? = null,
)

@Serializable
private data class SavedDemoAttachment(
    val id: String,
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val kindName: String,
    val durationMs: Long = 0L,
)

@Composable
fun DemoPreviewFlow(
    themeModeKey: String,
    onSetThemeMode: (String) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val json = remember { Json { ignoreUnknownKeys = true } }
    var page by rememberSaveable(
        stateSaver = Saver<DemoPage, String>(
            save = { json.encodeToString(DemoPage.serializer(), it) },
            restore = { raw ->
                runCatching { json.decodeFromString(DemoPage.serializer(), raw) }
                    .getOrDefault(DemoPage.Home)
            },
        ),
    ) { mutableStateOf(DemoPage.Home) }
    val messages = rememberSaveable(
        saver = listSaver<SnapshotStateList<MessageEntity>, String>(
            save = { list ->
                list.drop(DemoSampleData.initialMessages.size).map { m ->
                    json.encodeToString(
                        SavedDemoMessage(
                            bridgeId = m.bridgeId,
                            convId = m.convId,
                            seq = m.seq,
                            role = m.role,
                            partsJson = m.partsJson,
                            createdMs = m.createdMs,
                            model = m.model,
                        ),
                    )
                }
            },
            restore = { saved ->
                mutableStateListOf<MessageEntity>().apply {
                    addAll(DemoSampleData.initialMessages)
                    saved.forEach { raw ->
                        runCatching { json.decodeFromString<SavedDemoMessage>(raw) }.getOrNull()?.let { sm ->
                            add(
                                MessageEntity(
                                    bridgeId = sm.bridgeId,
                                    convId = sm.convId,
                                    seq = sm.seq,
                                    role = sm.role,
                                    partsJson = sm.partsJson,
                                    createdMs = sm.createdMs,
                                    model = sm.model,
                                ),
                            )
                        }
                    }
                }
            },
        ),
    ) {
        mutableStateListOf<MessageEntity>().apply { addAll(DemoSampleData.initialMessages) }
    }
    val pendingAttachments = rememberSaveable(
        saver = listSaver<SnapshotStateList<PendingAttachment>, String>(
            save = { list ->
                list.map { a ->
                    json.encodeToString(
                        SavedDemoAttachment(
                            id = a.id,
                            uri = a.uri,
                            name = a.name,
                            mimeType = a.mimeType,
                            sizeBytes = a.sizeBytes,
                            kindName = a.kind.name,
                            durationMs = a.durationMs,
                        ),
                    )
                }
            },
            restore = { saved ->
                mutableStateListOf<PendingAttachment>().apply {
                    saved.forEach { raw ->
                        runCatching { json.decodeFromString<SavedDemoAttachment>(raw) }.getOrNull()?.let { sa ->
                            add(
                                PendingAttachment(
                                    id = sa.id,
                                    uri = sa.uri,
                                    name = sa.name,
                                    mimeType = sa.mimeType,
                                    sizeBytes = sa.sizeBytes,
                                    kind = runCatching { MediaKind.valueOf(sa.kindName) }.getOrDefault(MediaKind.IMAGE),
                                    durationMs = sa.durationMs,
                                ),
                            )
                        }
                    }
                }
            },
        ),
    ) { mutableStateListOf<PendingAttachment>() }
    val appendedTerminalLines = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>(
            save = { it.toList() },
            restore = { mutableStateListOf<String>().apply { addAll(it) } },
        ),
    ) { mutableStateListOf<String>() }
    val screenLines = remember(appendedTerminalLines.size) {
        if (appendedTerminalLines.isEmpty()) {
            DemoSampleData.terminalLines
        } else {
            DemoSampleData.terminalLines + appendedTerminalLines.flatMap { AnsiParser.parse(it) }
        }
    }
    var screenRevision by rememberSaveable { mutableLongStateOf(1L) }
    var fileQuery by remember { mutableStateOf("") }
    var demoToast by remember { mutableStateOf<String?>(null) }
    var pairStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var pairIsError by rememberSaveable { mutableStateOf(false) }
    val filteredDemoFiles = remember(fileQuery) {
        val q = fileQuery.trim().lowercase()
        if (q.isEmpty()) DemoSampleData.sampleFiles
        else DemoSampleData.sampleFiles.filter { it.relPath.lowercase().contains(q) }
    }

    BackHandler(enabled = page != DemoPage.Home) {
        page = when (val cur = page) {
            DemoPage.Home -> DemoPage.Home
            is DemoPage.Bridge -> DemoPage.Home
            DemoPage.Pair -> DemoPage.Home
            is DemoPage.Session -> DemoPage.Bridge(DemoSampleData.bridges.first().name)
            DemoPage.Settings -> DemoPage.Home
        }
    }

    when (val cur = page) {
        DemoPage.Home -> {
            HomeContent(
                connectionState = ConnectionState.CONNECTED,
                bridges = DemoSampleData.bridges,
                refreshing = false,
                error = null,
                isDemo = true,
                onRefresh = {},
                onOpenBridge = { dev -> page = DemoPage.Bridge(dev.name) },
                onOpenPair = {
                    pairStatus = null
                    pairIsError = false
                    page = DemoPage.Pair
                },
                onOpenSettings = { page = DemoPage.Settings },
            )
        }
        DemoPage.Pair -> {
            PairContent(
                status = pairStatus,
                isError = pairIsError,
                busy = false,
                paired = false,
                isDemo = true,
                onBack = { page = DemoPage.Home },
                onPairQrJson = { raw ->
                    val qr = runCatching { json.decodeFromString<QrPairingPayload>(raw) }.getOrNull()
                    if (qr == null || qr.v != 1 || qr.eid.isBlank() || qr.psk.isBlank()) {
                        pairIsError = true
                        pairStatus = "已扫码内容非 sidekick 配对二维码（需含 v=1/eid/psk）"
                    } else {
                        pairIsError = false
                        pairStatus = "已成功识别配对二维码（eid=${qr.eid}），请连接真实 Relay 后完成加密绑定"
                    }
                },
                onPairManualCode = { userCode, psk ->
                    if (psk.isBlank()) {
                        pairIsError = true
                        pairStatus = "缺少一次性 PSK，禁止无 PSK 握手"
                    } else {
                        pairIsError = false
                        pairStatus = "演示校验通过：配对码 ${userCode.trim()}"
                    }
                },
            )
        }
        is DemoPage.Bridge -> {
            BridgeContent(
                bridgeName = cur.name,
                sessions = DemoSampleData.sessions,
                history = DemoSampleData.history,
                tools = DemoSampleData.tools,
                workspaces = DemoSampleData.workspaces,
                error = null,
                isDemo = true,
                onBack = { page = DemoPage.Home },
                onRefresh = {},
                onClickSession = { s ->
                    page = DemoPage.Session(
                        sid = s.sid,
                        tool = s.tool,
                        title = s.title ?: s.sid,
                        structured = s.tool != "antigravity" && s.tool != "agy",
                    )
                },
                onClickHistory = { h ->
                    page = DemoPage.Session(
                        sid = h.liveSid ?: h.convId,
                        tool = h.tool,
                        title = h.title ?: h.convId,
                        structured = h.tool != "antigravity" && h.tool != "agy",
                    )
                },
                onStartNew = { tool, cwd ->
                    page = DemoPage.Session(
                        sid = "$tool-demo",
                        tool = tool,
                        title = "$tool · ${cwd.substringAfterLast('/')}",
                        structured = tool != "antigravity" && tool != "agy",
                    )
                },
            )
        }
        is DemoPage.Session -> {
            SessionContent(
                sessionId = cur.sid,
                title = cur.title,
                tool = cur.tool,
                structuredHistory = cur.structured,
                connectionState = ConnectionState.CONNECTED,
                messages = messages.toList(),
                decodeParts = DemoSampleData::decode,
                screenLines = screenLines,
                screenRevision = screenRevision,
                localTyping = false,
                isDemo = true,
                pendingAttachments = pendingAttachments.toList(),
                fileSuggestions = filteredDemoFiles,
                slashCommands = remember(cur.tool) { DemoSampleData.sampleCommands(cur.tool) },
                onSearchFiles = { fileQuery = it },
                toast = demoToast,
                onClearToast = { demoToast = null },
                onBack = { page = DemoPage.Bridge(DemoSampleData.bridges.first().name) },
                onAddAttachment = { att -> pendingAttachments += att },
                onRemoveAttachment = { id -> pendingAttachments.removeAll { it.id == id } },
                onRefreshMessages = { demoToast = "演示数据已刷新" },
                onStopSession = { page = DemoPage.Bridge(DemoSampleData.bridges.first().name) },
                onSendInput = { text, _ ->
                    val atts = pendingAttachments.toList()
                    if (atts.isNotEmpty()) {
                        pendingAttachments.clear()
                    }
                    if (text.isNotBlank() || atts.isNotEmpty()) {
                        val nextSeq = (messages.lastOrNull()?.seq ?: 0L) + 1L
                        val parts = buildList<UiPart> {
                            if (text.isNotBlank()) add(UiPart.Text(text))
                            atts.forEach { a ->
                                val thumb = if (a.kind == MediaKind.IMAGE) {
                                    readDemoImageThumbB64(context, a.uri)
                                } else {
                                    null
                                }
                                add(
                                    UiPart.Media(
                                        mediaId = a.id,
                                        kind = a.kind.name.lowercase(),
                                        mimeType = a.mimeType,
                                        name = a.name,
                                        sizeBytes = a.sizeBytes,
                                        durationMs = a.durationMs,
                                        thumbnailB64 = thumb,
                                    ),
                                )
                            }
                        }
                        messages += MessageEntity(
                            bridgeId = DemoSampleData.bridges.first().id,
                            convId = "conv-codex-1",
                            seq = nextSeq,
                            role = "ROLE_USER",
                            partsJson = json.encodeToString(parts),
                            createdMs = System.currentTimeMillis(),
                            model = null,
                        )
                        if (text.isNotBlank()) {
                            screenRevision++
                            appendedTerminalLines += "\u001B[32m> $text\u001B[0m"
                        }
                        if (atts.isNotEmpty()) {
                            val tier = if (atts.any { it.usesBlobTier }) "加密 Blob (>2MiB)" else "Noise 直传 (≤2MiB)"
                            demoToast = "演示发送 ${atts.size} 个媒体附件（$tier）"
                        }
                    }
                },
                onSendKey = { key -> demoToast = "演示按键: ${key.name}" },
                onFocusChanged = {},
            )
        }
        DemoPage.Settings -> {
            SettingsContent(
                themeModeKey = themeModeKey,
                relayUrl = "http://127.0.0.1:8080 (Demo)",
                deviceId = "demo-phone-device",
                devices = DemoSampleData.allDevices,
                otpauthUri = null,
                recoveryCodes = emptyList(),
                message = null,
                onBack = { page = DemoPage.Home },
                onSetThemeMode = onSetThemeMode,
                onEnrollTotp = {},
                onConfirmTotp = {},
                onDismissRecoveryCodes = {},
                onRevokeDevice = {},
                onLogout = onExit,
            )
        }
    }
}

private fun readDemoImageThumbB64(context: Context, uriStr: String): String? = runCatching {
    val uri = Uri.parse(uriStr)
    val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    val maxDim = 720
    val scale = maxOf(decoded.width, decoded.height)
    val scaled = if (scale > maxDim) {
        val w = (decoded.width * maxDim / scale).coerceAtLeast(1)
        val h = (decoded.height * maxDim / scale).coerceAtLeast(1)
        Bitmap.createScaledBitmap(decoded, w, h, true)
    } else {
        decoded
    }
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
    Base64.getEncoder().encodeToString(out.toByteArray())
}.getOrNull()
