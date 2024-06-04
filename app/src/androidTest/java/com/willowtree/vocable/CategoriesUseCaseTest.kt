package com.willowtree.vocable

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.basetest.utils.FakeLocaleProvider
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetPhrase
import com.willowtree.vocable.presets.RoomPresetCategoriesRepository
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.room.RoomPresetPhrasesRepository
import com.willowtree.vocable.room.RoomStoredCategoriesRepository
import com.willowtree.vocable.room.RoomStoredPhrasesRepository
import com.willowtree.vocable.room.VocableDatabase
import com.willowtree.vocable.utility.FakeDateProvider
import com.willowtree.vocable.utility.StubLegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.utility.VocableKoinTestRule
import com.willowtree.vocable.utils.locale.LocalesWithText
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

    private val presetCategoriesRepository = RoomPresetCategoriesRepository(
        database
    )

    private val storedCategoriesRepository = RoomStoredCategoriesRepository(
        database
    )

    private val presetPhrasesRepository = RoomPresetPhrasesRepository(
        database.presetPhrasesDao(),
        FakeDateProvider()
    )

    private val storedPhrasesRepository = RoomStoredPhrasesRepository(
        database,
        FakeDateProvider()
    )

    private fun createUseCase(): CategoriesUseCase {
        return CategoriesUseCase(
            FakeUUIDProvider(),
            FakeLocaleProvider(),
            storedCategoriesRepository,
            presetCategoriesRepository,
            PhrasesUseCase(
                StubLegacyCategoriesAndPhrasesRepository(),
                storedPhrasesRepository,
                presetPhrasesRepository,
                FakeDateProvider(),
                FakeUUIDProvider()
            )
        )
    }

    @Test
    fun preset_and_stored_categories_returned() = runTest {
        val useCase = createUseCase()
        useCase.addCategory("storedCategory1")
        useCase.addCategory("storedCategory2")

        assertEquals(
            listOf(
                *presetCategoriesRepository.getPresetCategories().first().toTypedArray(),
                Category.StoredCategory(
                    categoryId = "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "storedCategory1")),
                    hidden = false,
                    sortOrder = 7
                ),
                Category.StoredCategory(
                    categoryId = "2",
                    localizedName = LocalesWithText(mapOf("en_US" to "storedCategory2")),
                    hidden = false,
                    sortOrder = 8
                ),
            ),
            useCase.categories().first()
        )
    }

    @Test
    fun category_stored() = runTest {
        val useCase = createUseCase()

        useCase.addCategory("My Category")

        assertEquals(
            listOf(
                *presetCategoriesRepository.getPresetCategories().first().toTypedArray(),
                Category.StoredCategory(
                    categoryId = "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "My Category")),
                    hidden = false,
                    sortOrder = 7
                ),
            ),
            useCase.categories().first()
        )
        assertEquals(
            Category.StoredCategory(
                categoryId = "1",
                localizedName = LocalesWithText(mapOf("en_US" to "My Category")),
                hidden = false,
                sortOrder = 7
            ),
            useCase.getCategoryById("1")
        )
    }

    @Test
    fun category_moved_up() = runTest {
        val useCase = createUseCase()
        useCase.addCategory("storedCategory")
        val category = useCase.categories().first()
            .first { it.text(ApplicationProvider.getApplicationContext()) == "storedCategory" }

        useCase.moveCategoryUp(category.categoryId)

        assertEquals(
            listOf(
                Category.PresetCategory(
                    categoryId = PresetCategories.GENERAL.id,
                    sortOrder = 0,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.BASIC_NEEDS.id,
                    sortOrder = 1,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.PERSONAL_CARE.id,
                    sortOrder = 2,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.CONVERSATION.id,
                    sortOrder = 3,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.ENVIRONMENT.id,
                    sortOrder = 4,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.USER_KEYPAD.id,
                    sortOrder = 5,
                    hidden = false,
                ),
                Category.StoredCategory(
                    "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "storedCategory")),
                    hidden = false,
                    sortOrder = 6
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.RECENTS.id,
                    sortOrder = 7,
                    hidden = false,
                )
            ),
            useCase.categories().first()
        )
    }

    @Test
    fun category_moved_up_hidden_present() = runTest {
        val useCase = createUseCase()
        useCase.addCategory("storedCategory")
        val category = useCase.categories().first()
            .first { it.text(ApplicationProvider.getApplicationContext()) == "storedCategory" }

        useCase.updateCategoryHidden(PresetCategories.RECENTS.id, true)

        useCase.moveCategoryUp(category.categoryId)

        assertEquals(
            listOf(
                Category.PresetCategory(
                    categoryId = PresetCategories.GENERAL.id,
                    sortOrder = 0,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.BASIC_NEEDS.id,
                    sortOrder = 1,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.PERSONAL_CARE.id,
                    sortOrder = 2,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.CONVERSATION.id,
                    sortOrder = 3,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.ENVIRONMENT.id,
                    sortOrder = 4,
                    hidden = false,
                ),
                Category.StoredCategory(
                    "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "storedCategory")),
                    hidden = false,
                    sortOrder = 5
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.USER_KEYPAD.id,
                    sortOrder = 7,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.RECENTS.id,
                    sortOrder = 6,
                    hidden = true,
                )
            ),
            useCase.categories().first()
        )
    }

    @Test
    fun category_moved_down() = runTest {
        val useCase = createUseCase()
        useCase.addCategory("storedCategory")

        useCase.moveCategoryDown(PresetCategories.RECENTS.id)

        assertEquals(
            listOf(
                Category.PresetCategory(
                    categoryId = PresetCategories.GENERAL.id,
                    sortOrder = 0,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.BASIC_NEEDS.id,
                    sortOrder = 1,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.PERSONAL_CARE.id,
                    sortOrder = 2,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.CONVERSATION.id,
                    sortOrder = 3,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.ENVIRONMENT.id,
                    sortOrder = 4,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.USER_KEYPAD.id,
                    sortOrder = 5,
                    hidden = false,
                ),
                Category.StoredCategory(
                    "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "storedCategory")),
                    hidden = false,
                    sortOrder = 6
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.RECENTS.id,
                    sortOrder = 7,
                    hidden = false,
                )
            ),
            useCase.categories().first()
        )
    }

    @Test
    fun category_moved_down_hidden_present() = runTest {
        val useCase = createUseCase()
        useCase.addCategory("storedCategory")

        useCase.updateCategoryHidden(PresetCategories.RECENTS.id, true)

        useCase.moveCategoryDown(PresetCategories.USER_KEYPAD.id)

        assertEquals(
            listOf(
                Category.PresetCategory(
                    categoryId = PresetCategories.GENERAL.id,
                    sortOrder = 0,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.BASIC_NEEDS.id,
                    sortOrder = 1,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.PERSONAL_CARE.id,
                    sortOrder = 2,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.CONVERSATION.id,
                    sortOrder = 3,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.ENVIRONMENT.id,
                    sortOrder = 4,
                    hidden = false,
                ),
                Category.StoredCategory(
                    "1",
                    localizedName = LocalesWithText(mapOf("en_US" to "storedCategory")),
                    hidden = false,
                    sortOrder = 5
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.USER_KEYPAD.id,
                    sortOrder = 7,
                    hidden = false,
                ),
                Category.PresetCategory(
                    categoryId = PresetCategories.RECENTS.id,
                    sortOrder = 6,
                    hidden = true,
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
                localizedName = LocalesWithText(mapOf("en_US" to "storedCategory")),
                hidden = false,
                sortOrder = 0
            )
        )

        val useCase = createUseCase()

        assertEquals(
            Category.StoredCategory(
                "storedCategory",
                localizedName = LocalesWithText(mapOf("en_US" to "storedCategory")),
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
    fun update_category_name_deletes_preset_category_and_creates_stored_category_with_new_name() =
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
            assertEquals(
                1,
                useCase.categories().first()
                    .filter { it.categoryId == PresetCategories.BASIC_NEEDS.id }.size
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
            ),
            useCase.getCategoryById(PresetCategories.BASIC_NEEDS.id)
        )
    }

    @Test
    fun delete_stored_category() = runTest {
        storedCategoriesRepository.upsertCategory(
            Category.StoredCategory(
                "storedCategory1",
                localizedName = LocalesWithText(mapOf("en_US" to "storedCategory1")),
                hidden = false,
                sortOrder = 0
            )
        )
        storedPhrasesRepository.addPhrase(
            PhraseDto(
                phraseId = "phrase1",
                parentCategoryId = "storedCategory1",
                creationDate = 0,
                lastSpokenDate = null,
                localizedUtterance = LocalesWithText(mapOf("en_US" to "phrase1")),
                sortOrder = 0
            )
        )

        val useCase = createUseCase()

        useCase.deleteCategory("storedCategory1")

        assertEquals(
            presetCategoriesRepository.getPresetCategories().first(),
            useCase.categories().first()
        )
        assertEquals(
            null,
            storedPhrasesRepository.getPhrase("phrase1")
        )
    }

    @Test
    fun delete_preset_category() = runTest {
        val useCase = createUseCase()

        useCase.deleteCategory(PresetCategories.BASIC_NEEDS.id)

        assertEquals(
            presetCategoriesRepository.getPresetCategories().first()
                .filter { it.categoryId != PresetCategories.BASIC_NEEDS.id },
            useCase.categories().first()
        )

        assertEquals(
            emptyList<PresetPhrase>(),
            presetPhrasesRepository.getPhrasesForCategory(PresetCategories.BASIC_NEEDS.id)
        )
    }

    @Test
    fun category_added_before_hidden() = runTest {
        val useCase = createUseCase()
        useCase.updateCategoryHidden(PresetCategories.BASIC_NEEDS.id, true)

        useCase.addCategory("New category")

        assertEquals(
            Category.StoredCategory(
                categoryId = "1",
                localizedName = LocalesWithText(mapOf("en_US" to "New category")),
                hidden = false,
                sortOrder = 7
            ),
            useCase.categories().first()[6]
        )
    }
}