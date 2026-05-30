package com.nicholasbergesen.gunsout.feature.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.ui.components.ChipButton
import com.nicholasbergesen.gunsout.ui.components.MockupScreenColumn
import com.nicholasbergesen.gunsout.ui.components.ScreenTitle
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.StatusChip
import com.nicholasbergesen.gunsout.ui.components.ThemedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramEditScreen(
    programId: Long,
    onBack: () -> Unit,
    vm: ProgramEditViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()

    var pickerOpenForDay by remember { mutableStateOf<Long?>(null) }
    var schemeEditFor by remember { mutableStateOf<ProgramExercise?>(null) }

    MockupScreenColumn(modifier = Modifier.verticalScroll(scroll)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                SectionLabel("Programs")
                ScreenTitle(state.program?.name ?: "Program")
            }
            state.program?.takeIf { it.isActive }?.let { StatusChip("Active", selected = true) }
        }
        if (state.program == null) {
            Text("Program not found.")
            Button(onClick = onBack) { Text("Back") }
            return@MockupScreenColumn
        }

        state.days.forEach { day ->
            DayCard(
                day = day,
                programExercises = state.exercisesByDayId[day.id].orEmpty(),
                exercisesById = state.exercisesById,
                onRename = { vm.renameDay(day, it) },
                onToggleRest = { vm.toggleDayRest(day) },
                onDelete = { vm.deleteDay(day) },
                onAddExercise = { pickerOpenForDay = day.id },
                onEditScheme = { schemeEditFor = it },
                onRemoveExercise = { vm.deleteProgramExercise(it) }
            )
        }

        OutlinedButton(onClick = { vm.addDay() }, modifier = Modifier.fillMaxWidth()) { Text("+ Add day") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to programs") }
    }

    val openId = pickerOpenForDay
    if (openId != null) {
        val day = state.days.firstOrNull { it.id == openId } ?: return
        ExercisePickerSheet(
            onDismiss = { pickerOpenForDay = null },
            onPick = { exId ->
                vm.addExerciseToDay(day, exId)
                pickerOpenForDay = null
            }
        )
    }

    schemeEditFor?.let { pe ->
        SchemeEditDialog(
            pe = pe,
            onDismiss = { schemeEditFor = null },
            onSave = { updated ->
                vm.updateProgramExercise(updated)
                schemeEditFor = null
            }
        )
    }
}

@Composable
private fun DayCard(
    day: ProgramDay,
    programExercises: List<ProgramExercise>,
    exercisesById: Map<Long, Exercise>,
    onRename: (String) -> Unit,
    onToggleRest: () -> Unit,
    onDelete: () -> Unit,
    onAddExercise: () -> Unit,
    onEditScheme: (ProgramExercise) -> Unit,
    onRemoveExercise: (ProgramExercise) -> Unit
) {
    var renaming by remember(day.id) { mutableStateOf(false) }
    var label by remember(day.id) { mutableStateOf(day.label) }

    ThemedCard(accent = !day.isRest) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            if (renaming) {
                Row(Modifier.weight(1f), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { onRename(label); renaming = false }) { Text("Save") }
                }
            } else {
                Text(day.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                ChipButton("Rename", onClick = { renaming = true })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton(if (day.isRest) "Rest day" else "Training day", onClick = onToggleRest)
            ChipButton("Delete day", onClick = onDelete)
        }
        if (!day.isRest) {
            programExercises.forEach { pe ->
                val ex = exercisesById[pe.exerciseId]
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ex?.name ?: "(unknown)")
                        val protocolLabel = when (pe.protocol) {
                            Protocol.PULL_UP_5X2_3 -> "5x2-3"
                            Protocol.AMRAP -> "${pe.sets}xAMRAP"
                            Protocol.STANDARD -> "${pe.sets} x ${pe.repsMin}-${pe.repsMax}, rest ${pe.restSec}s"
                        }
                        Text(protocolLabel, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        TextButton(onClick = { onEditScheme(pe) }) { Text("Edit") }
                        TextButton(onClick = { onRemoveExercise(pe) }) { Text("Remove") }
                    }
                }
            }
            OutlinedButton(onClick = onAddExercise) { Text("+ Add exercise") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    vm: ExercisePickerViewModel = hiltViewModel()
) {
    val exercises by vm.exercises.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("Pick an exercise", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                items(exercises) { ex ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(ex.name)
                            Text("${ex.primaryMuscleGroup.name} - ${ex.equipment.name}", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { onPick(ex.id) }) { Text("Add") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchemeEditDialog(
    pe: ProgramExercise,
    onDismiss: () -> Unit,
    onSave: (ProgramExercise) -> Unit
) {
    var sets by remember(pe.id) { mutableStateOf(pe.sets.toString()) }
    var repsMin by remember(pe.id) { mutableStateOf(pe.repsMin.toString()) }
    var repsMax by remember(pe.id) { mutableStateOf(pe.repsMax.toString()) }
    var restSec by remember(pe.id) { mutableStateOf(pe.restSec.toString()) }
    var rpe by remember(pe.id) { mutableStateOf(pe.rpeTarget?.toString().orEmpty()) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit scheme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = sets, onValueChange = { sets = it.filter(Char::isDigit) },
                    label = { Text("Sets") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = repsMin, onValueChange = { repsMin = it.filter(Char::isDigit) },
                        label = { Text("Reps min") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = repsMax, onValueChange = { repsMax = it.filter(Char::isDigit) },
                        label = { Text("Reps max") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                OutlinedTextField(value = restSec, onValueChange = { restSec = it.filter(Char::isDigit) },
                    label = { Text("Rest seconds") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = rpe, onValueChange = { rpe = it.filter(Char::isDigit) },
                    label = { Text("RPE target (optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(pe.copy(
                    sets = sets.toIntOrNull() ?: pe.sets,
                    repsMin = repsMin.toIntOrNull() ?: pe.repsMin,
                    repsMax = repsMax.toIntOrNull() ?: pe.repsMax,
                    restSec = restSec.toIntOrNull() ?: pe.restSec,
                    rpeTarget = rpe.toIntOrNull()
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
