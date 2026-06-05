package com.example.projektmobpravi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firebaseId: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val note: String = "",
    val date: Long = 0L,
    val currency: String = ""
)