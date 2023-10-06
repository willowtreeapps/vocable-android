package com.willowtree.vocable.room

import androidx.room.*

@Dao
interface PhraseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: PhraseDto)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrases(vararg phrases: PhraseDto)

    @Query("DELETE FROM Phrase WHERE phrase_id == :phraseId")
    suspend fun deletePhrase(phraseId: Long)

    @Delete
    suspend fun deletePhrases(vararg phrases: PhraseDto)

    @Update
    suspend fun updatePhrase(phrase: PhraseDto)

    @Update(entity = PhraseDto::class)
    suspend fun updatePhraseSpokenDate(phraseSpokenDate: PhraseSpokenDate)

    @Query("SELECT * FROM Phrase WHERE last_spoken_date IS NOT NULL ORDER BY last_spoken_date DESC LIMIT 8")
    suspend fun getRecentPhrases(): List<PhraseDto>
}
