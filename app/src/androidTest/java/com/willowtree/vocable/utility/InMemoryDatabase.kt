package com.willowtree.vocable.utility

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.willowtree.vocable.room.VocableDatabase
import com.willowtree.vocable.room.addVocableMigrations
import org.koin.dsl.module

fun VocableDatabase.Companion.getInMemoryVocableDatabase() = Room
    .inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        VocableDatabase::class.java
    )
    .addVocableMigrations()
    .build()

val inMemoryDatabaseModule = module {
    single { VocableDatabase.getInMemoryVocableDatabase() }
}