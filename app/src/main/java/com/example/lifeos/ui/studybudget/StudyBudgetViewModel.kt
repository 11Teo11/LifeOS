package com.example.lifeos.ui.studybudget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.Transaction
import com.example.lifeos.data.repository.TransactionRepository
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
                val parsed = csvParser.parseRevolutCsv(inputStream)
                if (parsed.isEmpty()) {
                    _importState.value = ImportState.Error("No valid transactions found in file.")
                    return@launch
                }
                _importState.value = ImportState.Preview(
                    ImportResult(
                        imported = parsed.size,
                        preview = parsed.take(5)
                    )
                )
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Error reading file: ${e.message}")
            }
        }
    }

    fun confirmImport(inputStream: InputStream) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            try {
                val parsed = csvParser.parseRevolutCsv(inputStream)
                var imported = 0
                var skipped = 0

                val toInsert = mutableListOf<Transaction>()
                for (transaction in parsed) {
                    if (repository.isDuplicate(transaction.date, transaction.amount, transaction.description)) {
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
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Import error: ${e.message}")
            }
        }
    }

    fun resetState() {
        _importState.value = ImportState.Idle
    }
}