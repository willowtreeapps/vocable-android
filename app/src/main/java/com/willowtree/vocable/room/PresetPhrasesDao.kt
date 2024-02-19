package com.willowtree.vocable.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PresetPhrasesDao {

    @Query("SELECT * FROM PresetPhrase")
    suspend fun getAllPresetPhrases(): List<PresetPhraseDto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrases(phrases: List<PresetPhraseDto>)

    @Update(entity = PresetPhraseDto::class)
    suspend fun updatePhraseSpokenDate(phraseSpokenDate: PhraseSpokenDate)

    @Query("SELECT * FROM PresetPhrase WHERE last_spoken_date IS NOT NULL ORDER BY last_spoken_date DESC LIMIT 8")
    suspend fun getRecentPhrases(): List<PresetPhraseDto>

    @Query("SELECT * FROM PresetPhrase WHERE parent_category_id = :categoryId")
    suspend fun getPhrasesForCategory(categoryId: String): List<PresetPhraseDto>

    @Query("SELECT * FROM PresetPhrase WHERE phrase_id = :phraseId")
    suspend fun getPhrase(phraseId: String): PresetPhraseDto?

    @Query("UPDATE PresetPhrase SET deleted = :deleted WHERE phrase_id = :phraseId")
    suspend fun deletePhrase(phraseId: String, deleted: Boolean)
}
