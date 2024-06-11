package com.willowtree.vocable.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: PhraseDto)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrases(vararg phrases: PhraseDto)

    @Query("DELETE FROM Phrase WHERE phrase_id == :phraseId")
    suspend fun deletePhrase(phraseId: String)

    @Delete
    suspend fun deletePhrases(vararg phrases: PhraseDto)

    @Update(entity = PhraseDto::class)
    suspend fun updatePhraseSpokenDate(phraseSpokenDate: PhraseSpokenDate)

    @Update(entity = PhraseDto::class)
    suspend fun updatePhraseLocalizedUtterance(phraseLocalizedUtterance: PhraseLocalizedUtterance)

    @Query("SELECT * FROM Phrase WHERE last_spoken_date IS NOT NULL ORDER BY last_spoken_date DESC LIMIT 8")
    fun getRecentPhrases(): Flow<List<PhraseDto>>

    @Query("SELECT * FROM Phrase WHERE parent_category_id == :categoryId")
    fun getPhrasesForCategory(categoryId: String): Flow<List<PhraseDto>>

    @Query("SELECT * FROM Phrase WHERE phrase_id == :phraseId")
    suspend fun getPhrase(phraseId: String): PhraseDto?
}
