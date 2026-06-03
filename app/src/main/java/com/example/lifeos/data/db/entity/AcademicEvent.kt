package com.example.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "academic_events")
data class AcademicEvent(
    @PrimaryKey
    val googleEventId: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val pressureLevel: String = "low",
    val isManuallyAdded: Boolean = false
)