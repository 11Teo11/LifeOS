package com.example.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.lifeos.data.db.entity.Habit
import com.example.lifeos.data.db.entity.HabitLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Insert
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM habits WHERE isActive = 1")
    fun getActiveHabits(): Flow<List<Habit>>

    @Insert
    suspend fun insertLog(log: HabitLog)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>>

    @Query("""
        SELECT * FROM habit_logs 
        WHERE completedAt >= :startOfDay AND completedAt < :endOfDay
    """)
    fun getLogsForToday(startOfDay: Long, endOfDay: Long): Flow<List<HabitLog>>

    @Query("DELETE FROM habit_logs WHERE completedAt < :startOfDay")
    suspend fun deleteOldLogs(startOfDay: Long)
}