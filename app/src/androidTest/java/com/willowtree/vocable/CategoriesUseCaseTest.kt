package com.willowtree.vocable

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.LegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.RoomPresetCategoriesRepository
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.room.RoomStoredCategoriesRepository
import com.willowtree.vocable.room.VocableDatabase
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoriesUseCaseTest {

    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        VocableDatabase::class.java
    ).build()

    private val presetCategoriesRepository = RoomPresetCategoriesRepository(
        database
    )

    private val storedCategoriesRepository = RoomStoredCategoriesRepository(
        database
    )

    private val legacyCategoriesAndPhrasesRepository = LegacyCategoriesAndPhrasesRepository(
        ApplicationProvider.getApplicationContext(),
        database
    )

    private fun createUseCase(): CategoriesUseCase {
        return CategoriesUseCase(
            legacyCategoriesAndPhrasesRepository,
            ConstantUUIDProvider(),
            FakeLocaleProvider(),
            storedCategoriesRepository,
            presetCategoriesRepository
        )
    }

    @Test
    fun preset_and_stored_categories_returned() = runTest {
        storedCategoriesRepository.upsertCategory(
            Category.StoredCategory(
                "storedCategory1",
                localizedName = LocalesWithText(mapOf("en_US" to "storedCategory1")),
                hidden = false,
                sortOrder = 0
            )
        )
        storedCategoriesRepository.upsertCategory(
            Category.StoredCategory(
                "storedCategory2",
                localizedName = LocalesWithText(mapOf("en_US" to "storedCategory2")),
                hidden = false,
                sortOrder = 0
            )
        )

        val useCase = createUseCase()

        assertEquals(
            listOf(
                Category.StoredCategory(
                    categoryId = "storedCategory1",
                    localizedName = LocalesWithText(mapOf("en_US" to "storedCategory1")),
                    hidden = false,
                    sortOrder = 0
                ),
                Category.StoredCategory(
                    categoryId = "storedCategory2",
                    localizedName = LocalesWithText(mapOf("en_US" to "storedCategory2")),
                    hidden = false,
                    sortOrder = 0
                ),
                *presetCategoriesRepository.getPresetCategories().first().toTypedArray()
            ),
            useCase.categories().first()
        )
    }

    @Test
    fun category_stored() = runTest {
        val useCase = createUseCase()

        useCase.addCategory("My Category", 0)

        assertEquals(
            listOf(
                Category.StoredCategory(
                    categoryId = "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "My Category")),
                    hidden = false,
                    sortOrder = 0
                ),
                *presetCategoriesRepository.getPresetCategories().first().toTypedArray()
            ),
            useCase.categories().first()
        )
        assertEquals(
            Category.StoredCategory(
                categoryId = "1",
                localizedName = LocalesWithText(mapOf("en_US" to "My Category")),
                hidden = false,
                sortOrder = 0
            ),
            useCase.getCategoryById("1")
        )
    }

    @Test
    fun sort_order_updated() = runTest {
        storedCategoriesRepository.upsertCategory(
            Category.StoredCategory(
                "storedCategory",
                localizedName = null,
                hidden = false,
                sortOrder = 7 // PresetCategories take up the first 6
            )
        )

        val useCase = createUseCase()

        useCase.updateCategorySortOrders(
            listOf(
                CategorySortOrder("storedCategory", 6),
                CategorySortOrder(PresetCategories.RECENTS.id, 7)
            )
        )

        assertEquals(
            listOf(
                Category.StoredCategory(
                    "storedCategory",
                    localizedName = null,
                    hidden = false,
                    sortOrder = 6
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.GENERAL.id,
                    sortOrder = 0,
                    hidden = false,
                    resourceId = PresetCategories.GENERAL.getNameId()
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.BASIC_NEEDS.id,
                    sortOrder = 1,
                    hidden = false,
                    resourceId = PresetCategories.BASIC_NEEDS.getNameId()
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.PERSONAL_CARE.id,
                    sortOrder = 2,
                    hidden = false,
                    resourceId = PresetCategories.PERSONAL_CARE.getNameId()
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.CONVERSATION.id,
                    sortOrder = 3,
                    hidden = false,
                    resourceId = PresetCategories.CONVERSATION.getNameId()
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.ENVIRONMENT.id,
                    sortOrder = 4,
                    hidden = false,
                    resourceId = PresetCategories.ENVIRONMENT.getNameId()
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.USER_KEYPAD.id,
                    sortOrder = 5,
                    hidden = false,
                    resourceId = PresetCategories.USER_KEYPAD.getNameId()
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.RECENTS.id,
                    sortOrder = 7,
                    hidden = false,
                    resourceId = PresetCategories.RECENTS.getNameId()
                )
            ),
            useCase.categories().first()
        )
    }

    @Test
    fun get_category_by_id_returns_presets_and_stored() = runTest {
        storedCategoriesRepository.upsertCategory(
            Category.StoredCategory(
                "storedCategory",
                localizedName = null,
                hidden = false,
                sortOrder = 0
            )
        )

        val useCase = createUseCase()

        assertEquals(
            Category.StoredCategory(
                "storedCategory",
                localizedName = null,
                hidden = false,
                sortOrder = 0
            ),
            useCase.getCategoryById("storedCategory")
        )
        assertEquals(
            Category.PresetCategory(
                PresetCategories.BASIC_NEEDS.id,
                sortOrder = 1,
                hidden = false,
                resourceId = R.string.preset_basic_needs
            ),
            useCase.getCategoryById(PresetCategories.BASIC_NEEDS.id)
        )
    }

    @Test
    fun update_category_name_updates_stored_category() = runTest {
        storedCategoriesRepository.upsertCategory(
            Category.StoredCategory(
                "storedCategory1",
                localizedName = LocalesWithText(mapOf("en_US" to "storedCategory1")),
                hidden = false,
                sortOrder = 0
            )
        )

        val useCase = createUseCase()

        useCase.updateCategoryName(
            "storedCategory1",
            LocalesWithText(mapOf("en_US" to "newStoredCategory1"))
        )

        assertEquals(
            Category.StoredCategory(
                "storedCategory1",
                localizedName = LocalesWithText(mapOf("en_US" to "newStoredCategory1")),
                hidden = false,
                sortOrder = 0
            ),
            useCase.getCategoryById("storedCategory1")
        )
    }

    @Test
    fun update_category_name_hides_preset_category_and_creates_stored_category_with_new_name() =
        runTest {
            val useCase = createUseCase()

            useCase.updateCategoryName(
                PresetCategories.BASIC_NEEDS.id,
                LocalesWithText(mapOf("en_US" to "newPresetCategory1"))
            )

            assertEquals(
                Category.StoredCategory(
                    PresetCategories.BASIC_NEEDS.id,
                    sortOrder = 1,
                    hidden = false,
                    localizedName = LocalesWithText(mapOf("en_US" to "newPresetCategory1")),
                ),
                useCase.getCategoryById(PresetCategories.BASIC_NEEDS.id)
            )
        }

    @Test
    fun update_category_hidden_updates_stored_category() = runTest {
        storedCategoriesRepository.upsertCategory(
            Category.StoredCategory(
                "storedCategory1",
                localizedName = LocalesWithText(mapOf("en_US" to "storedCategory1")),
                hidden = false,
                sortOrder = 0
            )
        )

        val useCase = createUseCase()

        useCase.updateCategoryHidden("storedCategory1", true)

        assertEquals(
            Category.StoredCategory(
                "storedCategory1",
                localizedName = LocalesWithText(mapOf("en_US" to "storedCategory1")),
                hidden = true,
                sortOrder = 0
            ),
            useCase.getCategoryById("storedCategory1")
        )
    }

    @Test
    fun update_category_hidden_updates_preset_category() = runTest {
        val useCase = createUseCase()

        useCase.updateCategoryHidden(PresetCategories.BASIC_NEEDS.id, true)

        assertEquals(
            Category.PresetCategory(
                PresetCategories.BASIC_NEEDS.id,
                sortOrder = 1,
                hidden = true,
                resourceId = R.string.preset_basic_needs
            ),
            useCase.getCategoryById(PresetCategories.BASIC_NEEDS.id)
        )
    }
}