package com.hallzyx.detengue.feature.gamification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.gamification.Achievement
import com.hallzyx.detengue.core.gamification.AchievementProgress
import com.hallzyx.detengue.core.gamification.GamificationStats
import com.hallzyx.detengue.core.gamification.StreakState
import com.hallzyx.detengue.core.gamification.allProgress
import com.hallzyx.detengue.core.gamification.streakState
import com.hallzyx.detengue.ui.BackBar
import com.hallzyx.detengue.ui.descriptionRes
import com.hallzyx.detengue.ui.labelRes

/**
 * Logros: resumen de cifras, racha semanal y todas las insignias (conseguidas y por conseguir, con su avance).
 * [stats] es `null` mientras carga el historial: no se muestra nada para no dar cifras falsas un instante.
 */
@Composable
fun AchievementsScreen(stats: GamificationStats?, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 8.dp)) {
            BackBar(onBack = onBack)
            Text(
                text = stringResource(R.string.ach_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            if (stats == null) {
                Box(Modifier.weight(1f))
                return@Column
            }
            val progress = stats.allProgress()
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { StatsSummary(stats) }
                item { StreakSection(stats) }
                item {
                    Text(
                        text = stringResource(R.string.gami_badges, progress.count { it.unlocked }, progress.size),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(progress, key = { it.achievement.name }) { BadgeRow(it) }
            }
        }
    }
}

/** Cifras de [GamificationStats]. "Zonas revisadas" solo aparece cuando ese dato exista (hoy siempre es `null`). */
@Composable
private fun StatsSummary(stats: GamificationStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(stats.inspections, stringResource(R.string.ach_stat_inspections), Modifier.weight(1f))
            StatTile(stats.resolvedAlerts, stringResource(R.string.ach_stat_resolved), Modifier.weight(1f))
            StatTile(stats.streakWeeks, stringResource(R.string.ach_stat_streak), Modifier.weight(1f))
        }
        stats.sweptZones?.let { swept ->
            StatTile(swept, stringResource(R.string.ach_stat_swept), Modifier.fillMaxWidth())
        }
        Text(
            text = stringResource(R.string.ach_resolved_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun StatTile(value: Int, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StreakSection(stats: GamificationStats) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.ach_streak_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            WeekStrip(stats, cellSize = 26.dp)
            Text(
                text = stringResource(
                    when (streakState(stats)) {
                        StreakState.ACTIVE -> R.string.gami_streak_active
                        StreakState.AT_RISK -> R.string.gami_streak_pending
                        StreakState.NOT_STARTED -> R.string.gami_streak_none
                    },
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = stringResource(R.string.ach_streak_explain),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Una insignia: medalla, nombre, cómo se consigue y su estado en texto (nunca solo por color). */
@Composable
private fun BadgeRow(progress: AchievementProgress) {
    val achievement: Achievement = progress.achievement
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (progress.unlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
        // Las bloqueadas van sobre el fondo; el borde las mantiene legibles como tarjeta.
        border = if (progress.unlocked) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgeMedallion(achievement, unlocked = progress.unlocked)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(achievement.labelRes()),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(achievement.descriptionRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (progress.unlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.ach_status_unlocked),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                } else {
                    // El estado va en texto además del candado y la barra: no depende del color.
                    Text(
                        text = stringResource(R.string.ach_locked) + " · " +
                            stringResource(R.string.ach_status_progress, progress.current, progress.target),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GoalProgressBar(progress.fraction)
                }
            }
        }
    }
}
