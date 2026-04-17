package com.willowtree.vocable.tests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
class KeyboardScreenTest {

    @get:Rule(order = 0)
    val koinRule = VocableKoinTestRule()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @Test
    fun verifyKeyboardButtonBringsUpKeyboard() {
        composeRule.onNodeWithContentDescription("Keyboard").performClick()
        composeRule.onNodeWithText("Start typing…").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun verifySettingsButtonFromKeyboardNavigatesToSettings() {
        composeRule.onNodeWithContentDescription("Keyboard").performClick()
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun verifyTypingLetterUpdatesKeyboardText() {
        composeRule.onNodeWithContentDescription("Keyboard").performClick()
        composeRule.onNodeWithText("C").performClick()
        composeRule.onNodeWithTag("keyboard_input_text").assertTextContains("C")
    }

    @Test
    fun verifyClearButtonResetsKeyboardText() {
        composeRule.onNodeWithContentDescription("Keyboard").performClick()
        composeRule.onNodeWithText("C").performClick()
        composeRule.onNodeWithContentDescription("Clear").performClick()
        composeRule.onNodeWithText("Start typing…").assertIsDisplayed()
    }

    @Test
    fun verifyBackspaceRemovesCharacter() {
        composeRule.onNodeWithContentDescription("Keyboard").performClick()
        composeRule.onNodeWithText("C").performClick()
        composeRule.onNodeWithText("A").performClick()
        composeRule.onNodeWithContentDescription("Backspace").performClick()
        composeRule.onNodeWithTag("keyboard_input_text").assertTextContains("C")
    }
}
