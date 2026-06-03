package com.example.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_corrections")
data class TransactionCorrection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,        // cuvânt cheie din descrierea tranzacției
    val category: String,       // categoria corectă
    val createdAt: Long = System.currentTimeMillis()
)