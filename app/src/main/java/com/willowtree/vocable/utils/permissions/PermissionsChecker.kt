package com.willowtree.vocable.utils.permissions

interface PermissionsChecker {
    fun hasPermissions(permission: String): Boolean
    fun shouldShowRequestPermissionRationale(permission: String): Boolean
}
