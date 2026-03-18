package com.willowtree.vocable

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.basetest.utils.FakeLocaleProvider
import com.willowtree.vocable.core.locale.LocalesWithText
import com.willowtree.vocable.data.repository.RoomPresetCategoriesRepository
import com.willowtree.vocable.data.repository.RoomPresetPhrasesRepository
import com.willowtree.vocable.data.repository.RoomStoredCategoriesRepository
import com.willowtree.vocable.data.repository.RoomStoredPhrasesRepository
import com.willowtree.vocable.data.room.VocableDatabase
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.domain.model.PresetCategories
import com.willowtree.vocable.domain.usecase.CategoriesUseCase
import com.willowtree.vocable.domain.usecase.PhrasesUseCase
import com.willowtree.vocable.utility.FakeDateProvider
import com.willowtree.vocable.utility.VocableKoinTestRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoriesUseCaseTest {

    @get:Rule
    val vocableKoinTestRule = VocableKoinTestRule()

    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        VocableDatabase::class.java
    ).build()

    private val dateProvider = FakeDateProvider()
    private val presetCategoriesRepository = RoomPresetCategoriesRepository(database)
    private val storedCategoriesRepository = RoomStoredCategoriesRepository(database)
    private val presetPhrasesRepository = RoomPresetPhrasesRepository(
        database.presetPhrasesDao(),
        dateProvider
    )
    private val storedPhrasesRepository = RoomStoredPhrasesRepository(
        database,
        dateProvider
    )

    private fun createUseCase(): CategoriesUseCase {
        return CategoriesUseCase(
            FakeUUIDProvider(),
            FakeLocaleProvider(),
            storedCategoriesRepository,
            presetCategoriesRepository,
            PhrasesUseCase(
                storedPhrasesRepository,
                presetPhrasesRepository,
                dateProvider,
                FakeUUIDProvider(),
                FakeLocaleProvider()
            )
        )
    }

    @Test
    fun preset_and_stored_categories_returned() = runTest {
        val useCase = createUseCase()
        useCase.addCategory("storedCategory1")
        useCase.addCategory("storedCategory2")

        val categories = useCase.categories().first()
        assertEquals(9, categories.size)
        assertEquals("storedCategory1", (categories[7] as Category.StoredCategory).localizedName["en_US"])
        assertEquals("storedCategory2", (categories[8] as Category.StoredCategory).localizedName["en_US"])
    }

    @Test
    fun category_stored() = runTest {
        val useCase = createUseCase()

        useCase.addCategory("My Category")

        val categories = useCase.categories().first()
        val stored = categories.last() as Category.StoredCategory
        assertEquals("My Category", stored.localizedName["en_US"])
        assertEquals(stored, useCase.getCategoryById(stored.categoryId))
    }

    @Test
    fun category_moved_up() = runTest {
        val useCase = createUseCase()
        useCase.addCategory("storedCategory")
        val category = useCase.categories().first().first { it is Category.StoredCategory }

        useCase.moveCategoryUp(category.categoryId)

        val categories = useCase.categories().first()
        assertEquals(category.categoryId, categories[6].categoryId)
        assertEquals(PresetCategories.RECENTS.id, categories[7].categoryId)
    }

    @Test
    fun category_moved_down() = runTest {
        val useCase = createUseCase()
        useCase.moveCategoryDown(PresetCategories.GENERAL.id)

        val categories = useCase.categories().first()
        assertEquals(PresetCategories.BASIC_NEEDS.id, categories[0].categoryId)
        assertEquals(PresetCategories.GENERAL.id, categories[1].categoryId)
    }

    @Test
    fun updating_category_name_preserves_other_locale_values() = runTest {
        val useCase = createUseCase()
        useCase.addCategory("Old")
        val storedCategory = useCase.categories().first().last() as Category.StoredCategory
        useCase.updateCategoryName(
            categoryId = storedCategory.categoryId,
            localizedName = LocalesWithText(mapOf("en_US" to "New", "es_US" to "Viejo"))
        )

        val updated = useCase.getCategoryById(storedCategory.categoryId) as Category.StoredCategory
        assertEquals("New", updated.localizedName["en_US"])
        assertEquals("Viejo", updated.localizedName["es_US"])
    }

    @Test
    fun hiding_category_updates_hidden_flag() = runTest {
        val useCase = createUseCase()

        useCase.updateCategoryHidden(PresetCategories.GENERAL.id, true)

        assertEquals(true, useCase.getCategoryById(PresetCategories.GENERAL.id).hidden)
    }

    @Test
    fun deleting_stored_category_removes_it() = runTest {
        val useCase = createUseCase()
        useCase.addCategory("Delete me")
        val storedCategory = useCase.categories().first().last()

        useCase.deleteCategory(storedCategory.categoryId)

        assertEquals(7, useCase.categories().first().size)
    }
}
