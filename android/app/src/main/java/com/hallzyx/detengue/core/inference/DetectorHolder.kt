package com.hallzyx.detengue.core.inference

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.hallzyx.detengue.core.diagnostics.Diagnostics
import com.hallzyx.detengue.core.model.DetectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import android.graphics.Bitmap

/** Estado de la IA local. La UI solo afirma "IA disponible" cuando es [Ready]. */
sealed interface DetectorState {
    data object Loading : DetectorState
    data class Ready(val modelName: String, val inputSize: Int, val loadMs: Long) : DetectorState
    data class Unavailable(val reason: String) : DetectorState
}

/** Dueño único del detector: lo carga una vez, en segundo plano, al iniciar la app. */
class DetectorHolder(private val context: Context, private val diagnostics: Diagnostics) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val started = AtomicBoolean(false)
    private val _state = MutableStateFlow<DetectorState>(DetectorState.Loading)
    val state: StateFlow<DetectorState> = _state

    @Volatile
    private var detector: TfliteObjectDetector? = null

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            val t0 = SystemClock.elapsedRealtime()
            try {
                val loaded = TfliteObjectDetector.load(context)
                loaded.warmUp()
                detector = loaded
                _state.value = DetectorState.Ready(
                    modelName = loaded.modelName,
                    inputSize = loaded.inputSize,
                    loadMs = SystemClock.elapsedRealtime() - t0,
                )
            } catch (e: ModelUnavailableException) {
                Log.w(TAG, "IA local no disponible: ${e.message}")
                _state.value = DetectorState.Unavailable(e.message ?: "desconocido")
            } catch (t: Throwable) {
                Log.e(TAG, "Fallo al iniciar la IA local", t)
                _state.value = DetectorState.Unavailable(t.javaClass.simpleName)
            }
        }
    }

    suspend fun detect(frame: Bitmap): DetectionResult {
        val d = detector ?: throw ModelUnavailableException("La IA local no está lista")
        return d.detect(frame).also { diagnostics.record(it.timings) }
    }

    private companion object {
        const val TAG = "DetectorHolder"
    }
}
