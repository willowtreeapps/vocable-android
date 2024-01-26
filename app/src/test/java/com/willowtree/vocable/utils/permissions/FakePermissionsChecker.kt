package com.willowtree.vocable.utils.permissions

class FakePermissionsChecker(
    private val hasPermissions: Boolean = false,
    private val shouldShowRequestPermissionRationale: Boolean = true,
): PermissionsChecker {
    override fun hasPermissions(permission: String): Boolean {
        return hasPermissions
    }
    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return shouldShowRequestPermissionRationale
    }
}
