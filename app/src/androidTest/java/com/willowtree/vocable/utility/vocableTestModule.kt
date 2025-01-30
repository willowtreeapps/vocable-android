package com.willowtree.vocable.utility

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.willowtree.vocable.utils.VocableEnvironment
import com.willowtree.vocable.utils.IFaceTrackingPermissions
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.dsl.module

val vocableTestModule = module {
    single { getInMemoryVocableDatabase() }
    single<VocableEnvironment> { VocableTestEnvironment() }
    single { MockFaceTrackingManager() }
    single<IFaceTrackingPermissions> { MockFaceTrackingPermissions() }
    single { VocableSharedPreferences() }
    single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
}
