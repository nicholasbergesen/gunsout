package com.gunsout.feature.mealplan

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gunsout.data.entity.MacroSource
import com.gunsout.data.entity.MealType

@Composable
fun MealPlanListScreen(
    onEdit: (Long) -> Unit,
    vm: MealPlanListViewModel = hiltViewModel()
) {
    val plans by vm.plans.collectAsState()
    var newDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { newDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New plan") }
            )
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Meal plans", style = MaterialTheme.typography.headlineMedium)
            if (plans.isEmpty()) Text("No meal plans yet.")
            plans.forEach { plan ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(plan.name, style = MaterialTheme.typography.titleSmall)
                            if (plan.isActive) AssistChip(onClick = {}, label = { Text("Active") })
                        }
                        Text("${plan.kcalTarget} kcal | ${plan.proteinG}g P", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onEdit(plan.id) }) { Text("Edit") }
                            if (!plan.isActive) OutlinedButton(onClick = { vm.activate(plan.id) }) { Text("Activate") }
                            TextButton(onClick = { vm.duplicate(plan.id, "${plan.name} (copy)") }) { Text("Duplicate") }
                        }
                    }
                }
            }
        }
    }

    if (newDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newDialog = false },
            title = { Text("New meal plan") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }) },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) vm.create(name) { id -> newDialog = false; onEdit(id) }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { newDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun MealPlanEditScreen(
    planId: Long,
    onEditTemplate: (Long) -> Unit,
    onBack: () -> Unit,
    vm: MealPlanEditViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()
    val plan = state.plan ?: run {
        Text("Plan not found.", modifier = Modifier.padding(16.dp))
        return
    }

    var kcal by remember(plan.id) { mutableStateOf(plan.kcalTarget.toString()) }
    var protein by remember(plan.id) { mutableStateOf(plan.proteinG.toString()) }
    var carbs by remember(plan.id) { mutableStateOf(plan.carbsG.toString()) }
    var fat by remember(plan.id) { mutableStateOf(plan.fatG.toString()) }

    Column(
        Modifier.padding(16.dp).fillMaxWidth().verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(plan.name, style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Targets", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = kcal, onValueChange = { kcal = it.filter(Char::isDigit) },
                        label = { Text("kcal") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = protein, onValueChange = { protein = it.filter(Char::isDigit) },
                        label = { Text("Protein g") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it.filter(Char::isDigit) },
                        label = { Text("Carbs g") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = fat, onValueChange = { fat = it.filter(Char::isDigit) },
                        label = { Text("Fat g") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Button(onClick = {
                    vm.setTargets(
                        kcal = kcal.toIntOrNull() ?: plan.kcalTarget,
                        protein = protein.toIntOrNull() ?: plan.proteinG,
                        carbs = carbs.toIntOrNull() ?: plan.carbsG,
                        fat = fat.toIntOrNull() ?: plan.fatG
                    )
                }) { Text("Save targets") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Templates", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(4.dp))
                if (state.templates.isEmpty()) Text("No templates yet.")
                state.templates.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${t.name} (${t.mealType.name.lowercase()})")
                            Text(
                                "${t.kcal} kcal | ${t.proteinG.toInt()}g P | source: ${t.macroSource.name}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(onClick = { onEditTemplate(t.id) }) { Text("Edit") }
                    }
                }
            }
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
fun MealTemplateEditScreen(
    templateId: Long,
    onBack: () -> Unit,
    vm: MealTemplateEditViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val ingredients by vm.ingredients.collectAsState()
    val template = state.template ?: run {
        Text("Template not found.", modifier = Modifier.padding(16.dp))
        return
    }
    val scroll = rememberScrollState()

    var name by remember(template.id) { mutableStateOf(template.name) }
    var mealType by remember(template.id) { mutableStateOf(template.mealType) }
    var manualKcal by remember(template.id) { mutableStateOf(template.kcal.toString()) }
    var manualProtein by remember(template.id) { mutableStateOf(template.proteinG.toString()) }
    var manualCarbs by remember(template.id) { mutableStateOf(template.carbsG.toString()) }
    var manualFat by remember(template.id) { mutableStateOf(template.fatG.toString()) }

    var pickerOpen by remember { mutableStateOf(false) }

    Column(
        Modifier.padding(16.dp).fillMaxWidth().verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(template.name, style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MealType.values().forEach { mt ->
                FilterChip(selected = mealType == mt, onClick = { mealType = mt }, label = { Text(mt.name.lowercase()) })
            }
        }
        Button(onClick = { vm.setName(name, mealType) }) { Text("Save name/type") }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Macro source", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = template.macroSource == MacroSource.FROM_INGREDIENTS,
                        onClick = { vm.setMacroSource(MacroSource.FROM_INGREDIENTS) },
                        label = { Text("From ingredients") }
                    )
                    FilterChip(
                        selected = template.macroSource == MacroSource.MANUAL,
                        onClick = { vm.setMacroSource(MacroSource.MANUAL) },
                        label = { Text("Manual") }
                    )
                }
            }
        }

        if (template.macroSource == MacroSource.MANUAL) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = manualKcal, onValueChange = { manualKcal = it.filter(Char::isDigit) }, label = { Text("kcal") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = manualProtein, onValueChange = { manualProtein = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Protein g") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = manualCarbs, onValueChange = { manualCarbs = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Carbs g") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = manualFat, onValueChange = { manualFat = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Fat g") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        vm.setManualMacros(
                            kcal = manualKcal.toIntOrNull() ?: template.kcal,
                            protein = manualProtein.toDoubleOrNull() ?: template.proteinG,
                            carbs = manualCarbs.toDoubleOrNull() ?: template.carbsG,
                            fat = manualFat.toDoubleOrNull() ?: template.fatG
                        )
                    }) { Text("Save macros") }
                }
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Ingredients", style = MaterialTheme.typography.titleSmall)
                    state.rows.forEach { row ->
                        val ing = state.ingredients[row.ingredientId]
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${ing?.name ?: "(?)"}")
                            Text("${row.quantity} ${row.unit.name.lowercase()}")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val totals = vm.computedTotals()
                    Text("Totals: ${totals.kcal.toInt()} kcal | ${"%.1f".format(totals.protein)}g P | ${"%.1f".format(totals.carbs)}g C | ${"%.1f".format(totals.fat)}g F",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { pickerOpen = true }) { Text("+ Add ingredient") }
                }
            }
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }

    if (pickerOpen) {
        IngredientPickerDialog(
            ingredients = ingredients,
            onDismiss = { pickerOpen = false },
            onPick = { id, qty, unit ->
                vm.addIngredient(id, qty, unit)
                pickerOpen = false
            }
        )
    }
}

@Composable
private fun IngredientPickerDialog(
    ingredients: List<com.gunsout.data.entity.Ingredient>,
    onDismiss: () -> Unit,
    onPick: (Long, Double, com.gunsout.data.entity.IngredientUnit) -> Unit
) {
    var selected by remember { mutableStateOf<com.gunsout.data.entity.Ingredient?>(null) }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(com.gunsout.data.entity.IngredientUnit.G) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ingredient") },
        text = {
            Column(modifier = Modifier.height(420.dp)) {
                val current = selected
                if (current == null) {
                    LazyColumn {
                        items(items = ingredients, key = { it.id }) { ingredient ->
                            TextButton(
                                onClick = { selected = ingredient; unit = ingredient.defaultUnit },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(ingredient.name)
                            }
                        }
                    }
                } else {
                    Text("Selected: ${current.name}")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Quantity") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    com.gunsout.feature.ingredients.UnitDropdown(value = unit, onChange = { unit = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = selected ?: return@Button
                    val q = quantity.toDoubleOrNull() ?: return@Button
                    onPick(s.id, q, unit)
                },
                enabled = selected != null && quantity.toDoubleOrNull() != null
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
