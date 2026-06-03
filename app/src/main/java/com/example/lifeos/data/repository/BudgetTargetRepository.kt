package com.example.lifeos.data.repository

import com.example.lifeos.data.db.dao.BudgetTargetDao
import com.example.lifeos.data.db.dao.TransactionDao
import com.example.lifeos.data.db.entity.BudgetTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BudgetTargetRepository(
    private val budgetTargetDao: BudgetTargetDao,
    private val transactionDao: TransactionDao
) {

    val allBudgetTargets: Flow<List<BudgetTarget>> = budgetTargetDao.getAllBudgetTargets()

    suspend fun insertOrUpdate(budgetTarget: BudgetTarget) {
        budgetTargetDao.insertOrUpdate(budgetTarget)
    }

    suspend fun delete(budgetTarget: BudgetTarget) {
        budgetTargetDao.deleteBudgetTarget(budgetTarget)
    }

    suspend fun getSpentAmountForCategory(category: String): Double {
        if (category == "💰 Total") {
            val allTransactions = transactionDao.getAllTransactionsOnce()
            return allTransactions.filter { it.amount < 0 }.sumOf { Math.abs(it.amount) }
        }
        val transactions = transactionDao.getTransactionsByCategory(category).first()
        return transactions
            .filter { it.amount < 0 }
            .sumOf { Math.abs(it.amount) }
    }

    suspend fun checkBudgetStatus(category: String): BudgetStatus {
        val budget = budgetTargetDao.getBudgetForCategory(category) ?: return BudgetStatus.NoBudget
        val spent = getSpentAmountForCategory(category)
        val percentage = (spent / budget.monthlyLimit) * 100

        return when {
            percentage >= 100 -> BudgetStatus.Exceeded(spent, budget.monthlyLimit)
            percentage >= 80 -> BudgetStatus.Warning(spent, budget.monthlyLimit, percentage)
            else -> BudgetStatus.Ok(spent, budget.monthlyLimit, percentage)
        }
    }

    suspend fun markNotificationSent(category: String) {
        budgetTargetDao.updateNotificationStatus(category, true)
    }
}

sealed class BudgetStatus {
    object NoBudget : BudgetStatus()
    data class Ok(val spent: Double, val limit: Double, val percentage: Double) : BudgetStatus()
    data class Warning(val spent: Double, val limit: Double, val percentage: Double) : BudgetStatus()
    data class Exceeded(val spent: Double, val limit: Double) : BudgetStatus()
}