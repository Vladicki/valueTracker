package com.griffith.valuetracker.presentation

import com.griffith.valuetracker.data.repository.LoggedMealInput
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.data.repository.ProfileRepository
import com.griffith.valuetracker.domain.model.DailySummary
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.domain.model.UserProfile
import com.griffith.valuetracker.domain.model.WeightEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun statsViewModel_startsLoadingBeforeDataArrives() = runTest {
        val viewModel = StatsViewModel(
            nutritionRepository = StatsFakeNutritionRepository(DailySummary(1750, 2200, 112, 150, 125, 250, 39, 65, 0.75f, 0.5f, 0.6f, 2000)),
            profileRepository = StatsFakeProfileRepository(UserProfile(weightKg = 76.8f, heightCm = 175)),
        )

        val initialState = viewModel.uiState.value

        assertEquals(true, initialState.isLoading)
        assertEquals("0.0", initialState.bmiLabel)
    }

    @Test
    fun statsViewModel_exposesWeightAndNutritionHighlights() = runTest {
        val summary = DailySummary(1750, 2200, 112, 150, 125, 250, 39, 65, 0.75f, 0.5f, 0.6f, 2000)
        val profile = UserProfile(weightKg = 76.8f, heightCm = 175)
        val entries = listOf(
            WeightEntry(1, 78.5f, 1),
            WeightEntry(2, 77.6f, 2),
            WeightEntry(3, 76.8f, 3),
        )
        val viewModel = StatsViewModel(
            nutritionRepository = StatsFakeNutritionRepository(summary),
            profileRepository = StatsFakeProfileRepository(profile),
            initialWeights = entries,
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(3, state.weightPoints.size)
        assertEquals(1750, state.summary.consumedCalories)
        assertEquals("25.1", state.bmiLabel)
    }
}

private class StatsFakeNutritionRepository(
    private val summary: DailySummary,
) : NutritionRepository {
    override fun observeMeals(): Flow<List<Meal>> = flowOf(emptyList())
    override fun observeDailySummary(): Flow<DailySummary> = flowOf(summary)
    override fun observeDailySummarySince(cutoff: Long): Flow<DailySummary> = flowOf(summary)
    override fun observeDailySummaryBetween(startInclusive: Long, endExclusive: Long): Flow<DailySummary> = flowOf(summary)
    override fun observeMealHistorySince(cutoff: Long): Flow<List<Meal>> = flowOf(emptyList())
    override fun observeMealHistoryBetween(startInclusive: Long, endExclusive: Long): Flow<List<Meal>> = flowOf(emptyList())
    override suspend fun getHistoryMealDetails(mealId: Long) = throw NotImplementedError()
    override suspend fun logHistory(input: LoggedMealInput) = Unit
    override suspend fun updateHistoryMeal(meal: Meal, ingredients: List<com.griffith.valuetracker.data.repository.LoggedIngredientInput>, portionGrams: Float) = Unit
    override suspend fun deleteHistoryMeal(mealId: Long) = Unit
}

private class StatsFakeProfileRepository(
    private val profile: UserProfile,
) : ProfileRepository {
    override fun observeUserProfile(): Flow<UserProfile> = flowOf(profile)
    override suspend fun saveUserProfile(profile: UserProfile) = Unit
}
