package com.willowtree.vocable.utils.features

import com.willowtree.vocable.BuildConfig
import com.willowtree.vocable.utils.IVocableSharedPreferences

/**
 * Central location for managing feature flags in the application.
 * Supports both build-time and runtime feature flags.
 */
object FeatureFlags {
    private const val KEY_LISTEN_MODE = "listen_mode"

    /**
     * Interface for all feature flag implementations.
     */
    interface Flag {
        /**
         * Whether the feature is currently enabled.
         */
        val enabled: Boolean
    }

    /**
     * Build-time flags that are controlled via build configuration.
     */
    object BuildTimeFlag : Flag {
        override val enabled: Boolean = BuildConfig.ENABLE_LISTEN_MODE
    }

    /**
     * Runtime flags that can be toggled via SharedPreferences.
     */
    class RuntimeFlag(
        private val sharedPreferences: IVocableSharedPreferences,
        private val key: String,
        private val defaultValue: Boolean
    ) : Flag {
        override val enabled: Boolean
            get() = sharedPreferences.getFeatureEnabled(key, defaultValue)
    }

    // Feature flag declarations
    val LISTEN_MODE = BuildTimeFlag
} 