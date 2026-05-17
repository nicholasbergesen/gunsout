package com.gunsout.feature.mealplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.data.entity.Ingredient
import com.gunsout.data.entity.IngredientUnit
import com.gunsout.data.entity.MacroSource
import com.gunsout.data.entity.MealPlan
import com.gunsout.data.entity.MealTemplate
import com.gunsout.data.entity.MealTemplateIngredient
import com.gunsout.data.entity.MealType
import com.gunsout.data.repo.MealPlanRepository
import com.gunsout.domain.macros.MacroCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealPlanListViewModel @Inject constructor(
    private val repo: MealPlanRepository
) : ViewModel() {
    val plans: StateFlow<List<MealPlan>> = repo.observePlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun activate(id: Long) = viewModelScope.launch { repo.setActive(id) }
    fun duplicate(id: Long, name: String) = viewModelScope.launch { repo.duplicatePlan(id, name) }
    fun create(name: String, onCreated: (Long) -> Unit) = viewModelScope.launch {
        val id = repo.createPlan(MealPlan(
            name = name, kcalTarget = 2200, proteinG = 160, carbsG = 220, fatG = 70
        ))
        onCreated(id)
    }
}

data class MealPlanEditState(
    val plan: MealPlan? = null,
    val templates: List<MealTemplate> = emptyList(),
    val saved: Boolean = false
)

@HiltViewModel
class MealPlanEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: MealPlanRepository
) : ViewModel() {
    private val planId: Long = savedStateHandle.get<Long>("planId") ?: 0L
    private val _state = MutableStateFlow(MealPlanEditState())
    val state: StateFlow<MealPlanEditState> = _state

    init { reload() }

    fun reload() = viewModelScope.launch {
        val plan = repo.getPlan(planId)
        val templates = if (plan != null) repo.observeTemplatesFor(plan.id).firstOrNull().orEmpty() else emptyList()
        _state.value = MealPlanEditState(plan, templates)
    }

    fun updatePlan(plan: MealPlan) = viewModelScope.launch {
        repo.updatePlan(plan)
        reload()
    }

    fun setTargets(kcal: Int, protein: Int, carbs: Int, fat: Int) = viewModelScope.launch {
        val p = _state.value.plan ?: return@launch
        repo.updatePlan(p.copy(kcalTarget = kcal, proteinG = protein, carbsG = carbs, fatG = fat))
        reload()
    }
}

data class TemplateEditState(
    val template: MealTemplate? = null,
    val rows: List<MealTemplateIngredient> = emptyList(),
    val ingredients: Map<Long, Ingredient> = emptyMap(),
    val saved: Boolean = false
)

@HiltViewModel
class MealTemplateEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: MealPlanRepository
) : ViewModel() {
    private val templateId: Long = savedStateHandle.get<Long>("templateId") ?: 0L
    private val _state = MutableStateFlow(TemplateEditState())
    val state: StateFlow<TemplateEditState> = _state

    val ingredients: StateFlow<List<Ingredient>> = repo.observeIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { reload() }

    fun reload() = viewModelScope.launch {
        val template = repo.getTemplate(templateId)
        val rows = if (template != null) repo.getTemplateIngredients(template.id) else emptyList()
        val ingMap = rows.mapNotNull { row -> repo.getIngredient(row.ingredientId)?.let { it.id to it } }.toMap()
        _state.value = TemplateEditState(template, rows, ingMap)
    }

    fun setMacroSource(source: MacroSource) = viewModelScope.launch {
        val t = _state.value.template ?: return@launch
        repo.updateTemplate(t.copy(macroSource = source))
        if (source == MacroSource.FROM_INGREDIENTS) repo.recomputeTemplateMacros(t.id)
        reload()
    }

    fun setManualMacros(kcal: Int, protein: Double, carbs: Double, fat: Double) = viewModelScope.launch {
        val t = _state.value.template ?: return@launch
        repo.updateTemplate(t.copy(
            macroSource = MacroSource.MANUAL,
            kcal = kcal, proteinG = protein, carbsG = carbs, fatG = fat
        ))
        reload()
    }

    fun addIngredient(ingredientId: Long, quantity: Double, unit: IngredientUnit) = viewModelScope.launch {
        val t = _state.value.template ?: return@launch
        val current = _state.value.rows.size
        repo.setTemplateIngredients(t.id, listOf(
            MealTemplateIngredient(
                mealTemplateId = t.id,
                ingredientId = ingredientId,
                quantity = quantity,
                unit = unit,
                orderIndex = current
            )
        ))
        reload()
    }

    fun computedTotals(): com.gunsout.domain.macros.Macros =
        MacroCalculator.totalFor(_state.value.rows, _state.value.ingredients)

    fun setName(name: String, mealType: MealType) = viewModelScope.launch {
        val t = _state.value.template ?: return@launch
        repo.updateTemplate(t.copy(name = name, mealType = mealType))
        reload()
    }
}
