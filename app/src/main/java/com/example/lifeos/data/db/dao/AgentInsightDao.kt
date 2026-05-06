package com.example.lifeos.data.db.dao

import androidx.room.*
import com.example.lifeos.data.db.entity.AgentInsight
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentInsightDao {

    @Insert
    suspend fun insert(insight: AgentInsight)

    @Query("SELECT * FROM agent_insights WHERE agentId = :agentId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForAgent(agentId: Int): AgentInsight?

    @Query("SELECT * FROM agent_insights WHERE agentId = :agentId ORDER BY createdAt DESC")
    fun getAllForAgent(agentId: Int): Flow<List<AgentInsight>>

    @Query("DELETE FROM agent_insights WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}