package com.example.projektmobpravi.data.local.entity

import androidx.room.Entity

@Entity(tableName = "budgets", primaryKeys = ["userId", "category"])
data class BudgetEntity(
    val userId: String = "",
    val category: String = "",
    val limitAmount: Double = 0.0
)
