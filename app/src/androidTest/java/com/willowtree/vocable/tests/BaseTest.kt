package com.willowtree.vocable.tests

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.GrantPermissionRule
import com.willowtree.vocable.ui.splash.SplashActivity
import com.willowtree.vocable.utility.IdlingResourceTestRule
import com.willowtree.vocable.utility.VocableKoinTestRule
import org.junit.Ignore
import org.junit.Rule

@Ignore("Legacy Espresso screen tests rely on removed View-based UI IDs. Replace with Compose UI tests.")
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
