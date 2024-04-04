package com.willowtree.vocable.utils

interface MainActivityIdlingResourceContainer {
    fun setIdle()
}

class MainActivityIdlingResourceContainerImpl : MainActivityIdlingResourceContainer {
    override fun setIdle() {
        // Do nothing in prod
    }
}