package com.example.lifeos.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.PatternAlert
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatternAlertDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PatternAlertDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.patternAlertDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getLatest_returnsNullWhenEmpty() = runTest {
        dao.getLatest().test {
            assertNull(awaitItem())
            cancel()
        }
    }

    @Test
    fun insert_and_getLatestOnce_returnsMostRecentAlert() = runTest {
        dao.insert(alert(detectedAt = 1000L, type = "sleep",   severity = "low"))
        dao.insert(alert(detectedAt = 3000L, type = "stress",  severity = "high"))
        dao.insert(alert(detectedAt = 2000L, type = "energy",  severity = "medium"))

        val result = dao.getLatestOnce()

        assertNotNull(result)
        assertEquals("stress", result!!.patternType)   // highest detectedAt = 3000
        assertEquals("high", result.severity)
    }

    @Test
    fun getLatest_emitsUpdatedAlertAfterInsert() = runTest {
        dao.getLatest().test {
            assertNull(awaitItem())  // initially empty

            dao.insert(alert(detectedAt = 1000L, type = "sleep", severity = "low"))
            val after = awaitItem()

            assertNotNull(after)
            assertEquals("sleep", after!!.patternType)
            cancel()
        }
    }

    @Test
    fun insert_withNoPattern_isStoredCorrectly() = runTest {
        dao.insert(alert(type = "none", hasPattern = false, severity = "low"))

        val result = dao.getLatestOnce()

        assertNotNull(result)
        assertFalse(result!!.hasPattern)
        assertEquals("none", result.patternType)
    }

    private fun alert(
        detectedAt: Long = System.currentTimeMillis(),
        type: String = "sleep",
        severity: String = "low",
        hasPattern: Boolean = true,
        description: String = "Test pattern detected"
    ) = PatternAlert(
        detectedAt = detectedAt,
        hasPattern = hasPattern,
        patternType = type,
        severity = severity,
        description = description
    )
}