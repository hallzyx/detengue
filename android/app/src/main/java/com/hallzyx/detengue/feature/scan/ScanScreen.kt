package com.hallzyx.detengue.feature.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hallzyx.detengue.DetengueApplication
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.camera.CameraPreview
import com.hallzyx.detengue.core.camera.FrameGrabber
import com.hallzyx.detengue.core.inference.DetectorState
import com.hallzyx.detengue.ui.DetectionOverlay
import com.hallzyx.detengue.ui.OverlayBox
import com.hallzyx.detengue.ui.labelRes

private fun Context.hasCameraPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Pantalla de escaneo: preview + botón "Analizar" (PRD §14).
 * Fase 1: captura un frame real y lo descarta. La inferencia llega en la Fase 2.
 */
@Composable
fun ScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Si la persona vuelve desde Ajustes con el permiso concedido, lo detectamos al reanudar.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasPermission = context.hasCameraPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            ScanTopBar(onBack = onBack)

            if (hasPermission) {
                ScanContent()
            } else {
                PermissionNeeded(
                    onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ScanTopBar(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Text(
                text = stringResource(R.string.scan_back),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ScanContent() {
    val context = LocalContext.current
    val app = context.applicationContext as DetengueApplication
    val viewModel: ScanViewModel = viewModel(
        factory = viewModelFactory { initializer { ScanViewModel(app.detectorHolder) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detectorState by app.detectorHolder.state.collectAsStateWithLifecycle()
    val grabber = remember { FrameGrabber() }

    val ready = detectorState is DetectorState.Ready
    val analyzing = uiState is ScanUiState.Analyzing

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Contenedor 3:4 (vertical) = misma geometría que el frame analizado.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black),
        ) {
            // El preview sigue enlazado debajo para que "Escanear de nuevo" sea instantáneo.
            CameraPreview(frameGrabber = grabber, modifier = Modifier.fillMaxSize())

            (uiState as? ScanUiState.Result)?.let { result ->
                val boxes = remember(result) {
                    result.result.detections.map {
                        OverlayBox(
                            box = it.box,
                            label = context.getString(it.objectClass.labelRes()),
                            color = DetectionNeutral,
                        )
                    }
                }
                // Imagen analizada (solo en memoria) + cajas sobre exactamente la misma geometría.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(result.frame.width / result.frame.height.toFloat())
                        .align(Alignment.Center),
                ) {
                    Image(
                        bitmap = result.frame.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize(),
                    )
                    DetectionOverlay(boxes = boxes, modifier = Modifier.fillMaxSize())
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = statusMessage(detectorState, uiState),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = ready && !analyzing,
                onClick = {
                    if (uiState is ScanUiState.Result) viewModel.reset() else viewModel.analyze(grabber)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                if (analyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Text(
                        text = stringResource(
                            if (uiState is ScanUiState.Result) R.string.scan_again else R.string.scan_analyze,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

/** Color neutro de la Fase 2: aún no hay estados SAFE/REVIEW/RISK (llegan con el motor de reglas). */
private val DetectionNeutral = Color(0xFF0F766E)

@Composable
private fun statusMessage(detector: DetectorState, ui: ScanUiState): String {
    val context = LocalContext.current
    return when {
        detector is DetectorState.Unavailable -> stringResource(R.string.scan_model_unavailable)
        detector is DetectorState.Loading -> stringResource(R.string.scan_model_loading)
        else -> when (ui) {
            ScanUiState.Idle -> stringResource(R.string.scan_hint)
            ScanUiState.Analyzing -> stringResource(R.string.scan_analyzing)
            is ScanUiState.Result -> {
                val count = ui.result.detections.size
                if (count == 0) {
                    stringResource(R.string.scan_no_detections)
                } else {
                    context.resources.getQuantityString(R.plurals.scan_detected, count, count)
                }
            }
            is ScanUiState.Failed -> when (ui.error) {
                ScanError.CAPTURE_FAILED -> stringResource(R.string.scan_capture_error)
                ScanError.MODEL_UNAVAILABLE -> stringResource(R.string.scan_model_unavailable)
                ScanError.INFERENCE_FAILED -> stringResource(R.string.scan_inference_error)
            }
        }
    }
}

@Composable
private fun PermissionNeeded(onGrant: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.camera_permission_needed),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGrant,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            Text(
                text = stringResource(R.string.camera_permission_grant),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = stringResource(R.string.camera_permission_settings),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
