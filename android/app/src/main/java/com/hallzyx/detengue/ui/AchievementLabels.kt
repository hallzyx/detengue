package com.hallzyx.detengue.ui

import androidx.annotation.StringRes
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.gamification.Achievement

/** Nombre mostrado de cada insignia. */
@StringRes
fun Achievement.labelRes(): Int = when (this) {
    Achievement.FIRST_INSPECTION -> R.string.ach_first_inspection
    Achievement.FIRST_RESOLVED -> R.string.ach_first_resolved
    Achievement.FIVE_RESOLVED -> R.string.ach_five_resolved
    Achievement.STREAK_4_WEEKS -> R.string.ach_streak_4_weeks
}
