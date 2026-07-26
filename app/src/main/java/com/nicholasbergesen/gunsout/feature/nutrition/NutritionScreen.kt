package com.nicholasbergesen.gunsout.feature.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nicholasbergesen.gunsout.data.entity.CreatineSettings
import com.nicholasbergesen.gunsout.data.entity.ProteinEntry
import com.nicholasbergesen.gunsout.ui.components.BigValue
import com.nicholasbergesen.gunsout.ui.components.DividerLine
import com.nicholasbergesen.gunsout.ui.components.MockupScreenColumn
import com.nicholasbergesen.gunsout.ui.components.ProgressPill
import com.nicholasbergesen.gunsout.ui.components.ScreenTitle
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.StatusChip
import com.nicholasbergesen.gunsout.ui.components.ThemedCard
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ENTRY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun NutritionScreen(
    onOpenSettings: () -> Unit = {},
    vm: NutritionViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editingEntry by remember { mutableStateOf<ProteinEntry?>(null) }
    var editingCreatine by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refreshDay()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        MockupScreenColumn(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScreenTitle("Nutrition")
                StatusChip("Today")
            }

            TodayProteinCard(
                state = state,
                onAdd = { grams, label ->
                    scope.launch {
                        vm.addProtein(grams, label)
                    }
                },
                onEdit = { editingEntry = it },
                onOpenSettings = onOpenSettings
            )

            state.creatineSettings?.let { settings ->
                CreatineCard(
                    settings = settings,
                    checkedDoseGrams = state.creatineCheck?.doseGrams,
                    onCheckedChange = { taken ->
                        scope.launch { vm.setCreatineTaken(taken) }
                    },
                    onEdit = { editingCreatine = true }
                )
            }

            ProteinHistoryCard(
                series = state.history,
                onRangeSelected = vm::selectHistoryRange
            )
        }
    }

    editingEntry?.let { entry ->
        EditProteinEntryDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { grams, label ->
                scope.launch {
                    vm.updateEntry(entry.id, grams, label)
                    editingEntry = null
                }
            },
            onDelete = {
                editingEntry = null
                scope.launch {
                    if (!vm.deleteEntry(entry.id)) return@launch
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted ${entry.label ?: "protein entry"}",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        vm.restoreEntry(entry)
                    }
                }
            }
        )
    }

    if (editingCreatine) {
        state.creatineSettings?.let { settings ->
            EditCreatineDialog(
                settings = settings,
                onDismiss = { editingCreatine = false },
                onSave = { dose, reminder ->
                    scope.launch {
                        vm.updateCreatine(dose, reminder)
                        editingCreatine = false
                    }
                }
            )
        }
    }
}

@Composable
private fun TodayProteinCard(
    state: NutritionUiState,
    onAdd: (Int, String?) -> Unit,
    onEdit: (ProteinEntry) -> Unit,
    onOpenSettings: () -> Unit
) {
    var gramsInput by remember(state.today) { mutableStateOf("") }
    var labelInput by remember(state.today) { mutableStateOf("") }
    val parsedGrams = gramsInput.toIntOrNull()
    val gramsInvalid = gramsInput.isNotEmpty() && (parsedGrams == null || parsedGrams <= 0)

    ThemedCard(accent = true) {
        SectionLabel("Today's protein")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BigValue("${state.totalProteinGrams} g")
            state.proteinTarget?.let {
                Text(
                    "/ ${it.grams} g",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val target = state.proteinTarget
        if (target == null) {
            Text(
                "Set a valid goal weight or protein override to see a daily target.",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onOpenSettings) { Text("Open Settings") }
        } else {
            ProgressPill(
                state.totalProteinGrams.toFloat() / target.grams.coerceAtLeast(1)
            )
            val difference = target.grams.toLong() - state.totalProteinGrams
            Text(
                when {
                    difference > 0 -> "$difference g remaining"
                    difference == 0L -> "Target reached"
                    else -> "${-difference} g over target"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = gramsInput,
                onValueChange = { gramsInput = it.filter(Char::isDigit) },
                label = { Text("Protein g") },
                isError = gramsInvalid,
                supportingText = if (gramsInvalid) {
                    { Text("Enter positive whole grams") }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.38f)
            )
            OutlinedTextField(
                value = labelInput,
                onValueChange = { labelInput = it },
                label = { Text("Meal label (optional)") },
                singleLine = true,
                modifier = Modifier.weight(0.62f)
            )
        }
        Button(
            enabled = parsedGrams != null && parsedGrams > 0,
            onClick = {
                onAdd(parsedGrams!!, labelInput)
                gramsInput = ""
                labelInput = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add protein")
        }

        if (state.todayEntries.isEmpty()) {
            Text(
                "No protein logged yet today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.todayEntries.forEachIndexed { index, entry ->
                if (index > 0) DividerLine()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(entry) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(entry.label ?: "Protein")
                        Text(
                            formatEntryTime(entry.loggedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${entry.grams} g  ›",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatineCard(
    settings: CreatineSettings,
    checkedDoseGrams: Int?,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    ThemedCard {
        SectionLabel("Creatine")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Checkbox(
                    checked = checkedDoseGrams != null,
                    onCheckedChange = onCheckedChange
                )
                Column {
                    Text(if (checkedDoseGrams == null) "Not taken yet" else "Taken today")
                    Text(
                        "${checkedDoseGrams ?: settings.doseGrams} g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onEdit) { Text("Edit creatine") }
        }
        Text(
            settings.reminderTime?.let {
                "Reminder ${it.format(ENTRY_TIME_FORMATTER)}"
            } ?: "Reminder off",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (checkedDoseGrams != null && checkedDoseGrams != settings.doseGrams) {
            Text(
                "Configured ${settings.doseGrams} g for the next check.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditProteinEntryDialog(
    entry: ProteinEntry,
    onDismiss: () -> Unit,
    onSave: (Int, String?) -> Unit,
    onDelete: () -> Unit
) {
    var gramsInput by remember(entry.id) { mutableStateOf(entry.grams.toString()) }
    var labelInput by remember(entry.id) { mutableStateOf(entry.label.orEmpty()) }
    val grams = gramsInput.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit protein entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gramsInput,
                    onValueChange = { gramsInput = it.filter(Char::isDigit) },
                    label = { Text("Protein g") },
                    isError = grams == null || grams <= 0,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text("Meal label (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = grams != null && grams > 0,
                onClick = { onSave(grams!!, labelInput) }
            ) { Text("Save") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun EditCreatineDialog(
    settings: CreatineSettings,
    onDismiss: () -> Unit,
    onSave: (Int, LocalTime?) -> Unit
) {
    var doseInput by remember(settings) { mutableStateOf(settings.doseGrams.toString()) }
    var reminderEnabled by remember(settings) {
        mutableStateOf(settings.reminderTime != null)
    }
    var hourInput by remember(settings) {
        mutableStateOf((settings.reminderTime?.hour ?: 8).toString().padStart(2, '0'))
    }
    var minuteInput by remember(settings) {
        mutableStateOf((settings.reminderTime?.minute ?: 0).toString().padStart(2, '0'))
    }
    val dose = doseInput.toIntOrNull()
    val hour = hourInput.toIntOrNull()
    val minute = minuteInput.toIntOrNull()
    val validTime = !reminderEnabled ||
        (hour != null && hour in 0..23 && minute != null && minute in 0..59)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit creatine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = doseInput,
                    onValueChange = { doseInput = it.filter(Char::isDigit) },
                    label = { Text("Daily dose (g)") },
                    isError = dose == null || dose <= 0,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    Checkbox(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                    Text("Daily reminder", modifier = Modifier.padding(top = 12.dp))
                }
                if (reminderEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hourInput,
                            onValueChange = { hourInput = it.filter(Char::isDigit).take(2) },
                            label = { Text("Hour") },
                            isError = hour == null || hour !in 0..23,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minuteInput,
                            onValueChange = { minuteInput = it.filter(Char::isDigit).take(2) },
                            label = { Text("Minute") },
                            isError = minute == null || minute !in 0..59,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = dose != null && dose > 0 && validTime,
                onClick = {
                    onSave(
                        dose!!,
                        if (reminderEnabled) LocalTime.of(hour!!, minute!!) else null
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatEntryTime(loggedAt: Long): String =
    Instant.ofEpochMilli(loggedAt)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(ENTRY_TIME_FORMATTER)
