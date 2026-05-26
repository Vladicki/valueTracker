package com.griffith.valuetracker.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.data.repository.ProfileRepository
import com.griffith.valuetracker.domain.model.DailySummary
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.ui.components.CalorieRing
import com.griffith.valuetracker.ui.components.DateSwitcher
import com.griffith.valuetracker.ui.components.DaySelector
import com.griffith.valuetracker.ui.components.FadeInScreen
import com.griffith.valuetracker.ui.components.MacroInlineText
import com.griffith.valuetracker.ui.components.MacroProgressBar
import com.griffith.valuetracker.ui.components.dayLabel
import com.griffith.valuetracker.ui.components.startOfDayMillis
import com.griffith.valuetracker.ui.components.weekStart
import com.griffith.valuetracker.ui.theme.AppCarbs
import com.griffith.valuetracker.ui.theme.AppFat
import com.griffith.valuetracker.ui.theme.AppProtein
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onEditMeal: (Meal) -> Unit = {},
    onRemoveMeal: (Long) -> Unit = { mealId -> viewModel.removeMeal(mealId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onEditMeal = onEditMeal,
        onRemoveMeal = onRemoveMeal,
        onDaySelected = viewModel::onDaySelected,
        onPreviousWeekClick = viewModel::selectPreviousWeek,
        onNextWeekClick = if (state.canSelectNextWeek) viewModel::selectNextWeek else null,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onEditMeal: (Meal) -> Unit,
    onRemoveMeal: (Long) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onPreviousWeekClick: () -> Unit,
    onNextWeekClick: (() -> Unit)?,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_loading"),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    FadeInScreen(testTag = "home_screen_fade") {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Weekly goals",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                DaySelector(
                    selectedDate = state.selectedDate,
                    onDaySelected = onDaySelected,
                    onPreviousWeekClick = onPreviousWeekClick,
                    onNextWeekClick = onNextWeekClick,
                )
            }
            item {
                DateSwitcher(
                    dateText = state.selectedDayLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // item {
            //     CalorieRing(
            //         current = (state.summary.targetCalories - state.summary.consumedCalories).coerceAtLeast(0),
            //         target = state.summary.targetCalories,
            //     )
            // }
            item {
                EnergyAndMacroSummary(
                    summary = state.summary,
                )
            }
            item {
                Text(
                    text = "Meals",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (state.selectedDayMeals.isEmpty()) {
                item {
                    EmptyMealsCard(state.selectedDayLabel)
                }
            } else {
                items(state.selectedDayMeals, key = { it.id }) { meal ->
                    RecentMealCard(
                        meal = meal,
                        onEditMeal = onEditMeal,
                        onRemoveMeal = onRemoveMeal,
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergyAndMacroSummary(
    summary: DailySummary,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CalorieRing(
            current = summary.consumedCalories,
            target = summary.targetCalories,
        )
        MacroProgressBar("Protein", summary.consumedProteinGrams, summary.targetProteinGrams, AppProtein)
        MacroProgressBar("Fat", summary.consumedFatGrams, summary.targetFatGrams, AppFat)
        MacroProgressBar("Carbs", summary.consumedCarbsGrams, summary.targetCarbsGrams, AppCarbs)
    }
}

@Composable
private fun EmptyMealsCard(selectedDayLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(vertical = 40.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No meals logged for $selectedDayLabel",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap + to add the first one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDayLabel: String = "Today",
    val canSelectNextWeek: Boolean = false,
    val summary: DailySummary = DailySummary(0, 0, 0, 0, 0, 0, 0, 0, 0f, 0f, 0f, 0),
    val selectedDayMeals: List<Meal> = emptyList(),
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val nutritionRepository: NutritionRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HomeUiState> = selectedDate.flatMapLatest { date ->
        val dayStart = date.startOfDayMillis()
        val nextDayStart = date.plusDays(1).startOfDayMillis()
        combine(
            nutritionRepository.observeDailySummaryBetween(dayStart, nextDayStart),
            nutritionRepository.observeMealHistoryBetween(dayStart, nextDayStart),
        ) { summary, history ->
            HomeUiState(
                isLoading = false,
                selectedDate = date,
                selectedDayLabel = dayLabel(date),
                canSelectNextWeek = weekStart(date).isBefore(weekStart(LocalDate.now())),
                summary = summary,
                selectedDayMeals = history,
            )
        }
    }.onStart {
        emit(HomeUiState(isLoading = true))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState(isLoading = true),
    )

    fun onDaySelected(date: LocalDate) {
        selectedDate.value = date
    }

    fun selectPreviousWeek() {
        selectedDate.value = selectedDate.value.minusWeeks(1)
    }

    fun selectNextWeek() {
        val nextWeekDate = selectedDate.value.plusWeeks(1)
        if (!weekStart(nextWeekDate).isAfter(weekStart(LocalDate.now()))) {
            selectedDate.value = nextWeekDate
        }
    }

    fun editMeal(meal: Meal) {
    }

    fun removeMeal(mealId: Long) {
        viewModelScope.launch {
            nutritionRepository.deleteHistoryMeal(mealId)
        }
    }
}

@Composable
private fun RecentMealCard(
    meal: Meal,
    onEditMeal: (Meal) -> Unit,
    onRemoveMeal: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (meal.imageUrl != null) {
            AsyncImage(
                model = meal.imageUrl,
                contentDescription = meal.title,
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🍽", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onEditMeal(meal) }
        ) {
            Text(
                text = meal.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${meal.calories} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MacroInlineText(
                    label = "p:",
                    value = "${meal.proteinGrams}g",
                    color = AppProtein,
                    style = MaterialTheme.typography.bodyMedium,
                )
                MacroInlineText(
                    label = "f:",
                    value = "${meal.fatGrams}g",
                    color = AppFat,
                    style = MaterialTheme.typography.bodyMedium,
                )
                MacroInlineText(
                    label = "c:",
                    value = "${meal.carbsGrams}g",
                    color = AppCarbs,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Recent meal options")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Edit meal") },
                    onClick = {
                        expanded = false
                        onEditMeal(meal)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Remove") },
                    onClick = {
                        expanded = false
                        onRemoveMeal(meal.id)
                    },
                )
            }
        }
    }
}

