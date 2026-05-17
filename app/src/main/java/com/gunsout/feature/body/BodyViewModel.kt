package com.gunsout.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.data.entity.BodyMetricsLog
import com.gunsout.data.prefs.UserPreferences
import com.gunsout.data.prefs.UserProfile
import com.gunsout.data.repo.BodyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BodyUiState(
    val profile: UserProfile = UserProfile(),
    val logs: List<BodyMetricsLog> = emptyList()
)

@HiltViewModel
class BodyViewModel @Inject constructor(
    private val body: BodyRepository,
    private val userPrefs: UserPreferences
) : ViewModel() {

    val state: StateFlow<BodyUiState> = combine(
        userPrefs.profile,
        body.observeSince(LocalDate.now().minusYears(2))
    ) { profile, logs -> BodyUiState(profile, logs.sortedBy { it.date }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyUiState())

    fun logToday(
        weightKg: Double,
        bodyFatPct: Double?,
        muscleMassKg: Double?,
        waterPct: Double?,
        boneMassKg: Double?,
        visceralFatRating: Int?
    ) = viewModelScope.launch {
        body.log(
            date = LocalDate.now(),
            weightKg = weightKg,
            bodyFatPct = bodyFatPct,
            muscleMassKg = muscleMassKg,
            waterPct = waterPct,
            boneMassKg = boneMassKg,
            visceralFatRating = visceralFatRating
        )
        userPrefs.update { it.copy(currentBodyWeightKg = weightKg) }
    }
}
