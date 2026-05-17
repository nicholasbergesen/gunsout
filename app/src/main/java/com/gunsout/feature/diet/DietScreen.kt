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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DietScreen(
    onOpenMealPlans: () -> Unit = {},
    onOpenIngredients: () -> Unit = {},
    vm: DietViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()

    val totalKcal = state.todayEntries.sumOf { it.kcal }
    val totalProtein = state.todayEntries.sumOf { it.proteinG }
    val totalCarbs = state.todayEntries.sumOf { it.carbsG }
    val totalFat = state.todayEntries.sumOf { it.fatG }

    Column(
        Modifier.fillMaxWidth().padding(16.dp).verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Diet", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.material3.TextButton(onClick = onOpenMealPlans) { Text("Plans") }
                androidx.compose.material3.TextButton(onClick = onOpenIngredients) { Text("Ingredients") }
            }
        }
        state.activePlan?.let { plan ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    MacroRow("Calories", totalKcal.toDouble(), plan.kcalTarget.toDouble(), "kcal")
                    Spacer(Modifier.height(6.dp))
                    MacroRow("Protein", totalProtein, plan.proteinG.toDouble(), "g")
                    Spacer(Modifier.height(6.dp))
                    MacroRow("Carbs", totalCarbs, plan.carbsG.toDouble(), "g")
                    Spacer(Modifier.height(6.dp))
                    MacroRow("Fat", totalFat, plan.fatG.toDouble(), "g")
                }
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

        if (state.templates.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Quick log", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    state.templates.forEach { template ->
                        OutlinedButton(
                            onClick = { vm.logTemplate(template) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Text("${template.name} (${template.kcal} kcal, ${template.proteinG.toInt()}g P)")
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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(e.name)
                            Text("${e.kcal} kcal | ${e.proteinG.toInt()}g P")
                        }
                    }
                }
            }
        }
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
