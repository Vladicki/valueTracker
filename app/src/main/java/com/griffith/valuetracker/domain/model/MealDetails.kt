package com.griffith.valuetracker.domain.model

import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

@Serializable
data class MealDetails(
    val id: Long,
    val title: String,
    val baseCalories: Int,
    val baseProteinGrams: Int,
    val baseCarbsGrams: Int,
    val baseFatGrams: Int,
    val mealType: String,
    val servings: Float = 1.0f,
    val hasOil: Boolean = false,
    val addedOilGrams: Int = 0,
    val isBookmarked: Boolean = false,
    val imageUrl: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val secondaryNutrients: List<SecondaryNutrient> = emptyList(),
) {
    val calculatedCalories: Int
        get() = (baseCalories * servings).toInt() + (addedOilGrams * 9)

    val calculatedProteinGrams: Int
        get() = (baseProteinGrams * servings).toInt()

    val calculatedCarbsGrams: Int
        get() = (baseCarbsGrams * servings).toInt()

    val calculatedFatGrams: Int
        get() = (baseFatGrams * servings).toInt() + addedOilGrams

    val calculatedIngredients: List<CalculatedIngredient>
        get() = ingredients.map { ingredient ->
            CalculatedIngredient(
                name = ingredient.name,
                grams = (ingredient.baseGrams * servings).toInt(),
            )
        }

    val calculatedSecondaryNutrients: List<SecondaryNutrient>
        get() = secondaryNutrients.map { nutrient ->
            nutrient.scale(servings)
        }
}

@Serializable
data class Ingredient(
    val name: String,
    val baseGrams: Int,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
)

data class CalculatedIngredient(
    val name: String,
    val grams: Int,
)

@Serializable
data class SecondaryNutrient(
    val label: String,
    val value: String,
)

private fun SecondaryNutrient.scale(multiplier: Float): SecondaryNutrient {
    val amountText = value.substringBefore(' ')
    val unit = value.substringAfter(' ', "")
    val amount = amountText.toFloatOrNull() ?: return this
    val scaledAmount = ((amount * multiplier) * 10).roundToInt() / 10f
    return copy(
        value = if (unit.isBlank()) scaledAmount.toString() else "$scaledAmount $unit",
    )
}
