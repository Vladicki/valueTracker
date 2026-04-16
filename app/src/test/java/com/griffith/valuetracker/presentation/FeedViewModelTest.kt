package com.griffith.valuetracker.presentation

import com.griffith.valuetracker.data.repository.LoggedMealInput
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.domain.model.DailySummary
import com.griffith.valuetracker.domain.model.Meal
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
class FeedViewModelTest {
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
    fun feedViewModel_exposesLoggedMealsNewestFirst() = runTest {
        val meals = listOf(
            Meal(1, "Breakfast", 320, 20, 30, 10, "Breakfast", 10),
            Meal(2, "Lunch", 640, 35, 50, 18, "Lunch", 20),
        )
        val viewModel = FeedViewModel(FeedFakeNutritionRepository(meals))

        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.meals.size)
        assertEquals("Breakfast", viewModel.uiState.value.meals.first().title)
    }
}

private class FeedFakeNutritionRepository(
    private val meals: List<Meal>,
) : NutritionRepository {
    override fun observeMeals(): Flow<List<Meal>> = flowOf(meals)
    override fun observeDailySummary(): Flow<DailySummary> = flowOf(DailySummary(0, 0, 0, 0, 0, 0, 0, 0, 0f, 0f, 0f, 0))
    override fun observeDailySummarySince(cutoff: Long): Flow<DailySummary> = flowOf(DailySummary(0, 0, 0, 0, 0, 0, 0, 0, 0f, 0f, 0f, 0))
    override fun observeMealHistorySince(cutoff: Long): Flow<List<Meal>> = flowOf(meals)
    override suspend fun getHistoryMealDetails(mealId: Long) = throw NotImplementedError()
    override suspend fun logHistory(input: LoggedMealInput) = Unit
    override suspend fun updateHistoryMeal(meal: Meal, ingredients: List<com.griffith.valuetracker.data.repository.LoggedIngredientInput>, portionGrams: Float) = Unit
    override suspend fun deleteHistoryMeal(mealId: Long) = Unit
}
