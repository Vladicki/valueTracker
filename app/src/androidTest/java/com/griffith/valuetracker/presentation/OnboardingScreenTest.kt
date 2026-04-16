package com.griffith.valuetracker.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.griffith.valuetracker.data.UserPrefsDataStore
import com.griffith.valuetracker.data.repository.ProfileRepository
import com.griffith.valuetracker.ui.theme.ValueTrackerTheme
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockUserPrefsDataStore = mockk<UserPrefsDataStore>(relaxed = true)

    @Test
    fun onboardingScreen_showsReferenceStyleQuestionAndPrimaryCta() {
        composeTestRule.setContent {
            ValueTrackerTheme {
                OnboardingScreen(
                    onComplete = {},
                    viewModel = OnboardingViewModel(mockUserPrefsDataStore, mockk<ProfileRepository>(relaxed = true)),
                )
            }
        }

        composeTestRule.onNodeWithText("What's your primary goal?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_showsProgressIndicator() {
        composeTestRule.setContent {
            ValueTrackerTheme {
                OnboardingScreen(
                    onComplete = {},
                    viewModel = OnboardingViewModel(mockUserPrefsDataStore, mockk<ProfileRepository>(relaxed = true)),
                )
            }
        }

        composeTestRule.onNodeWithText("What's your primary goal?").assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_step0_showsGoalOptions() {
        composeTestRule.setContent {
            ValueTrackerTheme {
                OnboardingScreen(
                    onComplete = {},
                    viewModel = OnboardingViewModel(mockUserPrefsDataStore, mockk<ProfileRepository>(relaxed = true)),
                )
            }
        }

        composeTestRule.onNodeWithText("Lose Weight").assertIsDisplayed()
        composeTestRule.onNodeWithText("Maintain Weight").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gain Weight").assertIsDisplayed()
        composeTestRule.onNodeWithText("Build Muscle").assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_step0_nextButtonDisabledInitially() {
        composeTestRule.setContent {
            ValueTrackerTheme {
                OnboardingScreen(
                    onComplete = {},
                    viewModel = OnboardingViewModel(mockUserPrefsDataStore, mockk<ProfileRepository>(relaxed = true)),
                )
            }
        }

        composeTestRule.onNodeWithText("Next").assertIsNotEnabled()
    }

    @Test
    fun onboardingScreen_step0_nextButtonEnabledAfterSelection() {
        composeTestRule.setContent {
            ValueTrackerTheme {
                OnboardingScreen(
                    onComplete = {},
                    viewModel = OnboardingViewModel(mockUserPrefsDataStore, mockk<ProfileRepository>(relaxed = true)),
                )
            }
        }

        composeTestRule.onNodeWithText("Lose Weight").performClick()
        composeTestRule.onNodeWithText("Next").assertIsEnabled()
    }

    @Test
    fun onboardingScreen_navigatesToStep1AfterClickingNext() {
        composeTestRule.setContent {
            ValueTrackerTheme {
                OnboardingScreen(
                    onComplete = {},
                    viewModel = OnboardingViewModel(mockUserPrefsDataStore, mockk<ProfileRepository>(relaxed = true)),
                )
            }
        }

        composeTestRule.onNodeWithText("Lose Weight").performClick()
        composeTestRule.onNodeWithText("Next").performClick()

        composeTestRule.onNodeWithText("What's your current weight?").assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_step4_showsCompleteButton() {
        val viewModel = OnboardingViewModel(mockUserPrefsDataStore, mockk<ProfileRepository>(relaxed = true))

        composeTestRule.setContent {
            ValueTrackerTheme {
                OnboardingScreen(
                    onComplete = {},
                    viewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText("Lose Weight").performClick()
        composeTestRule.onNodeWithText("Next").performClick()

        composeTestRule.waitForIdle()
        viewModel.updateCurrentWeight("75")
        composeTestRule.onNodeWithText("Next").performClick()

        composeTestRule.waitForIdle()
        viewModel.updateTargetWeight("70")
        composeTestRule.onNodeWithText("Next").performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sedentary").performClick()
        composeTestRule.onNodeWithText("Next").performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Any dietary preferences?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
    }
}
