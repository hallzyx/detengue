package com.hallzyx.detengue.feature.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hallzyx.detengue.DetengueApplication
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.rules.AssessedDetection
import com.hallzyx.detengue.core.rules.Condition
import com.hallzyx.detengue.core.rules.UserAnswer
import com.hallzyx.detengue.core.session.ReviewComparison
import com.hallzyx.detengue.ui.color
import com.hallzyx.detengue.ui.labelRes
import com.hallzyx.detengue.ui.statusRes
import com.hallzyx.detengue.ui.textRes

private fun Condition.priority(): Int = when (this) {
    Condition.RISK -> 0
    Condition.REVIEW -> 1
    Condition.UNCERTAIN -> 2
    Condition.SAFE -> 3
}

/**
 * Resultados del escaneo (PRD §33) y comparación antes/después tras reescanear (PRD §34).
 * Las acciones son locales y determinísticas; nada de esto usa red ni LLM.
 */
@Composable
fun ResultsScreen(onRecheck: () -> Unit, onFinish: () -> Unit) {
    val context = LocalContext.current
    val store = (context.applicationContext as DetengueApplication).inspectionStore
    val session by store.session.collectAsStateWithLifecycle()

    val latest = session?.latest
    val items = remember(latest) {
        latest?.assessed.orEmpty()
            .sortedWith(compareBy({ it.condition.priority() }, { -it.detection.confidence }))
    }
    val pending = items.count { it.condition.needsAttention }
    val comparison = session?.comparison

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    Header(hasItems = items.isNotEmpty(), pending = pending, comparison = comparison)
                }
                items(items, key = { it.id }) { item ->
                    ItemCard(item = item, onAnswer = { store.answer(item.id, it) })
                }
                item(key = "disclaimer") {
                    Text(
                        text = stringResource(R.string.results_disclaimer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            BottomActions(pending = pending, onRecheck = onRecheck, onFinish = onFinish)
        }
    }
}

@Composable
private fun Header(hasItems: Boolean, pending: Int, comparison: ReviewComparison?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (comparison != null) {
            ComparisonCard(comparison)
        } else {
            val title = when {
                !hasItems -> stringResource(R.string.scan_no_detections)
                pending == 0 -> stringResource(R.string.results_none_pending)
                else -> pluralStringResource(R.plurals.results_found, pending, pending)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ComparisonCard(comparison: ReviewComparison) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.comparison_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CountColumn(stringResource(R.string.comparison_before), comparison.before)
                Text(
                    text = "→",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                CountColumn(stringResource(R.string.comparison_after), comparison.after)
            }
            if (comparison.fixed > 0) {
                Text(
                    text = pluralStringResource(R.plurals.comparison_fixed, comparison.fixed, comparison.fixed),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (comparison.completed) {
                Text(
                    text = stringResource(R.string.results_completed),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.results_none_pending),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = stringResource(R.string.comparison_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CountColumn(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun ItemCard(item: AssessedDetection, onAnswer: (UserAnswer) -> Unit) {
    var expanded by remember(item.id) { mutableStateOf(item.condition == Condition.RISK) }
    // Si la persona confirma que hay agua, la acción se muestra de inmediato.
    LaunchedEffect(item.condition) {
        if (item.condition == Condition.RISK) expanded = true
    }

    val showQuestion = item.condition == Condition.REVIEW || item.userAnswer != null

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(item.condition.color()),
                )
                Text(
                    text = stringResource(item.detection.objectClass.labelRes()),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(
                text = stringResource(item.condition.statusRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (showQuestion) {
                Text(
                    text = stringResource(R.string.results_question),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                AnswerRow(selected = item.userAnswer, onAnswer = onAnswer)
            }

            item.actionCode?.let { action ->
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        text = stringResource(
                            if (expanded) R.string.results_hide_recommendation else R.string.results_show_recommendation,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (expanded) {
                    Text(
                        text = stringResource(action.textRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerRow(selected: UserAnswer?, onAnswer: (UserAnswer) -> Unit) {
    val options = listOf(
        UserAnswer.HAS_WATER to R.string.answer_yes,
        UserAnswer.NO_WATER to R.string.answer_no,
        UserAnswer.UNSURE to R.string.answer_unsure,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { (answer, labelRes) ->
            val buttonModifier = Modifier.weight(1f).heightIn(min = 52.dp)
            val label: @Composable () -> Unit = {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
            if (selected == answer) {
                Button(
                    onClick = { onAnswer(answer) },
                    modifier = buttonModifier,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) { label() }
            } else {
                OutlinedButton(
                    onClick = { onAnswer(answer) },
                    modifier = buttonModifier,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) { label() }
            }
        }
    }
}

@Composable
private fun BottomActions(pending: Int, onRecheck: () -> Unit, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (pending > 0) {
            Button(onClick = onRecheck, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text(
                    text = stringResource(R.string.results_recheck),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
            TextButton(onClick = onFinish) {
                Text(
                    text = stringResource(R.string.results_finish),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text(
                    text = stringResource(R.string.results_finish),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRecheck, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(
                    text = stringResource(R.string.scan_again),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
