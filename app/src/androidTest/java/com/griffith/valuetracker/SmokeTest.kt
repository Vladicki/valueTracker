package com.griffith.valuetracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivity_displaysOnboardingTitle_onFreshStart() {
        // Assert: The expected onboarding title is displayed
        composeTestRule
            .onNodeWithText("Welcome to ValueTracker")
            .assertIsDisplayed()
    }
}
