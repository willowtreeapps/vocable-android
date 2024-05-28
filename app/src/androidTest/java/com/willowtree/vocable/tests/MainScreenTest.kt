package com.willowtree.vocable.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.willowtree.vocable.screens.KeyboardScreen
import com.willowtree.vocable.screens.MainScreen
import com.willowtree.vocable.utility.assertTextMatches
import com.willowtree.vocable.utility.tap
import kotlinx.coroutines.delay
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest : BaseTest() {

    private val mainScreen = MainScreen()
    private val keyboardScreen = KeyboardScreen()

    @Test
    fun verifyDefaultTextAppears() {
        mainScreen.apply {
            val defaultText = "Select something below to speak."
            currentText.assertTextMatches(defaultText)
        }
    }

    @Test
    fun verifyClickingPhraseUpdatesCurrentText() {
        mainScreen.apply {
            tapPhrase(defaultPhraseGeneral[0])
            currentText.assertTextMatches(defaultPhraseGeneral[0])
        }
    }

    @Test
    fun verifyDefaultCategoriesExist() {
        mainScreen.apply {
            verifyDefaultCategoriesExist()
        }
    }

    @Test
    fun verifyDefaultSayingsInCategoriesExist() {
        mainScreen.apply {
            scrollRightAndTapCurrentCategory(0)
            verifyGivenPhrasesDisplay(defaultPhraseGeneral)
        }
    }

    @Test
    fun verifySelectingCategoryChangesPhrasesWhenNavigatingRight() {
        mainScreen.apply {
            scrollRightAndTapCurrentCategory(1)
            verifyGivenPhrasesDisplay(defaultPhraseBasicNeeds)
        }
    }

    @Test
    fun verifySelectingCategoryChangesPhrasesWhenNavigatingLeft() {
        mainScreen.apply {
            verifyGivenPhrasesDisplay(defaultPhraseGeneral)
            scrollRightAndTapCurrentCategory(2)
            scrollLeft(1)
            verifyGivenPhrasesDisplay(defaultPhraseBasicNeeds)
            scrollLeft(1)
            verifyGivenPhrasesDisplay(defaultPhraseGeneral)
        }
    }

    @Test
    fun verifyCategoryStaysAfterNavigatingToKeyboardImmediately() {
        mainScreen.apply {
            verifyGivenPhrasesDisplay(defaultPhraseGeneral)
            scrollRightAndTapCurrentCategory(1)
            verifyGivenPhrasesDisplay(defaultPhraseBasicNeeds)
            keyboardNavitgationButton.tap()
        }

        keyboardScreen.apply {
            keyboardPresetsButton.tap()
        }

        mainScreen.apply {
            scrollLeft(1)
            verifyGivenPhrasesDisplay(defaultPhraseGeneral)
        }
    }

    @Test
    // Bug: https://github.com/willowtreeapps/vocable-android/issues/531
    fun verifyCategoryStaysAfterNavigatingToKeyboardAfterDelay() {
        mainScreen.apply {
            verifyGivenPhrasesDisplay(defaultPhraseGeneral)
            scrollRightAndTapCurrentCategory(1)
            verifyGivenPhrasesDisplay(defaultPhraseBasicNeeds)
            keyboardNavitgationButton.tap()
        }

        Thread.sleep(10000)

        keyboardScreen.apply {
            keyboardPresetsButton.tap()
        }

        mainScreen.apply {
            scrollLeft(1)
            verifyGivenPhrasesDisplay(defaultPhraseGeneral)
        }
    }
}