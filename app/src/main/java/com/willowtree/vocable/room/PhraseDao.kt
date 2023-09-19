package com.willowtree.vocable.room

import androidx.room.*

@Dao
interface PhraseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: Phrase)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrases(vararg phrases: Phrase)

    @Delete
    suspend fun deletePhrase(phrase: Phrase)

    @Delete
    suspend fun deletePhrases(vararg phrases: Phrase)

    @Update
    suspend fun updatePhrase(phrase: Phrase)

    @Update(entity = Phrase::class)
    suspend fun updatePhraseSpokenDate(phraseSpokenDate: PhraseSpokenDate)

    @Query("SELECT * FROM Phrase WHERE last_spoken_date IS NOT NULL ORDER BY last_spoken_date DESC LIMIT 8")
    suspend fun getRecentPhrases(): List<Phrase>
}
