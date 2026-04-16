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
import com.griffith.valuetracker.domain.model.WeightEntry
import com.griffith.valuetracker.ui.components.MacroProgressBar
import com.griffith.valuetracker.ui.components.StatsCard
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.compose.koinViewModel

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(state = state)
}

@Composable
fun StatsScreen(
    state: StatsUiState,
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroProgressBar("Protein", state.summary.consumedProteinGrams, state.summary.targetProteinGrams, com.griffith.valuetracker.ui.theme.AppProtein)
                MacroProgressBar("Carbs", state.summary.consumedCarbsGrams, state.summary.targetCarbsGrams, com.griffith.valuetracker.ui.theme.AppCarbs)
                MacroProgressBar("Fat", state.summary.consumedFatGrams, state.summary.targetFatGrams, com.griffith.valuetracker.ui.theme.AppFat)
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
    val summary: DailySummary = DailySummary(0, 0, 0, 0, 0, 0, 0, 0, 0f, 0f, 0f, 0),
    val weightPoints: List<WeightEntry> = emptyList(),
    val bmiLabel: String = "0.0",
)

class StatsViewModel(
    nutritionRepository: NutritionRepository,
    profileRepository: ProfileRepository,
    initialWeights: List<WeightEntry> = emptyList(),
) : ViewModel() {
    val uiState: StateFlow<StatsUiState> = combine(
        nutritionRepository.observeDailySummarySince(0),
        profileRepository.observeUserProfile(),
    ) { summary, profile ->
        val heightMeters = profile.heightCm / 100f
        val bmi = if (heightMeters > 0f) profile.weightKg / (heightMeters * heightMeters) else 0f
        StatsUiState(
            isLoading = false,
            summary = summary,
            weightPoints = initialWeights,
            bmiLabel = String.format(Locale.US, "%.1f", bmi),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = StatsUiState(isLoading = true),
    )
}
