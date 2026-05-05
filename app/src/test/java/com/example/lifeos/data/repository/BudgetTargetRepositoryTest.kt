package com.example.lifeos.data.repository

import com.example.lifeos.data.db.dao.BudgetTargetDao
import com.example.lifeos.data.db.dao.TransactionDao
import com.example.lifeos.data.db.entity.BudgetTarget
import com.example.lifeos.data.db.entity.Transaction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BudgetTargetRepositoryTest {

    private lateinit var budgetTargetDao: BudgetTargetDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var repository: BudgetTargetRepository

    private val testBudget = BudgetTarget(category = "food", monthlyLimit = 100.0)

    @Before
    fun setUp() {
        budgetTargetDao = mockk()
        transactionDao = mockk()
        repository = BudgetTargetRepository(budgetTargetDao, transactionDao)
    }

    @Test
    fun `checkBudgetStatus returns NoBudget when no budget set`() = runTest {
        coEvery { budgetTargetDao.getBudgetForCategory("food") } returns null

        val status = repository.checkBudgetStatus("food")

        assertTrue(status is BudgetStatus.NoBudget)
    }

    @Test
    fun `checkBudgetStatus returns Ok when spent is below 80 percent`() = runTest {
        coEvery { budgetTargetDao.getBudgetForCategory("food") } returns testBudget
        every { transactionDao.getTransactionsByCategory("food") } returns flowOf(
            listOf(transaction(amount = -70.0))   // 70% of 100
        )

        val status = repository.checkBudgetStatus("food")

        assertTrue(status is BudgetStatus.Ok)
        assertEquals(70.0, (status as BudgetStatus.Ok).spent, 0.001)
    }

    @Test
    fun `checkBudgetStatus returns Warning when spent is between 80 and 100 percent`() = runTest {
        coEvery { budgetTargetDao.getBudgetForCategory("food") } returns testBudget
        every { transactionDao.getTransactionsByCategory("food") } returns flowOf(
            listOf(transaction(amount = -85.0))   // 85% of 100
        )

        val status = repository.checkBudgetStatus("food")

        assertTrue(status is BudgetStatus.Warning)
        assertEquals(85.0, (status as BudgetStatus.Warning).spent, 0.001)
    }

    @Test
    fun `checkBudgetStatus returns Exceeded when spent is at or above 100 percent`() = runTest {
        coEvery { budgetTargetDao.getBudgetForCategory("food") } returns testBudget
        every { transactionDao.getTransactionsByCategory("food") } returns flowOf(
            listOf(transaction(amount = -120.0))  // 120% of 100
        )

        val status = repository.checkBudgetStatus("food")

        assertTrue(status is BudgetStatus.Exceeded)
        assertEquals(120.0, (status as BudgetStatus.Exceeded).spent, 0.001)
    }

    @Test
    fun `getSpentAmount ignores positive transactions (income)`() = runTest {
        coEvery { budgetTargetDao.getBudgetForCategory("food") } returns testBudget
        every { transactionDao.getTransactionsByCategory("food") } returns flowOf(
            listOf(
                transaction(amount = -30.0),   // expense — counts
                transaction(amount = 50.0),    // income — ignored
                transaction(amount = -20.0)    // expense — counts
            )
        )

        val status = repository.checkBudgetStatus("food")

        // Only -30 and -20 count → 50 out of 100 → Ok
        assertTrue(status is BudgetStatus.Ok)
        assertEquals(50.0, (status as BudgetStatus.Ok).spent, 0.001)
    }

    @Test
    fun `checkBudgetStatus at exactly 80 percent returns Warning`() = runTest {
        coEvery { budgetTargetDao.getBudgetForCategory("food") } returns testBudget
        every { transactionDao.getTransactionsByCategory("food") } returns flowOf(
            listOf(transaction(amount = -80.0))  // exactly 80%
        )

        val status = repository.checkBudgetStatus("food")

        assertTrue(status is BudgetStatus.Warning)
    }

    private fun transaction(amount: Double) = Transaction(
        date = "2024-01-15",
        description = "test",
        amount = amount,
        currency = "RON",
        category = "food"
    )
}