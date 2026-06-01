package com.nicholasbergesen.gunsout.feature.body

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.nicholasbergesen.gunsout.domain.inbody.InBodyQrParseResult
import com.nicholasbergesen.gunsout.domain.inbody.InBodyQrPayloadParser
import com.nicholasbergesen.gunsout.ui.components.MockupScreenColumn
import com.nicholasbergesen.gunsout.ui.components.ScreenTitle
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.ThemedCard
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ExecutionException

@Composable
fun InBodyScanScreen(
    onBack: () -> Unit,
    onSupportedQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val parser = remember { InBodyQrPayloadParser() }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("Point the camera at an InBody result-sheet QR code.") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                hasCameraPermission = granted
                if (granted) {
                    permissionDenied = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            InBodyCameraPreview(
                onQrCode = { rawValue ->
                    when (val parsed = parser.parse(rawValue)) {
                        is InBodyQrParseResult.Parsed -> {
                            onSupportedQrScanned(rawValue)
                            true
                        }
                        is InBodyQrParseResult.Failed -> {
                            scanMessage = parsed.failure.userMessage
                            false
                        }
                    }
                },
                onCameraError = { message -> scanMessage = message },
                modifier = Modifier.fillMaxSize()
            )
        }

        MockupScreenColumn(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScreenTitle("Scan InBody QR")
                    Text(scanMessage, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onBack) { Text("Back") }
                }
            }
        }

        if (permissionDenied) {
            PermissionDeniedCard(
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                },
                onBack = onBack,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun PermissionDeniedCard(
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ThemedCard(modifier = modifier.padding(16.dp)) {
        SectionLabel("Camera permission required")
        Text(
            "Camera permission is required to scan InBody QR codes.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Open app settings")
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun InBodyCameraPreview(
    onQrCode: (String) -> Boolean,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    DisposableEffect(context, lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        var cameraProvider: ProcessCameraProvider? = null
        var disposed = false

        cameraProviderFuture.addListener(
            listener@{
                val provider = try {
                    cameraProviderFuture.get()
                } catch (exception: ExecutionException) {
                    onCameraError("Camera could not start.")
                    return@listener
                } catch (exception: InterruptedException) {
                    Thread.currentThread().interrupt()
                    onCameraError("Camera could not start.")
                    return@listener
                }
                if (disposed) {
                    provider.unbindAll()
                    return@listener
                }
                cameraProvider = provider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            analysisExecutor,
                            QrAnalyzer(
                                scanner = scanner,
                                gate = InBodyScanGate(),
                                mainExecutor = mainExecutor,
                                onQrCode = onQrCode
                            )
                        )
                    }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (exception: RuntimeException) {
                    onCameraError("Camera could not start.")
                }
            },
            mainExecutor
        )

        onDispose {
            disposed = true
            cameraProvider?.unbindAll()
            scanner.close()
            analysisExecutor.shutdown()
        }
    }
}

private class QrAnalyzer(
    private val scanner: BarcodeScanner,
    private val gate: InBodyScanGate,
    private val mainExecutor: Executor,
    private val onQrCode: (String) -> Boolean
) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        if (!gate.tryStart()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            gate.finish(false)
            imageProxy.close()
            return
        }

        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener(mainExecutor) { barcodes ->
                val rawValue = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
                val accepted = rawValue?.let(onQrCode) ?: false
                gate.finish(accepted)
            }
            .addOnFailureListener(mainExecutor) {
                gate.finish(false)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
