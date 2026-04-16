package com.griffith.valuetracker.presentation.camera

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class ImagePreviewScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun imagePreviewScreen_displaysBackButton() {
        composeTestRule.setContent {
            ImagePreviewScreen(
                imageUri = "content://test",
                onConfirm = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .assertExists()
    }

    @Test
    fun imagePreviewScreen_displaysConfirmButton() {
        composeTestRule.setContent {
            ImagePreviewScreen(
                imageUri = "content://test",
                onConfirm = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Confirm")
            .assertExists()
    }
}
