package com.willowtree.vocable.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Category::class, Phrase::class], version = 1)
abstract class VocableDatabase : RoomDatabase() {

    companion object {
        private var vocableDatabase: VocableDatabase? = null

        fun getVocableDatabase(context: Context): VocableDatabase {
            if (vocableDatabase == null) {
                vocableDatabase =
                    Room.databaseBuilder(context, VocableDatabase::class.java, "VocableDatabase")
                        .build()
            }
            return vocableDatabase as VocableDatabase
        }
    }

    abstract fun categoryDao(): CategoryDao

    abstract fun phraseDao(): PhraseDao
}