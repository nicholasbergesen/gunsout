package com.gunsout.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HistoryListScreen(
    onOpenSession: (Long) -> Unit,
    vm: HistoryListViewModel = hiltViewModel()
) {
    val sessions by vm.sessions.collectAsState()

    Column(Modifier.padding(16.dp).fillMaxWidth()) {
        Text("Session history", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        if (sessions.isEmpty()) {
            Text("No completed sessions yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = sessions, key = { it.id }) { s ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(s.programDayLabelSnapshot, style = MaterialTheme.typography.titleSmall)
                                Text(s.date.toString(), style = MaterialTheme.typography.bodySmall)
                                s.kneeFeel?.let { Text("Knee feel: $it/5", style = MaterialTheme.typography.bodySmall) }
                            }
                            OutlinedButton(onClick = { onOpenSession(s.id) }) { Text("Open") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    vm: HistoryDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()
    val session = state.session ?: run {
        Text("Session not found.", modifier = Modifier.padding(16.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.padding(16.dp)) { Text("Back") }
        return
    }

    val sortedSets = state.sets.sortedWith(compareBy({ it.exerciseIdSnapshot }, { it.setIndex }))
    val byExercise = sortedSets.groupBy { it.exerciseNameSnapshot }

    Column(
        Modifier.padding(16.dp).fillMaxWidth().verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(session.programDayLabelSnapshot, style = MaterialTheme.typography.headlineMedium)
        Text(session.date.toString(), style = MaterialTheme.typography.bodyMedium)
        session.kneeFeel?.let { Text("Knee feel: $it/5", style = MaterialTheme.typography.bodySmall) }
        session.notes?.takeIf { it.isNotBlank() }?.let { Text("Notes: $it", style = MaterialTheme.typography.bodySmall) }

        byExercise.forEach { (exerciseName, sets) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(exerciseName, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    sets.forEach { set ->
                        val label = buildString {
                            append("Set ${set.setIndex}: ")
                            if (set.weightKg != null) append("${set.weightKg} kg ") else append("bw ")
                            append("x ")
                            append(set.reps?.toString() ?: "-")
                            set.rpe?.let { append(" @ RPE ").append(it) }
                            if (set.isWarmup) append(" (warmup)")
                        }
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
