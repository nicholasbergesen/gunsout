package com.gunsout.feature.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

@Composable
fun DietScreen(
    vm: DietViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refreshDay()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var editing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.gunsout.data.entity.FoodEntry?>(null) }
    var reminderEditing by remember { mutableStateOf<com.gunsout.data.entity.Supplement?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val totalKcal = state.todayEntries.sumOf { it.kcal }
    val totalProtein = state.todayEntries.sumOf { it.proteinG }
    val totalCarbs = state.todayEntries.sumOf { it.carbsG }
    val totalFat = state.todayEntries.sumOf { it.fatG }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { inner ->
    Column(
        Modifier.fillMaxWidth().padding(inner).padding(16.dp).verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Diet", style = MaterialTheme.typography.headlineMedium)
        }
        // TODO Phase 3: render a "Daily targets" card backed by MacroTargetCalculator + the
        // UserPreferences overrideKcal/Protein/Carbs/Fat fields. For now, show today's totals
        // without a target comparison so the screen stays useful between phases.
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Today's macros", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("${totalKcal} kcal | ${totalProtein.toInt()}g P | ${totalCarbs.toInt()}g C | ${totalFat.toInt()}g F")
            }
        }

        if (state.supplements.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Today's supplements", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    state.supplements.forEach { sup ->
                        val taken = sup.id in state.supplementsTakenToday
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${sup.name} (${sup.defaultDose} ${sup.unit.name.lowercase()})")
                                sup.takeWith?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                                sup.reminderTime?.let { rt ->
                                    Text("Reminder ${rt}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                androidx.compose.material3.TextButton(onClick = { reminderEditing = sup }) {
                                    Text(if (sup.reminderTime == null) "Set reminder" else "Edit")
                                }
                                AssistChip(
                                    onClick = { if (!taken) vm.toggleSupplement(sup) },
                                    label = { Text(if (taken) "Taken" else "Mark taken") }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.templates.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Quick log", style = MaterialTheme.typography.titleMedium)
                    Text("Tap for 1x, long-press for half or double.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    state.templates.forEach { template ->
                        var menuOpen by remember(template.id) { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { vm.logTemplate(template, 1.0) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${template.name} (${template.kcal} kcal)")
                                }
                                androidx.compose.material3.TextButton(onClick = { menuOpen = true }) { Text("×") }
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false }
                            ) {
                                listOf(0.5, 1.0, 1.5, 2.0).forEach { mul ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("${mul}x = ${(template.kcal * mul).toInt()} kcal") },
                                        onClick = {
                                            vm.logTemplate(template, mul)
                                            menuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.todayEntries.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Today", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    state.todayEntries.forEach { e ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(e.name)
                                Text("${e.kcal} kcal | ${e.proteinG.toInt()}g P", style = MaterialTheme.typography.bodySmall)
                            }
                            androidx.compose.material3.TextButton(onClick = { editing = e }) { Text("Edit") }
                        }
                    }
                }
            }
        }
    }

    editing?.let { entry ->
        FoodEntryEditDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { updated -> vm.updateEntry(updated); editing = null },
            onDelete = {
                vm.deleteEntry(entry.id)
                editing = null
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted \"${entry.name}\"",
                        actionLabel = "Undo",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        vm.restoreEntry(entry)
                    }
                }
            }
        )
    }
    reminderEditing?.let { sup ->
        ReminderTimeDialog(
            current = sup.reminderTime,
            onDismiss = { reminderEditing = null },
            onClear = { vm.setReminder(sup.id, null); reminderEditing = null },
            onSave = { hour, minute ->
                vm.setReminder(sup.id, java.time.LocalTime.of(hour, minute))
                reminderEditing = null
            }
        )
    }

    } // Scaffold lambda
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    current: java.time.LocalTime?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    val pickerState = androidx.compose.material3.rememberTimePickerState(
        initialHour = current?.hour ?: 8,
        initialMinute = current?.minute ?: 0
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily reminder") },
        text = {
            Column {
                androidx.compose.material3.TimePicker(state = pickerState)
                Text(
                    "Inexact daily reminder. Notifications can be silenced via system Settings if needed.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = { onSave(pickerState.hour, pickerState.minute) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun FoodEntryEditDialog(
    entry: com.gunsout.data.entity.FoodEntry,
    onDismiss: () -> Unit,
    onSave: (com.gunsout.data.entity.FoodEntry) -> Unit,
    onDelete: () -> Unit
) {
    var name by androidx.compose.runtime.remember(entry.id) { androidx.compose.runtime.mutableStateOf(entry.name) }
    var kcal by androidx.compose.runtime.remember(entry.id) { androidx.compose.runtime.mutableStateOf(entry.kcal.toString()) }
    var protein by androidx.compose.runtime.remember(entry.id) { androidx.compose.runtime.mutableStateOf(entry.proteinG.toString()) }
    var carbs by androidx.compose.runtime.remember(entry.id) { androidx.compose.runtime.mutableStateOf(entry.carbsG.toString()) }
    var fat by androidx.compose.runtime.remember(entry.id) { androidx.compose.runtime.mutableStateOf(entry.fatG.toString()) }
    var confirmDelete by androidx.compose.runtime.remember(entry.id) { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(value = kcal, onValueChange = { kcal = it.filter(Char::isDigit) }, label = { Text("kcal") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                androidx.compose.material3.OutlinedTextField(value = protein, onValueChange = { protein = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Protein g") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(value = carbs, onValueChange = { carbs = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Carbs g") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(value = fat, onValueChange = { fat = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Fat g") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = {
                onSave(entry.copy(
                    name = name.trim().ifBlank { entry.name },
                    kcal = kcal.toIntOrNull() ?: entry.kcal,
                    proteinG = protein.toDoubleOrNull() ?: entry.proteinG,
                    carbsG = carbs.toDoubleOrNull() ?: entry.carbsG,
                    fatG = fat.toDoubleOrNull() ?: entry.fatG
                ))
            }) { Text("Save") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("This will permanently remove \"${entry.name}\" from today's log.") },
            confirmButton = {
                androidx.compose.material3.Button(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MacroRow(label: String, value: Double, target: Double, unit: String) {
    val pct = (value / target.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            "${value.toInt()} / ${target.toInt()} $unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
    Spacer(Modifier.height(2.dp))
    LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
}
