package com.willowtree.vocable.tests

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.willowtree.vocable.R
import com.willowtree.vocable.utility.SplashScreenIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.File


abstract class BaseTest<T : Activity> {

    private val idleRegistry = IdlingRegistry.getInstance()
    private val idlingResource = SplashScreenIdlingResource(
        ViewMatchers.withId(R.id.current_text),
        ViewMatchers.isDisplayed()
    )

    @Rule
    lateinit var activityRule: ActivityTestRule<T>

    @Before
    fun setup() {
        idleRegistry.register(idlingResource)
        getActivityTestRule().launchActivity(Intent())
    }

    @After
    fun teardown() {
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
    fun dismissFullscreenPrompt() {
        val device = UiDevice.getInstance(getInstrumentation())
        try {
            device.wait(Until.findObject(By.text("Got it")), 15000).click()
        } catch (e: NullPointerException) {
            Log.d("Test", "Prompt not found, continuing with test")
        }
    }

    abstract fun getActivityTestRule(): ActivityTestRule<T>
}