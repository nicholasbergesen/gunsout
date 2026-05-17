package com.gunsout.feature.ingredients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gunsout.data.entity.Ingredient
import com.gunsout.data.entity.IngredientUnit
import com.gunsout.data.remote.CalorieNinjasClient
import com.gunsout.data.remote.LookupResult
import com.gunsout.data.repo.MealPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IngredientListViewModel @Inject constructor(
    private val repo: MealPlanRepository
) : ViewModel() {
    val ingredients: StateFlow<List<Ingredient>> = repo.observeIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class IngredientEditState(
    val name: String = "",
    val kcalPer100g: String = "",
    val proteinPer100g: String = "",
    val carbsPer100g: String = "",
    val fatPer100g: String = "",
    val defaultUnit: IngredientUnit = IngredientUnit.G,
    val gramsPerUnit: String = "1.0",
    val lookupQuery: String = "",
    val lookupBusy: Boolean = false,
    val lookupMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class IngredientEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: MealPlanRepository,
    private val calorieNinjas: CalorieNinjasClient
) : ViewModel() {
    private val ingredientId: Long = savedStateHandle.get<Long>("ingredientId") ?: 0L
    private val _state = MutableStateFlow(IngredientEditState())
    val state: StateFlow<IngredientEditState> = _state.asStateFlow()

    private var existing: Ingredient? = null

    init {
        if (ingredientId > 0) viewModelScope.launch {
            val ing = repo.getIngredient(ingredientId) ?: return@launch
            existing = ing
            _state.value = IngredientEditState(
                name = ing.name,
                kcalPer100g = ing.kcalPer100g.toString(),
                proteinPer100g = ing.proteinPer100g.toString(),
                carbsPer100g = ing.carbsPer100g.toString(),
                fatPer100g = ing.fatPer100g.toString(),
                defaultUnit = ing.defaultUnit,
                gramsPerUnit = ing.gramsPerUnit.toString()
            )
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setKcal(v: String) = _state.update { it.copy(kcalPer100g = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setProtein(v: String) = _state.update { it.copy(proteinPer100g = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setCarbs(v: String) = _state.update { it.copy(carbsPer100g = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setFat(v: String) = _state.update { it.copy(fatPer100g = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setDefaultUnit(v: IngredientUnit) = _state.update { it.copy(defaultUnit = v) }
    fun setGramsPerUnit(v: String) = _state.update { it.copy(gramsPerUnit = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setLookupQuery(v: String) = _state.update { it.copy(lookupQuery = v) }

    fun runLookup() = viewModelScope.launch {
        val q = _state.value.lookupQuery
        if (q.isBlank()) return@launch
        _state.update { it.copy(lookupBusy = true, lookupMessage = null) }
        val result = calorieNinjas.lookup(q)
        when (result) {
            is LookupResult.Success -> {
                val item = result.item
                val servingG = if (item.servingSizeG > 0.0) item.servingSizeG else 100.0
                val factor = 100.0 / servingG
                _state.update {
                    it.copy(
                        lookupBusy = false,
                        lookupMessage = "Pre-filled from ${item.name}.",
                        name = if (it.name.isBlank()) item.name else it.name,
                        kcalPer100g = (item.calories * factor).toString(),
                        proteinPer100g = (item.proteinG * factor).toString(),
                        carbsPer100g = (item.carbohydratesTotalG * factor).toString(),
                        fatPer100g = (item.fatTotalG * factor).toString()
                    )
                }
            }
            LookupResult.MissingKey -> _state.update { it.copy(lookupBusy = false, lookupMessage = "No API key. Set it in Settings.") }
            LookupResult.NoResult -> _state.update { it.copy(lookupBusy = false, lookupMessage = "No match found.") }
            LookupResult.Unauthorized -> _state.update { it.copy(lookupBusy = false, lookupMessage = "API key rejected.") }
            LookupResult.RateLimited -> _state.update { it.copy(lookupBusy = false, lookupMessage = "Rate limited. Try later.") }
            is LookupResult.NetworkError -> _state.update { it.copy(lookupBusy = false, lookupMessage = result.message) }
        }
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.name.isBlank()) return@launch
        val ingredient = (existing ?: Ingredient(
            name = "", kcalPer100g = 0.0, proteinPer100g = 0.0, carbsPer100g = 0.0, fatPer100g = 0.0,
            defaultUnit = IngredientUnit.G, gramsPerUnit = 1.0, isUserCreated = true
        )).copy(
            name = s.name.trim(),
            kcalPer100g = s.kcalPer100g.toDoubleOrNull() ?: 0.0,
            proteinPer100g = s.proteinPer100g.toDoubleOrNull() ?: 0.0,
            carbsPer100g = s.carbsPer100g.toDoubleOrNull() ?: 0.0,
            fatPer100g = s.fatPer100g.toDoubleOrNull() ?: 0.0,
            defaultUnit = s.defaultUnit,
            gramsPerUnit = s.gramsPerUnit.toDoubleOrNull() ?: 1.0
        )
        if (ingredient.id > 0) repo.updateIngredient(ingredient) else repo.createIngredient(ingredient.copy(isUserCreated = true))
        _state.update { it.copy(saved = true) }
    }
}
