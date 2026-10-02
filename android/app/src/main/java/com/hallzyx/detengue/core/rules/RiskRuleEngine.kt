package com.hallzyx.detengue.core.rules

import com.hallzyx.detengue.core.config.DetectionConfig
import com.hallzyx.detengue.core.model.Detection
import com.hallzyx.detengue.core.model.ObjectClass
import com.hallzyx.detengue.core.model.ObservedState

/**
 * Motor de reglas determinístico y local (PRD §31). No usa ningún LLM ni red.
 *
 * Orden de decisión:
 *  1. Si la persona respondió la pregunta de confirmación, su respuesta manda (human-in-the-loop).
 *  2. Confianza baja → UNCERTAIN ("Revisa manualmente"), nunca una conclusión forzada.
 *  3. Estado observado con confianza suficiente → SAFE / RISK.
 *  4. Todo lo demás → REVIEW ("Revisa si contiene agua").
 */
class RiskRuleEngine(
    private val riskThreshold: Float = DetectionConfig.RISK_THRESHOLD,
    private val uncertainThreshold: Float = DetectionConfig.UNCERTAIN_THRESHOLD,
) {
    fun assess(detection: Detection, answer: UserAnswer? = null): Assessment {
        when (answer) {
            UserAnswer.HAS_WATER -> return Assessment(Condition.RISK, riskAction(detection.objectClass))
            UserAnswer.NO_WATER -> return Assessment(Condition.SAFE, null)
            UserAnswer.UNSURE -> return Assessment(Condition.REVIEW, ActionCode.CHECK_STANDING_WATER)
            null -> Unit
        }

        if (detection.confidence < uncertainThreshold) {
            return Assessment(Condition.UNCERTAIN, ActionCode.MANUAL_REVIEW)
        }

        val confident = detection.confidence >= riskThreshold
        val review = Assessment(Condition.REVIEW, ActionCode.CHECK_STANDING_WATER)

        return when (detection.state) {
            ObservedState.WATER ->
                if (confident) Assessment(Condition.RISK, riskAction(detection.objectClass)) else review

            ObservedState.CLOSED, ObservedState.INVERTED ->
                if (confident) Assessment(Condition.SAFE, null) else review

            // Tanques y cilindros sin tapar son riesgo por sí mismos (PRD §31: water_tank + uncovered).
            ObservedState.OPEN ->
                if (confident && detection.objectClass in COVERABLE) {
                    Assessment(Condition.RISK, ActionCode.KEEP_TANK_COVERED)
                } else {
                    review
                }

            ObservedState.UNKNOWN -> review
        }
    }

    fun assessAll(detections: List<Detection>, answers: Map<Int, UserAnswer> = emptyMap()): List<AssessedDetection> =
        detections.mapIndexed { index, detection ->
            val answer = answers[index]
            val assessment = assess(detection, answer)
            AssessedDetection(index, detection, assessment.condition, assessment.actionCode, answer)
        }

    private fun riskAction(objectClass: ObjectClass): ActionCode = when (objectClass) {
        ObjectClass.BUCKET, ObjectClass.CONTAINER, ObjectClass.DRUM, ObjectClass.BOTTLE -> ActionCode.EMPTY_AND_INVERT
        ObjectClass.PLANT_POT -> ActionCode.REMOVE_POT_WATER
        ObjectClass.PET_BOWL -> ActionCode.REFRESH_PET_WATER
        ObjectClass.WATER_TANK -> ActionCode.KEEP_TANK_COVERED
        ObjectClass.TIRE -> ActionCode.PROTECT_TIRE
    }

    private companion object {
        val COVERABLE = setOf(ObjectClass.WATER_TANK, ObjectClass.DRUM)
    }
}
