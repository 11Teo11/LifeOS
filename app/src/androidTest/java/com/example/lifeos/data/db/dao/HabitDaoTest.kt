package com.example.lifeos.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.Habit
import com.example.lifeos.data.db.entity.HabitLog
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: HabitDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.habitDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertHabit_and_getActiveHabits_returnsInsertedHabit() = runTest {
        val habit = Habit(name = "Read", frequency = "daily", colorHex = "#FF0000")
        dao.insertHabit(habit)

        dao.getActiveHabits().test {
            val habits = awaitItem()
            assertEquals(1, habits.size)
            assertEquals("Read", habits[0].name)
            cancel()
        }
    }

    @Test
    fun deleteHabit_removesItFromActiveList() = runTest {
        val habit = Habit(name = "Run", frequency = "daily", colorHex = "#00FF00")
        val id = dao.insertHabit(habit)
        val inserted = habit.copy(id = id.toInt())

        dao.deleteHabit(inserted)

        dao.getActiveHabits().test {
            val habits = awaitItem()
            assertTrue(habits.isEmpty())
            cancel()
        }
    }

    @Test
    fun insertLog_and_getLogsForToday_returnsLog() = runTest {
        val habit = Habit(name = "Meditate", frequency = "daily", colorHex = "#0000FF")
        val habitId = dao.insertHabit(habit).toInt()

        val now = System.currentTimeMillis()
        val startOfDay = now - (now % 86_400_000)
        val endOfDay = startOfDay + 86_400_000

        dao.insertLog(HabitLog(habitId = habitId, completedAt = now))

        dao.getLogsForToday(startOfDay, endOfDay).test {
            val logs = awaitItem()
            assertEquals(1, logs.size)
            assertEquals(habitId, logs[0].habitId)
            cancel()
        }
    }

    @Test
    fun deleteOldLogs_removesOnlyOldEntries() = runTest {
        val habit = Habit(name = "Stretch", frequency = "daily", colorHex = "#FFFF00")
        val habitId = dao.insertHabit(habit).toInt()

        val now = System.currentTimeMillis()
        val yesterday = now - 86_400_000

        dao.insertLog(HabitLog(habitId = habitId, completedAt = yesterday - 1000)) // old
        dao.insertLog(HabitLog(habitId = habitId, completedAt = now))               // today

        dao.deleteOldLogs(startOfDay = now - (now % 86_400_000))

        dao.getLogsForHabit(habitId).test {
            val logs = awaitItem()
            assertEquals(1, logs.size)  // only today's log remains
            cancel()
        }
    }
}