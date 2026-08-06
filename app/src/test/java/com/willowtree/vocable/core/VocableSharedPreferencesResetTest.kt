package com.willowtree.vocable.core

import com.willowtree.vocable.utils.FakeVocableSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * There's no Robolectric in this repo, so the real EncryptedSharedPreferences-backed
 * [VocableSharedPreferences] can't be exercised from a JVM unit test. This instead pins the
 * documented defaults via [FakeVocableSharedPreferences], which mirrors the production contract.
 */
class VocableSharedPreferencesResetTest {

    @Test
    fun `clearAll resets My Sayings to empty`() {
        val prefs = FakeVocableSharedPreferences(mySayings = listOf("Hello", "Thanks"))

        prefs.clearAll()

        assertEquals(emptyList<String>(), prefs.getMySayings())
    }

    @Test
    fun `clearAll resets dwell time to one second`() {
        val prefs = FakeVocableSharedPreferences(dwellTime = 3000L)

        prefs.clearAll()

        assertEquals(VocableSharedPreferences.DEFAULT_DWELL_TIME, prefs.getDwellTime())
    }

    @Test
    fun `clearAll resets sensitivity to medium`() {
        val prefs = FakeVocableSharedPreferences(sensitivity = 0.9f)

        prefs.clearAll()

        assertEquals(VocableSharedPreferences.DEFAULT_SENSITIVITY, prefs.getSensitivity())
    }

    @Test
    fun `clearAll resets head tracking enabled to true`() {
        val prefs = FakeVocableSharedPreferences(headTrackingEnabled = false)

        prefs.clearAll()

        assertEquals(VocableSharedPreferences.DEFAULT_HEAD_TRACKING_ENABLED, prefs.getHeadTrackingEnabled())
    }

    @Test
    fun `clearAll resets selected voice to unset`() {
        val prefs = FakeVocableSharedPreferences(selectedVoiceName = "Aria")

        prefs.clearAll()

        assertNull(prefs.getSelectedVoiceName())
    }
}
