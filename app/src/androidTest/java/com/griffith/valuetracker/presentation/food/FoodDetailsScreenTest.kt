package com.griffith.valuetracker.presentation.food

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import com.griffith.valuetracker.domain.model.Ingredient
import com.griffith.valuetracker.domain.model.MealDetails
import com.griffith.valuetracker.ui.theme.ValueTrackerTheme
import org.junit.Rule
import org.junit.Test

class FoodDetailsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun foodDetailsScreen_showsHeaderActionsAndFloatingBottomButtons() {
        val mealDetails = MealDetails(
            id = 201,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "Dinner",
            servings = 1.0f,
            hasOil = false,
            addedOilGrams = 1,
            isBookmarked = false,
            ingredients = listOf(
                Ingredient("Salmon", 150, 300, 30, 0, 15),
                Ingredient("Rice", 200, 260, 5, 50, 1),
            ),
        )

        composeRule.setContent {
            ValueTrackerTheme {
                FoodDetailsScreen(
                    state = FoodDetailsUiState(isLoading = false, mealDetails = mealDetails),
                    onBackClick = {},
                    onBookmarkClick = {},
                    onEditClick = {},
                    onDeleteClick = {},
                    onServingsModeClick = {},
                    onServingsStepChange = {},
                    onAddedOilChange = {},
                    onPickImageCamera = {},
                    onPickImageGallery = {},
                    onPictureMenuToggle = {},
                    onSaveRecipe = {},
                    onConfirmOverrideRecipe = {},
                    onDismissOverrideDialog = {},
                    onDoneClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("food_details_screen_fade").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Bookmark").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("More options").assertIsDisplayed()
        composeRule.onNodeWithText("Salmon Bowl").assertIsDisplayed()
        composeRule.onNodeWithText("620").assertIsDisplayed()
        composeRule.onNodeWithText("55g").assertIsDisplayed()
        composeRule.onNodeWithText("24g").assertIsDisplayed()
        composeRule.onNodeWithText("38g").assertIsDisplayed()
        composeRule.onNodeWithText("1 portion").assertIsDisplayed()
        composeRule.onNodeWithTag("food_details_portion_control").assertIsDisplayed()
        composeRule.onNodeWithText("󰹺").assertIsDisplayed()
        try {
            composeRule.onNodeWithTag("food_details_servings_stepper").assertIsDisplayed()
            throw AssertionError("Stepper should not be displayed")
        } catch (_: AssertionError) {
            // expected: stepper removed
        }
        composeRule.onNodeWithText("Salmon").assertIsDisplayed()
        composeRule.onNodeWithText("150g").assertIsDisplayed()
        composeRule.onNodeWithText("Rice").assertIsDisplayed()
        composeRule.onNodeWithText("200g").assertIsDisplayed()
        composeRule.onNodeWithTag("food_details_bottom_actions").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
        composeRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun foodDetailsScreen_bottomActionsStayPinnedToBottom() {
        lateinit var screenBounds: DpRect
        lateinit var actionsBounds: DpRect

        val mealDetails = MealDetails(
            id = 201,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "Dinner",
            servings = 1.0f,
            hasOil = false,
            addedOilGrams = 1,
            isBookmarked = false,
            ingredients = listOf(
                Ingredient("Salmon", 150, 300, 30, 0, 15),
                Ingredient("Rice", 200, 260, 5, 50, 1),
            ),
        )

        composeRule.setContent {
            ValueTrackerTheme {
                FoodDetailsScreen(
                    state = FoodDetailsUiState(isLoading = false, mealDetails = mealDetails),
                    onBackClick = {},
                    onBookmarkClick = {},
                    onEditClick = {},
                    onDeleteClick = {},
                    onServingsModeClick = {},
                    onServingsStepChange = {},
                    onAddedOilChange = {},
                    onPickImageCamera = {},
                    onPickImageGallery = {},
                    onPictureMenuToggle = {},
                    onSaveRecipe = {},
                    onConfirmOverrideRecipe = {},
                    onDismissOverrideDialog = {},
                    onDoneClick = {},
                )
            }
        }

        screenBounds = composeRule.onNodeWithTag("food_details_screen_fade").getBoundsInRoot()
        actionsBounds = composeRule.onNodeWithTag("food_details_bottom_actions").getBoundsInRoot()

        org.junit.Assert.assertTrue(actionsBounds.bottom.value >= screenBounds.bottom.value - 24f)
    }

    @Test
    fun foodDetailsScreen_withOil_showsOilSlider() {
        val mealDetails = MealDetails(
            id = 201,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "Dinner",
            servings = 1.0f,
            hasOil = true,
            addedOilGrams = 10,
            isBookmarked = false,
            ingredients = emptyList(),
        )

        composeRule.setContent {
            ValueTrackerTheme {
                FoodDetailsScreen(
                    state = FoodDetailsUiState(isLoading = false, mealDetails = mealDetails),
                    onBackClick = {},
                    onBookmarkClick = {},
                    onEditClick = {},
                    onDeleteClick = {},
                    onServingsModeClick = {},
                    onServingsStepChange = {},
                    onAddedOilChange = {},
                    onPickImageCamera = {},
                    onPickImageGallery = {},
                    onPictureMenuToggle = {},
                    onSaveRecipe = {},
                    onConfirmOverrideRecipe = {},
                    onDismissOverrideDialog = {},
                    onDoneClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Added Oil").assertIsDisplayed()
        composeRule.onNodeWithTag("food_details_oil_slider").assertIsDisplayed()
    }

    @Test
    fun foodDetailsScreen_withoutImage_showsPlaceholderAndPicturePopup() {
        val mealDetails = MealDetails(
            id = 201,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "Dinner",
            servings = 1.0f,
            hasOil = false,
            addedOilGrams = 1,
            isBookmarked = false,
            ingredients = emptyList(),
        )

        composeRule.setContent {
            ValueTrackerTheme {
                FoodDetailsScreen(
                    state = FoodDetailsUiState(isLoading = false, mealDetails = mealDetails),
                    onBackClick = {},
                    onBookmarkClick = {},
                    onEditClick = {},
                    onDeleteClick = {},
                    onServingsModeClick = {},
                    onServingsStepChange = {},
                    onAddedOilChange = {},
                    onPickImageCamera = {},
                    onPickImageGallery = {},
                    onPictureMenuToggle = {},
                    onSaveRecipe = {},
                    onConfirmOverrideRecipe = {},
                    onDismissOverrideDialog = {},
                    onDoneClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Add picture to the Recipe").assertIsDisplayed()
        composeRule.onNodeWithTag("food_details_image_placeholder").performClick()
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("Gallery").assertIsDisplayed()
    }

    @Test
    fun foodDetailsScreen_tappingPortionControlSwitchesModes() {
        val mealDetails = MealDetails(
            id = 201,
            title = "Salmon Bowl",
            baseCalories = 620,
            baseProteinGrams = 38,
            baseCarbsGrams = 55,
            baseFatGrams = 24,
            mealType = "Dinner",
            servings = 1.0f,
            hasOil = false,
            addedOilGrams = 1,
            isBookmarked = false,
            ingredients = emptyList(),
        )
        var selectedMode: PortionMode? = null

        composeRule.setContent {
            ValueTrackerTheme {
                FoodDetailsScreen(
                    state = FoodDetailsUiState(isLoading = false, mealDetails = mealDetails, portionMode = PortionMode.Portions),
                    onBackClick = {},
                    onBookmarkClick = {},
                    onEditClick = {},
                    onDeleteClick = {},
                    onServingsModeClick = { selectedMode = it },
                    onServingsStepChange = {},
                    onAddedOilChange = {},
                    onPickImageCamera = {},
                    onPickImageGallery = {},
                    onPictureMenuToggle = {},
                    onSaveRecipe = {},
                    onConfirmOverrideRecipe = {},
                    onDismissOverrideDialog = {},
                    onDoneClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("food_details_portion_control").performClick()
        org.junit.Assert.assertEquals(PortionMode.Grams, selectedMode)
    }
}
