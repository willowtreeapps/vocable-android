package com.willowtree.vocable.core.permissions

interface PermissionsChecker {
    fun hasPermissions(permission: String): Boolean
    fun shouldShowRequestPermissionRationale(permission: String): Boolean
}
