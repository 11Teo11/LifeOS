package com.example.lifeos.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.Transaction
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAll_and_getAllTransactions_returnsInsertedItems() = runTest {
        val transactions = listOf(
            transaction(date = "2024-01-15", description = "Coffee",    amount = -5.0,  category = "food"),
            transaction(date = "2024-01-16", description = "Groceries", amount = -50.0, category = "food")
        )
        dao.insertAll(transactions)

        dao.getAllTransactions().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancel()
        }
    }

    @Test
    fun getTransactionsByCategory_returnsOnlyMatchingCategory() = runTest {
        dao.insertAll(listOf(
            transaction(description = "Coffee",  amount = -5.0,  category = "food"),
            transaction(description = "Netflix", amount = -15.0, category = "entertainment"),
            transaction(description = "Lunch",   amount = -10.0, category = "food")
        ))

        dao.getTransactionsByCategory("food").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.category == "food" })
            cancel()
        }
    }

    @Test
    fun countDuplicates_returnsZeroWhenNoDuplicate() = runTest {
        val count = dao.countDuplicates("2024-01-15", -5.0, "Coffee")
        assertEquals(0, count)
    }

    @Test
    fun countDuplicates_returnsOneAfterInsert() = runTest {
        dao.insertAll(listOf(transaction(date = "2024-01-15", description = "Coffee", amount = -5.0)))

        val count = dao.countDuplicates("2024-01-15", -5.0, "Coffee")
        assertEquals(1, count)
    }

    @Test
    fun insertAll_withIgnoreConflict_doesNotInsertDuplicates() = runTest {
        val tx = transaction(date = "2024-01-15", description = "Coffee", amount = -5.0)
        dao.insertAll(listOf(tx))
        dao.insertAll(listOf(tx))  // same object → same primary key → ignored

        dao.getAllTransactions().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            cancel()
        }
    }

    private fun transaction(
        date: String = "2024-01-15",
        description: String = "Test",
        amount: Double = -10.0,
        category: String = "uncategorized"
    ) = Transaction(date = date, description = description, amount = amount, currency = "RON", category = category)
}