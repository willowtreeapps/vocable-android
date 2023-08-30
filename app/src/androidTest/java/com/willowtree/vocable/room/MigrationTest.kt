/**

These tests confirm that users can successfully upgrade from older versions of Vocable to newer versions.
All new migrations should have a test confirming the migration.

**/

package com.willowtree.vocable.room

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "migration-test"
    }

    private val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VocableDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        val mySayingsCategoryNameV2 = "My Sayings"
        val mySayingTestPhraseV2 = "Test Phrase"

        helper.createDatabase(TEST_DB, 2).apply {
            // Create mock My Sayings category
            val catId = System.currentTimeMillis()
            execSQL("INSERT INTO Category (identifier, creation_date, is_user_generated, name) VALUES ($catId, ${System.currentTimeMillis()}, 0, '$mySayingsCategoryNameV2')")

            // Add a custom phrase to My Sayings category
            val phraseId = System.currentTimeMillis()
            execSQL("INSERT INTO Phrase (identifier, creation_date, is_user_generated, last_spoken_date, utterance, category_id) VALUES ($phraseId, ${System.currentTimeMillis()}, 0, ${System.currentTimeMillis()}, '$mySayingTestPhraseV2', $catId)")

            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, VocableDatabaseMigrations.MIGRATION_2_3)
            .apply {
                // Verify that V2 custom phrase was saved in SharedPreferences
                val sharedPrefs = VocableSharedPreferences()
                val v2MySayings = sharedPrefs.getMySayings()
                Assert.assertEquals(1, v2MySayings.size)
                Assert.assertEquals(mySayingTestPhraseV2, v2MySayings.first())

                // Verify that new schema is as expected
                val categoryIdV3 = "Category_V3"
                val categoryNameV3 = "Category_Name_V3"
                execSQL("INSERT INTO Category (category_id, creation_date, is_user_generated, localized_name, hidden, sort_order) VALUES ('$categoryIdV3', ${System.currentTimeMillis()}, 0, '$categoryNameV3', 0, 0)")

                val phraseIdV3 = "Phrase_V3"
                val phraseUtteranceV3 = "Phrase_Utterance_V3"
                execSQL("INSERT INTO Phrase (phrase_id, creation_date, is_user_generated, last_spoken_date, localized_utterance, sort_order) VALUES ('$phraseIdV3', ${System.currentTimeMillis()}, 0, ${System.currentTimeMillis()}, '$phraseUtteranceV3', 0)")

                close()
            }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3to4() {
        val categoryNameMap = HashMap<String, String>()
        categoryNameMap["en_US"] = "My Sayings"

        val phraseOneMap = HashMap<String, String>()
        phraseOneMap["en_US"] = "Saying 1"

        val phraseTwoMap = HashMap<String, String>()
        phraseTwoMap["en_US"] = "Saying 2"

        val mySayingsCategoryNameV3 = Converters.stringMapToJson(categoryNameMap)
        val mySayingTestPhraseOneV3 = Converters.stringMapToJson(phraseOneMap)
        val mySayingTestPhraseTwoV3 = Converters.stringMapToJson(phraseTwoMap)

        val phraseOneId = UUID.randomUUID().toString()
        val creationDateOne = System.currentTimeMillis()

        val phraseTwoId = UUID.randomUUID().toString()
        val creationDateTwo = System.currentTimeMillis()

        helper.createDatabase(TEST_DB, 3).apply {
            // Create mock My Sayings category
            execSQL("INSERT INTO Category (category_id, creation_date, is_user_generated, localized_name, hidden, sort_order) VALUES ('${PresetCategories.MY_SAYINGS.id}', ${System.currentTimeMillis()}, 0, '$mySayingsCategoryNameV3', 0, ${PresetCategories.MY_SAYINGS.initialSortOrder})")

            // Add custom phrases to My Sayings category
            execSQL("INSERT INTO Phrase (phrase_id, creation_date, is_user_generated, last_spoken_date, localized_utterance, sort_order) VALUES ('$phraseOneId', $creationDateOne, 1, $creationDateOne, '$mySayingTestPhraseOneV3', 0)")
            execSQL("INSERT INTO CategoryPhraseCrossRef (category_id, phrase_id) VALUES ('${PresetCategories.MY_SAYINGS.id}', '$phraseOneId')")

            execSQL("INSERT INTO Phrase (phrase_id, creation_date, is_user_generated, last_spoken_date, localized_utterance, sort_order) VALUES ('$phraseTwoId', $creationDateTwo, 1, $creationDateTwo, '$mySayingTestPhraseTwoV3', 1)")
            execSQL("INSERT INTO CategoryPhraseCrossRef (category_id, phrase_id) VALUES ('${PresetCategories.MY_SAYINGS.id}', '$phraseTwoId')")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 4, true, VocableDatabaseMigrations.MIGRATION_3_4)
            .apply {
                // Verify that new schema is as expected
                val categoryId = PresetCategories.MY_SAYINGS.id
                val crossRefCursor =
                    query("SELECT phrase_id FROM CategoryPhraseCrossRef WHERE category_id = '$categoryId'")
                val myLocalizedSayings = arrayListOf<String>()
                val phraseIds =  mutableListOf<String>()
                while (crossRefCursor.moveToNext()) {
                    val phraseId = crossRefCursor.getString(crossRefCursor.getColumnIndex("phrase_id"))
                    phraseIds.add(phraseId)
                }
                crossRefCursor.close()

                phraseIds.forEach {
                    val phraseCursor = query("SELECT localized_utterance FROM Phrase WHERE phrase_id = '$it'")
                    while (phraseCursor.moveToNext()) {
                        val saying = phraseCursor.getString(phraseCursor.getColumnIndex("localized_utterance"))
                        myLocalizedSayings.add(saying)
                    }
                    phraseCursor.close()
                }
                Assert.assertTrue(myLocalizedSayings.contains(mySayingTestPhraseOneV3))
                Assert.assertTrue(myLocalizedSayings.contains(mySayingTestPhraseTwoV3))
                close()
            }
    }

    @Test
    @Throws(IOException::class)
    fun migrateSharedPreferenceSayingsDuring3to4() {
        val categoryNameMap = HashMap<String, String>()
        categoryNameMap["en_US"] = "My Sayings"

        val mySayingsCategoryNameV3 = Converters.stringMapToJson(categoryNameMap)

        val mySayingLegacyPhrase = "Legacy Phrase"
        val legacyMyPhrases = LinkedHashSet<String>()
        legacyMyPhrases.add(mySayingLegacyPhrase)
        VocableSharedPreferences().setMySayings(legacyMyPhrases)

        helper.createDatabase(TEST_DB, 3).apply {
            // Create mock My Sayings category
            execSQL("INSERT INTO Category (category_id, creation_date, is_user_generated, localized_name, hidden, sort_order) VALUES ('${PresetCategories.MY_SAYINGS.id}', ${System.currentTimeMillis()}, 0, '$mySayingsCategoryNameV3', 0, ${PresetCategories.MY_SAYINGS.initialSortOrder})")

            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 4, true, VocableDatabaseMigrations.MIGRATION_3_4)
            .apply {
                // Verify that new schema is as expected
                val categoryId = PresetCategories.MY_SAYINGS.id
                val crossRefCursor =
                    query("SELECT phrase_id FROM CategoryPhraseCrossRef WHERE category_id = '$categoryId'")
                val myLocalizedSayings = arrayListOf<String>()
                val phraseIds =  mutableListOf<String>()
                while (crossRefCursor.moveToNext()) {
                    val phraseId = crossRefCursor.getString(crossRefCursor.getColumnIndex("phrase_id"))
                    phraseIds.add(phraseId)
                }
                crossRefCursor.close()

                phraseIds.forEach {
                    val phraseCursor = query("SELECT localized_utterance FROM Phrase WHERE phrase_id = '$it'")
                    while (phraseCursor.moveToNext()) {
                        val saying = phraseCursor.getString(phraseCursor.getColumnIndex("localized_utterance"))
                        myLocalizedSayings.add(saying)
                    }
                    phraseCursor.close()
                }
                val hashMap = HashMap<String, String>()
                hashMap["en_US"] = mySayingLegacyPhrase
                val localizedLegacyPhrase = Converters.stringMapToJson(hashMap)
                Assert.assertTrue(myLocalizedSayings.contains(localizedLegacyPhrase))
                close()
            }
    }

    @Test
    @Throws(IOException::class)
    fun migrate4to5() {
        // This migration adds a timestamp to phrases.

        val categoryNameMap = HashMap<String, String>()
        categoryNameMap["en_US"] = "My Sayings"

        val phraseOneMap = HashMap<String, String>()
        phraseOneMap["en_US"] = "Saying 1"

        val categoryNameV4 = Converters.stringMapToJson(categoryNameMap)
        val testPhraseV4 = Converters.stringMapToJson(phraseOneMap)

        val phraseId = UUID.randomUUID().toString()
        val creationDate = System.currentTimeMillis()

        helper.createDatabase(TEST_DB, 4).apply {
            // Create mock My Sayings category. This replicates a user who's added phrases before Custom Categories was a thing.
            execSQL("INSERT INTO Category (category_id, creation_date, is_user_generated, localized_name, hidden, sort_order) VALUES ('${PresetCategories.MY_SAYINGS.id}', ${System.currentTimeMillis()}, 0, '$categoryNameV4', 0, ${PresetCategories.MY_SAYINGS.initialSortOrder})")

            // Add custom phrase to the category you just created.
            execSQL("INSERT INTO Phrase (phrase_id, creation_date, is_user_generated, last_spoken_date, localized_utterance, sort_order) VALUES ('$phraseId', $creationDate, 1, $creationDate, '$testPhraseV4', 0)")
            execSQL("INSERT INTO CategoryPhraseCrossRef (category_id, phrase_id) VALUES ('${PresetCategories.MY_SAYINGS.id}', '$phraseId')")

            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 5, true, VocableDatabaseMigrations.MIGRATION_4_5)
            .apply {
                // Verify that new schema is as expected.
                val categoryId = PresetCategories.MY_SAYINGS.id
                val crossRefCursor =
                    query("SELECT * FROM CategoryPhraseCrossRef WHERE category_id = '$categoryId'")
                val phrasesAdded = arrayListOf<String>()
                val phraseIds =  mutableListOf<String>()
                var timestamp: Long? = null
                while (crossRefCursor.moveToNext()) {
                    val phraseId = crossRefCursor.getString(crossRefCursor.getColumnIndex("phrase_id"))
                    timestamp = crossRefCursor.getLong(crossRefCursor.getColumnIndex("timestamp"))
                    phraseIds.add(phraseId)
                }
                crossRefCursor.close()

                    val phraseCursor = query("SELECT localized_utterance FROM Phrase WHERE phrase_id = '$phraseId'")
                    while (phraseCursor.moveToNext()) {
                        val saying = phraseCursor.getString(phraseCursor.getColumnIndex("localized_utterance"))
                        phrasesAdded.add(saying)
                    }
                    phraseCursor.close()

                Assert.assertTrue(phrasesAdded.contains(testPhraseV4))
                Assert.assertEquals(0L, timestamp)
               close()
            }
    }

    @Test
    fun migrate5to6() = runTest {
        // Migration from a many-to-many relationship of categories and phrases
        // to a one-to-many relationship of categories to phrases

        helper.createDatabase(TEST_DB, 5).use {
            //Create tables
            it.execSQL(
                "CREATE TABLE IF NOT EXISTS `Category` " +
                        "(`category_id` TEXT NOT NULL, `creation_date` INTEGER NOT NULL, `is_user_generated` INTEGER NOT NULL, " +
                        "`resource_id` INTEGER, `localized_name` TEXT, `hidden` INTEGER NOT NULL, `sort_order` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`category_id`))"
            )

            it.execSQL(
                "CREATE TABLE IF NOT EXISTS `Phrase` (`phrase_id` TEXT NOT NULL, `creation_date` INTEGER NOT NULL, " +
                        "`is_user_generated` INTEGER NOT NULL, `last_spoken_date` INTEGER NOT NULL, `resource_id` INTEGER, " +
                        "`localized_utterance` TEXT, `sort_order` INTEGER NOT NULL, PRIMARY KEY(`phrase_id`))"
            )

            it.execSQL(
                "CREATE TABLE IF NOT EXISTS `CategoryPhraseCrossRef` (`category_id` TEXT NOT NULL, `phrase_id` TEXT NOT NULL, " +
                        "`timestamp` INTEGER, PRIMARY KEY(`category_id`, `phrase_id`))"
            )

            // Add data
            it.execSQL(
                "INSERT INTO Category " +
                        "(category_id, creation_date, is_user_generated, localized_name, hidden, sort_order) VALUES " +
                        "('custom', 0, 1, '{\"english\":\"custom\"}', 0, 7)"
            )
            it.execSQL(
                "INSERT INTO Category " +
                        "(category_id, creation_date, is_user_generated, localized_name, hidden, sort_order) VALUES " +
                        "('recents', 0, 0, '{\"english\":\"recents\"}', 0, 8)"
            )

            it.execSQL(
                "INSERT INTO Phrase (phrase_id, creation_date, is_user_generated, last_spoken_date, localized_utterance, sort_order) VALUES " +
                        "('1', 0, 1, 0, '{\"english\":\"hi\"}', 0)"
            )
            it.execSQL(
                "INSERT INTO CategoryPhraseCrossRef (category_id, phrase_id) VALUES " +
                        "('custom', '1')"
            )

            // Add additional reference for a phrase included in multiple categories ie, how recents previously worked
            it.execSQL(
                "INSERT INTO CategoryPhraseCrossRef (category_id, phrase_id) VALUES " +
                        "('recents', '1')"
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 6, true, VocableDatabaseMigrations.MIGRATION_5_6)

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VocableDatabase::class.java,
            TEST_DB
        ).build()

        val categories = db.categoryDao().getAllCategories()
        assertEquals(
            listOf(
                Category(
                    "custom",
                    0L,
                    null,
                    mapOf("english" to "custom"),
                    false,
                    7
                ),
                Category(
                    "recents",
                    0L,
                    null,
                    mapOf("english" to "recents"),
                    false,
                    8
                )
            ), categories
        )

        val customPhrases = db.categoryDao().getCategoryWithPhrases("custom")?.phrases
        assertEquals(
            listOf(
                Phrase(
                    1L,
                    "custom",
                    0L,
                    0L,
                    mapOf("english" to "hi"),
                    0
                )
            ), customPhrases
        )
    }

}