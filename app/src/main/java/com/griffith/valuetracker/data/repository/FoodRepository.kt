package com.griffith.valuetracker.data.repository

import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.domain.model.MealDetails
import kotlinx.coroutines.flow.Flow

data class SavedRecipeIngredientInput(
    val ingredientName: String,
    val grams: Float,
    val ingredientFdcId: Long? = null,
)

interface FoodRepository {
    suspend fun getFavoriteMeals(): List<Meal>
    suspend fun getHistoryMeals(): List<Meal>
    suspend fun getMealDetails(mealId: Long): MealDetails
    suspend fun updateMealBookmark(mealId: Long, isBookmarked: Boolean)
    suspend fun deleteMeal(mealId: Long)
    suspend fun searchFoods(query: String): List<Meal>
    fun observeSavedFoods(): Flow<List<Meal>>
    fun observeSavedRecipes(): Flow<List<Meal>>
    suspend fun getSavedRecipeIngredients(recipeId: Long): List<com.griffith.valuetracker.data.SavedRecipeIngredientEntity>
    suspend fun setFoodSaved(fdcId: Long, saved: Boolean)
    suspend fun setRecipeSaved(recipeId: Long, saved: Boolean)
    suspend fun getSavedRecipesForBaseFood(fdcId: Long): List<Meal>
    suspend fun saveRecipe(baseFood: Meal, ingredients: List<SavedRecipeIngredientInput>, overrideExisting: Boolean)
    suspend fun getPortionPreference(foodId: Long): Float? = null
    suspend fun savePortionPreference(foodId: Long, portionGrams: Float) {}
}
