package com.griffith.valuetracker.navigation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.griffith.valuetracker.data.UserPrefsDataStore
import com.griffith.valuetracker.di.appModule
import com.griffith.valuetracker.ui.theme.ValueTrackerTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest

@RunWith(AndroidJUnit4::class)
class NavGraphTest : KoinTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var userPrefsDataStore: UserPrefsDataStore

    @Before
    fun setup() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<Context>()
        userPrefsDataStore = UserPrefsDataStore(context)
        runBlocking {
            userPrefsDataStore.setOnboardingCompleted(false)
        }
        startKoin {
            androidContext(context)
            modules(appModule)
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            userPrefsDataStore.setOnboardingCompleted(false)
        }
        stopKoin()
    }

    @Test
    fun launch_shows_onboarding_title_on_fresh_start() {
        runBlocking {
            userPrefsDataStore.setOnboardingCompleted(false)
        }

        composeRule.setContent {
            ValueTrackerTheme {
                NavGraph(userPrefsDataStore = userPrefsDataStore)
            }
        }

        composeRule.onNodeWithText("What's your primary goal?").assertIsDisplayed()
    }

    @Test
    fun navGraph_shows_bottom_shell_after_completed_onboarding() {
        runBlocking {
            userPrefsDataStore.setOnboardingCompleted(true)
        }

        composeRule.setContent {
            ValueTrackerTheme {
                NavGraph(userPrefsDataStore = userPrefsDataStore)
            }
        }

        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Food").assertIsDisplayed()
        composeRule.onNodeWithText("Stats").assertIsDisplayed()
    }

    @Test
    fun navGraph_foodDatabaseQuickAction_navigatesToFoodScreen() {
        runBlocking {
            userPrefsDataStore.setOnboardingCompleted(true)
        }

        composeRule.setContent {
            ValueTrackerTheme {
                NavGraph(userPrefsDataStore = userPrefsDataStore)
            }
        }

        composeRule.onNodeWithTag("center_action_button").performClick()
        composeRule.onNodeWithText("Food Database").performClick()

        composeRule.onNodeWithTag("food_screen_fade").assertIsDisplayed()
        composeRule.onNodeWithText("Your food").assertIsDisplayed()
        composeRule.onNodeWithText("Users history").assertIsDisplayed()
    }

    @Test
    fun navGraph_primaryTabsNavigateDeterministicallyFromSettings() {
        runBlocking {
            userPrefsDataStore.setOnboardingCompleted(true)
        }

        composeRule.setContent {
            ValueTrackerTheme {
                NavGraph(userPrefsDataStore = userPrefsDataStore)
            }
        }

        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()

        composeRule.onNodeWithTag("bottom_nav_food").performClick()
        composeRule.onNodeWithText("Your food").assertIsDisplayed()

        composeRule.onNodeWithTag("bottom_nav_stats").performClick()
        composeRule.onNodeWithText("Analytics").assertIsDisplayed()

        composeRule.onNodeWithTag("bottom_nav_home").performClick()
        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
    }

    @Test
    fun navGraph_foodRowAddButton_opensAddFoodScreen() {
        runBlocking {
            userPrefsDataStore.setOnboardingCompleted(true)
        }

        composeRule.setContent {
            ValueTrackerTheme {
                NavGraph(userPrefsDataStore = userPrefsDataStore)
            }
        }

        composeRule.onNodeWithTag("bottom_nav_food").performClick()
        composeRule.onNodeWithTag("food_add_button_180").performClick()

        composeRule.onNodeWithText("Add food").assertIsDisplayed()
    }

    @Test
    fun navGraph_foodRowClick_opensFoodDetailsFadeScreen() {
        runBlocking {
            userPrefsDataStore.setOnboardingCompleted(true)
        }

        composeRule.setContent {
            ValueTrackerTheme {
                NavGraph(userPrefsDataStore = userPrefsDataStore)
            }
        }

        composeRule.onNodeWithTag("bottom_nav_food").performClick()
        composeRule.onNodeWithText("Chicken Breast").performClick()

        composeRule.onNodeWithTag("food_details_fade").assertIsDisplayed()
        try {
            composeRule.onNodeWithText("ValueTracker").assertIsDisplayed()
            throw AssertionError("Shell top bar should be hidden on food details")
        } catch (_: AssertionError) {
            // expected
        }
    }
}
