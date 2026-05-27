package com.gunsout.feature.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.auth.CurrentUserIdProvider
import com.gunsout.data.entity.FoodEntry
import com.gunsout.data.entity.MealTemplate
import com.gunsout.data.entity.Supplement
import com.gunsout.data.repo.DietRepository
import com.gunsout.data.repo.SupplementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class DietUiState(
    val today: LocalDate = LocalDate.now(),
    val templates: List<MealTemplate> = emptyList(),
    val todayEntries: List<FoodEntry> = emptyList(),
    val supplements: List<Supplement> = emptyList(),
    val supplementsTakenToday: Set<Long> = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DietViewModel @Inject constructor(
    private val diet: DietRepository,
    private val supplements: SupplementRepository,
    private val currentUserIdProvider: CurrentUserIdProvider
) : ViewModel() {

    // Emits the current date and re-emits whenever the day rolls over (or the user re-enters the
    // screen via refresh()). Backed by a delay-to-midnight loop plus a manual ticker.
    private val dayFlow: MutableStateFlow<LocalDate> = MutableStateFlow(LocalDate.now())

    init {
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val sleepMs = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L)
                delay(sleepMs)
                dayFlow.value = LocalDate.now()
            }
        }
    }

    /** Manually refresh "today". Useful from ON_RESUME or when the user pulls down. */
    fun refreshDay() {
        dayFlow.value = LocalDate.now()
    }

    val state: StateFlow<DietUiState> = currentUserIdProvider.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            val entriesFlow = dayFlow.flatMapLatest { date ->
                diet.observeEntriesForDate(userId, date)
            }
            val supplementLogsFlow = dayFlow.flatMapLatest { date ->
                supplements.observeLogsForDate(userId, date)
            }
            combine(
                diet.observeTemplates(userId),
                entriesFlow,
                supplements.observeActive(userId),
                supplementLogsFlow
            ) { templates, entries, supps, supLogs ->
                DietUiState(
                    today = dayFlow.value,
                    templates = templates,
                    todayEntries = entries,
                    supplements = supps,
                    supplementsTakenToday = supLogs.map { it.supplementId }.toSet()
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DietUiState())

    fun logTemplate(template: MealTemplate, multiplier: Double = 1.0) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        diet.logFromTemplate(userId, template, dayFlow.value, multiplier)
    }

    fun toggleSupplement(supplement: Supplement) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        supplements.markTakenToday(userId, supplement)
    }

    fun setReminder(supplementId: Long, time: java.time.LocalTime?) = viewModelScope.launch {
        supplements.setReminderTime(supplementId, time)
    }

    fun deleteEntry(entryId: Long) = viewModelScope.launch {
        diet.deleteEntry(entryId)
    }

    fun restoreEntry(entry: FoodEntry) = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        // Re-insert with the original macros. ID auto-regenerates; createdAt stays the same so it
        // sorts back to its original position in today's list.
        diet.logCustomFood(
            userId = userId,
            date = entry.date,
            mealType = entry.mealType,
            name = entry.name,
            kcal = entry.kcal,
            proteinG = entry.proteinG,
            carbsG = entry.carbsG,
            fatG = entry.fatG
        )
    }

    fun updateEntry(entry: FoodEntry) = viewModelScope.launch {
        diet.updateEntry(entry)
    }
}
