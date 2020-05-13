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
    fun verifyClickingPhraseUpdatesCurrentText() {
        mainScreen.apply {
            tapPhrase(defaultPhraseGeneral[0])
            currentText.assertTextMatches(defaultPhraseGeneral[0])
        }
    }

    @Test
    fun verifyDefaultTextAppears() {
        mainScreen.apply {
            val defaultText = "Select something below to speak."
            currentText.assertTextMatches(defaultText)
        }
    }

    @Test
    fun verifyDefaultCategoriesExist() {
        mainScreen.apply {
            verifyDefaultCategoriesExist()
        }
    }

    @Test
    fun testSelectingCategoryChangesPhrases() {
        mainScreen.apply {
            mainScreen.scrollRightAndTapCurrentCategory(1)
            verifyGivenPhrasesDisplay(mainScreen.defaultPhraseBasicNeeds)
        }
    }


}