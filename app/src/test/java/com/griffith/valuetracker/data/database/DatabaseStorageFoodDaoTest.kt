package com.griffith.valuetracker.data.database

import com.griffith.valuetracker.data.AllFoodEntity
import com.griffith.valuetracker.data.DatabaseStorage
import com.griffith.valuetracker.data.FoodItemEntity
import com.griffith.valuetracker.data.FoodNutrientEntity
import com.griffith.valuetracker.data.FoodPortionEntity
import com.griffith.valuetracker.data.MealDetailEntity
import com.griffith.valuetracker.data.MealIngredientEntity
import com.griffith.valuetracker.data.NutrientEntity

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DatabaseStorageFoodDaoTest {
    private lateinit var database: DatabaseStorage

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DatabaseStorage::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun foodSearchDao_searchByName_returnsMatchesOrderedByName() = runTest {
        database.foodItemDao().upsertAll(
            listOf(
                FoodItemEntity(2, "Chicken Thigh", null, "sr_legacy_food"),
                FoodItemEntity(1, "Chicken Breast", null, "sr_legacy_food"),
                FoodItemEntity(3, "Rice", null, "sr_legacy_food"),
            ),
        )
        database.nutrientDao().upsertAll(listOf(NutrientEntity(1008, "Energy", "KCAL", "208")))
        database.foodNutrientDao().upsertAll(
            listOf(
                FoodNutrientEntity(2, 1008, 209f),
                FoodNutrientEntity(1, 1008, 165f),
                FoodNutrientEntity(3, 1008, 130f),
            ),
        )

        val results = database.foodSearchDao().searchFoods("Chicken")

        assertEquals(listOf("Chicken Breast", "Chicken Thigh"), results.map { it.name })
    }

    @Test
    fun foodSearchDao_searchByName_excludesBrandedFoodRows() = runTest {
        database.foodItemDao().upsertAll(
            listOf(
                FoodItemEntity(1, "Chicken Breast", "Poultry", "sr_legacy_food"),
                FoodItemEntity(2, "Chicken Brand Fillet", "Poultry", "branded_food"),
            ),
        )
        database.nutrientDao().upsertAll(listOf(NutrientEntity(1008, "Energy", "KCAL", "208")))
        database.foodNutrientDao().upsertAll(
            listOf(
                FoodNutrientEntity(1, 1008, 165f),
                FoodNutrientEntity(2, 1008, 190f),
            ),
        )

        val results = database.foodSearchDao().searchFoods("Chicken")

        assertEquals(listOf("Chicken Breast"), results.map { it.name })
    }

    @Test
    fun allFoodDao_searchByName_returnsPrecomputedRowWithoutJoin() = runTest {
        database.allFoodDao().upsertAll(
            listOf(
                AllFoodEntity(
                    fdcId = 1001,
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
                ),
            ),
        )

        val rows = database.allFoodDao().searchByName("Chicken")

        assertEquals(listOf("Chicken Breast"), rows.map { it.foodName })
        assertEquals(120f, rows.first().defaultPortionGrams)
    }

    @Test
    fun mealDetailDao_roundTripsIngredientsAndBookmark() = runTest {
        database.mealDetailDao().upsert(
            MealDetailEntity(
                mealId = 201,
                title = "Salmon Bowl",
                baseCalories = 620,
                baseProteinGrams = 38,
                baseCarbsGrams = 55,
                baseFatGrams = 24,
                mealType = "Dinner",
                servings = 1f,
                hasOil = true,
                addedOilGrams = 0,
                isBookmarked = true,
                listBucket = "favorite",
            ),
        )
        database.mealIngredientDao().replaceForMeal(
            mealId = 201,
            ingredients = listOf(
                MealIngredientEntity(201, 0, "Salmon", 200, 280, 28, 0, 18),
                MealIngredientEntity(201, 1, "Rice", 150, 195, 4, 43, 0),
            ),
        )

        val detail = database.mealDetailDao().findByMealId(201)
        val ingredients = database.mealIngredientDao().findByMealId(201)

        assertEquals("Salmon Bowl", detail?.title)
        assertTrue(detail?.isBookmarked == true)
        assertEquals(2, ingredients.size)
    }

    @Test
    fun foodSearchDao_searchByName_joinsNutrientsAndGramWeight() = runTest {
        database.foodItemDao().upsertAll(
            listOf(
                FoodItemEntity(1001, "Chicken Breast", "Poultry", "sr_legacy_food"),
                FoodItemEntity(1002, "Rice", "Grains", "sr_legacy_food"),
            ),
        )
        database.nutrientDao().upsertAll(
            listOf(
                NutrientEntity(1008, "Energy", "KCAL", "208"),
                NutrientEntity(1003, "Protein", "G", "203"),
                NutrientEntity(1005, "Carbohydrate, by difference", "G", "205"),
            ),
        )
        database.foodNutrientDao().upsertAll(
            listOf(
                FoodNutrientEntity(1001, 1008, 165f),
                FoodNutrientEntity(1001, 1003, 31f),
                FoodNutrientEntity(1002, 1005, 28f),
            ),
        )
        database.foodPortionDao().upsertAll(
            listOf(
                FoodPortionEntity(1, 1001, 120f),
                FoodPortionEntity(2, 1002, 50f),
            ),
        )

        val results = database.foodSearchDao().searchFoods("Chicken")

        assertEquals(1, results.size)
        assertEquals("Chicken Breast", results.first().name)
        assertEquals(120f, results.first().gramWeight)
        assertTrue(results.first().nutrientsSummary.contains("Protein: 31.0 G"))
        assertEquals(165f, results.first().calories)
    }
}
