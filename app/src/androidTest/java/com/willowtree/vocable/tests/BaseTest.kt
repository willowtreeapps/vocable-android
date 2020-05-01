package com.willowtree.vocable.tests

import android.app.Activity
import android.content.Intent
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule


abstract class BaseTest<T : Activity> {

    @Rule
    lateinit var activityRule: ActivityTestRule<T>

    @Before
    fun setup() {
        println("setup")
        getActivityTestRule().launchActivity(Intent())
    }

    abstract fun getActivityTestRule(): ActivityTestRule<T>
}