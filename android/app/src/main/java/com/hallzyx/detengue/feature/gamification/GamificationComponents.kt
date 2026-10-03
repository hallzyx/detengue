package com.hallzyx.detengue.feature.gamification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.gamification.Achievement
import com.hallzyx.detengue.core.gamification.AchievementProgress
import com.hallzyx.detengue.core.gamification.GamificationStats
import com.hallzyx.detengue.core.gamification.WeekCell
import com.hallzyx.detengue.core.gamification.weekStrip
import com.hallzyx.detengue.ui.icon
import com.hallzyx.detengue.ui.labelRes
import com.hallzyx.detengue.ui.remainingRes

/** Semanas visibles en la tira: alcanza para ver completa la meta de 4 semanas más la actual. */
const val WEEK_STRIP_SLOTS = 5

/**
 * Tira de semanas (la última es la actual). Es decorativa para lectores de pantalla: el texto vecino ya dice la racha.
 * No depende solo del color: hecha = círculo lleno con ✓, pendiente = anillo grueso con punto, fuera de racha = anillo tenue.
 */
@Composable
fun WeekStrip(stats: GamificationStats, modifier: Modifier = Modifier, cellSize: Dp = 18.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val faint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(cellSize / 3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clearAndSetSemantics {},
    ) {
        weekStrip(stats, WEEK_STRIP_SLOTS).forEach { cell ->
            when (cell) {
                WeekCell.DONE -> Box(
                    modifier = Modifier.size(cellSize).background(primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(cellSize * 0.72f),
                    )
                }
                WeekCell.PENDING -> Box(
                    modifier = Modifier.size(cellSize).border(2.dp, primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(cellSize / 4).background(primary, CircleShape))
                }
                WeekCell.EMPTY -> Box(Modifier.size(cellSize).border(1.dp, faint, CircleShape))
            }
        }
    }
}

/**
 * Medalla circular de una insignia. Bloqueada = fondo neutro, ícono atenuado y candado (no solo cambio de color).
 * Decorativa: el nombre y el estado siempre van en texto al lado.
 */
@Composable
fun BadgeMedallion(achievement: Achievement, unlocked: Boolean, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier.size(size).clearAndSetSemantics {}) {
        val base = Modifier.size(size).let {
            if (unlocked) {
                it.background(colors.primary, CircleShape)
            } else {
                it.background(colors.surface, CircleShape).border(1.5.dp, colors.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
            }
        }
        Box(modifier = base, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = achievement.icon(),
                contentDescription = null,
                tint = if (unlocked) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(size * 0.5f),
            )
        }
        if (!unlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.38f)
                    .background(colors.onSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = colors.surface,
                    modifier = Modifier.size(size * 0.22f),
                )
            }
        }
    }
}

/** Barra de avance hacia una insignia. El indicador ya expone su progreso a lectores de pantalla. */
@Composable
fun GoalProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        modifier = modifier.fillMaxWidth().height(6.dp),
    )
}

/** "Te faltan 3 alertas atendidas para «5 alertas atendidas»." */
@Composable
fun goalLine(progress: AchievementProgress): String = stringResource(
    R.string.gami_goal_line,
    pluralStringResource(progress.achievement.metric.remainingRes(), progress.remaining, progress.remaining),
    stringResource(progress.achievement.labelRes()),
)
