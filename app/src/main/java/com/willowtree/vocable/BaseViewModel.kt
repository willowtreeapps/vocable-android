package com.willowtree.vocable

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

open class BaseViewModel : ViewModel() {

    private val viewModelJob = SupervisorJob()
    protected val backgroundScope = CoroutineScope(viewModelJob + Dispatchers.IO)
    protected val uiScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}