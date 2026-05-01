package com.example.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_checkins")
data class DailyCheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val energyLevel: Int,
    val sleepHours: Float,
    val mood: Int,
    val notes: String = ""
)