package com.willowtree.vocable.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import com.willowtree.vocable.utils.locale.LocalesWithText
import com.willowtree.vocable.utils.locale.LocalizedResourceUtility
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddUpdateCategoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
            FakeUUIDProvider()
        )
    )

    private fun createViewModel(): AddUpdateCategoryViewModel {
        return AddUpdateCategoryViewModel(
            categoriesUseCase,
            LocalizedResourceUtility(ApplicationProvider.getApplicationContext()),
            FakeLocaleProvider()
        )
    }

    @Test
    fun preset_category_name_updated() = runTest {
        val vm = createViewModel()

        vm.updateCategory(PresetCategories.GENERAL.id, "General 2")

        assertEquals(
            Category.StoredCategory(
                categoryId = PresetCategories.GENERAL.id,
                localizedName = LocalesWithText(mapOf("en_US" to "General 2")),
                hidden = false,
                sortOrder = 0
            ),
            categoriesUseCase.getCategoryById(PresetCategories.GENERAL.id)
        )
    }

}