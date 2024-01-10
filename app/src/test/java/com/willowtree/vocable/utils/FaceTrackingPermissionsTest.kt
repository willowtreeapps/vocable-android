package com.willowtree.vocable.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.willowtree.vocable.utils.permissions.FakePermissionRegisterForLaunch
import com.willowtree.vocable.utils.permissions.FakePermissionsDialogShower
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any

@OptIn(ExperimentalCoroutinesApi::class)
class FaceTrackingPermissionsTest {

    @Before
    fun setup() {
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(any(), any())
        } returns PackageManager.PERMISSION_DENIED
    }

    private val fakePermissionRegisterForLaunch = FakePermissionRegisterForLaunch()

    private fun createFaceTrackingPermissions(sharedPreferences: IVocableSharedPreferences): IFaceTrackingPermissions {
        return FaceTrackingPermissions(
            sharedPreferences = sharedPreferences,
            activity = mockk(relaxed = true),
            permissionRequester = fakePermissionRegisterForLaunch,
            permissionsDialogShower = FakePermissionsDialogShower(),
        )
    }

    private fun createSharedPrefs(headTrackingEnabled: Boolean): IVocableSharedPreferences {
        return FakeVocableSharedPreferences(headTrackingEnabled = headTrackingEnabled)
    }

    @Test
    fun `permission state Requested on init if head tracking Enabled`() = runTest {

        val permissions = createFaceTrackingPermissions(createSharedPrefs(headTrackingEnabled = true))

        advanceUntilIdle()

        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.PermissionRequested)
    }

    @Test
    fun `permission state Disabled on init if head tracking disabled`() = runTest {

        val permissions = createFaceTrackingPermissions(createSharedPrefs(headTrackingEnabled = false))

        advanceUntilIdle()

        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.Disabled)
    }

    @Test
    fun `requestFaceTracking() sets permission state to PermissionRequested`() = runTest {

        // Setting false so its not Requested on init
        val permissions = createFaceTrackingPermissions(createSharedPrefs(headTrackingEnabled = false))

        advanceUntilIdle()

        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.Disabled)

        permissions.requestFaceTracking()

        advanceUntilIdle()

        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.PermissionRequested)
    }

    @Test
    fun `disableFaceTracking() sets permission state to Disabled`() = runTest {
        val sharedPreference = createSharedPrefs(headTrackingEnabled = true)
        assertTrue(sharedPreference.getHeadTrackingEnabled())

        val permissions = createFaceTrackingPermissions(sharedPreference)
        advanceUntilIdle()
        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.PermissionRequested)
        permissions.disableFaceTracking()

        advanceUntilIdle()
        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.Disabled)
        assertFalse(sharedPreference.getHeadTrackingEnabled())
    }

    @Test
    fun `when requestFaceTracking successful, permission state set to enabled`() = runTest {
        val sharedPreference = createSharedPrefs(headTrackingEnabled = false)
        val permissions = createFaceTrackingPermissions(sharedPreference)
        advanceUntilIdle()

        permissions.requestFaceTracking()
        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.PermissionRequested)

        fakePermissionRegisterForLaunch.triggerActivityResult(contract = Manifest.permission.CAMERA, result = true)
        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.Enabled)
    }

    @Test
    fun `when requestFaceTracking unsuccessful, permission state set to disabled`() = runTest {
        val sharedPreference = createSharedPrefs(headTrackingEnabled = false)
        val permissions = createFaceTrackingPermissions(sharedPreference)
        advanceUntilIdle()

        permissions.requestFaceTracking()
        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.PermissionRequested)

        fakePermissionRegisterForLaunch.triggerActivityResult(contract = Manifest.permission.CAMERA, result = false)
        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.Disabled)
    }
}