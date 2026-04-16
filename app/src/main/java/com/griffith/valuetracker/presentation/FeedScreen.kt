package com.griffith.valuetracker.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.domain.model.Meal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Composable
fun FeedScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Feed Screen - Placeholder")
    }
}

data class FeedUiState(
    val meals: List<Meal> = emptyList(),
)

class FeedViewModel(
    nutritionRepository: NutritionRepository,
) : ViewModel() {
    val uiState: StateFlow<FeedUiState> = nutritionRepository.observeMeals()
        .map { meals -> FeedUiState(meals = meals) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FeedUiState(),
        )
}
