package com.hallzyx.detengue.feature.gamification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.gamification.Achievement
import com.hallzyx.detengue.core.gamification.GamificationStats
import com.hallzyx.detengue.core.gamification.nextGoal

/**
 * Tarjeta compacta de progreso en Inicio: racha como tira de semanas + la meta más cercana con su barra.
 * Toda la tarjeta abre la pantalla de logros (objetivo táctil amplio). Nada sale del teléfono.
 */
@Composable
fun ProgressCard(stats: GamificationStats, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stats.inspections == 0) EmptyContent(stats) else StreakAndGoal(stats)
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                // Lo lee TalkBack junto al resto de la tarjeta: anuncia a dónde lleva el toque.
                contentDescription = stringResource(R.string.gami_open_achievements),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/** Estado vacío: la medalla de la primera insignia "esperando" y la semana actual como hueco por llenar. */
@Composable
private fun EmptyContent(stats: GamificationStats) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BadgeMedallion(Achievement.FIRST_INSPECTION, unlocked = false, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.gami_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            WeekStrip(stats, cellSize = 14.dp)
        }
    }
}

@Composable
private fun StreakAndGoal(stats: GamificationStats) {
    Text(
        text = if (stats.streakWeeks > 0) {
            pluralStringResource(R.plurals.gami_streak_title, stats.streakWeeks, stats.streakWeeks)
        } else {
            stringResource(R.string.gami_streak_start)
        },
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        WeekStrip(stats)
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(if (stats.activeThisWeek) R.string.gami_week_done else R.string.gami_week_pending),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
    val goal = nextGoal(stats)
    if (goal == null) {
        Text(
            text = stringResource(R.string.gami_all_done),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = goalLine(goal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            GoalProgressBar(goal.fraction)
        }
    }
}
