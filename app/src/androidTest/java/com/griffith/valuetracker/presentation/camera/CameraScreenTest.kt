package com.griffith.valuetracker.presentation.camera

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class CameraScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cameraScreen_displaysShutterButton() {
        composeTestRule.setContent {
            CameraScreen(
                onNavigateToSavedFoods = {},
                onNavigateToPreview = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Take photo")
            .assertExists()
    }

    @Test
    fun cameraScreen_displaysGalleryButton() {
        composeTestRule.setContent {
            CameraScreen(
                onNavigateToSavedFoods = {},
                onNavigateToPreview = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Pick from gallery")
            .assertExists()
    }
}
