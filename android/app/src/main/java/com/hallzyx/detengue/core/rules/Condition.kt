package com.hallzyx.detengue.core.rules

import com.hallzyx.detengue.core.model.Detection

/**
 * Prioridad de revisión de un objeto (PRD §16). Separa "qué objeto es" de "cuánto conviene revisarlo".
 * Son condiciones VISUALES: nunca se presentan como diagnóstico ni como criadero confirmado (PRD §17).
 */
enum class Condition {
    /** Sin riesgo visible en esta imagen. */
    SAFE,

    /** Podría acumular agua; la imagen no permite confirmarlo. */
    REVIEW,

    /** Posible acumulación de agua con evidencia visual clara. */
    RISK,

    /** Confianza insuficiente: pedir revisión manual en vez de forzar una conclusión. */
    UNCERTAIN,
    ;

    /** Cuenta como "elemento pendiente" en la comparación antes/después (PRD §53). */
    val needsAttention: Boolean get() = this == REVIEW || this == RISK
}

/** Acción recomendada, determinística y local (PRD §20). El texto vive en los recursos de la UI. */
enum class ActionCode {
    CHECK_STANDING_WATER,
    EMPTY_AND_INVERT,
    REMOVE_POT_WATER,
    REFRESH_PET_WATER,
    KEEP_TANK_COVERED,
    PROTECT_TIRE,
    MANUAL_REVIEW,
}

/** Respuesta de la persona a "¿Tiene agua acumulada?" (PRD §19). */
enum class UserAnswer { HAS_WATER, NO_WATER, UNSURE }

data class Assessment(val condition: Condition, val actionCode: ActionCode?)

/** Un objeto detectado junto con su evaluación. [id] es el índice dentro de su escaneo. */
data class AssessedDetection(
    val id: Int,
    val detection: Detection,
    val condition: Condition,
    val actionCode: ActionCode?,
    val userAnswer: UserAnswer?,
)
