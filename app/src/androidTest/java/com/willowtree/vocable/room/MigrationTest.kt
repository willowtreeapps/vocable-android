package com.willowtree.vocable.room

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

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
}