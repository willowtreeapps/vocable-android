package com.willowtree.vocable.core.permissions

import androidx.activity.result.contract.ActivityResultContract

interface PermissionRequester {
    fun registerForActivityResult(
        contract: ActivityResultContract<String, Boolean>,
        activityResultCallback: (Boolean) -> Unit,
    ) : PermissionRequestLauncher
}

interface PermissionRequestLauncher {
    fun launch(input: String)
    fun unregister()
}