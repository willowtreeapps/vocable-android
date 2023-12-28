package com.willowtree.vocable.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(vararg categories: CategoryDto)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryDto)

    @Query("SELECT * FROM Category WHERE category_id != 'preset_user_favorites' ORDER BY sort_order ASC")
    suspend fun getAllCategories(): List<CategoryDto>

    @Query("SELECT * FROM Category WHERE category_id != 'preset_user_favorites' ORDER BY sort_order ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryDto>>

    @Query("SELECT * FROM Category WHERE category_id = :categoryId")
    suspend fun getCategoryById(categoryId: String): CategoryDto?

    @Query("DELETE FROM Category WHERE category_id = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    @Update(entity = CategoryDto::class)
    suspend fun updateCategory(categoryLocalizedName: CategoryLocalizedName)

    @Update(entity = CategoryDto::class)
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)

    @Query("SELECT COUNT(*) FROM Category WHERE NOT hidden")
    suspend fun getNumberOfShownCategories(): Int
}