package com.example.lifeos.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.repository.BudgetStatus
import com.example.lifeos.data.repository.BudgetTargetRepository
import com.example.lifeos.util.NotificationHelper
import kotlin.math.abs

class BudgetCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = BudgetTargetRepository(
            db.budgetTargetDao(),
            db.transactionDao()
        )

        val budgetTargets = db.budgetTargetDao().getAllBudgetTargetsOnce()
        val allTransactions = db.transactionDao().getAllTransactionsOnce()
        val totalSpent = allTransactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }

        for (target in budgetTargets) {
            if (target.notificationSentAt80) continue

            val percentage = if (target.category == "💰 Total") {
                // Buget total — suma tuturor tranzactiilor
                (totalSpent / target.monthlyLimit) * 100
            } else {
                // Buget per categorie
                val status = repository.checkBudgetStatus(target.category)
                when (status) {
                    is BudgetStatus.Warning -> status.percentage
                    is BudgetStatus.Exceeded -> 100.0
                    else -> continue
                }
            }

            if (percentage >= 80) {
                NotificationHelper.sendBudgetWarning(
                    applicationContext,
                    target.category,
                    percentage
                )
                repository.markNotificationSent(target.category)
            }
        }

        return Result.success()
    }
}