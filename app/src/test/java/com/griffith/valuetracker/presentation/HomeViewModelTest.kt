package com.griffith.valuetracker.presentation

import com.griffith.valuetracker.data.repository.LoggedMealInput
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.data.repository.ProfileRepository
import com.griffith.valuetracker.domain.model.DailySummary
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.domain.model.UserProfile
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
class HomeViewModelTest {
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
    fun homeViewModel_startsLoadingBeforeDataIsReady() = runTest {
        val summary = DailySummary(
            consumedCalories = 1850,
            targetCalories = 2200,
            consumedProteinGrams = 120,
            targetProteinGrams = 150,
            consumedCarbsGrams = 175,
            targetCarbsGrams = 250,
            consumedFatGrams = 39,
            targetFatGrams = 65,
            proteinProgress = 0.8f,
            carbsProgress = 0.7f,
            fatProgress = 0.6f,
            waterMl = 1800,
        )
        val meals = listOf(
            Meal(1, "Greek Yogurt", 180, 15, 20, 5, "Snack", 1),
            Meal(2, "Salmon Bowl", 620, 38, 55, 24, "Dinner", 2),
            Meal(3, "Chicken Salad", 480, 42, 18, 22, "Lunch", 3),
            Meal(4, "Oatmeal", 350, 12, 58, 8, "Breakfast", 4),
        )
        val profile = UserProfile(firstName = "Vlad")
        val viewModel = HomeViewModel(
            nutritionRepository = HomeFakeNutritionRepository(summary, meals),
            profileRepository = HomeFakeProfileRepository(profile),
        )

        assertEquals(true, viewModel.uiState.value.isLoading)
    }

    @Test
    fun homeViewModel_exposesSummaryAndLast24HourHistoryAfterLoadingCompletes() = runTest {
        val summary = DailySummary(
            consumedCalories = 1850,
            targetCalories = 2200,
            consumedProteinGrams = 120,
            targetProteinGrams = 150,
            consumedCarbsGrams = 175,
            targetCarbsGrams = 250,
            consumedFatGrams = 39,
            targetFatGrams = 65,
            proteinProgress = 0.8f,
            carbsProgress = 0.7f,
            fatProgress = 0.6f,
            waterMl = 1800,
        )
        val meals = listOf(
            Meal(1, "Greek Yogurt", 180, 15, 20, 5, "Snack", 1),
            Meal(2, "Salmon Bowl", 620, 38, 55, 24, "Dinner", 2),
            Meal(3, "Chicken Salad", 480, 42, 18, 22, "Lunch", 3),
            Meal(4, "Oatmeal", 350, 12, 58, 8, "Breakfast", 4),
        )
        val profile = UserProfile(firstName = "Vlad")
        val viewModel = HomeViewModel(
            nutritionRepository = HomeFakeNutritionRepository(summary, meals),
            profileRepository = HomeFakeProfileRepository(profile),
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(1850, state.summary.consumedCalories)
        assertEquals(listOf("Greek Yogurt", "Salmon Bowl", "Chicken Salad", "Oatmeal"), state.recentMeals.map { it.title })
        assertEquals("Today", state.selectedDayLabel)
    }
}

private class HomeFakeNutritionRepository(
    private val summary: DailySummary,
    private val meals: List<Meal>,
) : NutritionRepository {
    override fun observeMeals(): Flow<List<Meal>> = flowOf(meals)
    override fun observeDailySummary(): Flow<DailySummary> = flowOf(summary)
    override fun observeDailySummarySince(cutoff: Long): Flow<DailySummary> = flowOf(summary)
    override fun observeMealHistorySince(cutoff: Long): Flow<List<Meal>> = flowOf(meals)
    override suspend fun getHistoryMealDetails(mealId: Long) = throw NotImplementedError()
    override suspend fun logHistory(input: LoggedMealInput) = Unit
    override suspend fun updateHistoryMeal(meal: Meal, ingredients: List<com.griffith.valuetracker.data.repository.LoggedIngredientInput>, portionGrams: Float) = Unit
    override suspend fun deleteHistoryMeal(mealId: Long) = Unit
}

private class HomeFakeProfileRepository(
    private val profile: UserProfile,
) : ProfileRepository {
    override fun observeUserProfile(): Flow<UserProfile> = flowOf(profile)
    override suspend fun saveUserProfile(profile: UserProfile) = Unit
}
