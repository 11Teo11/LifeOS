package com.example.lifeos.data.ai

import android.content.Context
import com.example.lifeos.data.preferences.OllamaPreferences
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.Transaction
import com.example.lifeos.data.db.entity.TransactionCorrection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class Agent2Classifier(private val context: Context) {

    private val ollamaPreferences = OllamaPreferences(context)
    private val db = AppDatabase.getDatabase(context)

    companion object {
        private const val BATCH_SIZE = 10
    }

    suspend fun classifyTransactions(transactions: List<Transaction>): List<Transaction> {
        val host = ollamaPreferences.ollamaHost.first()
        val ollamaService = OllamaService(host)

        return withContext(Dispatchers.IO) {
            val corrections = db.transactionCorrectionDao().getAllCorrectionsOnce()
            val correctionMap = corrections.associate {
                it.keyword.lowercase() to it.category
            }

            // Separam tranzactiile cu corectii manuale de cele care trebuie clasificate
            val manuallyClassified = mutableListOf<Pair<Int, Transaction>>()
            val toClassify = mutableListOf<Pair<Int, Transaction>>()

            transactions.forEachIndexed { index, transaction ->
                val manualCategory = findManualCorrection(transaction.description, correctionMap)
                if (manualCategory != null) {
                    manuallyClassified.add(index to transaction.copy(
                        category = manualCategory,
                        isManuallyCorrected = true
                    ))
                } else {
                    toClassify.add(index to transaction)
                }
            }

            // Clasificam in batch-uri de BATCH_SIZE
            val classified = mutableMapOf<Int, Transaction>()
            manuallyClassified.forEach { (index, t) -> classified[index] = t }

            toClassify.chunked(BATCH_SIZE).forEach { chunk ->
                val descriptions = chunk.map { (_, t) -> t.description }
                val categories = ollamaService.classifyBatch(descriptions)

                chunk.forEachIndexed { i, (originalIndex, transaction) ->
                    val category = categories.getOrElse(i) { "📦 Other" }
                    classified[originalIndex] = transaction.copy(category = category)
                }
            }

            // Returnam in ordinea originala
            transactions.indices.map { index ->
                classified[index] ?: transactions[index].copy(category = "📦 Other")
            }
        }
    }

    suspend fun saveCorrection(description: String, correctCategory: String) {
        withContext(Dispatchers.IO) {
            val keyword = extractKeyword(description)
            db.transactionCorrectionDao().insert(
                TransactionCorrection(
                    keyword = keyword,
                    category = correctCategory
                )
            )

            val allTransactions = db.transactionDao().getAllTransactionsOnce()
            val toUpdate = allTransactions.filter {
                it.description.lowercase().contains(keyword)
            }
            toUpdate.forEach { transaction ->
                db.transactionDao().updateTransaction(
                    transaction.copy(
                        category = correctCategory,
                        isManuallyCorrected = true
                    )
                )
            }
        }
    }

    private fun findManualCorrection(
        description: String,
        correctionMap: Map<String, String>
    ): String? {
        val descLower = description.lowercase()
        return correctionMap.entries.firstOrNull { (keyword, _) ->
            descLower.contains(keyword)
        }?.value
    }

    private fun extractKeyword(description: String): String {
        return description.trim()
            .split(" ")
            .firstOrNull { it.length > 2 }
            ?.lowercase()
            ?: description.lowercase().take(10)
    }
}