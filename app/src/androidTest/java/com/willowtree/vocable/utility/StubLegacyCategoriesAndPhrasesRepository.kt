package com.willowtree.vocable.utility

import com.willowtree.vocable.presets.ILegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.locale.LocalesWithText


// Stub for legacy categories/phrases repository. We are using this stub to test the transition of
// PhrasesUseCase from using the legacy categories/phrases repository to using the room-based
// stored and preset phrases repositories. This stub ensures that none of the behavior/data in the
// legacy repository is accidentally used in the testing of newly refactored tests.
class StubLegacyCategoriesAndPhrasesRepository : ILegacyCategoriesAndPhrasesRepository {
    override suspend fun getPhrasesForCategory(categoryId: String) = error("Not implemented")

    override fun getAllCategoriesFlow() = error("Not implemented")

    override suspend fun getAllCategories() = error("Not implemented")

    override suspend fun deletePhrase(phraseId: String) = error("Not implemented")

    override suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) =
        error("Not implemented")

    override suspend fun updateCategoryName(categoryId: String, localizedName: LocalesWithText)
        = error("Not implemented")

    override suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean)
        = error("Not implemented")

    override suspend fun deleteCategory(categoryId: String) = error("Not implemented")

    override suspend fun getRecentPhrases(): List<PhraseDto> = error("Not implemented")
}