package com.example.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_insights")
data class AgentInsight(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val agentId: Int,                    // 1-5
    val insightType: String,             // "daily", "weekly", "budget", "academic"
    val content: String,                 // JSON string cu outputul agentului
    val createdAt: Long = System.currentTimeMillis()
)