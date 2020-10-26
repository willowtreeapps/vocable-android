package com.willowtree.vocable.screens

import androidx.lifecycle.Observer
import com.BaseUnitTest
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.capture
import com.nhaarman.mockitokotlin2.whenever
import com.willowtree.vocable.getOrAwaitValue
import com.willowtree.vocable.mock
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.CategoryDao
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.room.VocableDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalStdlibApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PresetsViewModelTest : BaseUnitTest() {

    private lateinit var presetsViewModel: PresetsViewModel

    @Mock
    private lateinit var categoryObserver: Observer<Category>

    @Mock
    private lateinit var categoryListObserver: Observer<List<Category>>

    @Mock
    private lateinit var phraseListObserver: Observer<List<Phrase>>

    @Mock
    private lateinit var presetsRepository: PresetsRepository

    private val allCategories = buildList {
        PresetCategories.values().forEach {
            add(
                if (it.id == PresetCategories.USER_FAVORITES.id) {
                    Category(
                        it.id,
                        System.currentTimeMillis(),
                        false,
                        it.getNameId(),
                        null,
                        true,
                        it.initialSortOrder
                    )
                } else {
                    Category(
                        it.id,
                        System.currentTimeMillis(),
                        false,
                        it.getNameId(),
                        null,
                        false,
                        it.initialSortOrder
                    )
                }
            )
        }
    }

    @Before
    fun init() {
        MockitoAnnotations.initMocks(this)
        presetsViewModel = PresetsViewModel(presetsRepository)
        presetsViewModel.selectedCategory.observeForever(categoryObserver)
        presetsViewModel.categoryList.observeForever(categoryListObserver)
        presetsViewModel.currentPhrases.observeForever(phraseListObserver)
    }

    @Test
    fun `categories are populated correctly`() {
        runBlocking {
            val expectedCategoryIdList = listOf(
                "preset_general_category_id",
                "preset_basic_needs_category_id",
                "preset_personal_care_category_id",
                "preset_conversation_category_id",
                "preset_environment_category_id",
                "preset_user_keypad",
                "preset_recents"
            )

            `when`(presetsRepository.getAllCategories()).thenReturn(allCategories)

            presetsViewModel.populateCategories()

            Assert.assertEquals(
                presetsViewModel.categoryList.getOrAwaitValue().map { it.categoryId },
                expectedCategoryIdList
            )
        }
    }

    @Test
    fun `user selects a preset category`() {
        val category = with(PresetCategories.GENERAL) {
            Category(
                this.id,
                System.currentTimeMillis(),
                false,
                this.getNameId(),
                null,
                false,
                this.initialSortOrder
            )
        }

        presetsViewModel.onCategorySelected(category)

        val captor = ArgumentCaptor.forClass(Category::class.java)
        captor.run {
            verify(categoryObserver, times(1)).onChanged(capture())
            Assert.assertEquals(category, value)
        }
    }
}