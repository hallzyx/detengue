package com.hallzyx.detengue.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.hallzyx.detengue.core.gamification.GamificationStats
import com.hallzyx.detengue.core.gamification.InspectionOutcome
import com.hallzyx.detengue.feature.gamification.OutcomeCelebration
import com.hallzyx.detengue.feature.gamification.ProgressCard

/**
 * Pantalla de inicio (PRD §11).
 * No muestra terminología técnica (INT8, TFLite, NPU…).
 *
 * - [aiReady]: solo es `true` cuando el modelo local realmente cargó.
 * - [online]: estado de conexión del teléfono; sin conexión se resalta que la IA sigue disponible (PRD §52).
 * - [onOpenDiagnostics]: solo se pasa en builds de demo (PRD §28, §45).
 * - [stats]: `null` mientras carga el historial; la tarjeta de progreso abre la pantalla de logros con [onOpenAchievements].
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
    onOpenAchievements: () -> Unit = {},
) {
    if (outcome != null) OutcomeCelebration(outcome, onDismissOutcome)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // Si con fuente grande el contenido no cabe, se puede desplazar; si cabe, SpaceBetween reparte igual que antes.
        BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            // En pantallas bajas el logo se encoge para que la tarjeta de progreso quepa sin desplazar.
            val compact = stats != null && maxHeight < 720.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(Modifier.height(8.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 72.dp else 112.dp),
                    )
                    Spacer(Modifier.height(if (compact) 8.dp else 20.dp))
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
                        ProgressCard(stats, onOpen = onOpenAchievements)
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
