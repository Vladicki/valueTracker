package com.griffith.valuetracker.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.griffith.valuetracker.domain.model.DailySummary
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.ui.theme.ValueTrackerTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreen_loadingStateShowsOnlyLoadingSurface() {
        composeRule.setContent {
            ValueTrackerTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = true,
                        greeting = "Vlad",
                        recentMeals = listOf(Meal(1, "Greek Yogurt", 180, 15, 20, 5, "Snack", 1)),
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("home_screen_loading").assertIsDisplayed()
        composeRule.onAllNodesWithTag("home_screen_fade").assertCountEquals(0)
        composeRule.onAllNodesWithText("Welcome back, Vlad").assertCountEquals(0)
    }

    @Test
    fun homeScreen_loadedStateShowsFadeWrappedContent() {
        composeRule.setContent {
            ValueTrackerTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        greeting = "Vlad",
                        summary = DailySummary(
                            consumedCalories = 1850,
                            targetCalories = 2200,
                            consumedProteinGrams = 120,
                            targetProteinGrams = 150,
                            consumedCarbsGrams = 175,
                            targetCarbsGrams = 250,
                            consumedFatGrams = 39,
                            targetFatGrams = 65,
                            proteinProgress = 0.8f,
                            carbsProgress = 0.7f,
                            fatProgress = 0.6f,
                            waterMl = 1800,
                        ),
                        recentMeals = listOf(Meal(1, "Greek Yogurt", 180, 15, 20, 5, "Snack", 1)),
                        weeklyGoalDays = (3..9).toList(),
                    ),
                )
            }
        }

        composeRule.onAllNodesWithTag("home_screen_loading").assertCountEquals(0)
        composeRule.onNodeWithTag("home_screen_fade").assertIsDisplayed()
        composeRule.onNodeWithText("VibeMeal, Vlad").assertIsDisplayed()
        composeRule.onNodeWithText("Weekly goals").assertIsDisplayed()
        composeRule.onNodeWithText("Today's meals").assertIsDisplayed()
        composeRule.onNodeWithText("Greek Yogurt").assertIsDisplayed()
        composeRule.onNodeWithText("Today").assertIsDisplayed()
    }
}
