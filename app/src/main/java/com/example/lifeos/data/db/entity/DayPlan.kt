package com.example.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_plans")
data class DayPlan(
    @PrimaryKey val targetDate: String,
    val generatedAt: Long,
    val source: String,
    val energyAvg: Float? = null
)
