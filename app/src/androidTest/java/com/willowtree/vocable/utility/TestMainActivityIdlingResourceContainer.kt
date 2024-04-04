package com.willowtree.vocable.utility

import androidx.test.espresso.idling.CountingIdlingResource
import com.willowtree.vocable.utils.MainActivityIdlingResourceContainer

class TestMainActivityIdlingResourceContainer: MainActivityIdlingResourceContainer {

    val idlingResource = CountingIdlingResource("MainActivityIdlingResource").apply {
        increment()
    }

    override fun setIdle() {
        if (!idlingResource.isIdleNow) {
            idlingResource.decrement()
        }
    }
}