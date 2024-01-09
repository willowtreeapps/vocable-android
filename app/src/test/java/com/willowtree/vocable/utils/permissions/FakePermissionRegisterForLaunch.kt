package com.willowtree.vocable.utils.permissions

import androidx.activity.result.contract.ActivityResultContract

class FakePermissionRegisterForLaunch  : PermissionRequester {

    private var callbacks = mutableMapOf<String, (Boolean) -> Unit>()

    override fun registerForActivityResult(
        contract: ActivityResultContract<String, Boolean>,
        activityResultCallback: (Boolean) -> Unit,
    ): PermissionRequestLauncher {
        return object: PermissionRequestLauncher {
            override fun launch(input: String) {
                callbacks[input] = activityResultCallback
            }

            override fun unregister() {
                // no-op
            }
        }
    }

    fun triggerActivityResult(contract: String, result: Boolean) {
        callbacks[contract]?.invoke(result)
    }

}