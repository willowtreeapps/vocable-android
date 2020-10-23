package com.willowtree.vocable.screens

import androidx.lifecycle.Observer
import com.BaseUnitTest
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.willowtree.vocable.mock
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*

@ExperimentalCoroutinesApi
class PresetsViewModelTest : BaseUnitTest() {

    private lateinit var presetsViewModel: PresetsViewModel

    private val categoryObserver: Observer<Category> = mock()
    private val categoryListObserver: Observer<List<Category>> = mock()
    private val phraseListObserver: Observer<List<Phrase>> = mock()

    @Before
    fun init() {
        presetsViewModel = PresetsViewModel()
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

            runBlocking {
                presetsViewModel.populateCategories()
            }

            val captor = argumentCaptor<List<Category>>()
            captor.run {
                verify(categoryListObserver, times(1)).onChanged(capture())

                // assert that the list received contains all of the expected elements, and no more
                Assert.assertTrue(this.firstValue.map { it.categoryId }
                    .containsAll(expectedCategoryIdList))
                Assert.assertTrue(this.firstValue.size == expectedCategoryIdList.size)
            }
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