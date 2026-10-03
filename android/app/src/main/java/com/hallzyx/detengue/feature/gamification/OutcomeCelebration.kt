package com.hallzyx.detengue.feature.gamification

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.gamification.Achievement
import com.hallzyx.detengue.core.gamification.InspectionOutcome
import com.hallzyx.detengue.core.gamification.nextGoal
import com.hallzyx.detengue.ui.labelRes
import kotlinx.coroutines.delay

/**
 * Cierre de una inspección (se muestra una sola vez). Animaciones cortas y baratas (escala + contador), pensadas
 * para gama media-baja. Sin alertas atendidas no hay celebración falsa: mensaje honesto y sin rebote.
 */
@Composable
fun OutcomeCelebration(outcome: InspectionOutcome, onDismiss: () -> Unit) {
    val celebrate = outcome.resolvedNow > 0 || outcome.newAchievements.isNotEmpty()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeroIcon(celebrate)
                Text(
                    text = stringResource(R.string.gami_outcome_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )
                if (outcome.resolvedNow > 0) {
                    ResolvedCounter(outcome.resolvedNow)
                } else {
                    Text(
                        text = stringResource(R.string.gami_outcome_none),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StreakRow(outcome)
                if (outcome.newAchievements.isNotEmpty()) NewBadges(outcome.newAchievements)
                nextGoal(outcome.stats)?.let { goal ->
                    Text(
                        text = goalLine(goal),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(stringResource(R.string.gami_outcome_ok), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** Con logro: medalla primaria que entra con un rebote suave. Sin logro: ícono neutro de "volver a revisar", quieto. */
@Composable
private fun HeroIcon(celebrate: Boolean) {
    val scale = remember { Animatable(if (celebrate) 0.6f else 1f) }
    LaunchedEffect(celebrate) {
        if (celebrate) {
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(88.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(if (celebrate) colors.primary else colors.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (celebrate) Icons.Filled.CheckCircle else Icons.Filled.Refresh,
            contentDescription = null,
            tint = if (celebrate) colors.onPrimary else colors.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
    }
}

/** Número grande que cuenta de 0 al total. TalkBack recibe el valor final, no los intermedios. */
@Composable
private fun ResolvedCounter(resolved: Int) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val shown by animateIntAsState(
        targetValue = if (started) resolved else 0,
        animationSpec = tween(durationMillis = 700),
        label = "resolvedCounter",
    )
    val spoken = pluralStringResource(R.plurals.gami_outcome_resolved_cd, resolved, resolved)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
    ) {
        Text(
            text = shown.toString(),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = pluralStringResource(R.plurals.gami_outcome_resolved_label, resolved),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StreakRow(outcome: InspectionOutcome) {
    if (outcome.streakWeeks <= 0) return
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = pluralStringResource(R.plurals.gami_streak_title, outcome.streakWeeks, outcome.streakWeeks),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        WeekStrip(outcome.stats, cellSize = 22.dp)
    }
}

/** Cada insignia nueva entra escalando, una tras otra (escalonado corto). */
@Composable
private fun NewBadges(badges: List<Achievement>) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = pluralStringResource(R.plurals.gami_outcome_new_badges, badges.size, badges.size),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            badges.forEachIndexed { index, badge ->
                val scale = remember(badge) { Animatable(0f) }
                LaunchedEffect(badge) {
                    delay(250L + index * 180L)
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                }
                val name = stringResource(badge.labelRes())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                ) {
                    BadgeMedallion(
                        achievement = badge,
                        unlocked = true,
                        size = 52.dp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}
