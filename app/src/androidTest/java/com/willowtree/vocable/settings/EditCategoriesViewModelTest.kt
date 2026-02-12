package com.willowtree.vocable.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.willowtree.vocable.CategoriesUseCase
import com.willowtree.vocable.FakeUUIDProvider
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.PhrasesUseCase
import com.willowtree.vocable.basetest.utils.FakeLocaleProvider
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.RoomPresetCategoriesRepository
import com.willowtree.vocable.room.RoomPresetPhrasesRepository
import com.willowtree.vocable.room.RoomStoredCategoriesRepository
import com.willowtree.vocable.room.RoomStoredPhrasesRepository
import com.willowtree.vocable.room.VocableDatabase
import com.willowtree.vocable.utility.FakeDateProvider
import com.willowtree.vocable.utility.StubLegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.utility.VocableKoinTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditCategoriesViewModelTest {

    @get:Rule
    val koinTestRule = VocableKoinTestRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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

    private val categoriesUseCase = CategoriesUseCase(
        FakeUUIDProvider(),
        FakeLocaleProvider(),
        storedCategoriesRepository,
        presetCategoriesRepository,
        PhrasesUseCase(
            StubLegacyCategoriesAndPhrasesRepository(),
            storedPhrasesRepository,
            presetPhrasesRepository,
            FakeDateProvider(),
            FakeUUIDProvider(),
            FakeLocaleProvider()
        )
    )

    private fun createViewModel(): EditCategoriesViewModel {
        return EditCategoriesViewModel(
            categoriesUseCase
        )
    }

    // TODO: CC - the following 4 tests are flaky due to the way refreshCategories() is implemented
    @Test
    fun  refreshing_categories_without_adding_new_category_remains_at_first_index() = runTest {
        val vm = createViewModel()
        vm.refreshCategories()

        vm.liveLastViewedIndex.test {
            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun  adding_new_category_and_refreshing_once_flips_to_new_categorys_index() = runTest {

        val vm = createViewModel()
        vm.refreshCategories()

        categoriesUseCase.addCategory("new category")
        vm.refreshCategories()

        vm.liveLastViewedIndex.test {
            assertEquals(0, awaitItem())
            assertEquals(7, awaitItem())
        }
    }

    @Test
    fun  adding_new_category_and_refreshing_twice_flips_to_first_index() = runTest {
        val vm = createViewModel()
        vm.refreshCategories()

        categoriesUseCase.addCategory("new category")

        vm.refreshCategories()
        vm.liveLastViewedIndex.test {
            assertEquals(0, awaitItem())
            assertEquals(7, awaitItem())
        }

        vm.refreshCategories()
        vm.liveLastViewedIndex.test {
            assertEquals(7, awaitItem())
            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun  adding_new_category_amongst_hidden_categories_and_refreshing_once_flips_to_last_non_hidden_index() = runTest {
        val vm = createViewModel()
        categoriesUseCase.updateCategoryHidden("preset_general", hidden = true)
        categoriesUseCase.updateCategoryHidden("preset_basic_needs", hidden = true)

        vm.refreshCategories()

        categoriesUseCase.addCategory("new category")

        vm.refreshCategories()
        vm.liveLastViewedIndex.test {
            assertEquals(0, awaitItem())
            assertEquals(5, awaitItem())
        }

    }

    @Test
    fun categories_are_populated() = runTest {
        val vm = createViewModel()
        vm.refreshCategories()

        vm.categoryList.test {
            assertEquals(
                listOf(
                    Category.PresetCategory(PresetCategories.GENERAL.id, 0, false),
                    Category.PresetCategory(PresetCategories.BASIC_NEEDS.id, 1, false),
                    Category.PresetCategory(PresetCategories.PERSONAL_CARE.id, 2, false),
                    Category.PresetCategory(PresetCategories.CONVERSATION.id, 3, false),
                    Category.PresetCategory(PresetCategories.ENVIRONMENT.id, 4, false),
                    Category.PresetCategory(PresetCategories.USER_KEYPAD.id, 5, false),
                    Category.PresetCategory(PresetCategories.RECENTS.id, 6, false),
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun move_category_up() = runTest {
        val vm = createViewModel()
        vm.refreshCategories()
        vm.moveCategoryUp(PresetCategories.RECENTS.id)

        vm.categoryList.test {
            awaitItem() // Skip unmoved emission
            assertEquals(
                listOf(
                    Category.PresetCategory(PresetCategories.GENERAL.id, 0, false),
                    Category.PresetCategory(PresetCategories.BASIC_NEEDS.id, 1, false),
                    Category.PresetCategory(PresetCategories.PERSONAL_CARE.id, 2, false),
                    Category.PresetCategory(PresetCategories.CONVERSATION.id, 3, false),
                    Category.PresetCategory(PresetCategories.ENVIRONMENT.id, 4, false),
                    Category.PresetCategory(PresetCategories.RECENTS.id, 5, false),
                    Category.PresetCategory(PresetCategories.USER_KEYPAD.id, 6, false),
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun move_category_down() = runTest {
        val vm = createViewModel()
        vm.refreshCategories()
        vm.moveCategoryDown(PresetCategories.GENERAL.id)

        vm.categoryList.test {
            awaitItem() // Skip unmoved emission
            assertEquals(
                listOf(
                    Category.PresetCategory(PresetCategories.BASIC_NEEDS.id, 0, false),
                    Category.PresetCategory(PresetCategories.GENERAL.id, 1, false),
                    Category.PresetCategory(PresetCategories.PERSONAL_CARE.id, 2, false),
                    Category.PresetCategory(PresetCategories.CONVERSATION.id, 3, false),
                    Category.PresetCategory(PresetCategories.ENVIRONMENT.id, 4, false),
                    Category.PresetCategory(PresetCategories.USER_KEYPAD.id, 5, false),
                    Category.PresetCategory(PresetCategories.RECENTS.id, 6, false),
                ),
                awaitItem()
            )
        }
    }

}