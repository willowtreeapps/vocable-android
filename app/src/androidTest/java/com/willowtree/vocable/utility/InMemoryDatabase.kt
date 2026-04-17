package com.willowtree.vocable.utility

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.willowtree.vocable.data.room.VocableDatabase
import com.willowtree.vocable.data.room.addVocableMigrations

fun getInMemoryVocableDatabase() = Room
    .inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        VocableDatabase::class.java
    )
    .addVocableMigrations()
    .build()
