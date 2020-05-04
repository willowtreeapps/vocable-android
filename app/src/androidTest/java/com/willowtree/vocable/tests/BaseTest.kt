package com.willowtree.vocable.tests

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.willowtree.vocable.R
import com.willowtree.vocable.utility.SplashScreenIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Rule


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
        val device = UiDevice.getInstance(getInstrumentation())
        Thread.sleep(15000)
        device.findObject(By.text("GOT IT")).click()
    }

    @After
    fun teardown() {
        idleRegistry.unregister(idlingResource)
    }

    abstract fun getActivityTestRule(): ActivityTestRule<T>
}