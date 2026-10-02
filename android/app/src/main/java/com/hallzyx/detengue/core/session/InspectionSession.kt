package com.hallzyx.detengue.core.session

import com.hallzyx.detengue.core.model.Detection
import com.hallzyx.detengue.core.rules.AssessedDetection
import com.hallzyx.detengue.core.rules.RiskRuleEngine
import com.hallzyx.detengue.core.rules.UserAnswer
import kotlin.math.max

/**
 * Resultado de UN escaneo: lo que detectó el modelo y lo que la persona respondió.
 * No contiene la imagen (PRD §67): solo metadatos.
 */
data class ScanSnapshot(
    val detections: List<Detection>,
    val answers: Map<Int, UserAnswer> = emptyMap(),
) {
    /** Evaluación con las respuestas de la persona incluidas. */
    val assessed: List<AssessedDetection> get() = ENGINE.assessAll(detections, answers)

    /** Evaluación solo con lo que vio la IA, sin respuestas manuales. */
    val automaticAssessed: List<AssessedDetection> get() = ENGINE.assessAll(detections)

    val pendingCount: Int get() = assessed.count { it.condition.needsAttention }
    val automaticPendingCount: Int get() = automaticAssessed.count { it.condition.needsAttention }

    private companion object {
        val ENGINE = RiskRuleEngine()
    }
}

/** Comparación antes/después (PRD §34). */
data class ReviewComparison(val before: Int, val after: Int) {
    val fixed: Int get() = max(0, before - after)
    val completed: Boolean get() = after == 0
}

/**
 * Una revisión: uno o más escaneos del mismo entorno. Inmutable; cada cambio devuelve una copia.
 */
data class InspectionSession(
    val startedAtMillis: Long,
    val scans: List<ScanSnapshot> = emptyList(),
) {
    val latest: ScanSnapshot? get() = scans.lastOrNull()

    /**
     * Escaneo de referencia para "antes": el primero que tuvo elementos pendientes según la IA.
     * Si el primer escaneo salió vacío (mal encuadre), no sirve como base de comparación.
     */
    private val baselineIndex: Int?
        get() = scans.indexOfFirst { it.automaticPendingCount > 0 }.takeIf { it >= 0 }

    val initialPendingCount: Int? get() = baselineIndex?.let { scans[it].automaticPendingCount }

    /** Solo existe cuando hay un reescaneo posterior a la base. */
    val comparison: ReviewComparison?
        get() {
            val base = baselineIndex ?: return null
            if (scans.lastIndex <= base) return null
            return ReviewComparison(before = scans[base].automaticPendingCount, after = scans.last().pendingCount)
        }

    fun withScan(snapshot: ScanSnapshot): InspectionSession = copy(scans = scans + snapshot)

    /** Registra la respuesta de la persona sobre un objeto del ÚLTIMO escaneo. */
    fun withAnswer(detectionId: Int, answer: UserAnswer): InspectionSession {
        val current = latest ?: return this
        if (detectionId !in current.detections.indices) return this
        val updated = current.copy(answers = current.answers + (detectionId to answer))
        return copy(scans = scans.dropLast(1) + updated)
    }
}
