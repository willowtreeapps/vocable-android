package com.willowtree.vocable.utility

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context.KEYGUARD_SERVICE
import android.content.Intent
import android.view.WindowManager
import androidx.test.espresso.intent.rule.IntentsTestRule
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule


abstract class BaseUITest<T : Activity> {

    @Rule
    lateinit var activityRule: IntentsTestRule<T>

    @Before
    fun setup() {
        println("setup")
        activityRule = getActivityTestRule()

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