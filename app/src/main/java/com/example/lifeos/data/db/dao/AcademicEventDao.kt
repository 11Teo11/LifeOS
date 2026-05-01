package com.example.lifeos.data.db.dao

import androidx.room.*
import com.example.lifeos.data.db.entity.AcademicEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AcademicEvent>)

    @Query("SELECT * FROM academic_events ORDER BY startDate ASC")
    fun getAllEvents(): Flow<List<AcademicEvent>>

    @Query("SELECT * FROM academic_events WHERE isHighPressure = 1 ORDER BY startDate ASC")
    fun getHighPressureEvents(): Flow<List<AcademicEvent>>

    @Query("SELECT * FROM academic_events WHERE startDate BETWEEN :startDate AND :endDate")
    fun getEventsBetweenDates(startDate: String, endDate: String): Flow<List<AcademicEvent>>

    @Query("DELETE FROM academic_events")
    suspend fun deleteAllEvents()
}