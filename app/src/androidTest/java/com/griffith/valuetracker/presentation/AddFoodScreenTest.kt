package com.griffith.valuetracker.presentation

import androidx.compose.ui.test.assertIsDisplayed
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

class AddFoodScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addFoodScreen_showsFadeWrapperSuggestions_andForwardsActions() {
        var latestQuery = ""
        var loggedMealTitle: String? = null

        composeRule.setContent {
            ValueTrackerTheme {
                AddFoodScreen(
                    state = AddFoodUiState(
                        filteredSuggestions = listOf(
                            Meal(101, "Salmon Bowl", 620, 38, 55, 24, "Dinner", 0),
                            Meal(102, "Chicken Salad", 480, 42, 18, 22, "Lunch", 0),
                        ),
                    ),
                    onQueryChange = { latestQuery = it },
                    onLogMeal = { loggedMealTitle = it.title },
                )
            }
        }

        composeRule.onNodeWithTag("add_food_screen_fade").assertIsDisplayed()
        composeRule.onNodeWithText("Add food").assertIsDisplayed()
        composeRule.onNodeWithText("Salmon Bowl").performClick()
        composeRule.onNodeWithText("Search foods").performTextInput("sal")

        assertEquals("Salmon Bowl", loggedMealTitle)
        assertEquals("sal", latestQuery)
    }
}
