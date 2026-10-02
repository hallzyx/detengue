package com.hallzyx.detengue.feature.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hallzyx.detengue.core.camera.FrameGrabber
import com.hallzyx.detengue.core.inference.DetectorHolder
import com.hallzyx.detengue.core.inference.ModelUnavailableException
import com.hallzyx.detengue.core.model.DetectionResult
import com.hallzyx.detengue.core.rules.AssessedDetection
import com.hallzyx.detengue.core.session.InspectionStore
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

    data class Failed(val error: ScanError) : ScanUiState
}

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
     * Vuelve al preview en vivo. Solo se suelta la referencia al frame: no se hace `recycle()`
     * porque Compose podría estar dibujándolo todavía; el GC lo libera.
     */
    fun reset() {
        _uiState.value = ScanUiState.Idle
    }
}
