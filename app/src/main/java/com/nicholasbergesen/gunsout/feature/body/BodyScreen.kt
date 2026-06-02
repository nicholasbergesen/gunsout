package com.nicholasbergesen.gunsout.feature.body

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.ui.components.BigValue
import com.nicholasbergesen.gunsout.ui.components.MetricGrid
import com.nicholasbergesen.gunsout.ui.components.MetricItem
import com.nicholasbergesen.gunsout.ui.components.MockupScreenColumn
import com.nicholasbergesen.gunsout.ui.components.ScreenTitle
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.StatusChip
import com.nicholasbergesen.gunsout.ui.components.ThemedCard

@Composable
fun BodyScreen(
    vm: BodyViewModel = hiltViewModel(),
    onScanInBody: () -> Unit = {},
    scannedInBodyQrText: String? = null,
    onScannedInBodyQrConsumed: () -> Unit = {}
) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var weight by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }
    var water by remember { mutableStateOf("") }
    var visceral by remember { mutableStateOf("") }
    var showMore by remember { mutableStateOf(false) }
    var selectedTrend by remember { mutableStateOf("Weight") }

    LaunchedEffect(scannedInBodyQrText) {
        val rawValue = scannedInBodyQrText
        if (rawValue != null) {
            onScannedInBodyQrConsumed()
            if (rawValue.isNotBlank()) {
                vm.importInBodyQr(rawValue)
            }
        }
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is BodyUiEvent.InBodyImported -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        vm.undoInBodyImport(event.undo)
                    }
                }
                is BodyUiEvent.Message -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        MockupScreenColumn(modifier = Modifier.padding(inner).verticalScroll(scroll)) {
        val latest = state.logs.lastOrNull()
        val latestWeight = latest?.weightKg ?: state.profile.currentBodyWeightKg
        val trendSeries = listOf(
            "Weight" to state.logs.map { it.date to it.weightKg },
            "Body fat" to state.logs.mapNotNull { row -> row.bodyFatPct?.let { row.date to it } },
            "Muscle" to state.logs.mapNotNull { row -> row.muscleMassKg?.let { row.date to it } },
            "Water" to state.logs.mapNotNull { row -> row.waterLiters?.let { row.date to it } }
        ).filter { it.second.size >= 2 }
        val activeTrend = trendSeries.firstOrNull { it.first == selectedTrend } ?: trendSeries.firstOrNull()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ScreenTitle("Body")
            StatusChip(selectedTrend)
        }

        ThemedCard(accent = true) {
            SectionLabel("Latest weight")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BigValue("${"%.1f".format(latestWeight)} kg")
                latest?.let {
                    StatusChip("Goal ${"%.0f".format(state.profile.goalBodyWeightKg)}", selected = true)
                }
            }
            if (activeTrend != null) {
                DateAxisLineChart(
                    points = activeTrend.second,
                    goalValue = if (activeTrend.first == "Weight") state.profile.goalBodyWeightKg else null
                )
            } else {
                Text("Add another weigh-in to show the trend chart.", style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("14-day trend", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                activeTrend?.let { trend ->
                    Text(
                        if (trend.first == "Weight") {
                            "goal ${"%.0f".format(state.profile.goalBodyWeightKg)} kg"
                        } else {
                            trend.first
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Weight", "Body fat", "Muscle", "Water").forEach { label ->
                val enabled = trendSeries.any { it.first == label }
                val modifier = Modifier.weight(1f)
                if (activeTrend?.first == label && enabled) {
                    Button(onClick = { selectedTrend = label }, modifier = modifier) { Text(label) }
                } else {
                    OutlinedButton(
                        onClick = { selectedTrend = label },
                        modifier = modifier,
                        enabled = enabled
                    ) { Text(label) }
                }
            }
        }

        MetricGrid(
            items = listOf(
                MetricItem("Body fat", latest?.bodyFatPct?.let { "${"%.1f".format(it)}%" } ?: "-"),
                MetricItem("Muscle", latest?.muscleMassKg?.let { "%.1f".format(it) } ?: "-"),
                MetricItem("Water", latest?.waterLiters?.let { "${"%.1f".format(it)} L" } ?: "-")
            )
        )

        ThemedCard {
            SectionLabel("Log today")
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Weight (kg) - required") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { showMore = !showMore },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showMore) "Hide more metrics" else "Show more metrics")
            }
            OutlinedButton(
                onClick = onScanInBody,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import from InBody QR")
            }
            if (showMore) {
                OutlinedTextField(value = bodyFat, onValueChange = { bodyFat = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Body fat %") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = muscle, onValueChange = { muscle = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Muscle mass (kg)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = water, onValueChange = { water = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Water (L)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = visceral, onValueChange = { visceral = it.filter(Char::isDigit) }, label = { Text("Visceral fat rating") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }

        Button(
            onClick = {
                val w = weight.toDoubleOrNull() ?: return@Button
                vm.logToday(
                    weightKg = w,
                    bodyFatPct = bodyFat.toDoubleOrNull(),
                    muscleMassKg = muscle.toDoubleOrNull(),
                    waterLiters = water.toDoubleOrNull(),
                    visceralFatRating = visceral.toIntOrNull()
                )
                weight = ""; bodyFat = ""; muscle = ""; water = ""; visceral = ""
                showMore = false
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = weight.toDoubleOrNull() != null
        ) { Text("Log today") }

        ThemedCard {
            SectionLabel("Auto-adjust kcal target")
            Text(
                "Reads your recent weigh-ins and suggests a kcal change to keep you on track for your goal.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = { vm.suggestKcalAdjustment() }) { Text("Suggest now") }
            val suggestion by vm.kcalSuggestion.collectAsState()
            suggestion?.let { s ->
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
private fun BodyTrendChart(logs: List<BodyMetricsLog>, goalKg: Double) {
    if (logs.size < 2) return

    val seriesOptions = listOf(
        "Weight (kg)" to logs.map { it.date to it.weightKg },
        "Body fat %" to logs.mapNotNull { row -> row.bodyFatPct?.let { row.date to it } },
        "Muscle (kg)" to logs.mapNotNull { row -> row.muscleMassKg?.let { row.date to it } },
        "Water (L)" to logs.mapNotNull { row -> row.waterLiters?.let { row.date to it } }
    ).filter { it.second.size >= 2 }

    var selected by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(seriesOptions.first().first) }
    val current = seriesOptions.firstOrNull { it.first == selected } ?: seriesOptions.first()

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            seriesOptions.forEach { (label, _) ->
                androidx.compose.material3.FilterChip(
                    selected = selected == label,
                    onClick = { selected = label },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        DateAxisLineChart(
            points = current.second,
            goalValue = if (current.first == "Weight (kg)") goalKg else null
        )
    }
}

@Composable
private fun DateAxisLineChart(
    points: List<Pair<java.time.LocalDate, Double>>,
    goalValue: Double?
) {
    if (points.size < 2) return
    val minDate = points.first().first
    val maxDate = points.last().first
    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(minDate, maxDate).coerceAtLeast(1L).toFloat()

    val values = points.map { it.second }
    val minVal = minOf(values.min() - (values.max() - values.min()) * 0.1, goalValue ?: values.min())
    val maxVal = maxOf(values.max() + (values.max() - values.min()) * 0.1, goalValue ?: values.max())
    val range = (maxVal - minVal).coerceAtLeast(0.01)

    val primary = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.tertiary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        // Axis label row above the canvas.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${"%.1f".format(maxVal)}", style = MaterialTheme.typography.labelSmall, color = muted)
            Text("→", style = MaterialTheme.typography.labelSmall, color = muted)
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val w = size.width
            val h = size.height
            val path = Path()
            points.forEachIndexed { i, (date, value) ->
                val daysIn = java.time.temporal.ChronoUnit.DAYS.between(minDate, date).toFloat()
                val x = w * (daysIn / totalDays)
                val y = h - ((value - minVal) / range * h).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = primary, style = Stroke(width = 4f))
            goalValue?.let { gv ->
                val goalY = h - ((gv - minVal) / range * h).toFloat()
                drawLine(color = goalColor, start = Offset(0f, goalY), end = Offset(w, goalY), strokeWidth = 2f)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${"%.1f".format(minVal)}", style = MaterialTheme.typography.labelSmall, color = muted)
            Text("${minDate}  →  ${maxDate}", style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}
