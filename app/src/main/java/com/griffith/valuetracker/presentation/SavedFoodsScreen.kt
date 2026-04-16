package com.griffith.valuetracker.presentation

import androidx.compose.runtime.Composable
import com.griffith.valuetracker.presentation.food.FoodScreen
import com.griffith.valuetracker.presentation.food.FoodTab

@Composable
fun SavedFoodsScreen(
    onOpenRecipe: (Long) -> Unit = {},
) {
    FoodScreen(
        onAddFoodClick = { meal -> onOpenRecipe(meal.id) },
        initialTab = FoodTab.Saved,
    )
}
