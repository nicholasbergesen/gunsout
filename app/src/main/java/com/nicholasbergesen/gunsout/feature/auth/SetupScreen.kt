package com.nicholasbergesen.gunsout.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicholasbergesen.gunsout.auth.CurrentUserIdProvider
import com.nicholasbergesen.gunsout.data.prefs.Sex
import com.nicholasbergesen.gunsout.data.prefs.TrainingExperience
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import com.nicholasbergesen.gunsout.data.repo.BodyRepository
import com.nicholasbergesen.gunsout.ui.components.MockupScreenColumn
import com.nicholasbergesen.gunsout.ui.components.ScreenTitle
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.ThemedCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val bodyRepository: BodyRepository,
    private val currentUserIdProvider: CurrentUserIdProvider
) : ViewModel() {
    val profile: StateFlow<UserProfile> = currentUserIdProvider.currentUserId
        .filterNotNull()
        .flatMapLatest { userPreferences.profile(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    fun complete(
        trainingExperience: TrainingExperience,
        sex: Sex,
        age: Int,
        currentWeightKg: Double,
        goalWeightKg: Double,
        muscleMassKg: Double?,
        bodyFatPct: Double?
    ) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        bodyRepository.log(
            userId = userId,
            date = LocalDate.now(),
            weightKg = currentWeightKg,
            bodyFatPct = bodyFatPct,
            muscleMassKg = muscleMassKg
        )
        userPreferences.update(userId) {
            it.copy(
                currentBodyWeightKg = currentWeightKg,
                goalBodyWeightKg = goalWeightKg,
                age = age,
                sex = sex,
                trainingExperience = trainingExperience,
                profileSetupDone = true
            )
        }
    }
}

@Composable
fun SetupScreen() {
    Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Gunsout", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    "Setting up your account...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
                vm: ProfileSetupViewModel = hiltViewModel()
            ) {
                val profile by vm.profile.collectAsState()
                var trainingExperience by remember(profile.trainingExperience) {
                    mutableStateOf(profile.trainingExperience)
                }
                var sex by remember(profile.sex) { mutableStateOf(profile.sex) }
                var age by remember(profile.age) { mutableStateOf(profile.age?.toString() ?: "") }
                var currentWeight by remember(profile.currentBodyWeightKg) {
                    mutableStateOf(profile.currentBodyWeightKg.toString())
                }
                var goalWeight by remember(profile.goalBodyWeightKg) {
                    mutableStateOf(profile.goalBodyWeightKg.toString())
                }
                var muscleMass by remember { mutableStateOf("") }
                var bodyFat by remember { mutableStateOf("") }

                val ageValue = age.toIntOrNull()
                val currentWeightValue = currentWeight.toDoubleOrNull()
                val goalWeightValue = goalWeight.toDoubleOrNull()
                val canSave = sex != null &&
                    ageValue != null &&
                    ageValue > 0 &&
                    currentWeightValue != null &&
                    currentWeightValue > 0.0 &&
                    goalWeightValue != null &&
                    goalWeightValue > 0.0

                MockupScreenColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Column {
                        SectionLabel("Strength profile")
                        ScreenTitle("Set up recommendations")
                        Text(
                            "These fields let Gunsout suggest starting weights and bodyweight reps locally on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ThemedCard {
                        LabeledChoiceRow(
                            label = "Training experience",
                            selected = trainingExperience.name,
                            options = TrainingExperience.values().map { it.name to it.displayName() },
                            onSelect = { trainingExperience = TrainingExperience.valueOf(it!!) }
                        )
                        LabeledChoiceRow(
                            label = "Sex",
                            selected = sex?.name,
                            options = Sex.values().map { it.name to it.displayName() },
                            onSelect = { sex = it?.let(Sex::valueOf) }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it.filter(Char::isDigit) },
                                label = { Text("Age") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = currentWeight,
                                onValueChange = { currentWeight = it.filterDecimal() },
                                label = { Text("Current kg") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = goalWeight,
                            onValueChange = { goalWeight = it.filterDecimal() },
                            label = { Text("Goal weight (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ThemedCard {
                        SectionLabel("Optional body composition")
                        Text(
                            "If you know these values, they can temper progression. They are saved to today's Body log.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = muscleMass,
                                onValueChange = { muscleMass = it.filterDecimal() },
                                label = { Text("Muscle kg") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = bodyFat,
                                onValueChange = { bodyFat = it.filterDecimal() },
                                label = { Text("Body fat %") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(
                        enabled = canSave,
                        onClick = {
                            vm.complete(
                                trainingExperience = trainingExperience,
                                sex = sex!!,
                                age = ageValue!!,
                                currentWeightKg = currentWeightValue!!,
                                goalWeightKg = goalWeightValue!!,
                                muscleMassKg = muscleMass.toDoubleOrNull(),
                                bodyFatPct = bodyFat.toDoubleOrNull()
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start using Gunsout")
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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.forEach { (value, display) ->
                            FilterChip(
                                selected = selected == value,
                                onClick = { onSelect(value) },
                                label = { Text(display) }
                            )
                        }
                    }
                }
            }

            private fun TrainingExperience.displayName(): String =
                name.lowercase().replaceFirstChar { it.uppercase() }

            private fun Sex.displayName(): String =
                name.lowercase().replaceFirstChar { it.uppercase() }

            private fun String.filterDecimal(): String =
                filter { it.isDigit() || it == '.' }
