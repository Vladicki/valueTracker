package com.griffith.valuetracker.presentation

import com.griffith.valuetracker.data.SavedRecipeIngredientEntity
import com.griffith.valuetracker.data.repository.FoodRepository
import com.griffith.valuetracker.data.repository.SavedRecipeIngredientInput
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.domain.model.MealDetails
import com.griffith.valuetracker.presentation.food.FoodTab
import com.griffith.valuetracker.presentation.food.FoodViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class SavedFoodsViewModelTest {
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
    fun foodViewModel_loadsSavedFoodsIntoSavedTab() = runTest {
        val repository = SavedFoodsFakeFoodRepository(
            savedFoods = listOf(Meal(167945, "Chicken Breast", 165, 31, 0, 4, "120 g", 0)),
        )
        val viewModel = FoodViewModel(repository)

        advanceUntilIdle()
        viewModel.selectTab(FoodTab.Saved)

        assertEquals(listOf("Chicken Breast"), viewModel.uiState.value.savedMeals.map { it.title })
        assertEquals(listOf("Chicken Breast"), viewModel.uiState.value.filteredSavedMeals.map { it.title })
    }

    @Test
    fun foodViewModel_filtersSavedFoodsByQuery() = runTest {
        val repository = SavedFoodsFakeFoodRepository(
            savedFoods = listOf(
                Meal(167945, "Chicken Breast", 165, 31, 0, 4, "120 g", 0),
                Meal(123456, "Salmon Fillet", 208, 20, 0, 13, "100 g", 0),
            ),
        )
        val viewModel = FoodViewModel(repository)

        advanceUntilIdle()
        viewModel.selectTab(FoodTab.Saved)
        viewModel.updateQuery("chicken")
        advanceUntilIdle()

        assertEquals(listOf("Chicken Breast"), viewModel.uiState.value.filteredSavedMeals.map { it.title })
    }
}

private class SavedFoodsFakeFoodRepository(
    savedFoods: List<Meal> = emptyList(),
) : FoodRepository {
    private val savedFoodsFlow = MutableStateFlow(savedFoods)

    override suspend fun getFavoriteMeals(): List<Meal> = emptyList()
    override suspend fun getHistoryMeals(): List<Meal> = emptyList()
    override suspend fun getMealDetails(mealId: Long): MealDetails = throw NoSuchElementException()
    override suspend fun updateMealBookmark(mealId: Long, isBookmarked: Boolean) = Unit
    override suspend fun deleteMeal(mealId: Long) = Unit
    override suspend fun searchFoods(query: String): List<Meal> = emptyList()
    override fun observeSavedFoods(): Flow<List<Meal>> = savedFoodsFlow.asStateFlow()
    override fun observeSavedRecipes(): Flow<List<Meal>> = flowOf(emptyList())
    override suspend fun getSavedRecipeIngredients(recipeId: Long): List<SavedRecipeIngredientEntity> = emptyList()
    override suspend fun setFoodSaved(fdcId: Long, saved: Boolean) = Unit
    override suspend fun setRecipeSaved(recipeId: Long, saved: Boolean) = Unit
    override suspend fun getSavedRecipesForBaseFood(fdcId: Long): List<Meal> = emptyList()
    override suspend fun saveRecipe(baseFood: Meal, ingredients: List<SavedRecipeIngredientInput>, overrideExisting: Boolean) = Unit
    override suspend fun getPortionPreference(foodId: Long): Float? = null
    override suspend fun savePortionPreference(foodId: Long, portionGrams: Float) = Unit
}
