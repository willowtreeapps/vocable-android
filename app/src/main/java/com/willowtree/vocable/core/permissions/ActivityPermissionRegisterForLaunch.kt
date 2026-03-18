package com.willowtree.vocable.core.permissions

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract

class ActivityPermissionRegisterForLaunch(
    private val activity: ComponentActivity
) : PermissionRequester {

    override fun registerForActivityResult(
        contract: ActivityResultContract<String, Boolean>,
        activityResultCallback: (Boolean) -> Unit,
    ): PermissionRequestLauncher {
        val activityLauncher = activity.registerForActivityResult(contract) {
            activityResultCallback(it)
        }
        return object : PermissionRequestLauncher {
            override fun launch(input: String) {
                activityLauncher.launch(input)
            }

            override fun unregister() {
                activityLauncher.unregister()
            }
        }
    }
}
