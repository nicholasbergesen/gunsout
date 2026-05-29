package com.nicholasbergesen.gunsout.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nicholasbergesen.gunsout.data.entity.Protocol

@Composable
fun SessionScreen(
    sessionId: Long,
    onFinished: () -> Unit,
    vm: SessionViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }

    if (state.loading) {
        Text("Loading session...", modifier = Modifier.padding(16.dp))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        item {
            Column {
                Text(state.dayLabel, style = MaterialTheme.typography.headlineMedium)
                if (state.baselineWeekActive) {
                    Spacer(Modifier.height(6.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Text(
                            "Baseline week. Collect numbers, no progression suggestions yet.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        items(state.items, key = { it.programExercise.id }) { item ->
            ExerciseCard(item, vm)
        }

        item { KneeFeelAndFinish(vm) }
    }
}

@Composable
private fun ExerciseCard(item: PlannedExerciseUi, vm: SessionViewModel) {
    var swapOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(item.exercise.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (item.alternates.isNotEmpty()) {
                    androidx.compose.material3.TextButton(onClick = { swapOpen = true }) { Text("Swap") }
                }
            }
            val pe = item.programExercise
            val protocolLabel = when (pe.protocol) {
                Protocol.PULL_UP_5X2_3 -> "5 sets x 2-3 reps (with 1 rep in reserve)"
                Protocol.AMRAP -> "${pe.sets} sets x AMRAP"
                Protocol.STANDARD -> "${pe.sets} sets x ${pe.repsMin}-${pe.repsMax} reps. Rest ${pe.restSec}s"
            }
            Text(protocolLabel, style = MaterialTheme.typography.bodySmall)

            item.previousBest?.let { last ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "Last: ${last.weightKg?.let { "${it} kg" } ?: "bw"} x ${last.reps ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item.suggestion?.let { sug ->
                val (text, _) = when (sug) {
                    is com.nicholasbergesen.gunsout.domain.progression.ProgressionEngine.Suggestion.IncreaseWeight ->
                        "Suggested: +${sug.deltaKg} kg" to true
                    is com.nicholasbergesen.gunsout.domain.progression.ProgressionEngine.Suggestion.DecreaseWeight ->
                        "Suggested: drop 5 percent" to true
                    is com.nicholasbergesen.gunsout.domain.progression.ProgressionEngine.Suggestion.HoldWeight ->
                        "Suggested: hold weight" to true
                    is com.nicholasbergesen.gunsout.domain.progression.ProgressionEngine.Suggestion.GraduatePullUp ->
                        "Suggested: graduate to ${sug.newScheme}" to true
                    is com.nicholasbergesen.gunsout.domain.progression.ProgressionEngine.Suggestion.RegressPullUp ->
                        "Suggested: try ${sug.variant}" to true
                    com.nicholasbergesen.gunsout.domain.progression.ProgressionEngine.Suggestion.KeepCollectingData ->
                        "" to false
                }
                if (text.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(8.dp))
            for (setIndex in 1..item.programExercise.sets) {
                val existing = item.sets.firstOrNull { it.setIndex == setIndex }
                SetRow(
                    setIndex = setIndex,
                    existing = existing,
                    suggestedKg = (item.previousBest?.weightKg) ?: 0.0,
                    onLog = { weight, reps, rpe, isWarmup ->
                        vm.logSet(item.programExercise, item.exercise, setIndex, weight, reps, rpe, isWarmup)
                    }
                )
            }
        }
    }

    if (swapOpen) {
        SwapAlternateDialog(
            currentName = item.exercise.name,
            alternates = item.alternates,
            onDismiss = { swapOpen = false },
            onSwap = { newId, persist ->
                vm.swapExercise(item.programExercise, newId, persist)
                swapOpen = false
            }
        )
    }
}

@Composable
private fun SwapAlternateDialog(
    currentName: String,
    alternates: List<com.nicholasbergesen.gunsout.data.entity.Exercise>,
    onDismiss: () -> Unit,
    onSwap: (Long, Boolean) -> Unit
) {
    var selected by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.nicholasbergesen.gunsout.data.entity.Exercise?>(null) }
    var persist by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Swap from $currentName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                alternates.forEach { alt ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selected?.id == alt.id,
                            onClick = { selected = alt }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(alt.name)
                            Text(alt.equipment.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = persist, onCheckedChange = { persist = it })
                    Text("Also save the swap to the program")
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                enabled = selected != null,
                onClick = { onSwap(selected!!.id, persist) }
            ) { Text("Swap") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SetRow(
    setIndex: Int,
    existing: com.nicholasbergesen.gunsout.data.entity.SetEntry?,
    suggestedKg: Double,
    onLog: (weight: Double?, reps: Int?, rpe: Int?, isWarmup: Boolean) -> Unit
) {
    var weightText by remember(existing?.id) { mutableStateOf(existing?.weightKg?.toString() ?: "") }
    var repsText by remember(existing?.id) { mutableStateOf(existing?.reps?.toString() ?: "") }
    var rpeText by remember(existing?.id) { mutableStateOf(existing?.rpe?.toString() ?: "") }
    var isWarmup by remember(existing?.id) { mutableStateOf(existing?.isWarmup ?: false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("$setIndex", modifier = Modifier.padding(top = 16.dp).padding(end = 4.dp))
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("kg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = repsText,
                onValueChange = { repsText = it.filter(Char::isDigit) },
                label = { Text("reps") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = rpeText,
                onValueChange = { rpeText = it.filter(Char::isDigit) },
                label = { Text("RPE") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.Button(onClick = {
                onLog(weightText.toDoubleOrNull(), repsText.toIntOrNull(), rpeText.toIntOrNull(), isWarmup)
            }) { Text(if (existing == null) "Log" else "Save") }
        }
        Row(
            modifier = Modifier.padding(start = 24.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.Checkbox(checked = isWarmup, onCheckedChange = { isWarmup = it })
            Text("Warmup", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun KneeFeelAndFinish(vm: SessionViewModel) {
    val state by vm.state.collectAsState()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Knee feel", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { v ->
                    FilterChip(
                        selected = state.kneeFeel == v,
                        onClick = { vm.setKneeFeel(if (state.kneeFeel == v) null else v) },
                        label = { Text(v.toString()) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = vm::setNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.finish() }, modifier = Modifier.fillMaxWidth()) {
                Text("Finish session")
            }
        }
    }
}
