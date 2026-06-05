package com.example.projektmobpravi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.projektmobpravi.data.local.dao.BudgetDao
import com.example.projektmobpravi.data.local.dao.TransactionDao
import com.example.projektmobpravi.data.local.entity.BudgetEntity
import com.example.projektmobpravi.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, BudgetEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
}