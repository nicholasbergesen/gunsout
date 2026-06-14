package com.nicholasbergesen.gunsout.feature.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nicholasbergesen.gunsout.core.text.formatOneDecimalOrInt
import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.feature.exerciseguide.ExerciseVisualGuide
import com.nicholasbergesen.gunsout.ui.components.ChipButton
import com.nicholasbergesen.gunsout.ui.components.MockupScreenColumn
import com.nicholasbergesen.gunsout.ui.components.ScreenTitle
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.ThemedCard
import com.nicholasbergesen.gunsout.ui.components.WrappingRow

@Composable
fun LibraryListScreen(
    onEdit: (Long) -> Unit,
    onCreate: () -> Unit,
    vm: LibraryListViewModel = hiltViewModel()
) {
    val exercises by vm.exercises.collectAsState()
    var query by remember { mutableStateOf("") }
    var muscleFilter by remember { mutableStateOf<MuscleGroup?>(null) }
    var equipmentFilter by remember { mutableStateOf<Equipment?>(null) }
    var movementFilter by remember { mutableStateOf<MovementPattern?>(null) }
    val filtered = exercises.filter { ex ->
        (query.isBlank() || ex.name.contains(query, ignoreCase = true)) &&
            (muscleFilter == null || ex.primaryMuscleGroup == muscleFilter) &&
            (equipmentFilter == null || ex.equipment == equipmentFilter) &&
            (movementFilter == null || ex.movementPattern == movementFilter)
    }

    MockupScreenColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ScreenTitle("Exercises")
            ChipButton("+ New", selected = true, onClick = onCreate)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search exercise name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        FilterRow(
            label = "Muscle",
            selected = muscleFilter,
            values = MuscleGroup.values().toList(),
            onSelect = { muscleFilter = it }
        )
        FilterRow(
            label = "Equipment",
            selected = equipmentFilter,
            values = Equipment.values().toList(),
            onSelect = { equipmentFilter = it }
        )
        FilterRow(
            label = "Movement",
            selected = movementFilter,
            values = MovementPattern.values().toList(),
            onSelect = { movementFilter = it }
        )
        if (filtered.isEmpty()) {
            ThemedCard { Text("No exercises found.") }
        }
        filtered.groupBy { it.primaryMuscleGroup }.forEach { (muscle, list) ->
            SectionLabel(muscle.name)
            list.forEach { ex ->
                ThemedCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(ex.name)
                            Text(
                                "${ex.equipment.name} | ${ex.movementPattern.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ChipButton("Edit", onClick = { onEdit(ex.id) })
                    }
                }
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
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    MockupScreenColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Column {
            SectionLabel("Library")
            ScreenTitle(if (exerciseId > 0) "Edit exercise" else "New exercise")
        }

        OutlinedTextField(
            value = state.name,
            onValueChange = vm::setName,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnumDropdown(
                label = "Muscle group",
                value = state.muscle,
                values = MuscleGroup.values().toList(),
                onChange = vm::setMuscle,
                modifier = Modifier.weight(1f)
            )
            EnumDropdown(
                label = "Equipment",
                value = state.equipment,
                values = Equipment.values().toList(),
                onChange = vm::setEquipment,
                modifier = Modifier.weight(1f)
            )
        }
        EnumDropdown(
            label = "Movement pattern",
            value = state.movementPattern,
            values = MovementPattern.values().toList(),
            onChange = vm::setMovementPattern,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.defaultRestSec,
            onValueChange = vm::setRestSec,
            singleLine = true,
            label = { Text("Default rest, seconds") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = state.formNotes,
            onValueChange = vm::setFormNotes,
            label = { Text("Form notes") },
            modifier = Modifier.fillMaxWidth().height(58.dp)
        )

        ThemedCard {
            ExerciseVisualGuide(
                muscleGroup = state.muscle,
                movementPattern = state.movementPattern
            )
        }

        if (state.history.size >= 2) {
            ThemedCard {
                SectionLabel("Top working-set weight")
                HistoryChart(state.history)
                val latest = state.history.last()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${latest.date}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${formatOneDecimalOrInt(latest.topWeightKg)} kg x ${latest.reps}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Button(onClick = vm::save, modifier = Modifier.fillMaxWidth(), enabled = state.name.isNotBlank()) {
            Text("Save")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun <T : Enum<T>> FilterRow(
    label: String,
    selected: T?,
    values: List<T>,
    onSelect: (T?) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        WrappingRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
            values.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(value.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
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
            onValueChange = {},
            readOnly = true,
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
    val minW = ((weights.minOrNull() ?: 0.0) - 2).coerceAtLeast(0.0)
    val maxW = (weights.maxOrNull() ?: 0.0) + 2
    val range = (maxW - minW).coerceAtLeast(1.0)
    val primary = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
        val w = size.width
        val h = size.height
        val stepX = if (weights.size > 1) w / (weights.size - 1) else w
        val path = Path()
        weights.forEachIndexed { i, value ->
            val x = stepX * i
            val y = h - ((value - minW) / range * h).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = primary, style = Stroke(width = 4f))
    }
}
