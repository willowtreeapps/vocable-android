package com.willowtree.vocable.tests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.willowtree.vocable.MainActivity
import com.willowtree.vocable.utility.VocableKoinTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule(order = 0)
    val koinRule = VocableKoinTestRule()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @Test
    fun verifyDefaultTextAppears() {
        composeRule.onNodeWithText("Select something below to speak.").assertIsDisplayed()
    }

    @Test
    fun verifySettingsButtonNavigatesToSettings() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Categories and Phrases").assertIsDisplayed()
        composeRule.onNodeWithText("Selection Mode").assertIsDisplayed()
    }

    @Test
    fun verifyNavigateToEditCategoriesFromSettings() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Categories and Phrases").performClick()
        composeRule.onNodeWithText("Categories").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_categories_add_button").assertIsDisplayed()
    }

    @Test
    fun verifyNavigateToSelectionModeFromSettings() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Selection Mode").performClick()
        composeRule.onNodeWithText("Selection Mode").assertIsDisplayed()
        composeRule.onNodeWithText("Head Tracking").assertIsDisplayed()
    }

    @Test
    fun verifyNavigateToEditCategoryMenuFromEditCategories() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Categories and Phrases").performClick()
        composeRule.onNodeWithTag("edit_category_item_preset_general").performClick()
        composeRule.onNodeWithText("Rename Category").assertIsDisplayed()
        composeRule.onNodeWithText("Show Category").assertIsDisplayed()
        composeRule.onNodeWithText("Edit Phrases").assertIsDisplayed()
        composeRule.onNodeWithText("Remove Category").assertIsDisplayed()
    }

    @Test
    fun verifyNavigateToEditPhrasesFromEditCategoryMenu() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Categories and Phrases").performClick()
        composeRule.onNodeWithTag("edit_category_item_preset_general").performClick()
        composeRule.onNodeWithText("Edit Phrases").performClick()
        composeRule.onNodeWithText("General").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add Phrase").assertIsDisplayed()
    }

    @Test
    fun verifySelectionModeBackNavigationReturnsToSettings() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Selection Mode").performClick()
        composeRule.onNodeWithContentDescription("Close Settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun verifyEditCategoriesBackNavigationReturnsToSettings() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Categories and Phrases").performClick()
        composeRule.onNodeWithContentDescription("Close Settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun verifyEditPhrasesBackNavigationReturnsToEditCategoryMenu() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Categories and Phrases").performClick()
        composeRule.onNodeWithTag("edit_category_item_preset_general").performClick()
        composeRule.onNodeWithText("Edit Phrases").performClick()
        composeRule.onNodeWithContentDescription("Close Settings").performClick()
        composeRule.onNodeWithText("Rename Category").assertIsDisplayed()
    }

    @Test
    fun verifyEditCategoriesPagingControlsExist() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Categories and Phrases").performClick()
        composeRule.onNodeWithTag("edit_categories_page_indicator").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_categories_add_button").assertIsDisplayed()
    }

    @Test
    fun verifyEditCategoriesNextPageButtonWorks() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Categories and Phrases").performClick()
        composeRule.onNodeWithTag("edit_categories_next_page").performClick()
        // Page indicator updates after next page — exact value depends on screen size/item count
        composeRule.onNodeWithTag("edit_categories_page_indicator").assertIsDisplayed()
    }

    @Test
    fun verifySelectionModeHeadTrackingControlVisible() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Selection Mode").performClick()
        // Switch is inside a Button (mergeDescendants=true), so use unmerged tree to find it
        composeRule.onNodeWithTag("selection_mode_head_tracking_switch", useUnmergedTree = true).assertIsDisplayed()
    }
}
