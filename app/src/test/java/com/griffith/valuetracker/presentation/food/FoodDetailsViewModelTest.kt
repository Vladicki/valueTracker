package com.griffith.valuetracker.presentation.food

import com.griffith.valuetracker.data.SavedRecipeIngredientEntity
import com.griffith.valuetracker.data.repository.FoodRepository
import com.griffith.valuetracker.data.repository.LoggedIngredientInput
import com.griffith.valuetracker.data.repository.LoggedMealInput
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.data.repository.SavedRecipeIngredientInput
import com.griffith.valuetracker.domain.model.Ingredient
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.domain.model.MealDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FoodDetailsViewModelTest {
    private lateinit var foodRepository: FoodRepository
    private lateinit var nutritionRepository: NutritionRepository
    private lateinit var viewModel: FoodDetailsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        foodRepository = mockk()
        nutritionRepository = mockk(relaxed = true)
        coEvery { foodRepository.getPortionPreference(any()) } returns null
        coEvery { foodRepository.savePortionPreference(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadMealDetails sets loading state then loads meal details`() = runTest {
        val mealId = 201L
        val mealDetails = MealDetails(
            id = mealId,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "Dinner",
            servings = 1.0f,
            hasOil = true,
            addedOilGrams = 0,
            isBookmarked = false,
            ingredients = listOf(
                Ingredient("Salmon", 200, 280, 28, 0, 18),
                Ingredient("Rice", 150, 195, 4, 43, 0),
                Ingredient("Vegetables", 100, 45, 2, 8, 0),
            ),
        )
        coEvery { foodRepository.getMealDetails(mealId) } returns mealDetails
        coEvery { foodRepository.getPortionPreference(mealId) } returns null

        viewModel = FoodDetailsViewModel(mealId, false, null, foodRepository, nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.mealDetails)
        assertEquals("Salmon Bowl", state.mealDetails?.title)
        assertEquals(620, state.mealDetails?.calculatedCalories)
    }

    @Test
    fun `loadMealDetails applies saved portion preference`() = runTest {
        val mealId = 201L
        val mealDetails = MealDetails(
            id = mealId,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "120 g",
            servings = 1.0f,
            ingredients = emptyList(),
        )
        coEvery { foodRepository.getMealDetails(mealId) } returns mealDetails
        coEvery { foodRepository.getPortionPreference(mealId) } returns 180f

        viewModel = FoodDetailsViewModel(mealId, false, null, foodRepository, nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1.5f, viewModel.uiState.value.mealDetails?.servings)
    }

    @Test
    fun `updateServings recalculates all nutrition values`() = runTest {
        val mealId = 201L
        val mealDetails = MealDetails(
            id = mealId,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "Dinner",
            servings = 1.0f,
            hasOil = true,
            addedOilGrams = 0,
            isBookmarked = false,
            ingredients = listOf(
                Ingredient("Salmon", 200, 280, 28, 0, 18),
                Ingredient("Rice", 150, 195, 4, 43, 0),
                Ingredient("Vegetables", 100, 45, 2, 8, 0),
            ),
            secondaryNutrients = listOf(
                com.griffith.valuetracker.domain.model.SecondaryNutrient("Calcium", "12.0 mg"),
                com.griffith.valuetracker.domain.model.SecondaryNutrient("Vitamin C", "1.5 mg"),
            ),
        )
        coEvery { foodRepository.getMealDetails(mealId) } returns mealDetails

        viewModel = FoodDetailsViewModel(mealId, false, null, foodRepository, nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateServings(2.0f)

        val state = viewModel.uiState.value
        assertEquals(2.0f, state.mealDetails?.servings)
        assertEquals(1240, state.mealDetails?.calculatedCalories)
        assertEquals(76, state.mealDetails?.calculatedProteinGrams)
        assertEquals(110, state.mealDetails?.calculatedCarbsGrams)
        assertEquals(48, state.mealDetails?.calculatedFatGrams)
        assertEquals("24.0 mg", state.mealDetails?.calculatedSecondaryNutrients?.first { it.label == "Calcium" }?.value)
        assertEquals("3.0 mg", state.mealDetails?.calculatedSecondaryNutrients?.first { it.label == "Vitamin C" }?.value)
    }

    @Test
    fun `updateAddedOil recalculates calories and fat only`() = runTest {
        val mealId = 201L
        val mealDetails = MealDetails(
            id = mealId,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "Dinner",
            servings = 1.0f,
            hasOil = true,
            addedOilGrams = 0,
            isBookmarked = false,
            ingredients = listOf(),
        )
        coEvery { foodRepository.getMealDetails(mealId) } returns mealDetails

        viewModel = FoodDetailsViewModel(mealId, false, null, foodRepository, nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAddedOil(10)

        val state = viewModel.uiState.value
        assertEquals(10, state.mealDetails?.addedOilGrams)
        // Oil: 9 kcal/g, 1g fat per gram
        assertEquals(710, state.mealDetails?.calculatedCalories) // 620 + 90
        assertEquals(38, state.mealDetails?.calculatedProteinGrams) // unchanged
        assertEquals(55, state.mealDetails?.calculatedCarbsGrams) // unchanged
        assertEquals(34, state.mealDetails?.calculatedFatGrams) // 24 + 10
    }

    @Test
    fun `toggleBookmark updates bookmark state and calls repository`() = runTest {
        val mealId = 201L
        val mealDetails = MealDetails(
            id = mealId,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "Dinner",
            servings = 1.0f,
            hasOil = false,
            addedOilGrams = 0,
            isBookmarked = false,
            ingredients = listOf(),
        )
        coEvery { foodRepository.getMealDetails(mealId) } returns mealDetails
        coEvery { foodRepository.updateMealBookmark(mealId, any()) } returns Unit

        viewModel = FoodDetailsViewModel(mealId, false, null, foodRepository, nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleBookmark()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.mealDetails?.isBookmarked == true)
        coVerify { foodRepository.updateMealBookmark(mealId, true) }
    }

    @Test
    fun `deleteMeal calls repository delete`() = runTest {
        val mealId = 201L
        val mealDetails = MealDetails(
            id = mealId,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "Dinner",
            servings = 1.0f,
            hasOil = false,
            addedOilGrams = 0,
            isBookmarked = false,
            ingredients = listOf(),
        )
        coEvery { foodRepository.getMealDetails(mealId) } returns mealDetails
        coEvery { foodRepository.deleteMeal(mealId) } returns Unit
        coEvery { nutritionRepository.deleteHistoryMeal(mealId) } returns Unit

        viewModel = FoodDetailsViewModel(mealId, false, null, foodRepository, nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteMeal()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { foodRepository.deleteMeal(mealId) }
    }

    @Test
    fun `saveRecipe requests override when recipe for fdcId already exists`() = runTest {
        val mealId = 167945L
        val mealDetails = MealDetails(
            id = mealId,
            title = "Chicken Breast",
            baseCalories = 165,
            baseProteinGrams = 31,
            baseCarbsGrams = 0,
            baseFatGrams = 4,
            mealType = "120 g",
            ingredients = emptyList(),
        )
        coEvery { foodRepository.getMealDetails(mealId) } returns mealDetails
        coEvery { foodRepository.getSavedRecipesForBaseFood(mealId) } returns listOf(Meal(1, "Chicken Breast", 0, 0, 0, 0, "120 g", 0))

        viewModel = FoodDetailsViewModel(mealId, false, null, foodRepository, nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveRecipe()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showOverrideRecipeDialog)
    }

    @Test
    fun `hasOil is false when meal has no oil`() = runTest {
        val mealId = 202L
        val mealDetails = MealDetails(
            id = mealId,
            title = "Chicken Salad",
            baseCalories = 480,
            baseProteinGrams = 42,
            baseCarbsGrams = 18,
            baseFatGrams = 22,
            mealType = "Lunch",
            servings = 1.0f,
            hasOil = false,
            addedOilGrams = 0,
            isBookmarked = true,
            ingredients = listOf(),
        )
        coEvery { foodRepository.getMealDetails(mealId) } returns mealDetails

        viewModel = FoodDetailsViewModel(mealId, false, null, foodRepository, nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.mealDetails?.hasOil == true)
    }

    @Test
    fun `logMeal writes history and marks screen logged`() = runTest {
        val mealId = 201L
        val mealDetails = MealDetails(
            id = mealId,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "120 g",
            servings = 1.5f,
            hasOil = false,
            addedOilGrams = 0,
            isBookmarked = false,
            ingredients = listOf(
                Ingredient("Salmon", 200, 280, 28, 0, 18),
                Ingredient("Rice", 150, 195, 4, 43, 0),
            ),
        )
        val initialMeal = Meal(
            id = mealId,
            title = "Salmon Bowl",
            calories = 157,
            proteinGrams = 12,
            carbsGrams = 4,
            fatGrams = 9,
            mealType = "120 g",
            loggedAtEpochMillis = 0,
        )
        coEvery { foodRepository.getMealDetails(mealId) } returns mealDetails
        coEvery { nutritionRepository.getHistoryMealDetails(any()) } returns mealDetails
        coEvery { nutritionRepository.logHistory(any()) } returns Unit
        coEvery { nutritionRepository.deleteHistoryMeal(any()) } returns Unit
        coEvery { nutritionRepository.updateHistoryMeal(any(), any(), any()) } returns Unit

        viewModel = FoodDetailsViewModel(mealId, false, initialMeal, foodRepository, nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.logMeal()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showPortionPreferenceDialog)
        viewModel.confirmPortionPreference()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLogged)
        coVerify { foodRepository.savePortionPreference(mealId, 180f) }
        coVerify {
            nutritionRepository.logHistory(
                match<LoggedMealInput> {
                    it.sourceFdcId == mealId &&
                        it.calories == 235 &&
                        it.proteinGrams == 18 &&
                        it.carbsGrams == 6 &&
                        it.fatGrams == 13
                }
            )
        }
    }

    @Test
    fun `history edit loads history meal details and updates history rows`() = runTest {
        val mealId = 55L
        val historyDetails = MealDetails(
            id = mealId,
            title = "Logged Chicken",
            baseCalories = 157,
            baseProteinGrams = 12,
            baseCarbsGrams = 4,
            baseFatGrams = 9,
            mealType = "120 g",
            servings = 1f,
            ingredients = listOf(
                Ingredient("Chicken", 120, 157, 12, 4, 9),
            ),
        )
        coEvery { nutritionRepository.getHistoryMealDetails(mealId) } returns historyDetails
        coEvery { nutritionRepository.updateHistoryMeal(any(), any(), any()) } returns Unit
        coEvery { nutritionRepository.deleteHistoryMeal(any()) } returns Unit

        viewModel = FoodDetailsViewModel(mealId, true, null, foodRepository, nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Logged Chicken", viewModel.uiState.value.mealDetails?.title)

        viewModel.updateServings(2f)
        viewModel.logMeal()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            nutritionRepository.updateHistoryMeal(
                match<Meal> { it.id == mealId && it.calories == 314 && it.proteinGrams == 24 && it.carbsGrams == 8 && it.fatGrams == 18 },
                match<List<LoggedIngredientInput>> { it.size == 1 && it.first().normalizedWeightGrams == 240f },
                240f,
            )
        }
    }
}
