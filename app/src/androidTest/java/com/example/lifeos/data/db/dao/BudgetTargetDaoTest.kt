package com.example.lifeos.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.BudgetTarget
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetTargetDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BudgetTargetDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.budgetTargetDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertOrUpdate_and_getAllBudgetTargets_returnsInserted() = runTest {
        dao.insertOrUpdate(BudgetTarget(category = "food", monthlyLimit = 200.0))

        dao.getAllBudgetTargets().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("food", result[0].category)
            assertEquals(200.0, result[0].monthlyLimit, 0.001)
            cancel()
        }
    }

    @Test
    fun insertOrUpdate_withSameCategory_replacesExistingEntry() = runTest {
        dao.insertOrUpdate(BudgetTarget(category = "food", monthlyLimit = 200.0))
        dao.insertOrUpdate(BudgetTarget(category = "food", monthlyLimit = 350.0))

        dao.getAllBudgetTargets().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(350.0, result[0].monthlyLimit, 0.001)
            cancel()
        }
    }

    @Test
    fun getBudgetForCategory_returnsNullWhenNotFound() = runTest {
        val result = dao.getBudgetForCategory("nonexistent")
        assertNull(result)
    }

    @Test
    fun getBudgetForCategory_returnsCorrectEntry() = runTest {
        dao.insertOrUpdate(BudgetTarget(category = "food",          monthlyLimit = 200.0))
        dao.insertOrUpdate(BudgetTarget(category = "entertainment", monthlyLimit = 100.0))

        val result = dao.getBudgetForCategory("entertainment")

        assertNotNull(result)
        assertEquals(100.0, result!!.monthlyLimit, 0.001)
    }

    @Test
    fun updateNotificationStatus_updatesFlag() = runTest {
        dao.insertOrUpdate(BudgetTarget(category = "food", monthlyLimit = 200.0, notificationSentAt80 = false))

        dao.updateNotificationStatus("food", sent = true)

        val result = dao.getBudgetForCategory("food")
        assertTrue(result!!.notificationSentAt80)
    }

    @Test
    fun deleteBudgetTarget_removesEntry() = runTest {
        val target = BudgetTarget(category = "food", monthlyLimit = 200.0)
        dao.insertOrUpdate(target)
        dao.deleteBudgetTarget(target)

        dao.getAllBudgetTargets().test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            cancel()
        }
    }
}