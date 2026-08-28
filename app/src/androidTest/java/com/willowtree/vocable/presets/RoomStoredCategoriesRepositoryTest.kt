package com.willowtree.vocable.presets

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.core.locale.LocalesWithText
import com.willowtree.vocable.data.repository.RoomStoredCategoriesRepository
import com.willowtree.vocable.data.room.VocableDatabase
import com.willowtree.vocable.utility.VocableKoinTestRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomStoredCategoriesRepositoryTest {

    @get:Rule
    val vocableKoinTestRule = VocableKoinTestRule()

    private fun createDatabase(): VocableDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VocableDatabase::class.java
        ).build()

    @Test
    fun getAllCategories_treatsLiteralNullLocalizedNameAsEmpty() = runTest {
        // MIGRATION_5_6 has a string-interpolation bug that writes the literal text "null"
        // (not SQL NULL) into localized_name for some legacy rows. Converters.moshi parses that
        // as a JSON null, which the DAO must not surface as an actual null for a non-null field.
        val database = createDatabase()
        val repository = RoomStoredCategoriesRepository(database)
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO Category (category_id, creation_date, localized_name, hidden, sort_order) " +
                "VALUES ('legacy_literal_null', 0, 'null', 0, 0)"
        )

        val categories = repository.getAllCategories().first()

        assertEquals(LocalesWithText(emptyMap()), categories.single().localizedName)
    }
}
