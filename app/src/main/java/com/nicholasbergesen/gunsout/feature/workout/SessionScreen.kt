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
import com.nicholasbergesen.gunsout.domain.recommendation.RecommendationTarget
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.StatusChip
import com.nicholasbergesen.gunsout.ui.components.ThemedCard
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)
    ) {
        item {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        SectionLabel("Session")
                        Text(state.dayLabel, style = MaterialTheme.typography.headlineSmall)
                    }
                    StatusChip("Active", selected = true)
                }
                if (state.baselineWeekActive) {
                    Spacer(Modifier.height(6.dp))
                    ThemedCard(accent = true) {
                        Text(
                            "Baseline week. Collect numbers, no progression suggestions yet.",
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
    ThemedCard {
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
            Text(
                "Last: ${last.weightKg?.let { "${it} kg" } ?: "bw"} x ${last.reps ?: "-"}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        item.recommendation?.let { rec ->
            Text(rec.displayText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text(rec.explanation, style = MaterialTheme.typography.bodySmall)
        }

        for (setIndex in 1..item.programExercise.sets) {
            val existing = item.sets.firstOrNull { it.setIndex == setIndex }
            val recommendation = item.recommendation
            SetRow(
                setIndex = setIndex,
                existing = existing,
                prefillWeightKg = recommendation
                    ?.takeIf { it.target == RecommendationTarget.WEIGHT_KG && existing == null }
                    ?.weightKg,
                prefillReps = recommendation
                    ?.takeIf { it.target == RecommendationTarget.REPS && existing == null }
                    ?.reps,
                onLog = { weight, reps, rpe, isWarmup ->
                    vm.logSet(item.programExercise, item.exercise, setIndex, weight, reps, rpe, isWarmup)
                }
            )
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
    prefillWeightKg: Double?,
    prefillReps: Int?,
    onLog: (weight: Double?, reps: Int?, rpe: Int?, isWarmup: Boolean) -> Unit
) {
    var weightText by remember(existing?.id) {
        mutableStateOf(existing?.weightKg?.toString() ?: prefillWeightKg?.let(::formatKg).orEmpty())
    }
    var repsText by remember(existing?.id) {
        mutableStateOf(existing?.reps?.toString() ?: prefillReps?.toString().orEmpty())
    }
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

private fun formatKg(value: Double): String {
    val roundedInt = value.roundToInt()
    return if (abs(value - roundedInt) < 0.000_001) {
        roundedInt.toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

@Composable
private fun KneeFeelAndFinish(vm: SessionViewModel) {
    val state by vm.state.collectAsState()
    ThemedCard {
        Text("Knee feel", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { v ->
                FilterChip(
                    selected = state.kneeFeel == v,
                    onClick = { vm.setKneeFeel(if (state.kneeFeel == v) null else v) },
                    label = { Text(v.toString()) }
                )
            }
        }
        OutlinedTextField(
            value = state.notes,
            onValueChange = vm::setNotes,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { vm.finish() }, modifier = Modifier.fillMaxWidth()) {
            Text("Finish session")
        }
    }
}
