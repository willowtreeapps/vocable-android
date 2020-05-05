package com.willowtree.vocable.tests

import android.content.Intent
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResourceTimeoutException
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.willowtree.vocable.R
import com.willowtree.vocable.screens.MainScreen
import com.willowtree.vocable.splash.SplashActivity
import com.willowtree.vocable.utility.SplashScreenIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import java.io.File
import java.util.concurrent.TimeUnit


open class BaseTest {

    private val idleRegistry = IdlingRegistry.getInstance()
    private val idlingResource = SplashScreenIdlingResource(
        ViewMatchers.withId(R.id.current_text),
        ViewMatchers.isDisplayed()
    )
    private val name = TestName()
    private var firstLaunch = true
    private var activityRule = ActivityTestRule(SplashActivity::class.java, false, false)

    @Rule
    fun getTestName(): TestName = name

    @Rule
    fun getActivityRule(): ActivityTestRule<SplashActivity> = activityRule

    @Before
    open fun setup() {
        IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.SECONDS)
        idleRegistry.register(idlingResource)
        activityRule.launchActivity(Intent())

        firstLaunch = getInstrumentation().targetContext.filesDir.listFiles().size < 1

        if (firstLaunch) {
            dismissFullscreenPrompt()
        }


    }

    @After
    open fun teardown() {
        takeScreenshot(getTestName().methodName)
        idleRegistry.unregister(idlingResource)
    }

    /* This function will take a screenshot of the application and copy it to the sd card path
    on the emulator so that it can be pulled out with an adb command on the build machine.
    The copy shell command is necessary because the app and its contents are deleted
    once the tests finish running on CircleCI. */
    fun takeScreenshot(fileName: String) {
        val file = File(getInstrumentation().targetContext.filesDir.path, "$fileName.png")
        UiDevice.getInstance(getInstrumentation()).takeScreenshot(file)
        getInstrumentation().uiAutomation.executeShellCommand(
            "run-as com.willowtree.vocable cp $file /sdcard/Pictures/$fileName.png")
    }

    // This function dismisses the full screen immersive prompt which shows on first launch
    private fun dismissFullscreenPrompt() {
        val device = UiDevice.getInstance(getInstrumentation())
        device.wait(Until.findObject(By.text("GOT IT")), 15000).click()
    }
}