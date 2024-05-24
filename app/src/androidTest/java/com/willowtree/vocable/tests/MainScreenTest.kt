package com.willowtree.vocable.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.screens.MainScreen
import com.willowtree.vocable.utility.assertTextMatches
import com.willowtree.vocable.utility.tap
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest : BaseTest() {

    private val mainScreen = MainScreen()

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
    fun verifySelectingCategoryChangesPhrases() {
        mainScreen.apply {
            scrollRightAndTapCurrentCategory(1)
            verifyGivenPhrasesDisplay(defaultPhraseBasicNeeds)
        }
    }

    @Test
    fun verifyNextCategoryShownAfterNavigatingToAndHidingFirstPresetCategory() {
        mainScreen.apply {
            // already defaults to being on the first category

            // navigate to Show Category toggle
            settingsNavigationButton.tap()
            editCategoriesButton.tap()
            editGeneralCategoryButton.tap()
            showCategorySwitch.tap()
            // exit
            editOptionsBackButton.tap()
            backButton.tap()
            closeSettingsButton.tap()

            // verify basic needs is shown: Category button and Phrases
            verifyGivenCategoryDisplay("Basic Needs")
            verifyGivenPhrasesDisplay(defaultPhraseBasicNeeds)
        }
    }

    @Test
    fun verifyFirstCategoryIsShownAfterNavigatingToAndHidingLastPresetCategory() {
        mainScreen.apply {
            // navigate to last category
            categoryBackButton.tap()

            // navigate to Show Category toggle
            settingsNavigationButton.tap()
            editCategoriesButton.tap()
            categoryForwardButton.tap()
            editRecentCategoryButton.tap()
            showCategorySwitch.tap()
            // exit
            editOptionsBackButton.tap()
            backButton.tap()
            closeSettingsButton.tap()

            // verify General is shown
            verifyGivenCategoryDisplay("General")
            verifyGivenPhrasesDisplay(defaultPhraseGeneral)
        }
    }

    @Test
    fun verifyNothingChangesWhenNextCategoryIsHidden() {
        mainScreen.apply {
            // navigate to Show Category toggle
            settingsNavigationButton.tap()
            editCategoriesButton.tap()
            editBasicNeedsCategoryButton.tap()
            showCategorySwitch.tap()
            // exit
            editOptionsBackButton.tap()
            backButton.tap()
            closeSettingsButton.tap()

            // verify General is shown
            verifyGivenCategoryDisplay("General")
            verifyGivenPhrasesDisplay(defaultPhraseGeneral)
        }
    }

    @Test
    fun verifyNothingChangesWhenPreviousCategoryIsHidden() {
        mainScreen.apply {
            // navigate to Show Category toggle
            settingsNavigationButton.tap()
            editCategoriesButton.tap()
            categoryForwardButton.tap()
            editRecentCategoryButton.tap()
            showCategorySwitch.tap()
            // exit
            editOptionsBackButton.tap()
            backButton.tap()
            closeSettingsButton.tap()

            // verify General is shown
            verifyGivenCategoryDisplay("General")
            verifyGivenPhrasesDisplay(defaultPhraseGeneral)
        }
    }
}