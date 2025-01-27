package com.willowtree.vocable.utils.features

import com.willowtree.vocable.utils.FakeVocableSharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagsTest {
    @Test
    fun `test runtime flag respects shared preferences`() {
        val sharedPrefs = FakeVocableSharedPreferences()
        val flag = FeatureFlags.RuntimeFlag(sharedPrefs, "test_flag", false)
        
        // Should start with default value
        assertFalse(flag.enabled)
        
        // Should update when preference changes
        sharedPrefs.setFeatureEnabled("test_flag", true)
        assertTrue(flag.enabled)
    }

    @Test
    fun `test runtime flag uses default value when not set`() {
        val sharedPrefs = FakeVocableSharedPreferences()
        
        // Test with default true
        val trueFlag = FeatureFlags.RuntimeFlag(sharedPrefs, "test_flag", true)
        assertTrue(trueFlag.enabled)
        
        // Test with default false
        val falseFlag = FeatureFlags.RuntimeFlag(sharedPrefs, "test_flag2", false)
        assertFalse(falseFlag.enabled)
    }
} 