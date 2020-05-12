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
            firstPhrase.tap()
            currentText.assertTextMatches("Please")
        }
    }

    @Test
    fun verifyDefaultCategoriesExist() {
        mainScreen.apply {
            mainScreen.verifyDefaultCategoriesExist()
        }
    }

    @Test
    fun testWhenTappingPhrase_ThenThatPhraseDisplaysOnOutputLabel() {
        mainScreen.apply {
            mainScreen.tapPhrase(defaultPhraseGeneral[0])
            currentText.assertTextMatches(defaultPhraseGeneral[0])
        }
    }

    /*    func testSelectingCategoryChangesPhrases() {
        mainScreen.scrollRightAndTapCurrentCategory(numTimesToScroll: 1)
        verifyGivenPhrasesDisplay(setOfPhrases: mainScreen.defaultPhraseBasicNeeds)
    }*/

    @Test
    fun testSelectingCategoryChangesPhrases() {
        mainScreen.apply {
            mainScreen.scrollRightAndTapCurrentCategory(1)
            verifyGivenPhrasesDisplay(mainScreen.defaultPhraseBasicNeeds)
        }
    }


}