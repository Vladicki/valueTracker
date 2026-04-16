package com.griffith.valuetracker.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.griffith.valuetracker.data.repository.FoodRepository
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.ui.components.FadeInScreen
import com.griffith.valuetracker.ui.components.MealCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddFoodScreen(
    viewModel: AddFoodViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AddFoodScreen(
        state = state,
        onQueryChange = viewModel::updateQuery,
        onLogMeal = viewModel::logSuggestion,
    )
}

@Composable
fun AddFoodScreen(
    state: AddFoodUiState,
    onQueryChange: (String) -> Unit,
    onLogMeal: (Meal) -> Unit,
) {
    FadeInScreen(testTag = "add_food_screen_fade") {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Add food",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = { Text("Search foods") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            items(state.filteredSuggestions, key = { it.id }) { meal ->
                MealCard(
                    title = meal.title,
                    calories = meal.calories,
                    onClick = { onLogMeal(meal) },
                )
            }
        }
    }
}

data class AddFoodUiState(
    val query: String = "",
    val filteredSuggestions: List<Meal> = emptyList(),
)

class AddFoodViewModel(
    private val nutritionRepository: NutritionRepository,
    private val foodRepository: FoodRepository,
) : ViewModel() {
    private var latestSearchToken = 0L
    private val _uiState = MutableStateFlow(AddFoodUiState())
    val uiState: StateFlow<AddFoodUiState> = _uiState.asStateFlow()

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        val searchToken = ++latestSearchToken
        viewModelScope.launch {
            val suggestions = if (query.isBlank()) emptyList() else foodRepository.searchFoods(query)
            if (searchToken != latestSearchToken || _uiState.value.query != query) return@launch
            _uiState.update {
                it.copy(
                    filteredSuggestions = suggestions,
                )
            }
        }
    }

    fun logSuggestion(meal: Meal) {
        viewModelScope.launch {
            nutritionRepository.logHistory(
                com.griffith.valuetracker.data.repository.LoggedMealInput(
                    sourceFdcId = meal.id,
                    sourceRecipeId = null,
                    displayName = meal.title,
                    portionGrams = meal.mealType.removeSuffix(" g").toFloatOrNull() ?: 100f,
                    calories = meal.calories,
                    proteinGrams = meal.proteinGrams,
                    carbsGrams = meal.carbsGrams,
                    fatGrams = meal.fatGrams,
                    mealTypeLabel = meal.mealType,
                    ingredients = emptyList(),
                )
            )
        }
    }
}
