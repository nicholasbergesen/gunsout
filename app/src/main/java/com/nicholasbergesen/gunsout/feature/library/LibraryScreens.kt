package com.nicholasbergesen.gunsout.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup

@Composable
fun LibraryListScreen(
    onEdit: (Long) -> Unit,
    onCreate: () -> Unit,
    vm: LibraryListViewModel = hiltViewModel()
) {
    val exercises by vm.exercises.collectAsState()
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New exercise") }
            )
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Exercises", style = MaterialTheme.typography.headlineMedium)
            if (exercises.isEmpty()) Text("No exercises found.")
            val byMuscle = exercises.groupBy { it.primaryMuscleGroup }
            byMuscle.forEach { (muscle, list) ->
                Text(muscle.name, style = MaterialTheme.typography.titleSmall)
                list.forEach { ex ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(ex.name)
                                Text(ex.equipment.name, style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { onEdit(ex.id) }) { Text("Edit") }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun ExerciseEditScreen(
    exerciseId: Long,
    onBack: () -> Unit,
    vm: ExerciseEditViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Column(
        Modifier.padding(16.dp).fillMaxWidth().verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (exerciseId > 0) "Edit exercise" else "New exercise",
            style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = state.name, onValueChange = vm::setName, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        EnumDropdown(label = "Muscle group", value = state.muscle, values = MuscleGroup.values().toList(), onChange = vm::setMuscle, modifier = Modifier.fillMaxWidth())
        EnumDropdown(label = "Equipment", value = state.equipment, values = Equipment.values().toList(), onChange = vm::setEquipment, modifier = Modifier.fillMaxWidth())

        OutlinedTextField(value = state.defaultRestSec, onValueChange = vm::setRestSec,
            label = { Text("Default rest (s)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

        OutlinedTextField(
            value = state.formNotes, onValueChange = vm::setFormNotes,
            label = { Text("Form notes") },
            modifier = Modifier.fillMaxWidth().height(140.dp)
        )

        if (state.history.size >= 2) {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Top working-set weight over time", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    HistoryChart(state.history)
                    Spacer(Modifier.height(4.dp))
                    val latest = state.history.last()
                    Text(
                        "${latest.date}: ${"%.1f".format(latest.topWeightKg)} kg x ${latest.reps}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = vm::save, modifier = Modifier.fillMaxWidth(), enabled = state.name.isNotBlank()) {
            Text("Save")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    value: T,
    values: List<T>,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value.name,
            onValueChange = {}, readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            enabled = false
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { v ->
                DropdownMenuItem(text = { Text(v.name) }, onClick = { onChange(v); expanded = false })
            }
        }
    }
}

@Composable
private fun HistoryChart(history: List<HistoryPoint>) {
    if (history.size < 2) return
    val weights = history.map { it.topWeightKg }
    val minW = (weights.min() - 2).coerceAtLeast(0.0)
    val maxW = weights.max() + 2
    val range = (maxW - minW).coerceAtLeast(1.0)
    val primary = MaterialTheme.colorScheme.primary

    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxWidth().height(140.dp)
    ) {
        val w = size.width
        val h = size.height
        val stepX = if (weights.size > 1) w / (weights.size - 1) else w
        val path = androidx.compose.ui.graphics.Path()
        weights.forEachIndexed { i, value ->
            val x = stepX * i
            val y = h - ((value - minW) / range * h).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = primary, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
    }
}
