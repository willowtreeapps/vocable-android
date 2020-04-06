package com.willowtree.vocable.room

import androidx.room.*

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(vararg categories: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM Category")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM Category WHERE identifier = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category

    @Query("SELECT identifier FROM Category WHERE name = 'My Sayings'")
    suspend fun getMySayingsId(): Long

    @Query("SELECT identifier FROM Category WHERE name = :categoryName")
    suspend fun getCategoryId(categoryName: String): Long

    @Query("SELECT is_user_generated FROM Category WHERE name = :categoryName")
    suspend fun getUserGeneratedStatus(categoryName: String): Boolean

    @Update
    suspend fun updateCategory(category: Category)
}