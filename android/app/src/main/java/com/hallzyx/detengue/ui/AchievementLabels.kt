package com.hallzyx.detengue.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.gamification.Achievement
import com.hallzyx.detengue.core.gamification.GoalMetric

/** Nombre mostrado de cada insignia. */
@StringRes
fun Achievement.labelRes(): Int = when (this) {
    Achievement.FIRST_INSPECTION -> R.string.ach_first_inspection
    Achievement.FIRST_RESOLVED -> R.string.ach_first_resolved
    Achievement.FIVE_RESOLVED -> R.string.ach_five_resolved
    Achievement.STREAK_4_WEEKS -> R.string.ach_streak_4_weeks
}

/** Cómo se consigue, en una frase. */
@StringRes
fun Achievement.descriptionRes(): Int = when (this) {
    Achievement.FIRST_INSPECTION -> R.string.ach_first_inspection_desc
    Achievement.FIRST_RESOLVED -> R.string.ach_first_resolved_desc
    Achievement.FIVE_RESOLVED -> R.string.ach_five_resolved_desc
    Achievement.STREAK_4_WEEKS -> R.string.ach_streak_4_weeks_desc
}

/** Solo íconos del set base (material-icons-core): no se añade la librería extendida. */
fun Achievement.icon(): ImageVector = when (this) {
    Achievement.FIRST_INSPECTION -> Icons.Filled.Search
    Achievement.FIRST_RESOLVED -> Icons.Filled.CheckCircle
    Achievement.FIVE_RESOLVED -> Icons.Filled.Star
    Achievement.STREAK_4_WEEKS -> Icons.Filled.DateRange
}

/** "Te faltan N …" según lo que mide la insignia. */
@PluralsRes
fun GoalMetric.remainingRes(): Int = when (this) {
    GoalMetric.INSPECTIONS -> R.plurals.gami_remaining_inspections
    GoalMetric.RESOLVED_ALERTS -> R.plurals.gami_remaining_alerts
    GoalMetric.STREAK_WEEKS -> R.plurals.gami_remaining_weeks
}
