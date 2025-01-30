package com.willowtree.vocable.utility

import com.willowtree.vocable.utils.VocableEnvironment
import com.willowtree.vocable.utils.IFaceTrackingPermissions
import org.koin.dsl.module

val vocableTestModule = module {
    single { getInMemoryVocableDatabase() }
    single<VocableEnvironment> { VocableTestEnvironment() }
    single { MockFaceTrackingManager() }
    single<IFaceTrackingPermissions> { MockFaceTrackingPermissions() }
}
