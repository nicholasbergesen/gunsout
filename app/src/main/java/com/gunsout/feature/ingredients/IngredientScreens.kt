package com.gunsout.feature.ingredients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.gunsout.data.entity.IngredientUnit

@Composable
fun IngredientListScreen(
    onEdit: (Long) -> Unit,
    onCreate: () -> Unit,
    vm: IngredientListViewModel = hiltViewModel()
) {
    val ingredients by vm.ingredients.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New ingredient") }
            )
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Ingredients", style = MaterialTheme.typography.headlineMedium)
            if (ingredients.isEmpty()) Text("No ingredients yet.")
            ingredients.forEach { ing ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(ing.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${ing.kcalPer100g.toInt()} kcal | ${ing.proteinPer100g}g P / 100${ing.defaultUnit.name.lowercase()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(onClick = { onEdit(ing.id) }) { Text("Edit") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientEditScreen(
    ingredientId: Long,
    onBack: () -> Unit,
    vm: IngredientEditViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Column(
        Modifier.padding(16.dp).fillMaxWidth().verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (ingredientId > 0) "Edit ingredient" else "New ingredient",
            style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Look up via CalorieNinjas (optional)", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.lookupQuery,
                        onValueChange = vm::setLookupQuery,
                        label = { Text("Query (e.g. \"1 cup oats\")") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { vm.runLookup() }, enabled = !state.lookupBusy) {
                        Text(if (state.lookupBusy) "..." else "Look up")
                    }
                }
                state.lookupMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (state.pendingLookup != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.confirmApplyLookup() }) { Text("Apply lookup") }
                        TextButton(onClick = { vm.discardLookup() }) { Text("Keep my values") }
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.name, onValueChange = vm::setName,
            label = { Text("Name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = state.kcalPer100g, onValueChange = vm::setKcal,
                label = { Text("kcal / 100g") }, singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = state.proteinPer100g, onValueChange = vm::setProtein,
                label = { Text("Protein / 100g") }, singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = state.carbsPer100g, onValueChange = vm::setCarbs,
                label = { Text("Carbs / 100g") }, singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = state.fatPer100g, onValueChange = vm::setFat,
                label = { Text("Fat / 100g") }, singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            UnitDropdown(state.defaultUnit, vm::setDefaultUnit, modifier = Modifier.weight(1f))
            OutlinedTextField(value = state.gramsPerUnit, onValueChange = vm::setGramsPerUnit,
                label = { Text("Grams per unit") }, singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = state.defaultUnit != IngredientUnit.G)
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = vm::save, modifier = Modifier.fillMaxWidth(), enabled = state.name.isNotBlank()) {
            Text("Save")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
fun UnitDropdown(
    value: IngredientUnit,
    onChange: (IngredientUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Default unit") },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            enabled = false
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            IngredientUnit.values().forEach { u ->
                DropdownMenuItem(text = { Text(u.name) }, onClick = { onChange(u); expanded = false })
            }
        }
    }
}
