package com.example.lifeos.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.DailyCheckIn
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyCheckInDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DailyCheckInDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dailyCheckInDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getCheckInForDate_returnsNullWhenNoEntry() = runTest {
        val result = dao.getCheckInForDate("2024-01-15")
        assertNull(result)
    }

    @Test
    fun insert_and_getCheckInForDate_returnsCorrectEntry() = runTest {
        dao.insert(checkIn(date = "2024-01-15", sleep = 7.5f, energy = 8, stress = 3))

        val result = dao.getCheckInForDate("2024-01-15")

        assertNotNull(result)
        assertEquals("2024-01-15", result!!.date)
        assertEquals(7.5f, result.sleepHours)
        assertEquals(8, result.energyLevel)
        assertEquals(3, result.stressLevel)
    }

    @Test
    fun getCheckInForDate_returnsNullForDifferentDate() = runTest {
        dao.insert(checkIn(date = "2024-01-15"))

        val result = dao.getCheckInForDate("2024-01-16")
        assertNull(result)
    }

    @Test
    fun getCheckInsFromOnce_returnsOnlyEntriesAfterStartDate() = runTest {
        dao.insert(checkIn(date = "2024-01-10"))
        dao.insert(checkIn(date = "2024-01-15"))
        dao.insert(checkIn(date = "2024-01-20"))

        val result = dao.getCheckInsFromOnce(startDate = "2024-01-15")

        assertEquals(2, result.size)
        assertTrue(result.all { it.date >= "2024-01-15" })
    }

    @Test
    fun insert_withSymptoms_storesAndRetrievesCorrectly() = runTest {
        dao.insert(checkIn(date = "2024-01-15", symptoms = "headache,fatigue"))

        val result = dao.getCheckInForDate("2024-01-15")

        assertEquals("headache,fatigue", result!!.symptoms)
    }

    private fun checkIn(
        date: String,
        sleep: Float = 7f,
        energy: Int = 5,
        stress: Int = 5,
        symptoms: String = ""
    ) = DailyCheckIn(
        date = date,
        timestamp = System.currentTimeMillis(),
        sleepHours = sleep,
        energyLevel = energy,
        stressLevel = stress,
        symptoms = symptoms
    )
}