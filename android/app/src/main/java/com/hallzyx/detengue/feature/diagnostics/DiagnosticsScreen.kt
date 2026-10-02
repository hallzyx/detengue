package com.hallzyx.detengue.feature.diagnostics

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hallzyx.detengue.BuildConfig
import com.hallzyx.detengue.DetengueApplication
import com.hallzyx.detengue.core.config.DetectionConfig
import com.hallzyx.detengue.core.inference.DetectorState
import com.hallzyx.detengue.core.network.ConnectivityObserver
import com.hallzyx.detengue.ui.BackBar

private fun readPssMb(): Int {
    val info = Debug.MemoryInfo()
    Debug.getMemoryInfo(info)
    return info.totalPss / 1024
}

/**
 * "Detengue Diagnostics" (PRD §28): evidencia de viabilidad para la demo técnica.
 * NO es para el usuario final; solo se enlaza desde builds con DEMO_MODE.
 * Todo son mediciones reales del dispositivo, nunca valores de ejemplo.
 */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as DetengueApplication

    val detector by app.detectorHolder.state.collectAsStateWithLifecycle()
    val last by app.diagnostics.last.collectAsStateWithLifecycle()
    val online by remember { ConnectivityObserver(context) }.isOnline.collectAsStateWithLifecycle(initialValue = false)
    var ramMb by remember { mutableIntStateOf(readPssMb()) }

    val latency = app.diagnostics.latency
    val hasInternetPermission =
        ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED

    val rows = buildList {
        add("Model" to when (val d = detector) {
            is DetectorState.Ready -> d.modelName
            is DetectorState.Loading -> "cargando…"
            is DetectorState.Unavailable -> "NO DISPONIBLE (${d.reason})"
        })
        add("Version" to DetectionConfig.MODEL_VERSION)
        (detector as? DetectorState.Ready)?.let {
            add("Input" to "${it.inputSize}x${it.inputSize}")
            add("Load" to "${it.loadMs} ms")
        }
        add("Inference" to (last?.let { "pre ${it.preprocessMs} · inf ${it.inferenceMs} · post ${it.postprocessMs} = ${it.totalMs} ms" } ?: "—"))
        add("p50 / p95" to if (latency.count == 0) "—" else "${latency.p50} / ${latency.p95} ms (n=${latency.count})")
        add("RAM" to "$ramMb MB (PSS)")
        add("Network" to if (hasInternetPermission) "ON (permiso INTERNET declarado)" else "OFF (sin permiso INTERNET)")
        add("Phone link" to if (online) "con conexión" else "sin conexión")
        add("Device" to "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} · ${Runtime.getRuntime().availableProcessors()} cores")
        add("Build" to if (BuildConfig.DEMO_MODE) "demo/debug" else "release")
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 8.dp)) {
            BackBar(onBack = onBack)
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Detengue Diagnostics",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                rows.forEach { (label, value) ->
                    Text(
                        text = "${label.padEnd(11)} $value",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                OutlinedButton(onClick = { ramMb = readPssMb() }) { Text("Actualizar RAM") }
            }
        }
    }
}
