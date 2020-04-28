package com.willowtree.vocable.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Category::class, Phrase::class, CategoryPhraseCrossRef::class], version = 3)
@TypeConverters(Converters::class)
abstract class VocableDatabase : RoomDatabase() {

    companion object {
        private var vocableDatabase: VocableDatabase? = null
        private const val DATABASE_NAME = "VocableDatabase"

        fun getVocableDatabase(context: Context): VocableDatabase {
            if (vocableDatabase == null) {
                vocableDatabase =
                    Room.databaseBuilder(context, VocableDatabase::class.java, DATABASE_NAME)
                        .addMigrations(
                            VocableDatabaseMigrations.MIGRATION_1_2,
                            VocableDatabaseMigrations.MIGRATION_2_3
                        )
                        .build()
            }
            return vocableDatabase as VocableDatabase
        }
    }

    abstract fun categoryDao(): CategoryDao

    abstract fun phraseDao(): PhraseDao

    abstract fun categoryPhraseCrossRefDao(): CategoryPhraseCrossRefDao
}

