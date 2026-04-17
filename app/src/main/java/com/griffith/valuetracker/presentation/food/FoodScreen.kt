package com.griffith.valuetracker.presentation.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.griffith.valuetracker.data.repository.FoodRepository
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
fun FoodScreen(
    onAddFoodClick: (Meal) -> Unit = {},
    initialTab: FoodTab = FoodTab.FindFoods,
    viewModel: FoodViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialTab) {
        viewModel.selectTab(initialTab)
    }
    FoodScreen(
        state = state,
        onQueryChange = viewModel::updateQuery,
        onTabSelected = viewModel::selectTab,
        onAddFoodClick = onAddFoodClick,
        onUnsaveMeal = viewModel::unsaveMeal,
    )
}

@Composable
fun FoodScreen(
    state: FoodUiState,
    onQueryChange: (String) -> Unit,
    onTabSelected: (FoodTab) -> Unit,
    onAddFoodClick: (Meal) -> Unit,
    onUnsaveMeal: (Long) -> Unit,
) {
    val meals = if (state.selectedTab == FoodTab.FindFoods) {
        state.filteredHistoryMeals
    } else {
        state.filteredSavedMeals
    }

    FadeInScreen(testTag = "food_screen_fade") {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                    Tab(
                        selected = state.selectedTab == FoodTab.FindFoods,
                        onClick = { onTabSelected(FoodTab.FindFoods) },
                        modifier = Modifier.testTag("food_tab_find"),
                        text = { Text("Find Foods") },
                    )
                    Tab(
                        selected = state.selectedTab == FoodTab.Saved,
                        onClick = { onTabSelected(FoodTab.Saved) },
                        modifier = Modifier.testTag("food_tab_saved"),
                        text = { Text("Saved") },
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = { Text(if (state.selectedTab == FoodTab.FindFoods) "Search foods" else "Search saved meals") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            items(meals, key = { it.id }) { meal ->
                MealCard(
                    title = meal.title,
                    calories = meal.calories,
                    portion = meal.mealType,
                    proteinGrams = meal.proteinGrams,
                    fatGrams = meal.fatGrams,
                    carbsGrams = meal.carbsGrams,
                    compact = true,
                    onClick = { onAddFoodClick(meal) },
                    trailingContent = if (state.selectedTab == FoodTab.Saved) {
                        {
                            IconButton(onClick = { onUnsaveMeal(meal.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Unsave meal",
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else null,
                )
            }
        }
    }
}

data class FoodUiState(
    val query: String = "",
    val selectedTab: FoodTab = FoodTab.FindFoods,
    val savedMeals: List<Meal> = emptyList(),
    val historyMeals: List<Meal> = emptyList(),
    val filteredSavedMeals: List<Meal> = emptyList(),
    val filteredHistoryMeals: List<Meal> = emptyList(),
)

enum class FoodTab {
    FindFoods,
    Saved,
}

class FoodViewModel(
    private val foodRepository: FoodRepository,
) : ViewModel() {
    private var latestSearchToken = 0L
    private val _uiState = MutableStateFlow(FoodUiState())
    val uiState: StateFlow<FoodUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val findFoods = foodRepository.searchFoods("")
            _uiState.update {
                it.copy(
                    historyMeals = findFoods,
                    filteredHistoryMeals = findFoods,
                )
            }
        }
        viewModelScope.launch {
            foodRepository.observeSavedFoods().collect { savedFoods ->
                _uiState.update { state ->
                    state.copy(
                        savedMeals = savedFoods,
                        filteredSavedMeals = filterSavedMeals(savedFoods, state.query),
                    )
                }
            }
        }
    }

    fun updateQuery(query: String) {
        // Saved meals filter locally, while catalog search is async and token-guarded against stale responses.
        _uiState.update { state ->
            state.copy(
                query = query,
                filteredSavedMeals = filterSavedMeals(state.savedMeals, query),
            )
        }
        val searchToken = ++latestSearchToken
        viewModelScope.launch {
            val findFoods = foodRepository.searchFoods(query)
            if (searchToken != latestSearchToken || _uiState.value.query != query) return@launch
            _uiState.update { state ->
                state.copy(filteredHistoryMeals = findFoods)
            }
        }
    }

    fun selectTab(tab: FoodTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun filterSavedMeals(meals: List<Meal>, query: String): List<Meal> =
        meals.filter { it.title.contains(query, ignoreCase = true) }

    fun unsaveMeal(mealId: Long) {
        viewModelScope.launch {
            foodRepository.setFoodSaved(mealId, false)
        }
    }
}
