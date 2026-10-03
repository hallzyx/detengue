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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.hallzyx.detengue.core.rules.AssessedDetection
import com.hallzyx.detengue.ui.DetectionOverlay
import com.hallzyx.detengue.ui.OverlayBox
import com.hallzyx.detengue.ui.color
import com.hallzyx.detengue.ui.labelRes
import com.hallzyx.detengue.ui.onColor
import com.hallzyx.detengue.ui.shortLabelRes

private fun Context.hasCameraPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Pantalla de escaneo: preview + botón "Analizar" (PRD §14).
 * Fase 1: captura un frame real y lo descarta. La inferencia llega en la Fase 2.
 */
@Composable
fun ScanScreen(onBack: () -> Unit, onViewResults: () -> Unit) {
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
                ScanContent(onViewResults = onViewResults)
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
private fun ScanContent(onViewResults: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as DetengueApplication
    val viewModel: ScanViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ScanViewModel(app.detectorHolder, app.inspectionStore) }
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detectorState by app.detectorHolder.state.collectAsStateWithLifecycle()
    val grabber = remember { FrameGrabber() }

    val ready = detectorState is DetectorState.Ready
    val analyzing = uiState is ScanUiState.Analyzing
    val sweeping = uiState is ScanUiState.Sweeping

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

            (uiState as? ScanUiState.Sweeping)?.let { sweep ->
                // Cuenta atrás sobre el preview en vivo: la persona mueve el teléfono mientras tanto.
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = sweep.secondsLeft.toString(), style = MaterialTheme.typography.displayLarge, color = Color.White)
                        Text(text = stringResource(R.string.scan_sweeping_hint), style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        Text(text = stringResource(R.string.scan_sweeping_found, sweep.found), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
            }

            (uiState as? ScanUiState.SweepResult)?.let { sweep ->
                if (sweep.shots.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.scan_sweep_none),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                } else {
                    // Galería de hallazgos: el mejor fotograma de cada objeto, solo en memoria, con su caja y veredicto.
                    LazyRow(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(sweep.shots) { shot ->
                            val box = remember(shot) { overlayBoxOf(context, shot.assessed) }
                            Box(
                                modifier = Modifier
                                    .fillParentMaxHeight(0.92f)
                                    .aspectRatio(shot.frame.width / shot.frame.height.toFloat())
                                    .clip(RoundedCornerShape(12.dp)),
                            ) {
                                Image(
                                    bitmap = shot.frame.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                DetectionOverlay(boxes = listOf(box), modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }

            (uiState as? ScanUiState.Result)?.let { result ->
                val boxes = remember(result) {
                    result.assessed.map {
                        OverlayBox(
                            box = it.detection.box,
                            label = context.getString(it.detection.objectClass.labelRes()) + "\n" +
                                context.getString(it.condition.shortLabelRes()),
                            color = it.condition.color(),
                            labelColor = it.condition.onColor(),
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
            val result = uiState as? ScanUiState.Result
            val sweepResult = uiState as? ScanUiState.SweepResult
            Button(
                enabled = ready && !analyzing && !sweeping,
                onClick = {
                    if (result != null || sweepResult != null) {
                        // Se suelta la imagen analizada antes de salir: el preview vuelve a estar en vivo.
                        viewModel.reset()
                        onViewResults()
                    } else {
                        viewModel.analyze(grabber)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                if (analyzing || sweeping) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Text(
                        text = if (sweepResult != null) {
                            stringResource(R.string.scan_sweep_view_results, sweepResult.shots.size)
                        } else {
                            stringResource(if (result != null) R.string.scan_view_results else R.string.scan_analyze)
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            if (result != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.reset() }) {
                    Text(
                        text = stringResource(R.string.scan_again),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else if (sweepResult != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.reset() }) {
                    Text(
                        text = stringResource(R.string.scan_sweep_another),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    enabled = ready && !analyzing && !sweeping,
                    onClick = { viewModel.sweep(grabber) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(text = stringResource(R.string.scan_sweep), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** Caja con etiqueta (objeto + veredicto) y color de una detección ya evaluada. */
private fun overlayBoxOf(context: Context, assessed: AssessedDetection) = OverlayBox(
    box = assessed.detection.box,
    label = context.getString(assessed.detection.objectClass.labelRes()) + "\n" +
        context.getString(assessed.condition.shortLabelRes()),
    color = assessed.condition.color(),
    labelColor = assessed.condition.onColor(),
)

@Composable
private fun statusMessage(detector: DetectorState, ui: ScanUiState): String {
    val context = LocalContext.current
    return when {
        detector is DetectorState.Unavailable -> stringResource(R.string.scan_model_unavailable)
        detector is DetectorState.Loading -> stringResource(R.string.scan_model_loading)
        else -> when (ui) {
            ScanUiState.Idle -> stringResource(R.string.scan_hint)
            ScanUiState.Analyzing -> stringResource(R.string.scan_analyzing)
            is ScanUiState.Sweeping -> stringResource(R.string.scan_sweeping, ui.secondsLeft)
            is ScanUiState.SweepResult -> when {
                ui.shots.isEmpty() -> stringResource(R.string.scan_sweep_none)
                else -> context.resources.getQuantityString(R.plurals.scan_found_pending, ui.shots.size, ui.shots.size)
            }
            is ScanUiState.Result -> {
                val pending = ui.assessed.count { it.condition.needsAttention }
                when {
                    ui.assessed.isEmpty() -> stringResource(R.string.scan_no_detections)
                    pending == 0 -> stringResource(R.string.scan_nothing_pending)
                    else -> context.resources.getQuantityString(R.plurals.scan_found_pending, pending, pending)
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
