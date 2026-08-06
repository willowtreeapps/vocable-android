package com.willowtree.vocable.core

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.utility.VocableKoinTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.get

@RunWith(AndroidJUnit4::class)
class VocableSharedPreferencesTest {

    @get:Rule
    val vocableKoinTestRule = VocableKoinTestRule()

    @Test
    fun clearAll_resetsEveryPreferenceToItsDefault() {
        val prefs = get().get<VocableSharedPreferences>()
        prefs.setMySayings(setOf("Hello"))
        prefs.setDwellTime(3000L)
        prefs.setSensitivity(0.9f)
        prefs.setHeadTrackingEnabled(false)
        prefs.setSelectedVoiceName("Aria")

        prefs.clearAll()

        assertEquals(emptyList<String>(), prefs.getMySayings())
        assertEquals(VocableSharedPreferences.DEFAULT_DWELL_TIME, prefs.getDwellTime())
        assertEquals(VocableSharedPreferences.DEFAULT_SENSITIVITY, prefs.getSensitivity())
        assertEquals(VocableSharedPreferences.DEFAULT_HEAD_TRACKING_ENABLED, prefs.getHeadTrackingEnabled())
        assertEquals(null, prefs.getSelectedVoiceName())
    }

    /**
     * Regression test: a bare SharedPreferences.Editor.clear() does not notify
     * OnSharedPreferenceChangeListeners by itself - only keys explicitly put/removed in an edit
     * register as changed. Live UI that observes these keys (GazeButton's dwell time,
     * FaceTrackingViewModel's sensitivity/head-tracking, FaceTrackingPermissions) would silently
     * keep showing stale values after a reset if clearAll() only called clear().
     */
    @Test
    fun clearAll_notifiesListenersForLiveObservedKeys() {
        val prefs = get().get<VocableSharedPreferences>()
        prefs.setDwellTime(3000L)
        prefs.setSensitivity(0.9f)
        prefs.setHeadTrackingEnabled(false)

        val notifiedKeys = mutableSetOf<String>()
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null) notifiedKeys.add(key)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)

        prefs.clearAll()

        prefs.unregisterOnSharedPreferenceChangeListener(listener)
        assertTrue(notifiedKeys.contains(VocableSharedPreferences.KEY_DWELL_TIME))
        assertTrue(notifiedKeys.contains(VocableSharedPreferences.KEY_SENSITIVITY))
        assertTrue(notifiedKeys.contains(VocableSharedPreferences.KEY_HEAD_TRACKING_ENABLED))
    }
}
