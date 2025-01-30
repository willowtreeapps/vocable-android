package com.willowtree.vocable.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withText
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
            // First verify we can see the General category
            verifyDefaultCategoriesExist()
            
            // Select the General category (first category)
            onView(withText("General")).tap()
            
            // Verify the phrases are loaded
            verifyGivenPhrasesDisplay(defaultPhraseGeneral)
            
            // Now tap the first phrase
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
}