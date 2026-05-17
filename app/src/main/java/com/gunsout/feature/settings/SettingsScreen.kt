package com.gunsout.feature.settings

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.data.prefs.UserPreferences
import com.gunsout.data.prefs.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferences
) : ViewModel() {
    val profile: StateFlow<UserProfile> = userPrefs.profile.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile()
    )

    fun save(
        currentWeight: Double,
        goalWeight: Double,
        kneeInjury: Boolean,
        baselineWeek: Boolean
    ) = viewModelScope.launch {
        userPrefs.update {
            it.copy(
                currentBodyWeightKg = currentWeight,
                goalBodyWeightKg = goalWeight,
                kneeInjuryFlag = kneeInjury,
                baselineWeekActive = baselineWeek
            )
        }
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val profile by vm.profile.collectAsState()
    val scroll = rememberScrollState()

    var currentWeight by remember(profile) { mutableStateOf(profile.currentBodyWeightKg.toString()) }
    var goalWeight by remember(profile) { mutableStateOf(profile.goalBodyWeightKg.toString()) }
    var kneeInjury by remember(profile) { mutableStateOf(profile.kneeInjuryFlag) }
    var baselineWeek by remember(profile) { mutableStateOf(profile.baselineWeekActive) }

    Column(
        Modifier.fillMaxWidth().padding(16.dp).verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Body goals", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentWeight,
                    onValueChange = { currentWeight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Current weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = goalWeight,
                    onValueChange = { goalWeight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Goal weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SettingsToggle(
                    label = "Knee injury caution",
                    description = "Prompts knee-feel on leg sessions and shows knee notes on relevant exercises.",
                    checked = kneeInjury,
                    onCheckedChange = { kneeInjury = it }
                )
                Spacer(Modifier.height(8.dp))
                SettingsToggle(
                    label = "Baseline week active",
                    description = "While on, progression suggestions are suppressed. Turn off after week 1.",
                    checked = baselineWeek,
                    onCheckedChange = { baselineWeek = it }
                )
            }
        }

        Button(
            onClick = {
                vm.save(
                    currentWeight = currentWeight.toDoubleOrNull() ?: profile.currentBodyWeightKg,
                    goalWeight = goalWeight.toDoubleOrNull() ?: profile.goalBodyWeightKg,
                    kneeInjury = kneeInjury,
                    baselineWeek = baselineWeek
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
