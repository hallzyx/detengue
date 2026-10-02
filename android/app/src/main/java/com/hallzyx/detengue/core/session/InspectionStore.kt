package com.hallzyx.detengue.core.session

import com.hallzyx.detengue.core.model.Detection
import com.hallzyx.detengue.core.rules.UserAnswer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Revisión en curso, compartida entre las pantallas de escaneo y resultados. Solo en memoria. */
class InspectionStore(private val clock: () -> Long = System::currentTimeMillis) {
    private val _session = MutableStateFlow<InspectionSession?>(null)
    val session: StateFlow<InspectionSession?> = _session

    /** Empieza una revisión nueva, descartando cualquier revisión sin terminar. */
    fun begin() {
        _session.value = InspectionSession(startedAtMillis = clock())
    }

    fun recordScan(detections: List<Detection>): ScanSnapshot {
        val snapshot = ScanSnapshot(detections)
        _session.update { (it ?: InspectionSession(startedAtMillis = clock())).withScan(snapshot) }
        return snapshot
    }

    fun answer(detectionId: Int, answer: UserAnswer) {
        _session.update { it?.withAnswer(detectionId, answer) }
    }

    /** Cierra la revisión y devuelve su estado final (la Fase 4 lo persistirá en Room). */
    fun finish(): InspectionSession? {
        val finished = _session.value
        _session.value = null
        return finished
    }
}
