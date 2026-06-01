package com.example.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "day_plan_suggestions",
    foreignKeys = [ForeignKey(
        entity = DayPlan::class,
        parentColumns = ["targetDate"],
        childColumns = ["planTargetDate"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("planTargetDate")]
)
data class DayPlanSuggestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planTargetDate: String,
    val orderIndex: Int,
    val suggestion: String,
    val justification: String,
    val priority: String,
    val category: String,
    val effort: String,
    val isCompleted: Boolean = false
)
