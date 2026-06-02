package com.example.lifeos.ui.studybudget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.lifeos.data.agent.Agent3AcademicContext
import com.example.lifeos.data.agent.Agent3Result
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.BudgetTarget
import com.example.lifeos.data.db.entity.Transaction
import com.example.lifeos.data.preferences.OllamaPreferences
import com.example.lifeos.data.repository.TransactionRepository
import com.example.lifeos.util.CsvParseResult
import com.example.lifeos.util.CsvParser
import com.example.lifeos.worker.BudgetCheckWorker
import com.example.lifeos.data.ai.Agent2Classifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class ImportResult(
    val imported: Int = 0,
    val skipped: Int = 0,
    val preview: List<Transaction> = emptyList()
)

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Preview(val result: ImportResult) : ImportState()
    data class Success(val result: ImportResult) : ImportState()
    data class Error(val message: String) : ImportState()
}

private val REVOLUT_EXPORT_INSTRUCTIONS =
    "Invalid Revolut CSV format.\n\n" +
            "How to export from Revolut:\n" +
            "1. Open Revolut app\n" +
            "2. Go to Home → Transactions\n" +
            "3. Tap the download icon (top right)\n" +
            "4. Select CSV format\n" +
            "5. Choose date range and export"

class StudyBudgetViewModel(private val context: Context) : ViewModel() {

    private val repository = TransactionRepository(
        AppDatabase.getDatabase(context).transactionDao()
    )
    private val csvParser = CsvParser()
    private val agent2Classifier = Agent2Classifier(context)
    private val db = AppDatabase.getDatabase(context)
    private val ollamaPreferences = OllamaPreferences(context)

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _agent3Result = MutableStateFlow<Agent3Result?>(null)
    val agent3Result: StateFlow<Agent3Result?> = _agent3Result.asStateFlow()

    val transactions = repository.allTransactions

    val totalBudgetTarget: Flow<BudgetTarget?> = db.budgetTargetDao()
        .getAllBudgetTargets()
        .map { targets -> targets.find { it.category == "💰 Total" } }

    val totalSpent: Flow<Double> = transactions
        .map { list -> list.sumOf { Math.abs(it.amount) } }

    val allBudgetTargets: Flow<List<BudgetTarget>> = db.budgetTargetDao()
        .getAllBudgetTargets()
        .map { targets ->
            targets.sortedByDescending { it.category == "💰 Total" }
        }

    val spentPerCategory: Flow<Map<String, Double>> = transactions
        .map { list ->
            val map = mutableMapOf<String, Double>()
            map["💰 Total"] = list.sumOf { Math.abs(it.amount) }
            list.groupBy { it.category }.forEach { (cat, txs) ->
                map[cat] = txs.sumOf { Math.abs(it.amount) }
            }
            map
        }

    init {
        // Recalculam Agent 3 cand se schimba tranzactiile sau evenimentele
        viewModelScope.launch {
            combine(
                transactions,
                db.academicEventDao().getAllEvents()
            ) { txs, events -> Pair(txs, events) }
                .collect { (txs, events) ->
                    val host = ollamaPreferences.ollamaHost.first()
                    val result = withContext(Dispatchers.IO) {
                        Agent3AcademicContext.analyze(txs, events, host)
                    }
                    _agent3Result.value = result
                }
        }
    }

    fun previewCsv(inputStream: InputStream) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            try {
                when (val result = csvParser.parseRevolutCsv(inputStream)) {
                    is CsvParseResult.Success -> {
                        _importState.value = ImportState.Preview(
                            ImportResult(
                                imported = result.transactions.size,
                                preview = result.transactions.take(5)
                            )
                        )
                    }
                    is CsvParseResult.InvalidFormat -> {
                        _importState.value = ImportState.Error(REVOLUT_EXPORT_INSTRUCTIONS)
                    }
                    is CsvParseResult.EmptyFile -> {
                        _importState.value = ImportState.Error("The selected file is empty.")
                    }
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Error reading file: ${e.message}")
            }
        }
    }

    fun confirmImport(inputStream: InputStream) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            try {
                when (val result = csvParser.parseRevolutCsv(inputStream)) {
                    is CsvParseResult.Success -> {
                        var imported = 0
                        var skipped = 0
                        val toInsert = mutableListOf<Transaction>()

                        for (transaction in result.transactions) {
                            if (repository.isDuplicate(
                                    transaction.date,
                                    transaction.amount,
                                    transaction.description
                                )
                            ) {
                                skipped++
                            } else {
                                toInsert.add(transaction)
                                imported++
                            }
                        }

                        val classified = agent2Classifier.classifyTransactions(toInsert)
                        repository.insertTransactions(classified)

                        val budgetCheckRequest = OneTimeWorkRequestBuilder<BudgetCheckWorker>()
                            .build()
                        WorkManager.getInstance(context).enqueue(budgetCheckRequest)

                        _importState.value = ImportState.Success(
                            ImportResult(imported = imported, skipped = skipped)
                        )
                    }
                    is CsvParseResult.InvalidFormat -> {
                        _importState.value = ImportState.Error(REVOLUT_EXPORT_INSTRUCTIONS)
                    }
                    is CsvParseResult.EmptyFile -> {
                        _importState.value = ImportState.Error("The selected file is empty.")
                    }
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Import error: ${e.message}")
            }
        }
    }

    fun correctCategory(transaction: Transaction, newCategory: String) {
        viewModelScope.launch {
            agent2Classifier.saveCorrection(transaction.description, newCategory)
        }
    }

    fun reclassifyAll() {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            try {
                val allTransactions = withContext(Dispatchers.IO) {
                    db.transactionDao().getAllTransactionsOnce()
                }
                val notCorrected = allTransactions.filter { !it.isManuallyCorrected }
                val classified = agent2Classifier.classifyTransactions(notCorrected)
                withContext(Dispatchers.IO) {
                    classified.forEach {
                        db.transactionDao().updateTransaction(it)
                    }
                }
                _importState.value = ImportState.Idle
            } catch (e: Exception) {
                _importState.value = ImportState.Idle
            }
        }
    }

    fun resetState() {
        _importState.value = ImportState.Idle
    }
}