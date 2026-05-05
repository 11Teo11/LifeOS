package com.example.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_checkins")
data class DailyCheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val timestamp: Long,
    val sleepHours: Float,
    val energyLevel: Int,
    val stressLevel: Int,
    val symptoms: String = ""
)