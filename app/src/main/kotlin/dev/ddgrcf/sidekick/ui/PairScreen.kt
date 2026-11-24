package dev.ddgrcf.sidekick.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dev.ddgrcf.sidekick.ui.theme.SidekickThemeExtras
import dev.ddgrcf.sidekick.vm.PairViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(
    vm: PairViewModel,
    onBack: () -> Unit,
    onPaired: () -> Unit,
) {
    val context = LocalContext.current
    val status by vm.status.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val paired by vm.pairedSuccess.collectAsStateWithLifecycle()

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var manualMode by remember { mutableStateOf(!hasCameraPerm) }
    var userCode by remember { mutableStateOf("") }
    var pskText by remember { mutableStateOf("") }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPerm = granted
        manualMode = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPerm) {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(paired) {
        if (paired) onPaired()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = { Text("添加电脑") },
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
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
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
                        onDetected = { raw ->
                            if (!busy) vm.pairWithQrJson(raw)
                        },
                    )
                    ScannerOverlay(
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp),
                    ) {
                        Text(
                            text = "对准终端中的配对二维码",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                SidekickCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("如何在电脑生成二维码？", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "在工作站终端执行 sidekick-bridge enroll，扫描输出的二维码并核对公钥指纹（Noise XXpsk3）。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                SidekickCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("手动配对（6 位配对码 + 一次性 PSK）", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = userCode,
                            onValueChange = { userCode = it },
                            label = { Text("6 位配对码 (user_code)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = pskText,
                            onValueChange = { pskText = it },
                            label = { Text("一次性 PSK (base64)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { vm.pairWithManualCode(userCode, pskText) },
                            enabled = !busy && userCode.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        ) {
                            Text("确认配对")
                        }
                        if (hasCameraPerm) {
                            OutlinedButton(
                                onClick = { manualMode = false },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("切回相机扫码")
                            }
                        }
                    }
                }
            }

            if (!status.isNullOrBlank()) {
                val msg = status!!
                val extras = SidekickThemeExtras.colors
                val isError = msg.contains("失败") || msg.contains("无效") || msg.contains("缺少") || msg.contains("禁止")
                if (isError) {
                    ErrorBanner(msg)
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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

        // Four corner brackets
        drawLine(accentColor, Offset(left, top + radius), Offset(left, top + radius + cornerLen), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(left + radius, top), Offset(left + radius + cornerLen, top), stroke, StrokeCap.Round)

        drawLine(accentColor, Offset(right, top + radius), Offset(right, top + radius + cornerLen), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(right - radius, top), Offset(right - radius - cornerLen, top), stroke, StrokeCap.Round)

        drawLine(accentColor, Offset(left, bottom - radius), Offset(left, bottom - radius - cornerLen), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(left + radius, bottom), Offset(left + radius + cornerLen, bottom), stroke, StrokeCap.Round)

        drawLine(accentColor, Offset(right, bottom - radius), Offset(right, bottom - radius - cornerLen), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(right - radius, bottom), Offset(right - radius - cornerLen, bottom), stroke, StrokeCap.Round)

        // Subtle scan line
        drawLine(
            color = accentColor.copy(alpha = 0.7f),
            start = Offset(left + 16.dp.toPx(), top + boxSize * 0.5f),
            end = Offset(right - 16.dp.toPx(), top + boxSize * 0.5f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun QrScannerView(onDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    var handled by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !handled) {
                        val img = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(img)
                            .addOnSuccessListener { barcodes ->
                                val raw = barcodes.firstOrNull()?.rawValue
                                if (!raw.isNullOrBlank() && !handled) {
                                    handled = true
                                    onDetected(raw)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}
