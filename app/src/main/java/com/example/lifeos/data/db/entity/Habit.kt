package com.example.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val frequency: String, // "daily" sau "3x_per_week" etc.
    val colorHex: String,  // ex: "#FF6B9D"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)