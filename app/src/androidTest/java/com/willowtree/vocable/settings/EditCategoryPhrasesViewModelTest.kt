package com.willowtree.vocable.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetPhrase
import com.willowtree.vocable.room.PresetPhrasesRepository
import com.willowtree.vocable.utility.VocableKoinTestRule
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class EditCategoryPhrasesViewModelTest: KoinComponent {

    @get:Rule
    val koinTestRule = VocableKoinTestRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: EditCategoryPhrasesViewModel

    @Before
    fun setUp() {
        val presetPhrasesRepository: PresetPhrasesRepository = get()
        runBlocking {
            presetPhrasesRepository.populateDatabase()
        }
        viewModel = get()
    }

    @Test
    fun getCategoryName_withRecents_returnsRecents() {
        val result = viewModel.getCategoryName(Category.Recents(false, 0))
        assertEquals("Recents", result)
    }

    @Test
    fun getCategoryName_withPresetCategory_returnsCategoryName() {
        val result = viewModel.getCategoryName(Category.PresetCategory(PresetCategories.GENERAL.id, 0, false))
        assertEquals("General", result)
    }

    @Test
    fun fetchCategoryPhrases_withGeneral_returnsCorrectPhraseList() {
        viewModel.fetchCategoryPhrases(Category.PresetCategory(PresetCategories.GENERAL.id, 0, false))
        viewModel.categoryPhraseList.observeForever {
            assertEquals(9, it.size)
            assertEquals("preset_please", it[0].phraseId)
        }
    }

    @Test
    fun deletePhraseFromCategory_withPlease_deletesPleaseCorrectly() {
        viewModel.deletePhraseFromCategory(
            phrase = PresetPhrase(
                phraseId = "preset_please",
                sortOrder = 0,
                lastSpokenDate = 0,
                deleted = false,
                parentCategoryId = PresetCategories.GENERAL.id
            ),
            category = Category.PresetCategory(
                categoryId = PresetCategories.GENERAL.id,
                sortOrder = 0,
                hidden = false
            )
        )

        viewModel.categoryPhraseList.observeForever {
            assertEquals(8, it.size)
            assertEquals("preset_thank_you", it[0].phraseId)
        }
    }
}