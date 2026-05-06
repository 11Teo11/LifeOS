package com.example.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pattern_alerts")
data class PatternAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val detectedAt: Long,
    val hasPattern: Boolean,
    val patternType: String,   // "sleep" | "energy" | "stress" | "none"
    val severity: String,      // "low" | "medium" | "high"
    val description: String    // max 100 chars, English
)