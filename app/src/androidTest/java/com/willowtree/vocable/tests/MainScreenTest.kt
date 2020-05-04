package com.willowtree.vocable.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.willowtree.vocable.screens.MainScreen
import com.willowtree.vocable.splash.SplashActivity
import com.willowtree.vocable.utility.assertTextMatches
import com.willowtree.vocable.utility.tap
import org.junit.After
import org.junit.Rule

import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest : BaseTest<SplashActivity>() {

    @Rule
    val testName = TestName()

    @After
    override fun teardown() {
        takeScreenshot(testName.methodName)
        super.teardown()
    }

    private val mainScreen = MainScreen()

    override fun getActivityTestRule(): ActivityTestRule<SplashActivity> {
        return ActivityTestRule(
            SplashActivity::class.java, false, true
        )
    }

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
            mainScreen.verifyDefaultCategories()
        }
    }
}