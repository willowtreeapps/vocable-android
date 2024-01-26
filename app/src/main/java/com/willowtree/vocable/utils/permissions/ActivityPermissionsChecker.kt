package com.willowtree.vocable.utils.permissions

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class ActivityPermissionsChecker(
    private val activity: AppCompatActivity,
) : PermissionsChecker {

    override fun hasPermissions(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            permission
        )
    }

}
