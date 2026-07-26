package com.nicholasbergesen.gunsout.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicholasbergesen.gunsout.auth.AuthRepository
import com.nicholasbergesen.gunsout.auth.AuthUser
import com.nicholasbergesen.gunsout.auth.CurrentUserIdProvider
import com.nicholasbergesen.gunsout.core.text.normalizeDecimalInput
import com.nicholasbergesen.gunsout.core.text.toNormalizedDoubleOrNull
import com.nicholasbergesen.gunsout.data.prefs.ActivityLevel
import com.nicholasbergesen.gunsout.data.prefs.GoalType
import com.nicholasbergesen.gunsout.data.prefs.TargetOverrides
import com.nicholasbergesen.gunsout.data.prefs.Sex
import com.nicholasbergesen.gunsout.data.prefs.TrainingExperience
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import com.nicholasbergesen.gunsout.data.repo.WorkoutRepository
import com.nicholasbergesen.gunsout.data.repo.ProteinRepository
import com.nicholasbergesen.gunsout.domain.nutrition.CalorieTarget
import com.nicholasbergesen.gunsout.domain.nutrition.CalorieTargetCalculator
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinTarget
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinTargetCalculator
import com.nicholasbergesen.gunsout.domain.nutrition.TargetSource
import com.nicholasbergesen.gunsout.ui.components.ChipButton
import com.nicholasbergesen.gunsout.ui.components.MockupScreenColumn
import com.nicholasbergesen.gunsout.ui.components.ScreenTitle
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.StatusChip
import com.nicholasbergesen.gunsout.ui.components.ThemedCard
import com.nicholasbergesen.gunsout.ui.components.WrappingRow
import com.nicholasbergesen.gunsout.ui.theme.ThemeStyle
import com.nicholasbergesen.gunsout.ui.theme.backdropBrushFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

data class GuidanceTargetsUiState(
    val overrides: TargetOverrides = TargetOverrides(),
    val suggestedKcal: Int? = null,
    val suggestedProteinGrams: Int? = null,
    val calorieTarget: CalorieTarget? = null,
    val proteinTarget: ProteinTarget? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferences,
    private val backupManager: com.nicholasbergesen.gunsout.data.backup.BackupManager,
    private val workouts: WorkoutRepository,
    private val proteinRepository: ProteinRepository,
    private val currentUserIdProvider: CurrentUserIdProvider,
    private val authRepository: AuthRepository,
    private val reminderScheduler: com.nicholasbergesen.gunsout.feature.creatine.CreatineReminderScheduler
) : ViewModel() {
    // Per-user prefs (Phase 3): every observed profile/overrides flow is bound to the current
    // signed-in userId via flatMapLatest, so a sign-in to a different Google account on the same
    // device immediately switches the visible profile to that account's DataStore file. Settings
    // would otherwise display stale values from a previous account.
    private val userIdFlow = currentUserIdProvider.currentUserId.filterNotNull()

    val profile: StateFlow<UserProfile> = userIdFlow
        .flatMapLatest { userPrefs.profile(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    val guidanceTargets: StateFlow<GuidanceTargetsUiState> = userIdFlow
        .flatMapLatest { userId ->
            kotlinx.coroutines.flow.combine(
                userPrefs.profile(userId),
                userPrefs.targetOverrides(userId)
            ) { profile, overrides ->
                GuidanceTargetsUiState(
                    overrides = overrides,
                    suggestedKcal = CalorieTargetCalculator.suggest(profile),
                    suggestedProteinGrams = ProteinTargetCalculator.suggest(profile),
                    calorieTarget = CalorieTargetCalculator.effective(profile, overrides.kcal),
                    proteinTarget = ProteinTargetCalculator.effective(
                        profile,
                        overrides.proteinG
                    )
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GuidanceTargetsUiState()
        )

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
    private val _sessionExportMessage = MutableStateFlow<String?>(null)
    val sessionExportMessage: StateFlow<String?> = _sessionExportMessage.asStateFlow()

    fun clearMessage() { _backupMessage.value = null }

    suspend fun exportToJsonText(): String =
        backupManager.exportToJson(currentUserIdProvider.requireUserId())

    suspend fun exportExerciseSessionsJsonText(): String =
        workouts.exportExerciseSessionsJson(currentUserIdProvider.requireUserId())

    fun setSessionExportMessage(message: String) {
        _sessionExportMessage.value = message
    }

    fun importFromJsonText(json: String) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        val result = backupManager.importFromJson(userId, json)
        _backupMessage.value = when (result) {
            is com.nicholasbergesen.gunsout.data.backup.ImportResult.Success -> "Imported ${result.totalRows} rows."
            is com.nicholasbergesen.gunsout.data.backup.ImportResult.Error -> "Import failed: ${result.message}"
        }
    }

    fun save(
        currentWeight: Double,
        goalWeight: Double,
        heightCm: Int?,
        age: Int?,
        sex: Sex?,
        trainingExperience: TrainingExperience,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        kneeInjury: Boolean,
        baselineWeek: Boolean
    ) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        userPrefs.update(userId) {
            it.copy(
                currentBodyWeightKg = currentWeight,
                goalBodyWeightKg = goalWeight,
                heightCm = heightCm,
                age = age,
                sex = sex,
                trainingExperience = trainingExperience,
                activityLevel = activityLevel,
                goalType = goalType,
                kneeInjuryFlag = kneeInjury,
                baselineWeekActive = baselineWeek
            )
        }
        syncTodayProteinTarget(userId)
    }

    fun saveTargetOverrides(kcal: Int?, proteinG: Int?) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        userPrefs.updateTargetOverrides(userId) { TargetOverrides(kcal, proteinG) }
        syncTodayProteinTarget(userId)
    }

    fun resetTargetOverrides() = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        userPrefs.resetTargetOverrides(userId)
        syncTodayProteinTarget(userId)
    }

    fun saveThemeStyle(style: ThemeStyle) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        userPrefs.update(userId) { it.copy(themeStyle = style) }
    }

    private suspend fun syncTodayProteinTarget(userId: String) {
        val profile = userPrefs.profile(userId).first()
        val overrides = userPrefs.targetOverrides(userId).first()
        proteinRepository.syncTodayTarget(
            userId = userId,
            date = LocalDate.now(),
            targetGrams = ProteinTargetCalculator.effective(profile, overrides.proteinG)?.grams
        )
    }
}

@Composable
fun SettingsScreen(
    onOpenLibrary: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsState()
    val guidanceTargets by vm.guidanceTargets.collectAsState()
    val authUser by vm.signedInUser.collectAsState()
    val scroll = rememberScrollState()

    var currentWeight by remember(profile.currentBodyWeightKg) {
        mutableStateOf(profile.currentBodyWeightKg.toString())
    }
    var goalWeight by remember(profile.goalBodyWeightKg) {
        mutableStateOf(profile.goalBodyWeightKg.toString())
    }
    var heightCm by remember(profile.heightCm) { mutableStateOf(profile.heightCm?.toString() ?: "") }
    var age by remember(profile.age) { mutableStateOf(profile.age?.toString() ?: "") }
    var sex by remember(profile.sex) { mutableStateOf(profile.sex) }
    var trainingExperience by remember(profile.trainingExperience) { mutableStateOf(profile.trainingExperience) }
    var activityLevel by remember(profile.activityLevel) { mutableStateOf(profile.activityLevel) }
    var goalType by remember(profile.goalType) { mutableStateOf(profile.goalType) }
    var kneeInjury by remember(profile.kneeInjuryFlag) { mutableStateOf(profile.kneeInjuryFlag) }
    var baselineWeek by remember(profile.baselineWeekActive) { mutableStateOf(profile.baselineWeekActive) }

    var overrideKcal by remember(guidanceTargets.overrides) {
        mutableStateOf(guidanceTargets.overrides.kcal?.toString() ?: "")
    }
    var overrideProtein by remember(guidanceTargets.overrides) {
        mutableStateOf(guidanceTargets.overrides.proteinG?.toString() ?: "")
    }

    var confirmSignOut by remember { mutableStateOf(false) }

    MockupScreenColumn(modifier = Modifier.verticalScroll(scroll)) {
        ScreenTitle("Settings")

        ThemedCard {
                val displayName = authUser?.displayName?.takeIf { it.isNotBlank() }
                val email = authUser?.email?.takeIf { it.isNotBlank() }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusChip((displayName ?: email ?: "N").take(1), selected = true)
                        Column {
                            Text(displayName ?: email ?: "Signed in")
                            if (email != null && displayName != null) {
                                Text(email, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    ChipButton("Sign out", onClick = { confirmSignOut = true })
                }
        }

        AppearanceCard(
            selectedStyle = profile.themeStyle,
            onStyleSelected = vm::saveThemeStyle
        )

        ThemedCard {
            SectionLabel("Profile")
            OutlinedTextField(
                value = currentWeight,
                onValueChange = { currentWeight = it.normalizeDecimalInput() },
                label = { Text("Current weight (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = goalWeight,
                onValueChange = { goalWeight = it.normalizeDecimalInput() },
                label = { Text("Goal weight (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
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
            LabeledChoiceRow(
                label = "Sex",
                selected = sex?.name,
                options = Sex.values().map { it.name to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                onSelect = { sex = it?.let { Sex.valueOf(it) } }
            )
            LabeledChoiceRow(
                label = "Training experience",
                selected = trainingExperience.name,
                options = TrainingExperience.values().map { it.name to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                onSelect = { trainingExperience = TrainingExperience.valueOf(it!!) }
            )
            LabeledChoiceRow(
                label = "Activity",
                selected = activityLevel.name,
                options = ActivityLevel.values().map { it.name to it.name.lowercase().replace('_', ' ').replaceFirstChar { c -> c.uppercase() } },
                onSelect = { activityLevel = ActivityLevel.valueOf(it!!) }
            )
            LabeledChoiceRow(
                label = "Goal",
                selected = goalType.name,
                options = GoalType.values().map { it.name to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                onSelect = { goalType = GoalType.valueOf(it!!) }
            )
        }

        ThemedCard(accent = true) {
            SectionLabel("Guidance targets")
            Text(
                guidanceTargets.suggestedKcal?.let { "Suggested calorie guidance: $it kcal" }
                    ?: "Complete age, sex, height, current weight, and goal weight for calorie guidance.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                guidanceTargets.suggestedProteinGrams?.let {
                    "Suggested daily protein: $it g (2.0 g/kg goal weight)"
                } ?: "Set a goal weight from 30 to 300 kg for a protein target.",
                style = MaterialTheme.typography.bodySmall
            )
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
            }
            Text(
                "Leave either field blank to use its independent suggestion.",
                style = MaterialTheme.typography.bodySmall
            )
            WrappingRow {
                Button(onClick = {
                    vm.saveTargetOverrides(
                        kcal = overrideKcal.toIntOrNull(),
                        proteinG = overrideProtein.toIntOrNull()
                    )
                }) { Text("Save overrides", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                OutlinedButton(onClick = {
                    overrideKcal = ""
                    overrideProtein = ""
                    vm.resetTargetOverrides()
                }) { Text("Reset to suggested", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            guidanceTargets.calorieTarget?.let {
                Text(
                    "Active calorie guidance: ${it.kcal} kcal (${it.source.displayName()})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            guidanceTargets.proteinTarget?.let {
                Text(
                    "Active protein target: ${it.grams} g (${it.source.displayName()})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        ThemedCard {
            SettingsToggle(
                label = "Knee injury caution",
                description = "Prompts knee-feel on leg sessions and shows knee notes on relevant exercises.",
                checked = kneeInjury,
                onCheckedChange = { kneeInjury = it }
            )
            SettingsToggle(
                label = "Baseline week active",
                description = "While on, progression suggestions are suppressed. Turn off after week 1.",
                checked = baselineWeek,
                onCheckedChange = { baselineWeek = it }
            )
        }

        ThemedCard {
            Text("Library", style = MaterialTheme.typography.titleMedium)
            Text("Manage all exercises (seeded plus user-created).", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onOpenLibrary) { Text("Open exercise library") }
        }

        ThemedCard {
            Text("Exercise session export", style = MaterialTheme.typography.titleMedium)
            Text(
                "Export completed workouts and rest days as JSON for external analysis.",
                style = MaterialTheme.typography.bodySmall
            )
            SessionExportRow(vm)
            val msg by vm.sessionExportMessage.collectAsState()
            msg?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }

        ThemedCard {
            Text("Backup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Export everything to a JSON file or restore from a previous export. Importing replaces all current data.",
                style = MaterialTheme.typography.bodySmall
            )
            BackupRow(vm)
            val msg by vm.backupMessage.collectAsState()
            msg?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }

        Button(
            onClick = {
                vm.save(
                    currentWeight = currentWeight.toNormalizedDoubleOrNull() ?: profile.currentBodyWeightKg,
                    goalWeight = goalWeight.toNormalizedDoubleOrNull() ?: profile.goalBodyWeightKg,
                    heightCm = heightCm.toIntOrNull(),
                    age = age.toIntOrNull(),
                    sex = sex,
                    trainingExperience = trainingExperience,
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

private fun TargetSource.displayName(): String = when (this) {
    TargetSource.SUGGESTED -> "suggested"
    TargetSource.OVERRIDDEN -> "overridden"
}

@Composable
private fun AppearanceCard(
    selectedStyle: ThemeStyle,
    onStyleSelected: (ThemeStyle) -> Unit
) {
    ThemedCard {
        SectionLabel("Appearance")
        Text(
            "Choose one visual style. Themes use fixed palettes rather than separate light and dark modes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ThemeStyle.values().forEach { style ->
            ThemeStyleRow(
                style = style,
                selected = style == selectedStyle,
                onSelect = { onStyleSelected(style) }
            )
        }
    }
}

@Composable
private fun ThemeStyleRow(
    style: ThemeStyle,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeSwatch(style = style)
            Column(Modifier.weight(1f)) {
                Text(style.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    style.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
private fun ThemeSwatch(style: ThemeStyle) {
    val swatchSize = 52.dp
    val swatchSizePx = with(LocalDensity.current) { swatchSize.toPx() }
    Box(
        modifier = Modifier
            .size(swatchSize)
            .clip(MaterialTheme.shapes.small)
            .background(backdropBrushFor(style, Size(swatchSizePx, swatchSizePx)))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(style.swatchSurface)
            )
            Column(
                modifier = Modifier.width(14.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(style.swatchAccent)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(style.swatchBackgroundEnd)
                )
            }
        }
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
        WrappingRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, display) ->
                androidx.compose.material3.FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(display, maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
private fun SessionExportRow(vm: SettingsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = vm.exportExerciseSessionsJsonText()
                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: error("The selected file could not be opened.")
                        output.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
                    }
                    vm.setSessionExportMessage("Session export saved.")
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    vm.setSessionExportMessage(
                        "Session export failed: ${error.message ?: error::class.java.simpleName}"
                    )
                }
            }
        }
    }

    val ts = java.time.LocalDate.now().toString()
    OutlinedButton(onClick = { exportLauncher.launch("gunsout-exercise-sessions-$ts.json") }) {
        Text("Export sessions JSON")
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
    WrappingRow {
        OutlinedButton(onClick = { exportLauncher.launch("gunsout-backup-$ts.json") }) {
            Text("Export JSON", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
            Text("Import JSON", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    pendingImport?.let { jsonText ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Replace all data?") },
            text = {
                Text(
                    "Importing will permanently delete the current programs, sessions, sets, protein entries, creatine checks, body metrics, and profile in this install, and replace them with the contents of the selected file. This cannot be undone."
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
