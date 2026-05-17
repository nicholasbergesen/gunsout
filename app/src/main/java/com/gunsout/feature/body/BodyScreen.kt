package com.gunsout.feature.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gunsout.data.entity.BodyMetricsLog

@Composable
fun BodyScreen(vm: BodyViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()

    var weight by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }
    var water by remember { mutableStateOf("") }
    var bone by remember { mutableStateOf("") }
    var visceral by remember { mutableStateOf("") }
    var showMore by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().padding(16.dp).verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Body", style = MaterialTheme.typography.headlineMedium)

        val latest = state.logs.lastOrNull()
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Latest", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("Current: ${"%.1f".format(state.profile.currentBodyWeightKg)} kg")
                Text("Goal: ${"%.1f".format(state.profile.goalBodyWeightKg)} kg")
                latest?.bodyFatPct?.let { Text("Body fat: ${"%.1f".format(it)} %") }
                latest?.muscleMassKg?.let { Text("Muscle: ${"%.1f".format(it)} kg") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Log today", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Weight (kg) - required") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showMore = !showMore }) {
                    Text(if (showMore) "Hide more metrics" else "More metrics")
                }
                if (showMore) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = bodyFat, onValueChange = { bodyFat = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Body fat %") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = muscle, onValueChange = { muscle = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Muscle mass (kg)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = water, onValueChange = { water = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Water %") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bone, onValueChange = { bone = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Bone mass (kg)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = visceral, onValueChange = { visceral = it.filter(Char::isDigit) }, label = { Text("Visceral fat rating") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val w = weight.toDoubleOrNull() ?: return@Button
                        vm.logToday(
                            weightKg = w,
                            bodyFatPct = bodyFat.toDoubleOrNull(),
                            muscleMassKg = muscle.toDoubleOrNull(),
                            waterPct = water.toDoubleOrNull(),
                            boneMassKg = bone.toDoubleOrNull(),
                            visceralFatRating = visceral.toIntOrNull()
                        )
                        weight = ""; bodyFat = ""; muscle = ""; water = ""; bone = ""; visceral = ""
                        showMore = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = weight.toDoubleOrNull() != null
                ) { Text("Log") }
            }
        }

        if (state.logs.size >= 2) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Weight trend", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    WeightChart(
                        logs = state.logs,
                        goalKg = state.profile.goalBodyWeightKg
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Auto-adjust kcal target", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Reads your recent weigh-ins and suggests a kcal change to keep you on track for your goal.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.suggestKcalAdjustment() }) { Text("Suggest now") }
                val suggestion by vm.kcalSuggestion.collectAsState()
                suggestion?.let { s ->
                    Spacer(Modifier.height(8.dp))
                    Text(s.text, style = MaterialTheme.typography.bodyMedium)
                    if (s.newKcalTarget != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.applyKcalSuggestion() }) { Text("Apply ${s.newKcalTarget} kcal") }
                            androidx.compose.material3.TextButton(onClick = { vm.dismissKcalSuggestion() }) { Text("Dismiss") }
                        }
                    } else {
                        androidx.compose.material3.TextButton(onClick = { vm.dismissKcalSuggestion() }) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightChart(logs: List<BodyMetricsLog>, goalKg: Double) {
    if (logs.size < 2) return
    val weights = logs.map { it.weightKg }
    val minW = (weights.min() - 2).coerceAtMost(goalKg - 2)
    val maxW = (weights.max() + 2).coerceAtLeast(goalKg + 2)
    val range = (maxW - minW).coerceAtLeast(1.0)
    val primary = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
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
        // Goal line
        val goalY = h - ((goalKg - minW) / range * h).toFloat()
        drawLine(color = goalColor, start = Offset(0f, goalY), end = Offset(w, goalY), strokeWidth = 2f)
    }
}
