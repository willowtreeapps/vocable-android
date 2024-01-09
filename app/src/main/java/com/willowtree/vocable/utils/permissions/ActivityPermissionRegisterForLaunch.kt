package com.willowtree.vocable.utils.permissions

import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity

class ActivityPermissionRegisterForLaunch(
    private val activity: AppCompatActivity
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