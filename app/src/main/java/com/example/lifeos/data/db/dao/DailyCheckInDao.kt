package com.example.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.lifeos.data.db.entity.DailyCheckIn
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyCheckInDao {

    @Insert
    suspend fun insert(checkIn: DailyCheckIn)

    @Query("SELECT * FROM daily_checkins WHERE date = :date LIMIT 1")
    suspend fun getCheckInForDate(date: String): DailyCheckIn?

    @Query("SELECT * FROM daily_checkins WHERE date >= :startDate ORDER BY date DESC")
    fun getCheckInsFrom(startDate: String): Flow<List<DailyCheckIn>>
}