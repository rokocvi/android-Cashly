package com.example.projektmobpravi.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.projektmobpravi.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): TransactionEntity?

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllByUser(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND category = :category ORDER BY date DESC")
    fun getByCategory(userId: String, category: String): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND date >= :startDate")
    fun getTotalSpentSince(userId: String, startDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND category = :category")
    fun getTotalByCategory(userId: String, category: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND category = :category AND date >= :startDate")
    suspend fun getTotalByCategoryInMonth(userId: String, category: String, startDate: Long): Double?
}