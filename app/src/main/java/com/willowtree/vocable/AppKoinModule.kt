package com.willowtree.vocable

import com.willowtree.vocable.room.PhraseRepository
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.dsl.module

object AppKoinModule {

    fun getModule() = module {
        single { VocableSharedPreferences() }
        single { PhraseRepository(get()) }
    }
}