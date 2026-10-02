package com.hallzyx.detengue.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hallzyx.detengue.DetengueApplication
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.database.InspectionSummary
import com.hallzyx.detengue.core.history.DayBucket
import com.hallzyx.detengue.core.history.dayBucket
import com.hallzyx.detengue.ui.BackBar
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Historial local (PRD §35). Todo vive en el teléfono; no hay nube. */
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val repository = (LocalContext.current.applicationContext as DetengueApplication).inspectionRepository
    val scope = rememberCoroutineScope()
    val history by produceState<List<InspectionSummary>?>(initialValue = null, repository) {
        repository.history.collect { value = it }
    }
    var confirmClear by remember { mutableStateOf(false) }
    val now = remember { System.currentTimeMillis() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 8.dp)) {
            BackBar(onBack = onBack)
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )

            val items = history
            when {
                items == null -> Box(Modifier.weight(1f))
                items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), Alignment.Center) {
                    Text(
                        text = stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items, key = { it.id }) { HistoryRow(it, now) }
                    }
                    TextButton(
                        onClick = { confirmClear = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text = stringResource(R.string.history_clear),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch { repository.clear() }
                }) { Text(stringResource(R.string.history_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.history_cancel)) }
            },
        )
    }
}

@Composable
private fun HistoryRow(summary: InspectionSummary, nowMillis: Long) {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(summary.startedAtMillis).atZone(zone)
    val time = start.format(DateTimeFormatter.ofPattern("HH:mm"))
    val whenLabel = when (dayBucket(summary.startedAtMillis, nowMillis, zone)) {
        DayBucket.TODAY -> stringResource(R.string.history_today, time)
        DayBucket.YESTERDAY -> stringResource(R.string.history_yesterday, time)
        DayBucket.OTHER -> start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }
    val pendingLabel = summary.finalPending
        ?.let { stringResource(R.string.history_pending, summary.initialPending, it) }
        ?: stringResource(R.string.history_pending_only, summary.initialPending)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = whenLabel,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = pendingLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
