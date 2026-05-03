package com.example.lifeos.ui.studybudget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.Transaction
import com.example.lifeos.data.repository.TransactionRepository
import com.example.lifeos.util.CsvParseResult
import com.example.lifeos.util.CsvParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

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

class StudyBudgetViewModel(context: Context) : ViewModel() {

    private val repository = TransactionRepository(
        AppDatabase.getDatabase(context).transactionDao()
    )
    private val csvParser = CsvParser()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    val transactions = repository.allTransactions

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

                        repository.insertTransactions(toInsert)
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

    fun resetState() {
        _importState.value = ImportState.Idle
    }
}