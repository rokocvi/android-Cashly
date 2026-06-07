package com.example.projektmobpravi.ui.scan

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.projektmobpravi.ui.navigation.Screen
import com.example.projektmobpravi.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(navController: NavHostController) {
    val viewModel: ScanViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    ) { granted -> viewModel.onPermissionResult(granted) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) cameraPermissionState.launchPermissionRequest()
        else viewModel.onPermissionResult(true)
    }

    LaunchedEffect(uiState.ocrResult) {
        uiState.ocrResult?.let { result ->
            val amount   = result.amount    ?: ""
            val note     = result.storeName ?: ""
            val category = result.category  ?: ""
            val prevRoute = navController.previousBackStackEntry?.destination?.route

            if (prevRoute == Screen.Home.route) {
                navController.navigate(Screen.AddTransaction.fromScanRoute(amount, note, category)) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            } else {
                navController.previousBackStackEntry?.savedStateHandle?.set("scannedAmount",   amount)
                navController.previousBackStackEntry?.savedStateHandle?.set("scannedNote",     note)
                navController.previousBackStackEntry?.savedStateHandle?.set("scannedCategory", category)
                navController.popBackStack()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!cameraPermissionState.status.isGranted) {
            PermissionDeniedContent(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onBack              = { navController.popBackStack() }
            )
        } else {
            CameraContent(
                onTextRecognized = { text -> viewModel.onImageCaptured(text) },
                onError          = { error -> viewModel.onScanError(error) },
                onBack           = { navController.popBackStack() }
            )

            if (uiState.isScanning) {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape  = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Column(
                            modifier            = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                            Text(
                                text  = "Analiziram račun...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextDark
                            )
                        }
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        shape  = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.92f))
                    ) {
                        Text(
                            text     = error,
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Camera Content ────────────────────────────────────

@Composable
fun CameraContent(
    onTextRecognized: (String) -> Unit,
    onError: (String) -> Unit,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                val previewView          = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider    = cameraProviderFuture.get()
                    val preview           = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageCaptureUseCase = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = imageCaptureUseCase

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCaptureUseCase
                        )
                    } catch (e: Exception) {
                        onError("Greška pri pokretanju kamere")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            // Gornji gradijent + natrag gumb
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier         = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Nazad",
                            tint               = Color.White,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text  = "Skeniraj račun",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }

            // Scan okvir
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ScanFrame()
            }

            // Donji gradijent + gumb za slikanje
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text      = "Postavi račun unutar okvira",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier         = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                imageCapture?.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            processImage(image, onTextRecognized, onError)
                                        }
                                        override fun onError(exception: ImageCaptureException) {
                                            onError("Greška pri slikanju: ${exception.message}")
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Camera,
                            contentDescription = "Slikaj",
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Scan Frame ────────────────────────────────────────

@Composable
fun ScanFrame() {
    val cornerSize  = 22.dp
    val cornerWidth = 3.dp
    val frameColor  = AccentGold

    Box(
        modifier = Modifier
            .width(290.dp)
            .height(190.dp)
    ) {
        Box(modifier = Modifier.size(cornerSize).align(Alignment.TopStart)) {
            Box(Modifier.fillMaxWidth().height(cornerWidth).background(frameColor))
            Box(Modifier.width(cornerWidth).fillMaxHeight().background(frameColor))
        }
        Box(modifier = Modifier.size(cornerSize).align(Alignment.TopEnd)) {
            Box(Modifier.fillMaxWidth().height(cornerWidth).background(frameColor))
            Box(Modifier.width(cornerWidth).fillMaxHeight().align(Alignment.TopEnd).background(frameColor))
        }
        Box(modifier = Modifier.size(cornerSize).align(Alignment.BottomStart)) {
            Box(Modifier.fillMaxWidth().height(cornerWidth).align(Alignment.BottomStart).background(frameColor))
            Box(Modifier.width(cornerWidth).fillMaxHeight().background(frameColor))
        }
        Box(modifier = Modifier.size(cornerSize).align(Alignment.BottomEnd)) {
            Box(Modifier.fillMaxWidth().height(cornerWidth).align(Alignment.BottomEnd).background(frameColor))
            Box(Modifier.width(cornerWidth).fillMaxHeight().align(Alignment.BottomEnd).background(frameColor))
        }
    }
}

// ── ML Kit obrada slike ───────────────────────────────

private fun processImage(
    image: ImageProxy,
    onTextRecognized: (String) -> Unit,
    onError: (String) -> Unit
) {
    val mediaImage = image.image ?: run {
        image.close()
        onError("Greška pri obradi slike")
        return
    }
    val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            image.close()
            android.util.Log.d("OCR_RAW", "=====START=====\n${visionText.text}\n=====END=====")
            onTextRecognized(visionText.text)
        }
        .addOnFailureListener { e ->
            image.close()
            onError("OCR greška: ${e.message}")
        }
}

// ── Permission Denied ─────────────────────────────────

@Composable
fun PermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier         = Modifier.fillMaxSize().background(SurfaceLight),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📷", style = MaterialTheme.typography.displayLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = "Potrebna dozvola za kameru",
                style     = MaterialTheme.typography.titleLarge,
                color     = TextDark,
                textAlign = TextAlign.Center
            )
            Text(
                text      = "Za skeniranje računa potreban je pristup kameri",
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextMuted,
                textAlign = TextAlign.Center
            )
            Button(
                onClick  = onRequestPermission,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text  = "Dozvoli pristup kameri",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextOnDark
                )
            }
            TextButton(onClick = onBack) {
                Text(
                    text  = "Nazad",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
            }
        }
    }
}
