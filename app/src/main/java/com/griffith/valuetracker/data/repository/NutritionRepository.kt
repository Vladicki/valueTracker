package com.griffith.valuetracker.data.repository

import com.griffith.valuetracker.domain.model.DailySummary
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.domain.model.MealDetails
import kotlinx.coroutines.flow.Flow

data class LoggedIngredientInput(
    val ingredientTitle: String,
    val ingredientFdcId: Long?,
    val normalizedWeightGrams: Float,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
)

data class LoggedMealInput(
    val sourceFdcId: Long?,
    val sourceRecipeId: Long?,
    val displayName: String,
    val portionGrams: Float,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val mealTypeLabel: String,
    val imageUrl: String? = null,
    val ingredients: List<LoggedIngredientInput>,
)

interface NutritionRepository {
    fun observeMeals(): Flow<List<Meal>>
    fun observeDailySummary(): Flow<DailySummary>
    fun observeDailySummarySince(cutoff: Long): Flow<DailySummary>
    fun observeDailySummaryBetween(startInclusive: Long, endExclusive: Long): Flow<DailySummary>
    fun observeMealHistorySince(cutoff: Long): Flow<List<Meal>>
    fun observeMealHistoryBetween(startInclusive: Long, endExclusive: Long): Flow<List<Meal>>
    suspend fun getHistoryMealDetails(mealId: Long): MealDetails
    suspend fun logHistory(input: LoggedMealInput)
    suspend fun updateHistoryMeal(meal: Meal, ingredients: List<LoggedIngredientInput>, portionGrams: Float)
    suspend fun deleteHistoryMeal(mealId: Long)
}
