package com.gunsout.feature.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.data.entity.FoodEntry
import com.gunsout.data.entity.MealPlan
import com.gunsout.data.entity.MealTemplate
import com.gunsout.data.entity.Supplement
import com.gunsout.data.repo.DietRepository
import com.gunsout.data.repo.SupplementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DietUiState(
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

    private val today = LocalDate.now()

    private val templatesFlow = diet.observeActivePlan().flatMapLatest { plan ->
        if (plan == null) flowOf(emptyList()) else diet.observeTemplatesForPlan(plan.id)
    }

    val state: StateFlow<DietUiState> = combine(
        diet.observeActivePlan(),
        templatesFlow,
        diet.observeEntriesForDate(today),
        supplements.observeActive(),
        supplements.observeLogsForDate(today)
    ) { plan, templates, entries, supps, supLogs ->
        DietUiState(
            activePlan = plan,
            templates = templates,
            todayEntries = entries,
            supplements = supps,
            supplementsTakenToday = supLogs.map { it.supplementId }.toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DietUiState())

    fun logTemplate(template: MealTemplate) = viewModelScope.launch {
        diet.logFromTemplate(template)
    }

    fun toggleSupplement(supplement: Supplement) = viewModelScope.launch {
        supplements.markTakenToday(supplement)
    }
}
