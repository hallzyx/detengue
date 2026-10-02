package com.hallzyx.detengue.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.rules.ActionCode
import com.hallzyx.detengue.core.rules.Condition
import com.hallzyx.detengue.ui.theme.ConditionColors

/** Verde / ámbar / rojo / gris por semántica de estado (PRD §32). */
fun Condition.color(): Color = when (this) {
    Condition.SAFE -> ConditionColors.Safe
    Condition.REVIEW -> ConditionColors.Review
    Condition.RISK -> ConditionColors.Risk
    Condition.UNCERTAIN -> ConditionColors.Uncertain
}

/** Color de texto con contraste suficiente sobre [color]. El ámbar necesita texto oscuro. */
fun Condition.onColor(): Color = when (this) {
    Condition.REVIEW -> Color(0xFF1F1300)
    else -> Color.White
}

/** Segunda línea de la etiqueta de la caja ("Balde / Revisar"). */
@StringRes
fun Condition.shortLabelRes(): Int = when (this) {
    Condition.SAFE -> R.string.condition_short_safe
    Condition.REVIEW -> R.string.condition_short_review
    Condition.RISK -> R.string.condition_short_risk
    Condition.UNCERTAIN -> R.string.condition_short_uncertain
}

/** Frase completa para la lista de resultados. */
@StringRes
fun Condition.statusRes(): Int = when (this) {
    Condition.SAFE -> R.string.status_safe
    Condition.REVIEW -> R.string.status_review
    Condition.RISK -> R.string.status_risk
    Condition.UNCERTAIN -> R.string.status_uncertain
}

@StringRes
fun ActionCode.textRes(): Int = when (this) {
    ActionCode.CHECK_STANDING_WATER -> R.string.action_check_standing_water
    ActionCode.EMPTY_AND_INVERT -> R.string.action_empty_and_invert
    ActionCode.REMOVE_POT_WATER -> R.string.action_remove_pot_water
    ActionCode.REFRESH_PET_WATER -> R.string.action_refresh_pet_water
    ActionCode.KEEP_TANK_COVERED -> R.string.action_keep_tank_covered
    ActionCode.PROTECT_TIRE -> R.string.action_protect_tire
    ActionCode.MANUAL_REVIEW -> R.string.action_manual_review
}
