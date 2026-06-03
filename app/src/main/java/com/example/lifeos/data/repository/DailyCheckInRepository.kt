package com.example.lifeos.data.repository

import com.example.lifeos.data.db.dao.DailyCheckInDao
import com.example.lifeos.data.db.entity.DailyCheckIn
import kotlinx.coroutines.flow.Flow

class DailyCheckInRepository(private val dao: DailyCheckInDao) {

    suspend fun insert(checkIn: DailyCheckIn) = dao.insert(checkIn)

    suspend fun getCheckInForDate(date: String): DailyCheckIn? =
        dao.getCheckInForDate(date)

    fun getLast14Days(startDate: String): Flow<List<DailyCheckIn>> =
        dao.getCheckInsFrom(startDate)
}