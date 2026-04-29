package com.example.lifeos.data.repository

import com.example.lifeos.data.db.dao.TransactionDao
import com.example.lifeos.data.db.entity.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertAll(transactions)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(category)
    }

    fun getTransactionsBetweenDates(startDate: String, endDate: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetweenDates(startDate, endDate)
    }

    suspend fun isDuplicate(date: String, amount: Double, description: String): Boolean {
        return transactionDao.countDuplicates(date, amount, description) > 0
    }
}