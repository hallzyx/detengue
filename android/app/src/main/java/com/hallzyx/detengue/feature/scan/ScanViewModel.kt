package com.hallzyx.detengue.feature.scan

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hallzyx.detengue.core.camera.FrameGrabber
import com.hallzyx.detengue.core.config.DetectionConfig
import com.hallzyx.detengue.core.inference.DetectorHolder
import com.hallzyx.detengue.core.inference.ModelUnavailableException
import com.hallzyx.detengue.core.model.DetectionResult
import com.hallzyx.detengue.core.rules.AssessedDetection
import com.hallzyx.detengue.core.session.InspectionStore
import com.hallzyx.detengue.core.sweep.SweepTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ScanError { CAPTURE_FAILED, MODEL_UNAVAILABLE, INFERENCE_FAILED }

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Analyzing : ScanUiState

    /**
     * Resultado de un análisis. [frame] es la imagen analizada, que se mantiene SOLO en memoria
     * mientras se muestran los resultados (para que las cajas queden alineadas) y nunca se guarda.
     */
    data class Result(
        val frame: Bitmap,
        val result: DetectionResult,
        val assessed: List<AssessedDetection>,
    ) : ScanUiState

    /** Barrido de zona en curso: segundos que faltan y cuántos hallazgos van. */
    data class Sweeping(val secondsLeft: Int, val found: Int) : ScanUiState

    /** Fin del barrido: un [SweepShot] por hallazgo. Los fotogramas viven solo en memoria y nunca se guardan. */
    data class SweepResult(val shots: List<SweepShot>) : ScanUiState

    data class Failed(val error: ScanError) : ScanUiState
}

/** Un hallazgo del barrido: el mejor fotograma en que se vio y su evaluación. */
data class SweepShot(val frame: Bitmap, val assessed: AssessedDetection)

class ScanViewModel(
    private val detector: DetectorHolder,
    private val store: InspectionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState

    fun analyze(grabber: FrameGrabber) {
        if (_uiState.value is ScanUiState.Analyzing) return
        _uiState.value = ScanUiState.Analyzing

        viewModelScope.launch {
            _uiState.value = try {
                val frame = grabber.grab()
                if (frame == null) {
                    ScanUiState.Failed(ScanError.CAPTURE_FAILED)
                } else {
                    try {
                        val result = detector.detect(frame)
                        // Se guardan solo metadatos (clase, confianza, estado): nunca la imagen.
                        val snapshot = store.recordScan(result.detections)
                        ScanUiState.Result(frame, result, snapshot.assessed)
                    } catch (e: Throwable) {
                        // El frame no llegó a mostrarse: se descarta ya.
                        frame.recycle()
                        throw e
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: ModelUnavailableException) {
                ScanUiState.Failed(ScanError.MODEL_UNAVAILABLE)
            } catch (_: Exception) {
                ScanUiState.Failed(ScanError.INFERENCE_FAILED)
            }
        }
    }

    /**
     * Barrido de zona: durante [DetectionConfig.SWEEP_DURATION_MS] analiza fotogramas seguidos (tantos como dé el teléfono)
     * y junta los objetos que piden revisión. Los hallazgos se SUMAN al escaneo actual de la revisión, así que se pueden
     * barrer varias zonas y revisarlas juntas. Solo se guardan metadatos; los fotogramas quedan en memoria.
     */
    fun sweep(grabber: FrameGrabber) {
        val current = _uiState.value
        if (current is ScanUiState.Analyzing || current is ScanUiState.Sweeping) return
        val totalMs = DetectionConfig.SWEEP_DURATION_MS
        _uiState.value = ScanUiState.Sweeping(secondsLeft = secondsLeft(totalMs, 0L), found = 0)

        viewModelScope.launch {
            _uiState.value = try {
                val tracker = SweepTracker<Bitmap>()
                val start = SystemClock.elapsedRealtime()
                var frames = 0
                while (true) {
                    val elapsed = SystemClock.elapsedRealtime() - start
                    if (elapsed >= totalMs) break
                    val frame = grabber.grab() ?: break
                    tracker.update(detector.detect(frame).detections, frame)
                    frames++
                    val now = SystemClock.elapsedRealtime() - start
                    _uiState.value = ScanUiState.Sweeping(secondsLeft(totalMs, now), tracker.findings().size)
                }
                if (frames == 0) {
                    ScanUiState.Failed(ScanError.CAPTURE_FAILED)
                } else {
                    val findings = tracker.findings()
                    val snapshot = store.extendLatestScan(findings.map { it.detection })
                    // Lo recién agregado va al final de la lista evaluada, en el mismo orden que los hallazgos.
                    val offset = snapshot.assessed.size - findings.size
                    ScanUiState.SweepResult(findings.mapIndexed { i, f -> SweepShot(f.payload, snapshot.assessed[offset + i]) })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: ModelUnavailableException) {
                ScanUiState.Failed(ScanError.MODEL_UNAVAILABLE)
            } catch (_: Exception) {
                ScanUiState.Failed(ScanError.INFERENCE_FAILED)
            }
        }
    }

    private fun secondsLeft(totalMs: Long, elapsedMs: Long): Int =
        ((totalMs - elapsedMs + 999L) / 1000L).toInt().coerceAtLeast(0)

    /**
     * Vuelve al preview en vivo. Solo se suelta la referencia al frame: no se hace `recycle()`
     * porque Compose podría estar dibujándolo todavía; el GC lo libera.
     */
    fun reset() {
        _uiState.value = ScanUiState.Idle
    }
}
