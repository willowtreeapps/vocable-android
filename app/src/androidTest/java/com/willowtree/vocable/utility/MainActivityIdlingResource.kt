package com.willowtree.vocable.utility

import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

class MainActivityIdlingResource: IdlingResource {

    private val isIdle = AtomicBoolean(false)
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    fun setIdle() {
        if(!isIdle.getAndSet(true)) {
            resourceCallback?.onTransitionToIdle()
        }
    }

    override fun getName(): String = "MainActivityIdlingResource"

    override fun isIdleNow(): Boolean {
        return isIdle.get()
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        resourceCallback = callback
    }
}