package com.willowtree.vocable.tests

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.willowtree.vocable.splash.SplashActivity
import com.willowtree.vocable.utility.IdlingResourceTestRule
import com.willowtree.vocable.utility.VocableKoinTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import java.io.File

open class BaseTest {

    private var firstLaunch = true
    private val name = TestName()

    @get:Rule(order = 0)
    val vocableKoinTestRule = VocableKoinTestRule()

    @get:Rule(order = 1)
    val idlingResourceTestRule = IdlingResourceTestRule()

    @get:Rule(order = 2)
    val activityRule = ActivityScenarioRule(SplashActivity::class.java)

    @get:Rule
    var mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @Rule
    fun getTestName(): TestName = name

    @Before
    open fun setup() {
        // Since the build machine gets wiped after every run we can check the file storage
        // of the emulator to determine if this is a first time launch
        // and dismiss the full screen immersive popup on android if it is
        firstLaunch = getInstrumentation().targetContext.filesDir.listFiles()?.isEmpty() ?: true
        if (firstLaunch) {
            takeScreenshot("InitialSetup")
        }
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
}
