package com.willowtree.vocable.tests

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.GrantPermissionRule
import com.willowtree.vocable.splash.SplashActivity
import com.willowtree.vocable.utility.IdlingResourceTestRule
import com.willowtree.vocable.utility.VocableKoinTestRule
import org.junit.Rule

open class BaseTest {

    @get:Rule(order = 0)
    val vocableKoinTestRule = VocableKoinTestRule()

    @get:Rule(order = 1)
    val idlingResourceTestRule = IdlingResourceTestRule()

    @get:Rule(order = 2)
    val activityRule = ActivityScenarioRule(SplashActivity::class.java)

    @get:Rule
    var mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

}
