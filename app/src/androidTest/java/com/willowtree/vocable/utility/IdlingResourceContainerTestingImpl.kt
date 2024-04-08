package com.willowtree.vocable.utility

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import com.willowtree.vocable.utils.IdlingResourceContainer

class IdlingResourceContainerTestingImpl(name: String): IdlingResourceContainer {

    private val countingIdlingResource = CountingIdlingResource(name)
    val idlingResource: IdlingResource = countingIdlingResource

    override fun increment() {
        countingIdlingResource.increment()
    }

    override fun decrement() {
        countingIdlingResource.decrement()
    }
}