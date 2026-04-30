package com.example.lifeos.data.habit

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {

    fun getActiveHabits(): Flow<List<Habit>> = habitDao.getActiveHabits()

    fun getLogsForToday(startOfDay: Long, endOfDay: Long): Flow<List<HabitLog>> =
        habitDao.getLogsForToday(startOfDay, endOfDay)

    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>> =
        habitDao.getLogsForHabit(habitId)

    suspend fun insertHabit(habit: Habit) = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)

    suspend fun logHabitDone(habitId: Int) {
        habitDao.insertLog(HabitLog(habitId = habitId))
    }

    suspend fun deleteOldLogs(startOfDay: Long) {
        habitDao.deleteOldLogs(startOfDay)
    }
}