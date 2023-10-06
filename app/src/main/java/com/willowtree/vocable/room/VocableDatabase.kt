package com.willowtree.vocable.room

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CategoryDto::class, PhraseDto::class, PresetCategoryDto::class],
    version = 7,
    // TODO: PK - May be able to consolidate 6 and 7 since we never released 6
    autoMigrations = [AutoMigration(from = 6, to = 7)]
)
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
                            VocableDatabaseMigrations.MIGRATION_2_3,
                            VocableDatabaseMigrations.MIGRATION_3_4,
                            VocableDatabaseMigrations.MIGRATION_4_5,
                            VocableDatabaseMigrations.MIGRATION_5_6
                        )
                        .build()
            }
            return vocableDatabase as VocableDatabase
        }
    }

    abstract fun categoryDao(): CategoryDao

    abstract fun phraseDao(): PhraseDao

    abstract fun presetCategoryDao(): PresetCategoryDao
}

