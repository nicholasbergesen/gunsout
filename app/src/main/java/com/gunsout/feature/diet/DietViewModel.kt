package com.gunsout.feature.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.data.entity.FoodEntry
import com.gunsout.data.entity.MealPlan
import com.gunsout.data.entity.MealTemplate
import com.gunsout.data.entity.MealType
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class DietUiState(
    val today: LocalDate = LocalDate.now(),
    val activePlan: MealPlan? = null,
    val templates: List<MealTemplate> = emptyList(),
    val todayEntries: List<FoodEntry> = emptyList(),
    val supplements: List<Supplement> = emptyList(),
    val supplementsTakenToday: Set<Long> = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DietViewModel @Inject constructor(
    private val diet: DietRepository,
    private val supplements: SupplementRepository
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

    private val templatesFlow = diet.observeActivePlan().flatMapLatest { plan ->
        if (plan == null) flowOf(emptyList()) else diet.observeTemplatesForPlan(plan.id)
    }

    private val entriesFlow = dayFlow.flatMapLatest { date -> diet.observeEntriesForDate(date) }
    private val supplementLogsFlow = dayFlow.flatMapLatest { date -> supplements.observeLogsForDate(date) }

    val state: StateFlow<DietUiState> = combine(
        diet.observeActivePlan(),
        templatesFlow,
        entriesFlow,
        supplements.observeActive(),
        supplementLogsFlow
    ) { plan, templates, entries, supps, supLogs ->
        DietUiState(
            today = dayFlow.value,
            activePlan = plan,
            templates = templates,
            todayEntries = entries,
            supplements = supps,
            supplementsTakenToday = supLogs.map { it.supplementId }.toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DietUiState())

    fun logTemplate(template: MealTemplate, multiplier: Double = 1.0) = viewModelScope.launch {
        diet.logFromTemplate(template, dayFlow.value, multiplier)
    }

    fun toggleSupplement(supplement: Supplement) = viewModelScope.launch {
        supplements.markTakenToday(supplement)
    }

    fun setReminder(supplementId: Long, time: java.time.LocalTime?) = viewModelScope.launch {
        supplements.setReminderTime(supplementId, time)
    }

    fun deleteEntry(entryId: Long) = viewModelScope.launch {
        diet.deleteEntry(entryId)
    }

    fun restoreEntry(entry: FoodEntry) = viewModelScope.launch {
        // Re-insert with the original macros. ID auto-regenerates; createdAt stays the same so it
        // sorts back to its original position in today's list.
        diet.logCustomFood(
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
