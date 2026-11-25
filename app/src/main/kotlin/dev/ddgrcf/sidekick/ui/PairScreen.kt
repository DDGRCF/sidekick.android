package dev.ddgrcf.sidekick.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras
import dev.ddgrcf.sidekick.vm.PairViewModel
import java.util.concurrent.Executors

@Composable
fun PairScreen(
    vm: PairViewModel,
    onBack: () -> Unit,
    onPaired: () -> Unit,
) {
    val status by vm.status.collectAsStateWithLifecycle()
    val isError by vm.isError.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val paired by vm.pairedSuccess.collectAsStateWithLifecycle()

    LaunchedEffect(paired) {
        if (paired) onPaired()
    }

    PairContent(
        status = status,
        isError = isError,
        busy = busy,
        paired = paired,
        onBack = onBack,
        onPairQrJson = vm::pairWithQrJson,
        onPairManualCode = vm::pairWithManualCode,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairContent(
    status: String?,
    isError: Boolean,
    busy: Boolean,
    paired: Boolean,
    onBack: () -> Unit,
    onPairQrJson: (String) -> Unit,
    onPairManualCode: (String, String) -> Unit,
    isDemo: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun checkCamPerm(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var hasCameraPerm by remember { mutableStateOf(checkCamPerm()) }
    var manualMode by rememberSaveable { mutableStateOf(false) }
    var userCode by rememberSaveable { mutableStateOf("") }
    var pskText by rememberSaveable { mutableStateOf("") }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPerm = granted
        if (!granted) {
            manualMode = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = checkCamPerm()
                if (granted && !hasCameraPerm) {
                    hasCameraPerm = true
                    manualMode = false
                } else {
                    hasCameraPerm = granted
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPerm) {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("添加电脑")
                        if (isDemo) DemoTag()
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (hasCameraPerm) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !manualMode,
                        onClick = { manualMode = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) },
                    ) {
                        Text("扫码配对")
                    }
                    SegmentedButton(
                        selected = manualMode,
                        onClick = { manualMode = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Rounded.Keyboard, contentDescription = null) },
                    ) {
                        Text("手动输入")
                    }
                }
            }

            if (!manualMode && hasCameraPerm) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(24.dp)),
                ) {
                    QrScannerView(
                        canScan = !busy && !paired,
                        onDetected = { raw ->
                            if (!busy) onPairQrJson(raw)
                        },
                        onCameraError = { err -> cameraError = err },
                    )
                    ScannerOverlay(
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp),
                    ) {
                        Text(
                            text = "对准终端中的配对二维码（支持深色/亮色终端）",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                if (!cameraError.isNullOrBlank()) {
                    ErrorBanner(cameraError!!)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("如何在电脑生成二维码？", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "在工作站终端执行 sidekick-bridge enroll，扫描输出的二维码并核对公钥指纹（Noise XXpsk3）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("手动配对（6 位配对码 + 一次性 PSK）", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = userCode,
                        onValueChange = { userCode = it },
                        label = { Text("6 位配对码 (user_code)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = pskText,
                        onValueChange = { pskText = it },
                        label = { Text("一次性 PSK (base64)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { onPairManualCode(userCode, pskText) },
                        enabled = !busy && userCode.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("确认配对")
                        }
                    }
                    if (hasCameraPerm) {
                        OutlinedButton(
                            onClick = { manualMode = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text("切回相机扫码")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text("授予相机权限扫码")
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                )
                                runCatching { context.startActivity(intent) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("去系统设置开启相机权限")
                        }
                    }
                }
            }

            if (!status.isNullOrBlank()) {
                val msg = status
                val extras = SidekickThemeExtras.colors
                if (isError) {
                    ErrorBanner(msg)
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = extras.successContainer,
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = extras.onSuccessContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerOverlay(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        drawRect(color = Color.Black.copy(alpha = 0.45f))

        val boxSize = size.minDimension * 0.64f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f
        val radius = 20.dp.toPx()

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(radius, radius),
            blendMode = BlendMode.Clear,
        )

        val cornerLen = 26.dp.toPx()
        val stroke = 3.5.dp.toPx()
        val right = left + boxSize
        val bottom = top + boxSize

        drawLine(accentColor, Offset(left, top + radius), Offset(left, top + radius + cornerLen), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(left + radius, top), Offset(left + radius + cornerLen, top), stroke, StrokeCap.Round)

        drawLine(accentColor, Offset(right, top + radius), Offset(right, top + radius + cornerLen), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(right - radius, top), Offset(right - radius - cornerLen, top), stroke, StrokeCap.Round)

        drawLine(accentColor, Offset(left, bottom - radius), Offset(left, bottom - radius - cornerLen), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(left + radius, bottom), Offset(left + radius + cornerLen, bottom), stroke, StrokeCap.Round)

        drawLine(accentColor, Offset(right, bottom - radius), Offset(right, bottom - radius - cornerLen), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(right - radius, bottom), Offset(right - radius - cornerLen, bottom), stroke, StrokeCap.Round)
    }
}

@Composable
private fun QrScannerView(
    canScan: Boolean,
    onDetected: (String) -> Unit,
    onCameraError: (String?) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val zxingReader = remember {
        MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER to true,
                ),
            )
        }
    }
    val currentCanScan by rememberUpdatedState(canScan)
    val currentOnDetected by rememberUpdatedState(onDetected)
    val currentOnCameraError by rememberUpdatedState(onCameraError)
    var lastScanMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                // COMPATIBLE (TextureView) avoids blank/cutout SurfaceView compositing under rounded clips & overlays.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                runCatching {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { imageProxy ->
                        try {
                            val now = System.currentTimeMillis()
                            if (currentCanScan && now - lastScanMs > 2000L) {
                                val decoded = decodeQrFromYuvPlane(imageProxy, zxingReader)
                                val ts = System.currentTimeMillis()
                                if (!decoded.isNullOrBlank() && currentCanScan && ts - lastScanMs > 2000L) {
                                    lastScanMs = ts
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        currentOnDetected(decoded)
                                    }
                                }
                            }
                        } finally {
                            imageProxy.close()
                        }
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    currentOnCameraError(null)
                }.onFailure { e ->
                    currentOnCameraError("相机初始化失败: ${e.message ?: "请检查相机权限或设备支持"}")
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

private fun decodeQrFromYuvPlane(imageProxy: ImageProxy, reader: MultiFormatReader): String? {
    val plane = imageProxy.planes.firstOrNull() ?: return null
    val w = imageProxy.width
    val h = imageProxy.height
    if (w <= 0 || h <= 0) return null

    val buf = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val yData = ByteArray(w * h)
    if (pixelStride == 1 && rowStride == w) {
        buf.rewind()
        buf.get(yData, 0, yData.size)
    } else {
        val rowBuf = ByteArray(rowStride)
        for (row in 0 until h) {
            buf.position(row * rowStride)
            val toRead = minOf(rowStride, buf.remaining())
            buf.get(rowBuf, 0, toRead)
            var outIdx = row * w
            var inIdx = 0
            for (col in 0 until w) {
                yData[outIdx++] = rowBuf[inIdx]
                inIdx += pixelStride
            }
        }
    }

    val source = PlanarYUVLuminanceSource(yData, w, h, 0, 0, w, h, false)
    val candidates = arrayOf(source, source.invert())
    for (src in candidates) {
        val bitmap = BinaryBitmap(HybridBinarizer(src))
        val text = runCatching { reader.decodeWithState(bitmap)?.text }.getOrNull()
        if (!text.isNullOrBlank()) return text
        reader.reset()
    }
    return null
}
