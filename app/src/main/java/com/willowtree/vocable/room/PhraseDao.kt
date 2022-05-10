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

    @Query("SELECT * FROM Phrase WHERE phrase_id=:id")
    fun getPhraseById(id: String): Phrase
}
