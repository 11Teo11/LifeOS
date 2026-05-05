package com.example.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.lifeos.data.db.entity.PatternAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternAlertDao {

    @Insert
    suspend fun insert(alert: PatternAlert)

    @Query("SELECT * FROM pattern_alerts ORDER BY detectedAt DESC LIMIT 1")
    fun getLatest(): Flow<PatternAlert?>

    @Query("SELECT * FROM pattern_alerts ORDER BY detectedAt DESC LIMIT 1")
    suspend fun getLatestOnce(): PatternAlert?
}