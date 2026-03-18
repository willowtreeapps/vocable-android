package com.willowtree.vocable.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.willowtree.vocable.FakeUUIDProvider
import com.willowtree.vocable.MainDispatcherRule
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
import com.willowtree.vocable.ui.editcategories.EditCategoriesIntent
import com.willowtree.vocable.ui.editcategories.EditCategoriesViewModel
import com.willowtree.vocable.utility.FakeDateProvider
import com.willowtree.vocable.utility.VocableKoinTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val presetCategoriesRepository = RoomPresetCategoriesRepository(database)
    private val storedCategoriesRepository = RoomStoredCategoriesRepository(database)
    private val presetPhrasesRepository = RoomPresetPhrasesRepository(
        database.presetPhrasesDao(),
        FakeDateProvider()
    )
    private val storedPhrasesRepository = RoomStoredPhrasesRepository(database, FakeDateProvider())

    private val categoriesUseCase = CategoriesUseCase(
        FakeUUIDProvider(),
        FakeLocaleProvider(),
        storedCategoriesRepository,
        presetCategoriesRepository,
        PhrasesUseCase(
            storedPhrasesRepository,
            presetPhrasesRepository,
            FakeDateProvider(),
            FakeUUIDProvider(),
            FakeLocaleProvider()
        )
    )

    private fun createViewModel(): EditCategoriesViewModel {
        return EditCategoriesViewModel(categoriesUseCase)
    }

    @Test
    fun categories_are_populated() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()

        assertEquals(
            listOf(
                Category.PresetCategory(PresetCategories.GENERAL.id, 0, false),
                Category.PresetCategory(PresetCategories.BASIC_NEEDS.id, 1, false),
                Category.PresetCategory(PresetCategories.PERSONAL_CARE.id, 2, false),
                Category.PresetCategory(PresetCategories.CONVERSATION.id, 3, false),
                Category.PresetCategory(PresetCategories.ENVIRONMENT.id, 4, false),
                Category.PresetCategory(PresetCategories.USER_KEYPAD.id, 5, false),
                Category.Recents(hidden = false, sortOrder = 6),
            ),
            vm.uiState.value.categories
        )
    }

    @Test
    fun move_category_up() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()
        vm.onIntent(EditCategoriesIntent.MoveCategoryUp(PresetCategories.RECENTS.id))

        assertEquals(
            listOf(
                Category.PresetCategory(PresetCategories.GENERAL.id, 0, false),
                Category.PresetCategory(PresetCategories.BASIC_NEEDS.id, 1, false),
                Category.PresetCategory(PresetCategories.PERSONAL_CARE.id, 2, false),
                Category.PresetCategory(PresetCategories.CONVERSATION.id, 3, false),
                Category.PresetCategory(PresetCategories.ENVIRONMENT.id, 4, false),
                Category.Recents(hidden = false, sortOrder = 5),
                Category.PresetCategory(PresetCategories.USER_KEYPAD.id, 6, false),
            ),
            vm.uiState.value.categories
        )
    }

    @Test
    fun move_category_down() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()
        vm.onIntent(EditCategoriesIntent.MoveCategoryDown(PresetCategories.GENERAL.id))

        assertEquals(
            listOf(
                Category.PresetCategory(PresetCategories.BASIC_NEEDS.id, 0, false),
                Category.PresetCategory(PresetCategories.GENERAL.id, 1, false),
                Category.PresetCategory(PresetCategories.PERSONAL_CARE.id, 2, false),
                Category.PresetCategory(PresetCategories.CONVERSATION.id, 3, false),
                Category.PresetCategory(PresetCategories.ENVIRONMENT.id, 4, false),
                Category.PresetCategory(PresetCategories.USER_KEYPAD.id, 5, false),
                Category.Recents(hidden = false, sortOrder = 6),
            ),
            vm.uiState.value.categories
        )
    }

    @Test
    fun adding_category_updates_categories() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()
        categoriesUseCase.addCategory("new category")

        assertEquals(
            Category.StoredCategory(
                categoryId = "1",
                localizedName = LocalesWithText(mapOf("en_US" to "new category")),
                hidden = false,
                sortOrder = 7
            ),
            vm.uiState.value.categories.last()
        )
        assertEquals(1, vm.uiState.value.currentPage)
    }
}
