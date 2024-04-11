package com.willowtree.vocable.utils

interface IdlingResourceContainer {
    fun decrement()
    fun increment()

    suspend fun <T> run(action: suspend () -> T): T {
        increment()
        try {
            return action()
        } finally {
            decrement()
        }
    }
}

class IdlingResourceContainerImpl : IdlingResourceContainer {
    override fun decrement() {
        // Do nothing in prod
    }

    override fun increment() {
        // Do nothing in prod
    }
}