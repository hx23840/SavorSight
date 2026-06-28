package com.savorsight.savorsight

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val _listState = MutableStateFlow(RecipeListUiState())
    val listState: StateFlow<RecipeListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(RecipeDetailUiState())
    val detailState: StateFlow<RecipeDetailUiState> = _detailState.asStateFlow()

    private val apiClient = SavorSightApiClient(BASE_URL)

    fun loadRecipeList() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, errorMessage = null)

            val result = apiClient.listRecipes()
            if (result.isSuccess) {
                val list = result.getOrThrow()
                _listState.value = _listState.value.copy(
                    isLoading = false,
                    recipes = list.map {
                        RecipeSummaryData(
                            recipeId = it.recipeId,
                            dishName = it.dishName,
                            servings = it.servings,
                            estimatedTimeMinutes = it.estimatedTimeMinutes,
                            confidence = it.confidence,
                        )
                    },
                )
            } else {
                _listState.value = _listState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "加载菜谱列表失败"
                )
            }
        }
    }

    fun selectRecipe(recipeId: String, onLoaded: () -> Unit) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, errorMessage = null)

            val result = apiClient.getFullRecipe(recipeId)
            if (result.isSuccess) {
                val recipe = result.getOrThrow()
                _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    recipe = RecipeData(
                        recipeId = recipe.recipeId,
                        dishName = recipe.dishName,
                        servings = recipe.servings,
                        estimatedTimeMinutes = recipe.estimatedTimeMinutes,
                        ingredients = recipe.ingredients,
                        steps = recipe.steps,
                    ),
                    isConfirmed = true,
                )
                onLoaded()
            } else {
                _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "加载菜谱失败"
                )
            }
        }
    }

    fun loadRecipeDraft(recipeId: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, errorMessage = null)

            val result = apiClient.getFullRecipe(recipeId)
            if (result.isSuccess) {
                val recipe = result.getOrThrow()
                _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    recipe = RecipeData(
                        recipeId = recipe.recipeId,
                        dishName = recipe.dishName,
                        servings = recipe.servings,
                        estimatedTimeMinutes = recipe.estimatedTimeMinutes,
                        ingredients = recipe.ingredients,
                        steps = recipe.steps,
                    ),
                )
            } else {
                _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "加载菜谱失败"
                )
            }
        }
    }

    fun confirmRecipeDraft(onConfirmed: () -> Unit) {
        val recipe = _detailState.value.recipe ?: return

        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, errorMessage = null)

            val result = apiClient.confirmRecipe(recipe.recipeId)
            if (result.isSuccess) {
                _detailState.value = _detailState.value.copy(isConfirmed = true)
                loadRecipeList()
                onConfirmed()
            } else {
                _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "确认菜谱失败"
                )
            }
        }
    }

    fun startCooking(onStarted: () -> Unit) {
        if (_detailState.value.recipe != null) {
            _detailState.value = _detailState.value.copy(
                currentStepIndex = 0,
                isCooking = true
            )
            onStarted()
        }
    }

    fun nextStep(): Boolean {
        val state = _detailState.value
        val steps = state.recipe?.steps ?: return false
        val nextIndex = state.currentStepIndex + 1
        return if (nextIndex < steps.size) {
            _detailState.value = state.copy(currentStepIndex = nextIndex)
            true
        } else {
            false
        }
    }

    fun prevStep(): Boolean {
        val state = _detailState.value
        val prevIndex = state.currentStepIndex - 1
        return if (prevIndex >= 0) {
            _detailState.value = state.copy(currentStepIndex = prevIndex)
            true
        } else {
            false
        }
    }

    fun finishCooking() {
        _detailState.value = RecipeDetailUiState()
    }

    fun resetDetail() {
        _detailState.value = RecipeDetailUiState()
    }

    companion object {
        private const val TAG = "RecipeViewModel"
        private const val BASE_URL = "http://10.0.2.2:8080"
    }
}

data class RecipeListUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val recipes: List<RecipeSummaryData> = emptyList(),
)

data class RecipeSummaryData(
    val recipeId: String,
    val dishName: String,
    val servings: Int,
    val estimatedTimeMinutes: Int,
    val confidence: Double,
)

data class RecipeDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val recipe: RecipeData? = null,
    val isConfirmed: Boolean = false,
    val isCooking: Boolean = false,
    val currentStepIndex: Int = 0,
)

data class RecipeData(
    val recipeId: String,
    val dishName: String,
    val servings: Int,
    val estimatedTimeMinutes: Int,
    val ingredients: List<String>,
    val steps: List<RecipeStepData>,
)
