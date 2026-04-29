package com.example.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val description: String,
    val amount: Double,
    val currency: String,
    val category: String = "uncategorized",
    val isManuallyCorrected: Boolean = false
)