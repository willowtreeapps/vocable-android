package com.willowtree.vocable.utils

import android.Manifest
import com.willowtree.vocable.utils.permissions.FakePermissionRegisterForLaunch
import com.willowtree.vocable.utils.permissions.FakePermissionsChecker
import com.willowtree.vocable.utils.permissions.FakePermissionsRationaleDialogShower
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FaceTrackingPermissionsTest {

    private val fakePermissionRegisterForLaunch = FakePermissionRegisterForLaunch()
    private val fakePermissionsRationaleDialogShower = FakePermissionsRationaleDialogShower()

    private fun createFaceTrackingPermissions(
        sharedPreferences: IVocableSharedPreferences = createSharedPrefs(
            headTrackingEnabled = false
        ),
        hasPermissions: Boolean = false,
        shouldShowRequestPermissionRationale: Boolean = false,
    ): IFaceTrackingPermissions {
        return FaceTrackingPermissions(
            sharedPreferences = sharedPreferences,
            packageName = "com.willowtree.vocable",
            hasPermissionsChecker = FakePermissionsChecker(
                hasPermissions = hasPermissions,
                shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale,
            ),
            permissionRequester = fakePermissionRegisterForLaunch,
            permissionsRationaleDialogShower = fakePermissionsRationaleDialogShower,
        )
    }

    private fun createSharedPrefs(headTrackingEnabled: Boolean): IVocableSharedPreferences {
        return FakeVocableSharedPreferences(headTrackingEnabled = headTrackingEnabled)
    }

    @Test
    fun `permission state Enabled on init if head tracking Enabled`() = runTest {
        val permissions = createFaceTrackingPermissions(createSharedPrefs(headTrackingEnabled = true))

        advanceUntilIdle()

        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.Enabled)
    }

    @Test
    fun `permission state Disabled on init if head tracking disabled`() = runTest {
        val permissions = createFaceTrackingPermissions(createSharedPrefs(headTrackingEnabled = false))

        advanceUntilIdle()

        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.Disabled)
    }

    @Test
    fun `disableFaceTracking() sets permission state to Disabled`() = runTest {
        val sharedPreference = createSharedPrefs(headTrackingEnabled = true)
        assertTrue(sharedPreference.getHeadTrackingEnabled())

        val permissions = createFaceTrackingPermissions(sharedPreference)
        advanceUntilIdle()

        permissions.disableFaceTracking()

        advanceUntilIdle()
        assertEquals(
            permissions.permissionState.first(),
            IFaceTrackingPermissions.PermissionState.Disabled,
        )
        assertFalse(sharedPreference.getHeadTrackingEnabled())
    }

    @Test
    fun `when requestFaceTracking successful, permission state set to enabled`() = runTest {
        val permissions = createFaceTrackingPermissions()
        permissions.requestFaceTracking()

        fakePermissionRegisterForLaunch.triggerActivityResult(
            contract = Manifest.permission.CAMERA,
            result = true
        )
        assertEquals(
            permissions.permissionState.first(),
            IFaceTrackingPermissions.PermissionState.Enabled,
        )
    }

    @Test
    fun `when requestFaceTracking successful, permission rationale dialogs not shown`() = runTest {
        val permissions = createFaceTrackingPermissions()

        permissions.requestFaceTracking()
        fakePermissionRegisterForLaunch.triggerActivityResult(
            contract = Manifest.permission.CAMERA,
            result = true
        )

        assertEquals(0, fakePermissionsRationaleDialogShower.rationaleDialogShowedCount)
        assertEquals(0, fakePermissionsRationaleDialogShower.settingsRationaleDialogShowedCount)
    }

    @Test
    fun `when requestFaceTracking() denied, permission state set to disabled`() = runTest {
        val permissions = createFaceTrackingPermissions()

        permissions.requestFaceTracking()
        fakePermissionRegisterForLaunch.triggerActivityResult(
            contract = Manifest.permission.CAMERA,
            result = false
        )

        assertEquals(
            permissions.permissionState.first(),
            IFaceTrackingPermissions.PermissionState.Disabled,
        )
    }

    @Test
    fun `when rationale should be shown, requestFaceTracking() shows only permission rationale dialog`() = runTest {
        val permissions = createFaceTrackingPermissions(
            shouldShowRequestPermissionRationale = true
        )
        advanceUntilIdle()

        permissions.requestFaceTracking()

        assertEquals(1, fakePermissionsRationaleDialogShower.rationaleDialogShowedCount)
        assertEquals(0, fakePermissionsRationaleDialogShower.settingsRationaleDialogShowedCount)
    }

    @Test
    fun `when rationale should be shown, requestFaceTracking() shows dialog and onPositiveClick launches permissionLauncher`() =
        runTest {
            val permissions = createFaceTrackingPermissions(
                shouldShowRequestPermissionRationale = true
            )
            advanceUntilIdle()

            permissions.requestFaceTracking()

            assertEquals(1, fakePermissionsRationaleDialogShower.rationaleDialogShowedCount)
            assertEquals(0, fakePermissionsRationaleDialogShower.settingsRationaleDialogShowedCount)
            fakePermissionsRationaleDialogShower.rationaleDialogOnPositiveClick.invoke()
            assertEquals(1, fakePermissionRegisterForLaunch.launchCount)
        }

    @Test
    fun `when permission rationale should be shown, rationale dialog shown and onNegativeClick launches permissionLauncher`() =
        runTest {
            val sharedPreferences = createSharedPrefs(headTrackingEnabled = true)

            // requestFaceTracking() called implicitly in init block when headTrackingEnabled is true
            createFaceTrackingPermissions(
                sharedPreferences = sharedPreferences,
                shouldShowRequestPermissionRationale = true
            )
            advanceUntilIdle()

            assertEquals(1, fakePermissionsRationaleDialogShower.rationaleDialogShowedCount)
            assertEquals(0, fakePermissionsRationaleDialogShower.settingsRationaleDialogShowedCount)
            fakePermissionsRationaleDialogShower.rationaleDialogOnNegativeClick.invoke()
            assertFalse(
                "Head tracking should be disabled if user denies dialog",
                sharedPreferences.getHeadTrackingEnabled()
            )
        }

    @Test
    fun `given initial rationale dialog denied, settings dialog rationale shown`() = runTest {
        val permissions = createFaceTrackingPermissions(
            sharedPreferences = createSharedPrefs(headTrackingEnabled = false),
            shouldShowRequestPermissionRationale = true,
        )
        advanceUntilIdle()

        // Show rationale dialog
        permissions.requestFaceTracking()

        assertEquals(1, fakePermissionsRationaleDialogShower.rationaleDialogShowedCount)
        assertEquals(0, fakePermissionsRationaleDialogShower.settingsRationaleDialogShowedCount)

        // Show permissions dialog, deny permissions
        fakePermissionsRationaleDialogShower.rationaleDialogOnPositiveClick.invoke()
        fakePermissionRegisterForLaunch.triggerActivityResult(
            contract = Manifest.permission.CAMERA,
            result = false
        )

        assertEquals(1, fakePermissionsRationaleDialogShower.rationaleDialogShowedCount)
        assertEquals(1, fakePermissionsRationaleDialogShower.settingsRationaleDialogShowedCount)
    }

    @Test
    fun `given initial rationale dialog denied, settings permission success enables head tracking`() = runTest {
        val sharedPreferences = createSharedPrefs(headTrackingEnabled = false)
        val permissions = createFaceTrackingPermissions(
            sharedPreferences = sharedPreferences,
            shouldShowRequestPermissionRationale = true,
        )
        advanceUntilIdle()

        // Show rationale dialog
        permissions.requestFaceTracking()

        // Show permissions dialog, then deny permissions
        fakePermissionsRationaleDialogShower.rationaleDialogOnPositiveClick.invoke()
        fakePermissionRegisterForLaunch.triggerActivityResult(
            contract = Manifest.permission.CAMERA,
            result = false
        )

        assertFalse(
            "headTrackingEnabled should be set as false after permission failure, but it was true",
            sharedPreferences.getHeadTrackingEnabled()
        )

        // Settings rationale dialog shown, permission given
        fakePermissionsRationaleDialogShower.rationaleDialogOnPositiveClick.invoke()
        fakePermissionRegisterForLaunch.triggerActivityResult(
            contract = Manifest.permission.CAMERA,
            result = true
        )

        assertTrue(
            "Head tracking should be true after permission success, but it was false",
            sharedPreferences.getHeadTrackingEnabled()
        )
    }
}