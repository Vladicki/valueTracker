package com.griffith.valuetracker.data.database

import com.griffith.valuetracker.data.AllFoodEntity
import com.griffith.valuetracker.data.DatabaseFoodRepository
import com.griffith.valuetracker.data.DatabaseNutritionRepository
import com.griffith.valuetracker.data.DatabaseProfileRepository
import com.griffith.valuetracker.data.DatabaseStorage
import com.griffith.valuetracker.data.FoodItemEntity
import com.griffith.valuetracker.data.FoodNutrientEntity
import com.griffith.valuetracker.data.FoodPortionEntity
import com.griffith.valuetracker.data.MealDetailEntity
import com.griffith.valuetracker.data.MealIngredientEntity
import com.griffith.valuetracker.data.NutrientEntity
import com.griffith.valuetracker.data.seedDatabaseIfEmpty

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.griffith.valuetracker.data.repository.LoggedIngredientInput
import com.griffith.valuetracker.data.repository.LoggedMealInput
import com.griffith.valuetracker.data.repository.SavedRecipeIngredientInput
import com.griffith.valuetracker.domain.model.Meal
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DatabaseStorageRepositoryTest {
    private lateinit var database: DatabaseStorage
    private lateinit var foodRepository: DatabaseFoodRepository
    private lateinit var nutritionRepository: DatabaseNutritionRepository
    private lateinit var profileRepository: DatabaseProfileRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DatabaseStorage::class.java,
        ).allowMainThreadQueries().build()
        foodRepository = DatabaseFoodRepository(
            foodSearchDao = database.foodSearchDao(),
            mealDetailDao = database.mealDetailDao(),
            mealIngredientDao = database.mealIngredientDao(),
            allFoodDao = database.allFoodDao(),
            savedRecipeDao = database.savedRecipeDao(),
            savedRecipeIngredientDao = database.savedRecipeIngredientDao(),
            portionPreferenceDao = database.portionPreferenceDao(),
        )
        nutritionRepository = DatabaseNutritionRepository(
            mealLogDao = database.mealLogDao(),
            profileDao = database.profileDao(),
            mealHistoryDao = database.mealHistoryDao(),
            ingredientHistoryDao = database.ingredientHistoryDao(),
            allFoodDao = database.allFoodDao(),
        )
        profileRepository = DatabaseProfileRepository(database.profileDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseFoodRepository_returnsFavoritesHistoryAndDetails_fromSql() = runTest {
        database.mealDetailDao().upsert(
            MealDetailEntity(201, "Salmon Bowl", 620, 38, 55, 24, "Dinner", 1f, true, 0, true, "favorite"),
        )
        database.mealDetailDao().upsert(
            MealDetailEntity(301, "Greek Yogurt", 180, 15, 20, 5, "Snack", 1f, false, 0, false, "history"),
        )
        database.mealIngredientDao().replaceForMeal(
            201,
            listOf(MealIngredientEntity(201, 0, "Salmon", 200, 280, 28, 0, 18)),
        )

        val favorites = foodRepository.getFavoriteMeals()
        val history = foodRepository.getHistoryMeals()
        val details = foodRepository.getMealDetails(201)

        assertEquals(listOf("Salmon Bowl"), favorites.map { it.title })
        assertEquals(listOf("Greek Yogurt"), history.map { it.title })
        assertEquals("Salmon Bowl", details.title)
        assertEquals(1, details.ingredients.size)
    }

    @Test
    fun databaseFoodRepository_loadsImportedItemDetails_byFdcIdWhenNoSavedMealExists() = runTest {
        database.foodItemDao().upsertAll(listOf(FoodItemEntity(167945, "Chicken Breast", "Poultry", "sr_legacy_food")))
        database.nutrientDao().upsertAll(
            listOf(
                NutrientEntity(1008, "Energy", "KCAL", "208"),
                NutrientEntity(1003, "Protein", "G", "203"),
                NutrientEntity(1005, "Carbohydrate, by difference", "G", "205"),
                NutrientEntity(1004, "Total lipid (fat)", "G", "204"),
            ),
        )
        database.foodNutrientDao().upsertAll(
            listOf(
                FoodNutrientEntity(167945, 1008, 165f),
                FoodNutrientEntity(167945, 1003, 31f),
                FoodNutrientEntity(167945, 1005, 0f),
                FoodNutrientEntity(167945, 1004, 4f),
            ),
        )
        database.foodPortionDao().upsertAll(listOf(FoodPortionEntity(167945, 167945, 120f)))
        database.allFoodDao().upsertAll(
            listOf(
                AllFoodEntity(
                    fdcId = 167945,
                    foodName = "Chicken Breast",
                    defaultPortionGrams = 120f,
                    caloriesKcal = 165f,
                    proteinGrams = 31f,
                    carbsGrams = 0f,
                    fatGrams = 4f,
                    fiberGrams = 0f,
                    totalSugarsGrams = 0f,
                    starchGrams = 0f,
                    calciumMg = 12f,
                    ironMg = 1f,
                    magnesiumMg = 29f,
                    phosphorusMg = 220f,
                    potassiumMg = 256f,
                    sodiumMg = 74f,
                    zincMg = 1f,
                    seleniumUg = 27f,
                    caffeineMg = 0f,
                    alcoholGrams = 0f,
                    vitaminAiu = 0f,
                    vitaminCmg = 0f,
                    vitaminDug = 0f,
                    vitaminEmg = 0f,
                    vitaminKug = 0f,
                    thiaminMg = 0f,
                    riboflavinMg = 0f,
                    niacinMg = 0f,
                    vitaminB6Mg = 0f,
                    folateUg = 0f,
                    vitaminB12Ug = 0f,
                    pantothenicAcidMg = 0f,
                    cholineMg = 0f,
                    omega3Grams = 0f,
                    omega6Grams = 0f,
                    monounsaturatedFatGrams = 0f,
                    polyunsaturatedFatGrams = 0f,
                    saturatedFatGrams = 0f,
                    saved = false,
                )
            )
        )

        val details = foodRepository.getMealDetails(167945)

        assertEquals("Chicken Breast", details.title)
        assertEquals(165, details.baseCalories)
        assertEquals(31, details.baseProteinGrams)
        assertEquals(0, details.baseCarbsGrams)
        assertEquals(4, details.baseFatGrams)
        assertEquals("120.0 g", details.mealType)
        assertEquals("12.0 mg", details.secondaryNutrients.first { it.label == "Calcium" }.value)
        assertTrue(details.secondaryNutrients.none { it.label == "Fiber" })
    }

    @Test
    fun databaseFoodRepository_saveRecipe_overridesExistingRecipeForSameBaseFood() = runTest {
        foodRepository.saveRecipe(
            baseFood = Meal(167945, "Chicken Breast", 165, 31, 0, 4, "120 g", 0),
            ingredients = listOf(SavedRecipeIngredientInput("Salt", 2f)),
            overrideExisting = true,
        )
        foodRepository.saveRecipe(
            baseFood = Meal(167945, "Chicken Breast", 165, 31, 0, 4, "120 g", 0),
            ingredients = listOf(SavedRecipeIngredientInput("Garlic", 5f)),
            overrideExisting = true,
        )

        val saved = foodRepository.observeSavedRecipes().first()

        assertEquals(1, saved.size)
        assertEquals("Chicken Breast", saved.first().title)
        assertEquals(listOf("Garlic"), foodRepository.getSavedRecipeIngredients(saved.first().id).map { it.ingredientName })
    }

    @Test
    fun databaseFoodRepository_toggleBookmark_updatesSavedFlag_forImportedFood() = runTest {
        database.allFoodDao().upsertAll(
            listOf(
                AllFoodEntity(
                    fdcId = 167945,
                    foodName = "Chicken Breast",
                    defaultPortionGrams = 120f,
                    caloriesKcal = 165f,
                    proteinGrams = 31f,
                    carbsGrams = 0f,
                    fatGrams = 4f,
                    fiberGrams = 0f,
                    totalSugarsGrams = 0f,
                    starchGrams = 0f,
                    calciumMg = 0f,
                    ironMg = 0f,
                    magnesiumMg = 0f,
                    phosphorusMg = 0f,
                    potassiumMg = 0f,
                    sodiumMg = 0f,
                    zincMg = 0f,
                    seleniumUg = 0f,
                    caffeineMg = 0f,
                    alcoholGrams = 0f,
                    vitaminAiu = 0f,
                    vitaminCmg = 0f,
                    vitaminDug = 0f,
                    vitaminEmg = 0f,
                    vitaminKug = 0f,
                    thiaminMg = 0f,
                    riboflavinMg = 0f,
                    niacinMg = 0f,
                    vitaminB6Mg = 0f,
                    folateUg = 0f,
                    vitaminB12Ug = 0f,
                    pantothenicAcidMg = 0f,
                    cholineMg = 0f,
                    omega3Grams = 0f,
                    omega6Grams = 0f,
                    monounsaturatedFatGrams = 0f,
                    polyunsaturatedFatGrams = 0f,
                    saturatedFatGrams = 0f,
                    saved = false,
                )
            )
        )

        foodRepository.updateMealBookmark(167945, true)

        val details = foodRepository.getMealDetails(167945)
        assertTrue(details.isBookmarked)
        assertEquals(listOf("Chicken Breast"), foodRepository.observeSavedFoods().first().map { it.title })
    }

    @Test
    fun databaseNutritionRepository_logsMealHistory_withFrozenIngredientRows() = runTest {
        nutritionRepository.logHistory(
            LoggedMealInput(
                sourceFdcId = 167945,
                sourceRecipeId = null,
                displayName = "Chicken Breast",
                portionGrams = 120f,
                calories = 165,
                proteinGrams = 31,
                carbsGrams = 0,
                fatGrams = 4,
                mealTypeLabel = "Lunch",
                ingredients = listOf(
                    LoggedIngredientInput(
                        ingredientTitle = "Chicken Breast",
                        ingredientFdcId = 167945,
                        normalizedWeightGrams = 120f,
                        calories = 165,
                        proteinGrams = 31,
                        carbsGrams = 0,
                        fatGrams = 4,
                    )
                ),
            )
        )

        val history = nutritionRepository.observeMealHistorySince(0).first()

        assertEquals(1, history.size)
        assertEquals("Chicken Breast", history.first().title)
        assertEquals("Lunch", history.first().mealType)
    }

    @Test
    fun databaseNutritionRepository_historyMealDetails_restoreLoggedPortionAsServings() = runTest {
        nutritionRepository.logHistory(
            LoggedMealInput(
                sourceFdcId = 167945,
                sourceRecipeId = null,
                displayName = "Chicken Breast",
                portionGrams = 180f,
                calories = 235,
                proteinGrams = 46,
                carbsGrams = 0,
                fatGrams = 6,
                mealTypeLabel = "120 g",
                ingredients = listOf(
                    LoggedIngredientInput(
                        ingredientTitle = "Chicken Breast",
                        ingredientFdcId = 167945,
                        normalizedWeightGrams = 180f,
                        calories = 235,
                        proteinGrams = 46,
                        carbsGrams = 0,
                        fatGrams = 6,
                    )
                ),
            )
        )

        val historyId = nutritionRepository.observeMealHistorySince(0).first().first().id
        val details = nutritionRepository.getHistoryMealDetails(historyId)

        assertEquals("180 g", details.mealType)
        assertEquals(1f, details.servings)
        assertEquals(235, details.calculatedCalories)
        assertEquals(46, details.calculatedProteinGrams)
        assertEquals(180, details.calculatedIngredients.first().grams)
    }

    @Test
    fun databaseProfileRepository_returnsEmptyProfile_whenNothingSaved() = runTest {
        val profile = profileRepository.observeUserProfile().first()

        assertEquals("", profile.firstName)
        assertEquals("", profile.goalType)
    }

    @Test
    fun seedDatabaseIfEmpty_importsCatalog_only_withoutLocalSampleRows() = runTest {
        val projectDir = File(requireNotNull(System.getProperty("user.dir")))
        val baseDir = listOfNotNull(
            projectDir.resolve("data_storage/food_db"),
            projectDir.parentFile?.resolve("data_storage/food_db"),
        ).firstOrNull { it.exists() } ?: error("food_db directory not found from ${projectDir.absolutePath}")

        seedDatabaseIfEmpty(database, baseDir)

        assertTrue(database.foodItemDao().countRows() > 0)
        assertTrue(database.foodSearchDao().searchFoods("").isNotEmpty())
        val seededFoodId = 167945L
        val seededDetails = foodRepository.getMealDetails(seededFoodId)
        assertTrue(seededDetails.secondaryNutrients.isNotEmpty())
        assertEquals(0, database.mealLogDao().countRows())
        assertEquals(0, database.weightDao().countRows())
        assertEquals(null, database.profileDao().get())
        assertTrue(database.mealDetailDao().findByBucket("favorite").isEmpty())
    }
}
