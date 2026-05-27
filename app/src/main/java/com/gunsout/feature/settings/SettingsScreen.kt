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
import androidx.compose.material3.OutlinedButton
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
import com.gunsout.auth.AuthRepository
import com.gunsout.auth.AuthUser
import com.gunsout.auth.CurrentUserIdProvider
import com.gunsout.data.prefs.ActivityLevel
import com.gunsout.data.prefs.GoalType
import com.gunsout.data.prefs.MacroOverrides
import com.gunsout.data.prefs.Sex
import com.gunsout.data.prefs.UserPreferences
import com.gunsout.data.prefs.UserProfile
import com.gunsout.domain.nutrition.MacroTarget
import com.gunsout.domain.nutrition.MacroTargetCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferences,
    private val backupManager: com.gunsout.data.backup.BackupManager,
    private val currentUserIdProvider: CurrentUserIdProvider,
    private val authRepository: AuthRepository,
    private val reminderScheduler: com.gunsout.feature.supplements.SupplementReminderScheduler
) : ViewModel() {
    val profile: StateFlow<UserProfile> = userPrefs.profile.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile()
    )

    val overrides: StateFlow<MacroOverrides> = userPrefs.overrides.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), MacroOverrides()
    )

    val target: StateFlow<MacroTarget?> = kotlinx.coroutines.flow.combine(
        userPrefs.profile,
        userPrefs.overrides
    ) { p, o -> MacroTargetCalculator.effectiveTarget(p, o) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val suggestion: StateFlow<com.gunsout.domain.nutrition.MacroSuggestion?> = userPrefs.profile
        .map { MacroTargetCalculator.suggest(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val signedInUser: StateFlow<AuthUser?> = authRepository.signedInUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun signOut() = viewModelScope.launch {
        val leavingUserId = currentUserIdProvider.currentUserId.first()
        if (leavingUserId != null) {
            reminderScheduler.cancelForUser(leavingUserId)
        }
        authRepository.signOut()
    }

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    fun clearMessage() { _backupMessage.value = null }

    suspend fun exportToJsonText(): String =
        backupManager.exportToJson(currentUserIdProvider.requireUserId())

    fun importFromJsonText(json: String) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        val result = backupManager.importFromJson(userId, json)
        _backupMessage.value = when (result) {
            is com.gunsout.data.backup.ImportResult.Success -> "Imported ${result.totalRows} rows."
            is com.gunsout.data.backup.ImportResult.Error -> "Import failed: ${result.message}"
        }
    }

    fun save(
        currentWeight: Double,
        goalWeight: Double,
        heightCm: Int?,
        age: Int?,
        sex: Sex?,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        kneeInjury: Boolean,
        baselineWeek: Boolean
    ) = viewModelScope.launch {
        userPrefs.update {
            it.copy(
                currentBodyWeightKg = currentWeight,
                goalBodyWeightKg = goalWeight,
                heightCm = heightCm,
                age = age,
                sex = sex,
                activityLevel = activityLevel,
                goalType = goalType,
                kneeInjuryFlag = kneeInjury,
                baselineWeekActive = baselineWeek
            )
        }
    }

    fun saveOverrides(kcal: Int?, proteinG: Int?, carbsG: Int?, fatG: Int?) = viewModelScope.launch {
        userPrefs.updateOverrides { MacroOverrides(kcal, proteinG, carbsG, fatG) }
    }

    fun resetOverrides() = viewModelScope.launch {
        userPrefs.resetOverrides()
    }
}

@Composable
fun SettingsScreen(
    onOpenLibrary: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsState()
    val overrides by vm.overrides.collectAsState()
    val target by vm.target.collectAsState()
    val suggestion by vm.suggestion.collectAsState()
    val authUser by vm.signedInUser.collectAsState()
    val scroll = rememberScrollState()

    var currentWeight by remember(profile) { mutableStateOf(profile.currentBodyWeightKg.toString()) }
    var goalWeight by remember(profile) { mutableStateOf(profile.goalBodyWeightKg.toString()) }
    var heightCm by remember(profile) { mutableStateOf(profile.heightCm?.toString() ?: "") }
    var age by remember(profile) { mutableStateOf(profile.age?.toString() ?: "") }
    var sex by remember(profile) { mutableStateOf(profile.sex) }
    var activityLevel by remember(profile) { mutableStateOf(profile.activityLevel) }
    var goalType by remember(profile) { mutableStateOf(profile.goalType) }
    var kneeInjury by remember(profile) { mutableStateOf(profile.kneeInjuryFlag) }
    var baselineWeek by remember(profile) { mutableStateOf(profile.baselineWeekActive) }

    var overrideKcal by remember(overrides) { mutableStateOf(overrides.kcal?.toString() ?: "") }
    var overrideProtein by remember(overrides) { mutableStateOf(overrides.proteinG?.toString() ?: "") }
    var overrideCarbs by remember(overrides) { mutableStateOf(overrides.carbsG?.toString() ?: "") }
    var overrideFat by remember(overrides) { mutableStateOf(overrides.fatG?.toString() ?: "") }

    var confirmSignOut by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().padding(16.dp).verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Account", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val displayName = authUser?.displayName?.takeIf { it.isNotBlank() }
                val email = authUser?.email?.takeIf { it.isNotBlank() }
                Text(
                    displayName ?: email ?: "Signed in",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (email != null && displayName != null) {
                    Text(email, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { confirmSignOut = true }) { Text("Sign out") }
            }
        }

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
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = heightCm,
                        onValueChange = { heightCm = it.filter(Char::isDigit) },
                        label = { Text("Height (cm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it.filter(Char::isDigit) },
                        label = { Text("Age") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                LabeledChoiceRow(
                    label = "Sex",
                    selected = sex?.name,
                    options = Sex.values().map { it.name to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelect = { sex = it?.let { Sex.valueOf(it) } }
                )
                Spacer(Modifier.height(10.dp))
                LabeledChoiceRow(
                    label = "Activity",
                    selected = activityLevel.name,
                    options = ActivityLevel.values().map { it.name to it.name.lowercase().replace('_', ' ').replaceFirstChar { c -> c.uppercase() } },
                    onSelect = { activityLevel = ActivityLevel.valueOf(it!!) }
                )
                Spacer(Modifier.height(10.dp))
                LabeledChoiceRow(
                    label = "Goal",
                    selected = goalType.name,
                    options = GoalType.values().map { it.name to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelect = { goalType = GoalType.valueOf(it!!) }
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Daily targets", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                if (suggestion == null) {
                    Text(
                        "Fill in age, sex, height, current weight, and goal weight above to compute a suggested target. You can also set manual overrides below.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    val s = suggestion!!
                    Text(
                        "Suggested: ${s.kcal} kcal | ${s.proteinG}g P | ${s.carbsG}g C | ${s.fatG}g F",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = overrideKcal,
                        onValueChange = { overrideKcal = it.filter(Char::isDigit) },
                        label = { Text("kcal") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = overrideProtein,
                        onValueChange = { overrideProtein = it.filter(Char::isDigit) },
                        label = { Text("P g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = overrideCarbs,
                        onValueChange = { overrideCarbs = it.filter(Char::isDigit) },
                        label = { Text("C g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = overrideFat,
                        onValueChange = { overrideFat = it.filter(Char::isDigit) },
                        label = { Text("F g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Leave a field blank to fall back to the suggested value.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        vm.saveOverrides(
                            kcal = overrideKcal.toIntOrNull(),
                            proteinG = overrideProtein.toIntOrNull(),
                            carbsG = overrideCarbs.toIntOrNull(),
                            fatG = overrideFat.toIntOrNull()
                        )
                    }) { Text("Save overrides") }
                    OutlinedButton(onClick = {
                        overrideKcal = ""
                        overrideProtein = ""
                        overrideCarbs = ""
                        overrideFat = ""
                        vm.resetOverrides()
                    }) { Text("Reset to suggested") }
                }
                target?.let { t ->
                    Spacer(Modifier.height(8.dp))
                    val caption = when (t.source) {
                        MacroTarget.Source.SUGGESTED -> "Active target (suggested)"
                        MacroTarget.Source.OVERRIDDEN -> "Active target (overridden)"
                    }
                    Text(caption, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${t.kcal} kcal | ${t.proteinG}g P | ${t.carbsG}g C | ${t.fatG}g F",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Library", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Manage all exercises (seeded plus user-created).", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenLibrary) { Text("Open exercise library") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Backup", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Export everything to a JSON file or restore from a previous export. Importing replaces all current data.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                BackupRow(vm)
                val msg by vm.backupMessage.collectAsState()
                msg?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Button(
            onClick = {
                vm.save(
                    currentWeight = currentWeight.toDoubleOrNull() ?: profile.currentBodyWeightKg,
                    goalWeight = goalWeight.toDoubleOrNull() ?: profile.goalBodyWeightKg,
                    heightCm = heightCm.toIntOrNull(),
                    age = age.toIntOrNull(),
                    sex = sex,
                    activityLevel = activityLevel,
                    goalType = goalType,
                    kneeInjury = kneeInjury,
                    baselineWeek = baselineWeek
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save profile") }
    }

    if (confirmSignOut) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = {
                Text(
                    "Your on-device data stays. Signing back in with the same Google account brings it back. Signing in with a different account shows that account's separate data."
                )
            },
            confirmButton = {
                Button(onClick = {
                    confirmSignOut = false
                    vm.signOut()
                }) { Text("Sign out") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LabeledChoiceRow(
    label: String,
    selected: String?,
    options: List<Pair<String, String>>,
    onSelect: (String?) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (value, display) ->
                androidx.compose.material3.FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(display) }
                )
            }
        }
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

@Composable
private fun BackupRow(vm: SettingsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var pendingImport by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = vm.exportToJsonText()
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            }
        }
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (text != null) pendingImport = text
            }
        }
    }

    val ts = java.time.LocalDate.now().toString()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { exportLauncher.launch("gunsout-backup-$ts.json") }) { Text("Export JSON") }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) { Text("Import JSON") }
    }

    pendingImport?.let { jsonText ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Replace all data?") },
            text = {
                Text(
                    "Importing will permanently delete the current programs, meal plans, sessions, sets, food entries, supplements, body metrics and profile in this install, and replace them with the contents of the selected file. This cannot be undone."
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.importFromJsonText(jsonText)
                    pendingImport = null
                }) { Text("Replace") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pendingImport = null }) { Text("Cancel") } }
        )
    }
}
