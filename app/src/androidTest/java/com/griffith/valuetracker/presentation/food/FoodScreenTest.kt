package com.griffith.valuetracker.presentation.food

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.ui.theme.ValueTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FoodScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun foodScreen_showsFindFoodsTabByDefault_andForwardsQueryChanges() {
        var latestQuery = ""
        var addFoodMealId: Long? = null

        composeRule.setContent {
            ValueTrackerTheme {
                FoodScreen(
                    state = FoodUiState(
                        favoriteMeals = listOf(Meal(1, "Lasagna with Meat", 336, 20, 35, 12, "1 piece", 1)),
                        historyMeals = listOf(Meal(2, "Greek Yogurt", 180, 15, 20, 5, "1 cup", 2)),
                        filteredFavoriteMeals = listOf(Meal(1, "Lasagna with Meat", 336, 20, 35, 12, "1 piece", 1)),
                        filteredHistoryMeals = listOf(Meal(2, "Greek Yogurt", 180, 15, 20, 5, "1 cup", 2)),
                    ),
                    onQueryChange = { query -> latestQuery = query },
                    onTabSelected = {},
                    onAddFoodClick = { meal -> addFoodMealId = meal.id },
                )
            }
        }

        composeRule.onNodeWithTag("food_screen_fade").assertIsDisplayed()
        composeRule.onNodeWithTag("food_tab_find").assertIsSelected()
        composeRule.onNodeWithTag("food_tab_saved").assertIsNotSelected()
        composeRule.onNodeWithText("Find Foods").assertIsDisplayed()
        composeRule.onNodeWithText("Saved").assertIsDisplayed()
        composeRule.onNodeWithText("Lasagna with Meat").assertIsDisplayed()
        composeRule.onNodeWithText("336 kcal   p: 20g   f: 12g   c: 35g").assertIsDisplayed()

        composeRule.onNodeWithText("Search foods").performTextInput("las")
        assertEquals("las", latestQuery)

        composeRule.onNodeWithTag("food_add_button_2").performClick()
        assertEquals(2, addFoodMealId)
    }

    @Test
    fun foodScreen_savedTabUsesSamePreviewCardStyleAsFindFoods() {
        composeRule.setContent {
            ValueTrackerTheme {
                FoodScreen(
                    state = FoodUiState(
                        selectedTab = FoodTab.Saved,
                        favoriteMeals = listOf(Meal(1, "Lasagna with Meat", 336, 20, 35, 12, "1 piece", 1)),
                        historyMeals = listOf(Meal(2, "Greek Yogurt", 180, 15, 20, 5, "1 cup", 2)),
                        filteredFavoriteMeals = listOf(Meal(1, "Lasagna with Meat", 336, 20, 35, 12, "1 piece", 1)),
                        filteredHistoryMeals = listOf(Meal(2, "Greek Yogurt", 180, 15, 20, 5, "1 cup", 2)),
                    ),
                    onQueryChange = {},
                    onTabSelected = {},
                    onAddFoodClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("food_tab_saved").performClick()
        composeRule.onNodeWithText("Lasagna with Meat").assertIsDisplayed()
        composeRule.onNodeWithText("336 kcal   p: 20g   f: 12g   c: 35g").assertIsDisplayed()
    }
}
