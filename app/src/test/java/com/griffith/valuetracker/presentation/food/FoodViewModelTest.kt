package com.griffith.valuetracker.presentation.food

import com.griffith.valuetracker.data.SavedRecipeIngredientEntity
import com.griffith.valuetracker.data.repository.FoodRepository
import com.griffith.valuetracker.data.repository.SavedRecipeIngredientInput
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.domain.model.MealDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
class FoodViewModelTest {
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
    fun foodViewModel_loadsFindFoodsFromSql_forBlankAndTypedQuery() = runTest {
        val repository = FakeFoodRepository(
            favorites = listOf(
                Meal(1, "Saved Salmon Bowl", 620, 38, 55, 24, "Dinner", 1),
            ),
            history = listOf(
                Meal(3, "Chicken Breast", 165, 31, 0, 4, "120 g", 3),
                Meal(4, "Chicken Soup", 80, 6, 7, 2, "250 g", 4),
            ),
        )
        val viewModel = FoodViewModel(repository)

        advanceUntilIdle()
        assertEquals(listOf("Chicken Breast", "Chicken Soup"), viewModel.uiState.value.filteredHistoryMeals.map { it.title })

        viewModel.updateQuery("breast")
        advanceUntilIdle()

        assertEquals("breast", viewModel.uiState.value.query)
        assertEquals(listOf("Chicken Breast"), viewModel.uiState.value.filteredHistoryMeals.map { it.title })
    }

    @Test
    fun foodViewModel_keepsLatestQuery_whenEarlierSearchReturnsLater() = runTest {
        val repository = FakeFoodRepository(
            favorites = emptyList(),
            history = listOf(
                Meal(3, "Chicken Breast", 165, 31, 0, 4, "120 g", 3),
                Meal(4, "Chicken Soup", 80, 6, 7, 2, "250 g", 4),
            ),
            queryDelayMillis = mapOf("c" to 100L, "ch" to 0L),
        )
        val viewModel = FoodViewModel(repository)

        advanceUntilIdle()
        viewModel.updateQuery("c")
        viewModel.updateQuery("ch")
        advanceUntilIdle()

        assertEquals("ch", viewModel.uiState.value.query)
        assertEquals(listOf("Chicken Breast", "Chicken Soup"), viewModel.uiState.value.filteredHistoryMeals.map { it.title })
    }
}

private class FakeFoodRepository(
    private val favorites: List<Meal>,
    private val history: List<Meal>,
    private val queryDelayMillis: Map<String, Long> = emptyMap(),
) : FoodRepository {
    override suspend fun getFavoriteMeals(): List<Meal> = favorites

    override suspend fun getHistoryMeals(): List<Meal> = history

    override suspend fun getMealDetails(mealId: Long): MealDetails {
        throw NotImplementedError("Not used in FoodViewModel tests")
    }

    override suspend fun updateMealBookmark(mealId: Long, isBookmarked: Boolean) {
        throw NotImplementedError("Not used in FoodViewModel tests")
    }

    override suspend fun deleteMeal(mealId: Long) {
        throw NotImplementedError("Not used in FoodViewModel tests")
    }

    override suspend fun searchFoods(query: String): List<Meal> {
        queryDelayMillis[query]?.let { delay(it) }
        return if (query.isBlank()) {
            history
        } else {
            history.filter { it.title.contains(query, ignoreCase = true) }
        }
    }

    override fun observeSavedFoods(): Flow<List<Meal>> = emptyFlow()
    override fun observeSavedRecipes(): Flow<List<Meal>> = emptyFlow()
    override suspend fun getSavedRecipeIngredients(recipeId: Long): List<SavedRecipeIngredientEntity> = emptyList()
    override suspend fun setFoodSaved(fdcId: Long, saved: Boolean) = Unit
    override suspend fun setRecipeSaved(recipeId: Long, saved: Boolean) = Unit
    override suspend fun getSavedRecipesForBaseFood(fdcId: Long): List<Meal> = emptyList()
    override suspend fun saveRecipe(baseFood: Meal, ingredients: List<SavedRecipeIngredientInput>, overrideExisting: Boolean) = Unit
}
