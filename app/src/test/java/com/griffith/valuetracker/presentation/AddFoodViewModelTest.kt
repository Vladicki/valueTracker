package com.griffith.valuetracker.presentation

import com.griffith.valuetracker.data.SavedRecipeIngredientEntity
import com.griffith.valuetracker.data.repository.FoodRepository
import com.griffith.valuetracker.data.repository.LoggedMealInput
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.data.repository.SavedRecipeIngredientInput
import com.griffith.valuetracker.domain.model.DailySummary
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.domain.model.MealDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
class AddFoodViewModelTest {
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
    fun addFoodViewModel_startsWithEmptyList_untilSqlQueryRuns() = runTest {
        val viewModel = AddFoodViewModel(AddFoodFakeNutritionRepository(), FakeFoodRepository())

        assertEquals(emptyList(), viewModel.uiState.value.filteredSuggestions)
    }

    @Test
    fun addFoodViewModel_searchesSqlBackedFoodRepository_and_logsSelectedMeal() = runTest {
        val nutritionRepository = AddFoodFakeNutritionRepository()
        val foodRepository = FakeFoodRepository(
            searchResults = listOf(
                Meal(
                    id = 1001,
                    title = "Chicken Breast",
                    calories = 165,
                    proteinGrams = 31,
                    carbsGrams = 0,
                    fatGrams = 4,
                    mealType = "100 g",
                    loggedAtEpochMillis = 0,
                ),
            ),
        )
        val viewModel = AddFoodViewModel(nutritionRepository, foodRepository)

        viewModel.updateQuery("chicken")
        advanceUntilIdle()
        assertEquals(listOf("Chicken Breast"), viewModel.uiState.value.filteredSuggestions.map { it.title })

        viewModel.logSuggestion(viewModel.uiState.value.filteredSuggestions.first())
        advanceUntilIdle()
        assertEquals(1, nutritionRepository.addedMeals.size)
        assertEquals("Chicken Breast", nutritionRepository.addedMeals.first().title)
    }

    @Test
    fun addFoodViewModel_ignoresStaleSearchResults() = runTest {
        val viewModel = AddFoodViewModel(
            AddFoodFakeNutritionRepository(),
            FakeFoodRepository(
                resultsByQuery = mapOf(
                    "c" to listOf(Meal(1, "Cookie", 100, 1, 15, 4, "30 g", 0)),
                    "ch" to listOf(Meal(2, "Chicken Breast", 165, 31, 0, 4, "100 g", 0)),
                ),
                queryDelayMillis = mapOf(
                    "c" to 100,
                    "ch" to 10,
                ),
            ),
        )

        viewModel.updateQuery("c")
        viewModel.updateQuery("ch")
        advanceUntilIdle()

        assertEquals("ch", viewModel.uiState.value.query)
        assertEquals(listOf("Chicken Breast"), viewModel.uiState.value.filteredSuggestions.map { it.title })
    }
}

private class AddFoodFakeNutritionRepository : NutritionRepository {
    val addedMeals = mutableListOf<Meal>()

    override fun observeMeals(): Flow<List<Meal>> = flowOf(emptyList())
    override fun observeDailySummary(): Flow<DailySummary> = flowOf(
        DailySummary(0, 2200, 0, 0, 0, 0, 0, 0, 0f, 0f, 0f, 0),
    )
    override fun observeDailySummarySince(cutoff: Long): Flow<DailySummary> = flowOf(
        DailySummary(0, 2200, 0, 0, 0, 0, 0, 0, 0f, 0f, 0f, 0),
    )
    override fun observeDailySummaryBetween(startInclusive: Long, endExclusive: Long): Flow<DailySummary> = flowOf(
        DailySummary(0, 2200, 0, 0, 0, 0, 0, 0, 0f, 0f, 0f, 0),
    )
    override fun observeMealHistorySince(cutoff: Long): Flow<List<Meal>> = flowOf(emptyList())
    override fun observeMealHistoryBetween(startInclusive: Long, endExclusive: Long): Flow<List<Meal>> = flowOf(emptyList())
    override suspend fun getHistoryMealDetails(mealId: Long) = throw NotImplementedError()

    override suspend fun logHistory(input: LoggedMealInput) {
        addedMeals += Meal(
            id = input.sourceFdcId ?: 0,
            title = input.displayName,
            calories = input.calories,
            proteinGrams = input.proteinGrams,
            carbsGrams = input.carbsGrams,
            fatGrams = input.fatGrams,
            mealType = input.mealTypeLabel,
            loggedAtEpochMillis = 0,
        )
    }

    override suspend fun updateHistoryMeal(meal: Meal, ingredients: List<com.griffith.valuetracker.data.repository.LoggedIngredientInput>, portionGrams: Float) = Unit
    override suspend fun deleteHistoryMeal(mealId: Long) = Unit
}

private class FakeFoodRepository(
    private val searchResults: List<Meal> = emptyList(),
    private val resultsByQuery: Map<String, List<Meal>> = emptyMap(),
    private val queryDelayMillis: Map<String, Long> = emptyMap(),
) : FoodRepository {
    override suspend fun getFavoriteMeals(): List<Meal> = emptyList()
    override suspend fun getHistoryMeals(): List<Meal> = emptyList()
    override suspend fun getMealDetails(mealId: Long): MealDetails = throw NoSuchElementException()
    override suspend fun updateMealBookmark(mealId: Long, isBookmarked: Boolean) = Unit
    override suspend fun deleteMeal(mealId: Long) = Unit
    override suspend fun searchFoods(query: String): List<Meal> {
        queryDelayMillis[query]?.let { delay(it) }
        return resultsByQuery[query] ?: searchResults.filter { it.title.contains(query, ignoreCase = true) }
    }
    override fun observeSavedFoods(): Flow<List<Meal>> = emptyFlow()
    override fun observeSavedRecipes(): Flow<List<Meal>> = emptyFlow()
    override suspend fun getSavedRecipeIngredients(recipeId: Long): List<SavedRecipeIngredientEntity> = emptyList()
    override suspend fun setFoodSaved(fdcId: Long, saved: Boolean) = Unit
    override suspend fun setRecipeSaved(recipeId: Long, saved: Boolean) = Unit
    override suspend fun getSavedRecipesForBaseFood(fdcId: Long): List<Meal> = emptyList()
    override suspend fun saveRecipe(baseFood: Meal, ingredients: List<SavedRecipeIngredientInput>, overrideExisting: Boolean) = Unit
}
