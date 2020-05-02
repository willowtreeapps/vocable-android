package com.willowtree.vocable.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.willowtree.vocable.utils.VocableSharedPreferences

object VocableDatabaseMigrations {

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Phrases no longer have to have unique utterances
            database.execSQL("DROP INDEX index_Phrase_utterance")
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        // Moving to new JSON schema
        override fun migrate(database: SupportSQLiteDatabase) {
            //Create Category-Phrase relation
            database.execSQL("CREATE TABLE CategoryPhraseCrossRef (category_id TEXT NOT NULL, phrase_id TEXT NOT NULL, PRIMARY KEY(category_id, phrase_id))")

            // Create new Category and Phrase tables
            database.execSQL("CREATE TABLE Category_New (category_id TEXT NOT NULL, creation_date INTEGER NOT NULL, is_user_generated INTEGER NOT NULL, localized_name TEXT NOT NULL, hidden INTEGER NOT NULL, sort_order INTEGER NOT NULL, PRIMARY KEY(category_id))")
            database.execSQL("CREATE TABLE Phrase_New (phrase_id TEXT NOT NULL, creation_date INTEGER NOT NULL, is_user_generated INTEGER NOT NULL, last_spoken_date INTEGER NOT NULL, localized_utterance TEXT NOT NULL, sort_order INTEGER NOT NULL, PRIMARY KEY(phrase_id))")

            // Get id of My Sayings category
            val categoryCursor =
                database.query("SELECT identifier FROM Category WHERE name = 'My Sayings'")
            var categoryId = -1L
            while (categoryCursor.moveToNext()) {
                categoryId = categoryCursor.getLong(categoryCursor.getColumnIndex("identifier"))
            }
            categoryCursor.close()

            // Get My Sayings
            val phraseCursor =
                database.query("SELECT utterance FROM Phrase WHERE category_id = $categoryId ORDER BY creation_date ASC")
            val mySayings = LinkedHashSet<String>()
            while (phraseCursor.moveToNext()) {
                val saying = phraseCursor.getString(phraseCursor.getColumnIndex("utterance"))
                mySayings.add(saying)
            }
            phraseCursor.close()

            // Save My Sayings to Shared Prefs
            VocableSharedPreferences().setMySayings(mySayings)

            // Delete old tables and rename new ones to match old names
            database.execSQL("DROP TABLE Category")
            database.execSQL("ALTER TABLE Category_New RENAME TO Category")

            database.execSQL("DROP TABLE Phrase")
            database.execSQL("ALTER TABLE Phrase_New RENAME TO Phrase")
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        // Moving to new JSON schema
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create new Category, Phrase, & Cross Ref tables
            database.execSQL("CREATE TABLE Category_New (category_id TEXT NOT NULL, creation_date INTEGER NOT NULL, is_user_generated INTEGER NOT NULL, resource_id INTEGER, localized_name TEXT, hidden INTEGER NOT NULL, sort_order INTEGER NOT NULL, PRIMARY KEY(category_id))")
            database.execSQL("CREATE TABLE Phrase_New (phrase_id TEXT NOT NULL, creation_date INTEGER NOT NULL, is_user_generated INTEGER NOT NULL, last_spoken_date INTEGER NOT NULL, resource_id INTEGER, localized_utterance TEXT, sort_order INTEGER NOT NULL, PRIMARY KEY(phrase_id))")
            database.execSQL("CREATE TABLE CategoryPhraseCrossRef_New (category_id TEXT NOT NULL, phrase_id TEXT NOT NULL, PRIMARY KEY(category_id, phrase_id))")

            // Get id of My Sayings category
            val mySayingsMap = HashMap<String, String>()
            mySayingsMap["en"] = "My Sayings"
            val jsonName = Converters.stringMapToJson(mySayingsMap)
            val categoryCursor =
                database.query("SELECT category_id FROM Category WHERE localized_name = '$jsonName'")
            var categoryId = ""
            while (categoryCursor.moveToNext()) {
                categoryId = categoryCursor.getString(categoryCursor.getColumnIndex("category_id"))
            }
            categoryCursor.close()

            // Get My Sayings
            val crossRefCursor =
                database.query("SELECT phrase_id FROM CategoryPhraseCrossRef WHERE category_id = '$categoryId'")
            val myLocalizedSayings = LinkedHashSet<String>()
            val phraseIds =  mutableListOf<String>()
            while (crossRefCursor.moveToNext()) {
                val phraseId = crossRefCursor.getString(crossRefCursor.getColumnIndex("phrase_id"))
                phraseIds.add(phraseId)
            }
            crossRefCursor.close()

            phraseIds.forEach {
                val phraseCursor = database.query("SELECT phrase_id FROM Phrase WHERE phrase_id = '$it'")
                while (phraseCursor.moveToNext()) {
                    val saying = phraseCursor.getString(phraseCursor.getColumnIndex("localized_utterance"))
                    myLocalizedSayings.add(saying)
                }
                phraseCursor.close()
            }

            // If we didn't pick up any sayings from scheme 3, check if there were some in
            // scheme 2
            val nonLocalizedSayings = VocableSharedPreferences().getMySayings()
            if (myLocalizedSayings.isEmpty() && nonLocalizedSayings.isNotEmpty()) {
                nonLocalizedSayings.forEach {
                    val map = HashMap<String, String>()
                    map["en"] = it
                    myLocalizedSayings.add(Converters.stringMapToJson(map))
                }

            }

            VocableSharedPreferences().setMyLocalizedSayings(myLocalizedSayings)

            // Delete old tables and rename new ones to match old names
            database.execSQL("DROP TABLE Category")
            database.execSQL("ALTER TABLE Category_New RENAME TO Category")

            database.execSQL("DROP TABLE Phrase")
            database.execSQL("ALTER TABLE Phrase_New RENAME TO Phrase")

            database.execSQL("DROP TABLE CategoryPhraseCrossRef")
            database.execSQL("ALTER TABLE CategoryPhraseCrossRef_New RENAME TO CategoryPhraseCrossRef")
        }
    }
}