package com.gunsout.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gunsout.data.entity.ProgramDay

@Composable
fun TodayScreen(
    onStartSession: (Long) -> Unit,
    vm: TodayViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()

    LaunchedEffect(state.toast) {
        // Toast handling kept simple — could be promoted to a SnackbarHost later.
        vm.consumeToast()
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Today", style = MaterialTheme.typography.headlineMedium)

        if (state.loading) {
            Text("Loading...")
            return@Column
        }

        // Next session card
        state.nextDay?.let { day ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Next up", style = MaterialTheme.typography.labelLarge)
                        AssistChip(
                            onClick = {},
                            label = { Text(if (state.onSchedule) "On schedule" else "Off-day") }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(day.label, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.startSession(day) { id -> onStartSession(id) } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start ${day.label}") }

                    state.alternativeForToday?.let { alt ->
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { vm.startSession(alt) { id -> onStartSession(id) } },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Or do ${alt.label} (today's hint)") }
                    }
                }
            }
        }

        // Choice card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Today's choice", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.markRestDay() }) { Text("Mark today rest") }
                    OutlinedButton(onClick = { vm.skipNextDay() }) { Text("Skip next day") }
                }
            }
        }

        // Pick a different day
        if (state.allNonRestDays.size > 1) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Pick a different day", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    state.allNonRestDays.forEach { day ->
                        OutlinedButton(
                            onClick = { vm.startSession(day) { id -> onStartSession(id) } },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) { Text(day.label) }
                    }
                }
            }
        }

        // Summary
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("This week", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("${state.completedThisWeek} of ${state.sessionsTargetThisWeek} sessions completed")
                state.lastSessionLabel?.let { last ->
                    Spacer(Modifier.height(4.dp))
                    Text("Last session: $last (${state.daysSinceLastSession ?: 0}d ago)")
                }
            }
        }
    }
}
