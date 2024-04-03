package com.willowtree.vocable.tests

import androidx.test.espresso.IdlingPolicies
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.willowtree.vocable.splash.SplashActivity
import com.willowtree.vocable.utility.SplashIdlingResourceTestRule
import com.willowtree.vocable.utility.VocableKoinTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import java.io.File
import java.util.concurrent.TimeUnit

open class BaseTest {

    private var firstLaunch = true
    private val name = TestName()

    @get:Rule(order = 0)
    val vocableKoinTestRule = VocableKoinTestRule()

    @get:Rule(order = 1)
    val splashIdlingResourceTestRule = SplashIdlingResourceTestRule()

    @get:Rule(order = 2)
    val activityRule = ActivityScenarioRule(SplashActivity::class.java)

    @get:Rule
    var mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @Rule
    fun getTestName(): TestName = name

    @Before
    open fun setup() {
        IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.SECONDS)
        splashIdlingResourceTestRule.register()

        // Since the build machine gets wiped after every run we can check the file storage
        // of the emulator to determine if this is a first time launch
        // and dismiss the full screen immersive popup on android if it is
        firstLaunch = getInstrumentation().targetContext.filesDir.listFiles()?.isEmpty() ?: true
        if (firstLaunch) {
            takeScreenshot("InitialSetup")
        }
        dismissARCoreDialog()
    }

    @After
    open fun teardown() {
        // We need to re-register the idling resource for the splash screen
        // so that we can wait for the splash screen to finish loading
        splashIdlingResourceTestRule.unregister()
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

        val result = device.wait(Until.findObject(By.text("CONTINUE")), 10000)
        if (result != null) {
            // Only press back if the dialog is displayed. Otherwise this will press back on the
            // activity which will cause the test to fail.
            device.pressBack()
        }
    }
}
