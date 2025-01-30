package com.willowtree.vocable.utility

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class DisableAnimationsRule : TestRule {
    private val animationScales = listOf(
        "window_animation_scale",
        "transition_animation_scale",
        "animator_duration_scale"
    )

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                disableAnimations()
                try {
                    base.evaluate()
                } finally {
                    enableAnimations()
                }
            }
        }
    }

    private fun disableAnimations() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.apply {
            animationScales.forEach { scale ->
                executeShellCommand("settings put global $scale 0")
            }
        }
    }

    private fun enableAnimations() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.apply {
            animationScales.forEach { scale ->
                executeShellCommand("settings put global $scale 1")
            }
        }
    }
} 