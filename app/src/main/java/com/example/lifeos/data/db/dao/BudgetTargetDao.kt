package com.example.lifeos.data.db.dao

import androidx.room.*
import com.example.lifeos.data.db.entity.BudgetTarget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetTargetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budgetTarget: BudgetTarget)

    @Query("SELECT * FROM budget_targets")
    fun getAllBudgetTargets(): Flow<List<BudgetTarget>>

    @Query("SELECT * FROM budget_targets WHERE category = :category")
    suspend fun getBudgetForCategory(category: String): BudgetTarget?

    @Query("UPDATE budget_targets SET notificationSentAt80 = :sent WHERE category = :category")
    suspend fun updateNotificationStatus(category: String, sent: Boolean)

    @Delete
    suspend fun deleteBudgetTarget(budgetTarget: BudgetTarget)
}