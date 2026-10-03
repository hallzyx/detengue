package com.hallzyx.detengue.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.gamification.Achievement
import com.hallzyx.detengue.core.gamification.GamificationStats
import com.hallzyx.detengue.core.gamification.InspectionOutcome
import com.hallzyx.detengue.ui.labelRes

/**
 * Pantalla de inicio (PRD §11).
 * No muestra terminología técnica (INT8, TFLite, NPU…).
 *
 * - [aiReady]: solo es `true` cuando el modelo local realmente cargó.
 * - [online]: estado de conexión del teléfono; sin conexión se resalta que la IA sigue disponible (PRD §52).
 * - [onOpenDiagnostics]: solo se pasa en builds de demo (PRD §28, §45).
 */
@Composable
fun HomeScreen(
    aiReady: Boolean,
    online: Boolean,
    onStartScan: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenDiagnostics: (() -> Unit)? = null,
    stats: GamificationStats? = null,
    outcome: InspectionOutcome? = null,
    onDismissOutcome: () -> Unit = {},
) {
    if (outcome != null) OutcomeDialog(outcome, onDismissOutcome)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(112.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (stats != null) {
                    Spacer(Modifier.height(16.dp))
                    ProgressCard(stats)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onStartScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_cta),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Spacer(Modifier.height(16.dp))
                ConnectionStatus(aiReady = aiReady, online = online)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onOpenHistory) {
                        Text(stringResource(R.string.home_history), style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(onClick = onOpenPrivacy) {
                        Text(stringResource(R.string.home_privacy), style = MaterialTheme.typography.labelLarge)
                    }
                    if (onOpenDiagnostics != null) {
                        TextButton(onClick = onOpenDiagnostics) {
                            Text(stringResource(R.string.home_diagnostics), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.home_disclaimer),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Sin conexión + IA lista → "Sin conexión / La IA de Detengue sigue disponible." (PRD §52).
 * Solo se afirma disponibilidad si el modelo cargó de verdad.
 */
@Composable
private fun ConnectionStatus(aiReady: Boolean, online: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!online && aiReady) {
            Text(
                text = stringResource(R.string.home_offline_title),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.home_offline_ai),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(if (aiReady) R.string.home_ai_ready else R.string.home_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Progreso local: inspecciones, alertas resueltas, racha semanal e insignias. Nada sale del teléfono. */
@Composable
private fun ProgressCard(stats: GamificationStats) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (stats.inspections == 0) {
                Text(
                    text = stringResource(R.string.gami_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Text(
                text = stringResource(R.string.gami_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.gami_inspections, stats.inspections),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.gami_resolved, stats.resolvedAlerts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.gami_streak, stats.streakWeeks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(if (stats.activeThisWeek) R.string.gami_streak_active else R.string.gami_streak_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val unlocked = stats.achievements
            Text(
                text = stringResource(R.string.gami_badges, unlocked.size, Achievement.entries.size),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            unlocked.forEach {
                Text(
                    text = "• " + stringResource(it.labelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Resumen al volver de una inspección: lo logrado y las insignias nuevas. Se muestra una sola vez. */
@Composable
private fun OutcomeDialog(outcome: InspectionOutcome, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gami_outcome_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (outcome.resolvedNow > 0) {
                        stringResource(R.string.gami_outcome_resolved, outcome.resolvedNow)
                    } else {
                        stringResource(R.string.gami_outcome_none)
                    },
                )
                if (outcome.streakWeeks > 0) Text(stringResource(R.string.gami_outcome_streak, outcome.streakWeeks))
                outcome.newAchievements.forEach {
                    Text(
                        text = stringResource(R.string.gami_outcome_badge, stringResource(it.labelRes())),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.gami_outcome_ok)) } },
    )
}
