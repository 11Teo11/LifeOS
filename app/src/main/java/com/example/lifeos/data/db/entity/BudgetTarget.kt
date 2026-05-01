package com.example.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_targets")
data class BudgetTarget(
    @PrimaryKey
    val category: String,
    val monthlyLimit: Double,
    val currency: String = "RON",
    val notificationSentAt80: Boolean = false
)