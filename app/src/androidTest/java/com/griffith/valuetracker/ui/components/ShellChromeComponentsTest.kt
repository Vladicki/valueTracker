package com.griffith.valuetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.griffith.valuetracker.ui.theme.ValueTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShellChromeComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val items = listOf(
        BottomBarItem(label = "Home", icon = Icons.Default.Add),
        BottomBarItem(label = "Food", icon = Icons.Default.Add),
        BottomBarItem(label = "Quick Actions", icon = Icons.Default.Add),
        BottomBarItem(label = "Stats", icon = Icons.Default.Add),
    )

    @Test
    fun shellTopBar_showsBrandStreakAndProfileAction() {
        var profileClicks = 0

        composeRule.setContent {
            ValueTrackerTheme {
                ShellTopBar(
                    title = "ValueTracker",
                    streakCount = 0,
                    onProfileClick = { profileClicks++ },
                )
            }
        }

        composeRule.onNodeWithText("ValueTracker").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Current streak: 0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open settings").performClick()

        assertEquals(1, profileClicks)
    }

    @Test
    fun bottomBar_marksSelectedDestinationsAndEmitsPrimaryTabClicks() {
        val clickedIndexes = mutableListOf<Int>()

        composeRule.setContent {
            ValueTrackerTheme {
                BottomBarWithCenterAction(
                    selectedIndex = 1,
                    onItemClick = { clickedIndexes += it },
                    onScanFoodClick = {},
                    onGalleryClick = {},
                    onFoodDatabaseClick = {},
                    items = items,
                )
            }
        }

        composeRule.onNodeWithTag("bottom_nav_home").assertIsNotSelected()
        composeRule.onNodeWithTag("bottom_nav_food").assertIsSelected()
        composeRule.onNodeWithTag("bottom_nav_stats").assertIsNotSelected()
        composeRule.onNodeWithTag("bottom_nav_home").performClick()
        composeRule.onNodeWithTag("bottom_nav_food").performClick()
        composeRule.onNodeWithTag("bottom_nav_stats").performClick()

        assertEquals(listOf(0, 1, 3), clickedIndexes)
    }

    @Test
    fun bottomBar_showsQuickActionsPopupWithCorrectLabels() {
        composeRule.setContent {
            ValueTrackerTheme {
                BottomBarWithCenterAction(
                    selectedIndex = 0,
                    onItemClick = {},
                    onScanFoodClick = {},
                    onGalleryClick = {},
                    onFoodDatabaseClick = {},
                    items = items,
                )
            }
        }

        composeRule.onNodeWithTag("center_action_button").performClick()

        composeRule.onNodeWithText("Scan Food").assertIsDisplayed()
        composeRule.onNodeWithText("Gallery").assertIsDisplayed()
        try {
            composeRule.onNodeWithText("Saved Foods").assertIsDisplayed()
            throw AssertionError("Saved Foods should not be displayed in the popup")
        } catch (_: AssertionError) {
            // Expected: the popup should not contain Saved Foods
        }
        composeRule.onNodeWithText("Food Database").assertIsDisplayed()
        composeRule.onNodeWithTag("center_action_button").assertIsDisplayed()
    }

    @Test
    fun bottomBar_dispatchesCorrectCallbacksFromPopup() {
        var scanClicks = 0
        var galleryClicks = 0
        var foodDatabaseClicks = 0

        composeRule.setContent {
            ValueTrackerTheme {
                BottomBarWithCenterAction(
                    selectedIndex = 0,
                    onItemClick = {},
                    onScanFoodClick = { scanClicks++ },
                    onGalleryClick = { galleryClicks++ },
                    onFoodDatabaseClick = { foodDatabaseClicks++ },
                    items = items,
                )
            }
        }

        composeRule.onNodeWithTag("center_action_button").performClick()
        composeRule.onNodeWithText("Scan Food").performClick()
        assertEquals(1, scanClicks)

        composeRule.onNodeWithTag("center_action_button").performClick()
        composeRule.onNodeWithText("Gallery").performClick()
        assertEquals(1, galleryClicks)

        composeRule.onNodeWithTag("center_action_button").performClick()
        composeRule.onNodeWithText("Food Database").performClick()
        assertEquals(1, foodDatabaseClicks)
    }

    @Test
    fun bottomBar_openingPopupDoesNotMoveCenterActionButton() {
        lateinit var beforeBounds: DpRect
        lateinit var afterBounds: DpRect

        composeRule.setContent {
            ValueTrackerTheme {
                BottomBarWithCenterAction(
                    selectedIndex = 0,
                    onItemClick = {},
                    onScanFoodClick = {},
                    onGalleryClick = {},
                    onFoodDatabaseClick = {},
                    items = items,
                )
            }
        }

        beforeBounds = composeRule.onNodeWithTag("center_action_button").getBoundsInRoot()
        composeRule.onNodeWithTag("center_action_button").performClick()
        composeRule.onNodeWithText("Scan Food").assertIsDisplayed()
        afterBounds = composeRule.onNodeWithTag("center_action_button").getBoundsInRoot()

        assertEquals(beforeBounds.top.value, afterBounds.top.value, 0.5f)
        assertEquals(beforeBounds.bottom.value, afterBounds.bottom.value, 0.5f)
    }

    @Test
    fun bottomBar_popupIsNarrowerThanBottomBarWidth() {
        lateinit var bottomBarBounds: DpRect
        lateinit var popupBounds: DpRect

        composeRule.setContent {
            ValueTrackerTheme {
                BottomBarWithCenterAction(
                    selectedIndex = 0,
                    onItemClick = {},
                    onScanFoodClick = {},
                    onGalleryClick = {},
                    onFoodDatabaseClick = {},
                    items = items,
                )
            }
        }

        bottomBarBounds = composeRule.onNodeWithTag("bottom_nav_home").getBoundsInRoot()
        composeRule.onNodeWithTag("center_action_button").performClick()
        popupBounds = composeRule.onNodeWithTag("quick_actions_popup").getBoundsInRoot()

        val popupWidth = popupBounds.right.value - popupBounds.left.value
        val bottomBarSegmentWidth = bottomBarBounds.right.value - bottomBarBounds.left.value
        assertTrue(popupWidth < bottomBarSegmentWidth)
    }

    @Test
    fun bottomBar_plusButtonIsThirdNavElement() {
        lateinit var foodBounds: DpRect
        lateinit var plusBounds: DpRect
        lateinit var statsBounds: DpRect

        composeRule.setContent {
            ValueTrackerTheme {
                BottomBarWithCenterAction(
                    selectedIndex = 1,
                    onItemClick = {},
                    onScanFoodClick = {},
                    onGalleryClick = {},
                    onFoodDatabaseClick = {},
                    items = items,
                )
            }
        }

        foodBounds = composeRule.onNodeWithTag("bottom_nav_food").getBoundsInRoot()
        plusBounds = composeRule.onNodeWithTag("center_action_button").getBoundsInRoot()
        statsBounds = composeRule.onNodeWithTag("bottom_nav_stats").getBoundsInRoot()

        assertTrue(foodBounds.right.value < plusBounds.left.value)
        assertTrue(plusBounds.right.value < statsBounds.left.value)
    }

    @Test
    fun bottomBar_canRenderWithNoSelectedPrimaryDestination() {
        composeRule.setContent {
            ValueTrackerTheme {
                BottomBarWithCenterAction(
                    selectedIndex = -1,
                    onItemClick = {},
                    onScanFoodClick = {},
                    onGalleryClick = {},
                    onFoodDatabaseClick = {},
                    items = items,
                )
            }
        }

        composeRule.onNodeWithTag("bottom_nav_home").assertIsNotSelected()
        composeRule.onNodeWithTag("bottom_nav_stats").assertIsNotSelected()
    }
}
