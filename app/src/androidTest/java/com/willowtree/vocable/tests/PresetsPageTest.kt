package com.willowtree.vocable.tests

import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.pages.PresetsPage
import com.willowtree.vocable.splash.SplashActivity
import com.willowtree.vocable.utility.BaseUITest
import com.willowtree.vocable.utility.assertTextMatches
import com.willowtree.vocable.utility.tap

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresetsPageTest : BaseUITest<SplashActivity>() {

    private val presetsPage = PresetsPage()

    override fun getActivityTestRule(): IntentsTestRule<SplashActivity> {
        return IntentsTestRule(
            SplashActivity::class.java, false, false
        )
    }

    // Eventually we may want to get the currently selected category and then pull the first phrase of that category from the JSON
    // to make this test more dynamic.
    @Test
    fun verifyClickingPhraseUpdatesCurrentText() {

        // Wait for Splash Screen, we will want to make this wait more dynamic in the future and probably put it in the base test class
        Thread.sleep(10000)
        presetsPage.apply {
            firstPhrase.tap()
            currentText.assertTextMatches("Please")
        }
    }

}