package com.willowtree.vocable.tests

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.ActivityTestRule
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
    }

    @After
    fun teardown() {
        idleRegistry.unregister(idlingResource)
    }

    abstract fun getActivityTestRule(): ActivityTestRule<T>
}