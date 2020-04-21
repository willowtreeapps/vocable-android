package com.willowtree.vocable.utility

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule


abstract class BaseUITest<T : Activity> {

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Rule
    lateinit var activityRule: IntentsTestRule<T>

    @Before
    fun setup() {
        println("setup")
        activityRule = getActivityTestRule()

        device.wakeUp()

        if (shouldAutoLaunchActivity()) {
            activityRule.launchActivity(Intent())
        }
    }

    protected fun launchActivity() {
        runBlocking {
            activityRule.launchActivity(Intent())
        }
    }

    abstract fun getActivityTestRule(): IntentsTestRule<T>
    protected open fun shouldAutoLaunchActivity(): Boolean = true
}