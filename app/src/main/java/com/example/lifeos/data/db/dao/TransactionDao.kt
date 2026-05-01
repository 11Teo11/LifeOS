package com.example.lifeos.data.db.dao

import androidx.room.*
import com.example.lifeos.data.db.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    fun getTransactionsBetweenDates(startDate: String, endDate: String): Flow<List<Transaction>>

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT COUNT(*) FROM transactions WHERE date = :date AND amount = :amount AND description = :description")
    suspend fun countDuplicates(date: String, amount: Double, description: String): Int
}