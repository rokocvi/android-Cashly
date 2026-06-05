package com.example.projektmobpravi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.projektmobpravi.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    fun getAllByUser(userId: String): Flow<List<BudgetEntity>>

    @Query("DELETE FROM budgets WHERE userId = :userId AND category = :category")
    suspend fun delete(userId: String, category: String)

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)

    @Query("SELECT * FROM budgets WHERE userId = :userId AND category = :category LIMIT 1")
    suspend fun getBudget(userId: String, category: String): BudgetEntity?
}
