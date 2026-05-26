package com.griffith.valuetracker.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.data.repository.ProfileRepository
import com.griffith.valuetracker.domain.model.DailySummary
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.domain.model.WeightEntry
import com.griffith.valuetracker.ui.components.DateSwitcher
import com.griffith.valuetracker.ui.components.DaySelector
import com.griffith.valuetracker.ui.components.MealCard
import com.griffith.valuetracker.ui.components.MacroProgressBar
import com.griffith.valuetracker.ui.components.StatsCard
import com.griffith.valuetracker.ui.components.dayLabel
import com.griffith.valuetracker.ui.components.startOfDayMillis
import com.griffith.valuetracker.ui.components.weekStart
import com.griffith.valuetracker.ui.theme.AppCarbs
import com.griffith.valuetracker.ui.theme.AppFat
import com.griffith.valuetracker.ui.theme.AppProtein
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.compose.koinViewModel

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(
        state = state,
        onDaySelected = viewModel::onDaySelected,
        onPreviousWeekClick = viewModel::selectPreviousWeek,
        onNextWeekClick = if (state.canSelectNextWeek) viewModel::selectNextWeek else null,
    )
}

@Composable
fun StatsScreen(
    state: StatsUiState,
    onDaySelected: (LocalDate) -> Unit,
    onPreviousWeekClick: () -> Unit,
    onNextWeekClick: (() -> Unit)?,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatsCard(title = "BMI", value = state.bmiLabel, modifier = Modifier.fillMaxWidth())
                StatsCard(title = "Weight entries", value = state.weightPoints.size.toString(), modifier = Modifier.fillMaxWidth())
            }
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
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroProgressBar("Protein", state.summary.consumedProteinGrams, state.summary.targetProteinGrams, AppProtein)
                MacroProgressBar("Carbs", state.summary.consumedCarbsGrams, state.summary.targetCarbsGrams, AppCarbs)
                MacroProgressBar("Fat", state.summary.consumedFatGrams, state.summary.targetFatGrams, AppFat)
            }
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
                EmptyStatsMealsCard(state.selectedDayLabel)
            }
        } else {
            items(state.selectedDayMeals, key = { it.id }) { meal ->
                MealCard(
                    title = meal.title,
                    calories = meal.calories,
                    proteinGrams = meal.proteinGrams,
                    fatGrams = meal.fatGrams,
                    carbsGrams = meal.carbsGrams,
                    compact = true,
                    onClick = {},
                )
            }
        }
        item {
            Text(
                text = "Weight trend",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        items(state.weightPoints, key = { it.id }) { point ->
            StatsCard(
                title = "Entry ${point.id}",
                value = "${point.weightKg} kg",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

data class StatsUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDayLabel: String = "Today",
    val canSelectNextWeek: Boolean = false,
    val summary: DailySummary = DailySummary(0, 0, 0, 0, 0, 0, 0, 0, 0f, 0f, 0f, 0),
    val selectedDayMeals: List<Meal> = emptyList(),
    val weightPoints: List<WeightEntry> = emptyList(),
    val bmiLabel: String = "0.0",
)

@Composable
private fun EmptyStatsMealsCard(selectedDayLabel: String) {
    Text(
        text = "No meals logged for $selectedDayLabel",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StatsViewModel(
    nutritionRepository: NutritionRepository,
    profileRepository: ProfileRepository,
    initialWeights: List<WeightEntry> = emptyList(),
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<StatsUiState> = selectedDate.flatMapLatest { date ->
        val dayStart = date.startOfDayMillis()
        val nextDayStart = date.plusDays(1).startOfDayMillis()
        combine(
            nutritionRepository.observeDailySummaryBetween(dayStart, nextDayStart),
            nutritionRepository.observeMealHistoryBetween(dayStart, nextDayStart),
            profileRepository.observeUserProfile(),
        ) { summary, meals, profile ->
            val heightMeters = profile.heightCm / 100f
            val bmi = if (heightMeters > 0f) profile.weightKg / (heightMeters * heightMeters) else 0f
            StatsUiState(
                isLoading = false,
                selectedDate = date,
                selectedDayLabel = dayLabel(date),
                canSelectNextWeek = weekStart(date).isBefore(weekStart(LocalDate.now())),
                summary = summary,
                selectedDayMeals = meals,
                weightPoints = initialWeights,
                bmiLabel = String.format(Locale.US, "%.1f", bmi),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = StatsUiState(isLoading = true),
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
}
