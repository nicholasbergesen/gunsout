package com.nicholasbergesen.gunsout.feature.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nicholasbergesen.gunsout.core.text.formatOneDecimalOrInt
import com.nicholasbergesen.gunsout.core.text.normalizeDecimalInput
import com.nicholasbergesen.gunsout.core.text.toNormalizedDoubleOrNull
import com.nicholasbergesen.gunsout.data.entity.MealType
import com.nicholasbergesen.gunsout.domain.nutrition.MacroTarget
import com.nicholasbergesen.gunsout.ui.components.BigValue
import com.nicholasbergesen.gunsout.ui.components.ChipButton
import com.nicholasbergesen.gunsout.ui.components.DividerLine
import com.nicholasbergesen.gunsout.ui.components.MockupScreenColumn
import com.nicholasbergesen.gunsout.ui.components.ProgressPill
import com.nicholasbergesen.gunsout.ui.components.ScreenTitle
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.StatusChip
import com.nicholasbergesen.gunsout.ui.components.ThemedCard
import com.nicholasbergesen.gunsout.ui.components.WrappingRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    var editing by remember { mutableStateOf<com.nicholasbergesen.gunsout.data.entity.FoodEntry?>(null) }
    var reminderEditing by remember { mutableStateOf<com.nicholasbergesen.gunsout.data.entity.Supplement?>(null) }
    var addMealOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val totalKcal = state.todayEntries.sumOf { it.kcal }
    val totalProtein = state.todayEntries.sumOf { it.proteinG }
    val totalCarbs = state.todayEntries.sumOf { it.carbsG }
    val totalFat = state.todayEntries.sumOf { it.fatG }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        MockupScreenColumn(modifier = Modifier.padding(inner).verticalScroll(scroll)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScreenTitle("Diet")
                StatusChip("Today")
            }
            DailyTargetCard(
                target = state.target,
                totalKcal = totalKcal,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat
            )

            if (state.supplements.isNotEmpty()) {
                state.supplements.forEach { sup ->
                    val taken = sup.id in state.supplementsTakenToday
                    ThemedCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("${sup.name} ${sup.defaultDose.g} ${sup.unit.name.lowercase()}")
                                Text(
                                    sup.takeWith?.takeIf { it.isNotBlank() } ?: "Daily supplement",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                sup.reminderTime?.let { rt ->
                                    Text("Reminder $rt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            ChipButton(
                                text = if (taken) "Taken" else "Mark taken",
                                selected = taken,
                                onClick = { if (!taken) vm.toggleSupplement(sup) }
                            )
                        }
                        TextButton(onClick = { reminderEditing = sup }) {
                            Text(if (sup.reminderTime == null) "Set reminder" else "Edit reminder")
                        }
                    }
                }
            }

            if (state.templates.isNotEmpty()) {
                ThemedCard {
                    SectionLabel("Quick log")
                    Text("Tap for 1x, use the multiplier for half or double.", style = MaterialTheme.typography.bodySmall)
                    state.templates.forEach { template ->
                        var menuOpen by remember(template.id) { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { vm.logTemplate(template, 1.0) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${template.name} (${template.kcal} kcal)")
                                }
                                TextButton(onClick = { menuOpen = true }) { Text("x") }
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

            if (state.todayEntries.isNotEmpty()) {
                ThemedCard {
                    SectionLabel("Meals")
                    state.todayEntries.forEachIndexed { index, e ->
                        if (index > 0) DividerLine()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(e.name)
                                Text(
                                    "${e.proteinG.toInt()}P | ${e.carbsG.toInt()}C | ${e.fatG.toInt()}F",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { editing = e }) { Text("${e.kcal}") }
                        }
                    }
                }
            }

            Button(onClick = { addMealOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text("+ Add meal")
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
        if (addMealOpen) {
            AddMealSheet(
                onDismiss = { addMealOpen = false },
                onSubmit = { name, mealType, kcal, p, c, f, saveAsTemplate ->
                    vm.addMeal(name, mealType, kcal, p, c, f, saveAsTemplate)
                    addMealOpen = false
                }
            )
        }
    }
}

@Composable
private fun DailyTargetCard(
    target: MacroTarget?,
    totalKcal: Int,
    totalProtein: Double,
    totalCarbs: Double,
    totalFat: Double
) {
    ThemedCard(accent = true) {
        if (target == null) {
            SectionLabel("Calories")
            BigValue(totalKcal.toString())
            Text("Add your profile in Settings to see suggested daily macros.", style = MaterialTheme.typography.bodySmall)
        } else {
            SectionLabel("Calories")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BigValue(totalKcal.toString())
                Text("/ ${target.kcal} kcal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ProgressPill(totalKcal.toFloat() / target.kcal.coerceAtLeast(1))
            MacroRow("Protein", totalProtein, target.proteinG.toDouble(), "g")
            MacroRow("Carbs", totalCarbs, target.carbsG.toDouble(), "g")
            MacroRow("Fat", totalFat, target.fatG.toDouble(), "g")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMealSheet(
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        mealType: MealType,
        kcal: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        saveAsTemplate: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MealType.SNACK) }
    var saveAsTemplate by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.ime }
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Add a meal", style = MaterialTheme.typography.titleLarge)
            Text(
                "Log a meal directly by name and macros, like \"burger\" 500 kcal / 30P / 40C / 25F.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Meal name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            WrappingRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MealType.values().forEach { t ->
                    FilterChip(
                        selected = mealType == t,
                        onClick = { mealType = t },
                        label = {
                            Text(
                                t.name.lowercase().replaceFirstChar { it.uppercase() },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
            OutlinedTextField(
                value = kcal,
                onValueChange = { kcal = it.filter(Char::isDigit) },
                label = { Text("Calories (kcal)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it.normalizeDecimalInput() },
                    label = { Text("Protein g") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it.normalizeDecimalInput() },
                    label = { Text("Carbs g") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it.normalizeDecimalInput() },
                    label = { Text("Fat g") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = saveAsTemplate, onCheckedChange = { saveAsTemplate = it })
                Text("Save as quick-log template")
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.height(0.dp))
                Button(
                    enabled = name.isNotBlank() && kcal.toIntOrNull() != null,
                    onClick = {
                        onSubmit(
                            name,
                            mealType,
                            kcal.toIntOrNull() ?: 0,
                            protein.toNormalizedDoubleOrNull() ?: 0.0,
                            carbs.toNormalizedDoubleOrNull() ?: 0.0,
                            fat.toNormalizedDoubleOrNull() ?: 0.0,
                            saveAsTemplate
                        )
                    }
                ) { Text("Add") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            Button(onClick = { onSave(pickerState.hour, pickerState.minute) }) {
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
    entry: com.nicholasbergesen.gunsout.data.entity.FoodEntry,
    onDismiss: () -> Unit,
    onSave: (com.nicholasbergesen.gunsout.data.entity.FoodEntry) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(entry.id) { mutableStateOf(entry.name) }
    var kcal by remember(entry.id) { mutableStateOf(entry.kcal.toString()) }
    var protein by remember(entry.id) { mutableStateOf(entry.proteinG.toString()) }
    var carbs by remember(entry.id) { mutableStateOf(entry.carbsG.toString()) }
    var fat by remember(entry.id) { mutableStateOf(entry.fatG.toString()) }
    var confirmDelete by remember(entry.id) { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = kcal, onValueChange = { kcal = it.filter(Char::isDigit) }, label = { Text("kcal") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = protein, onValueChange = { protein = it.normalizeDecimalInput() }, label = { Text("Protein g") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = carbs, onValueChange = { carbs = it.normalizeDecimalInput() }, label = { Text("Carbs g") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fat, onValueChange = { fat = it.normalizeDecimalInput() }, label = { Text("Fat g") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(entry.copy(
                    name = name.trim().ifBlank { entry.name },
                    kcal = kcal.toIntOrNull() ?: entry.kcal,
                    proteinG = protein.toNormalizedDoubleOrNull() ?: entry.proteinG,
                    carbsG = carbs.toNormalizedDoubleOrNull() ?: entry.carbsG,
                    fatG = fat.toNormalizedDoubleOrNull() ?: entry.fatG
                ))
            }) { Text("Save") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("This will permanently remove \"${entry.name}\" from today's log.") },
            confirmButton = {
                Button(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
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
    ProgressPill(progress = pct)
}

private val Double.g: String
    get() = formatOneDecimalOrInt(this)
