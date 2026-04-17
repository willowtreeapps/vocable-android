package com.willowtree.vocable.utility

import com.willowtree.vocable.core.VocableEnvironment
import org.koin.dsl.module


val vocableTestModule = module {
    single { getInMemoryVocableDatabase() }
    single<VocableEnvironment> { VocableTestEnvironment() }
}
