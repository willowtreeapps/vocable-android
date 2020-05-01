package com.willowtree.vocable.tests

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.willowtree.vocable.R
import com.willowtree.vocable.screens.MainScreen
import com.willowtree.vocable.splash.SplashActivity
import com.willowtree.vocable.utility.SplashScreenIdlingResource
import com.willowtree.vocable.utility.assertTextMatches
import com.willowtree.vocable.utility.tap

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest : BaseTest<SplashActivity>() {

    private val mainScreen = MainScreen()

    override fun getActivityTestRule(): ActivityTestRule<SplashActivity> {
        return ActivityTestRule(
            SplashActivity::class.java, false, true
        )
    }

    @Test
    fun verifyClickingPhraseUpdatesCurrentText() {
        val idlingResource = SplashScreenIdlingResource(
            ViewMatchers.withId(R.id.current_text),
            ViewMatchers.isDisplayed()
        )
        val registry = IdlingRegistry.getInstance()

        registry.register(idlingResource)

        mainScreen.apply {
            firstPhrase.tap()
            currentText.assertTextMatches("Please")
        }
    }
}