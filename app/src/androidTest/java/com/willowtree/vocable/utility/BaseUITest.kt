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
        val activity = activityRule.activity

        activityRule.runOnUiThread {
            val kg =
                activity.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            val lock =
                kg.newKeyguardLock(KEYGUARD_SERVICE)
            lock.disableKeyguard()
            //turn the screen on
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }
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