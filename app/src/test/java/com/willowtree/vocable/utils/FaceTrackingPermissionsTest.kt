package com.willowtree.vocable.utils

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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

@OptIn(ExperimentalCoroutinesApi::class)
class FaceTrackingPermissionsTest {

    @Before
    fun setup() {
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(any(), any())
        } returns PackageManager.PERMISSION_DENIED
    }

    private fun createFaceTrackingPermissions(sharedPreferences: IVocableSharedPreferences): IFaceTrackingPermissions {
        return FaceTrackingPermissions(
            sharedPreferences = sharedPreferences,
            activity = mockk(relaxed = true)
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
    fun `enableFaceTracking() sets permission state to Enabled`() = runTest {

        val sharedPreference = createSharedPrefs(headTrackingEnabled = false)
        assertFalse(sharedPreference.getHeadTrackingEnabled())

        // Setting false so its not Requested on init
        val permissions = createFaceTrackingPermissions(sharedPreference)

        advanceUntilIdle()

        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.Disabled)

        permissions.enableFaceTracking()

        advanceUntilIdle()

        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.Enabled)
        // Check shared preferences is updated
        assertTrue(sharedPreference.getHeadTrackingEnabled())
    }

    @Test
    fun `disableFaceTracking() sets permission state to Disabled`() = runTest {

        val sharedPreference = createSharedPrefs(headTrackingEnabled = true)
        assertTrue(sharedPreference.getHeadTrackingEnabled())

        // Setting true so its not Disabled on init
        val permissions = createFaceTrackingPermissions(sharedPreference)

        advanceUntilIdle()

        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.PermissionRequested)

        permissions.disableFaceTracking()

        advanceUntilIdle()

        assertEquals(permissions.permissionState.first(), IFaceTrackingPermissions.PermissionState.Disabled)
        // Check shared preferences is updated
        assertFalse(sharedPreference.getHeadTrackingEnabled())
    }
}