package com.willowtree.vocable.tests

import android.graphics.Point
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.willowtree.vocable.MainActivity
import com.willowtree.vocable.R
import com.willowtree.vocable.VocableApp
import com.willowtree.vocable.screens.MainScreen
import com.willowtree.vocable.splash.SplashActivity
import com.willowtree.vocable.utility.SplashScreenIdlingResource
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import java.io.File

open class BaseTest {

    private var firstLaunch = true
    private val idleRegistry = IdlingRegistry.getInstance()
    private val idlingResource = SplashScreenIdlingResource(
        ViewMatchers.withId(R.id.current_text),
        ViewMatchers.isDisplayed()
    )
    private val name = TestName()
    //private var activityRule = ActivityTestRule(SplashActivity::class.java, false, false)

    @Rule
    fun getTestName(): TestName = name

    //@Rule
    //fun getActivityRule(): ActivityTestRule<SplashActivity> = activityRule

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    open fun setup() {
//        getInstrumentation().newApplication(
//            VocableApp::class.java.classLoader,
//            VocableApp::class.java.name,
//            ApplicationProvider.getApplicationContext()
//        )

        //IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.SECONDS)
        //idleRegistry.register(idlingResource)
        //activityRule.launchActivity(Intent())

        // Since the build machine gets wiped after every run we can check the file storage
        // of the emulator to determine if this is a first time launch
        // and dismiss the full screen immersive popup on android if it is
//        firstLaunch = getInstrumentation().targetContext.filesDir.listFiles().isEmpty()
//        if (firstLaunch) {
//            takeScreenshot("InitialSetup")
//        }
        takeScreenshot("InitialSetup")
        dismissARCoreDialog()
        // Once we confirm we are on the MainScreen we need to unregister the Splash Screen
        // idling resource so that we don't transition back to a not idle state when we
        // navigate off of the main screen which would cause a timeout exception
        MainScreen().checkOnMainScreen()
        //idleRegistry.unregister(idlingResource)


    }

    /* This function will take a screenshot of the application and copy it to the sd card path
    on the emulator so that it can be pulled out with an adb command on the build machine.
    The copy shell command is necessary because the app and its contents are deleted
    once the tests finish running on CircleCI. */
    fun takeScreenshot(fileName: String) {
        val file = File(getInstrumentation().targetContext.filesDir.path, "$fileName.png")
        UiDevice.getInstance(getInstrumentation()).takeScreenshot(file)
        getInstrumentation().uiAutomation.executeShellCommand(
            "run-as com.willowtree.vocable cp $file /sdcard/Pictures/$fileName.png"
        )
    }

    // This function dismisses the ar core/play services dialog if it is displayed
    private fun dismissARCoreDialog() {
        val device = UiDevice.getInstance(getInstrumentation())

        // Catch the exception if the popup doesn't appear so we don't fail the tests
        try {
            device.wait(Until.findObject(By.text("CONTINUE")), 10000).click()
        } catch (e: NullPointerException) {
            Log.d("Test", "Popup not found, continuing with test")
        }
    }

}