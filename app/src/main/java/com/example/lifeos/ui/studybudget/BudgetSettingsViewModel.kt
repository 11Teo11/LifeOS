package com.example.lifeos.ui.studybudget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeos.data.preferences.OllamaPreferences
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.BudgetTarget
import com.example.lifeos.data.repository.BudgetTargetRepository
import com.example.lifeos.data.repository.BudgetStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val DEFAULT_CATEGORIES = listOf(
    "🍔 Food", "🚌 Transport", "🎬 Entertainment",
    "🛍️ Shopping", "💊 Health", "📚 Education", "📦 Other"
)

val ALL_CATEGORIES = listOf("💰 Total") + DEFAULT_CATEGORIES

class BudgetSettingsViewModel(context: Context) : ViewModel() {

    private val repository = BudgetTargetRepository(
        AppDatabase.getDatabase(context).budgetTargetDao(),
        AppDatabase.getDatabase(context).transactionDao()
    )

    private val ollamaPreferences = OllamaPreferences(context)

    val budgetTargets = repository.allBudgetTargets

    val ollamaHost: StateFlow<String> = ollamaPreferences.ollamaHost
        .stateIn(viewModelScope, SharingStarted.Eagerly, OllamaPreferences.DEFAULT_HOST)

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _ollamaSaveSuccess = MutableStateFlow(false)
    val ollamaSaveSuccess: StateFlow<Boolean> = _ollamaSaveSuccess.asStateFlow()

    fun saveBudget(category: String, monthlyLimit: Double) {
        if (monthlyLimit <= 0) {
            _saveState.value = SaveState.Error("Budget must be greater than 0")
            return
        }
        viewModelScope.launch {
            repository.insertOrUpdate(
                BudgetTarget(
                    category = category,
                    monthlyLimit = monthlyLimit
                )
            )
            _saveState.value = SaveState.Success
        }
    }

    fun saveOllamaHost(host: String) {
        viewModelScope.launch {
            ollamaPreferences.setOllamaHost(host.trim())
            _ollamaSaveSuccess.value = true
            delay(2000)
            _ollamaSaveSuccess.value = false
        }
    }

    fun deleteBudget(budgetTarget: BudgetTarget) {
        viewModelScope.launch {
            repository.delete(budgetTarget)
        }
    }

    fun checkAllBudgets() {
        viewModelScope.launch {
            DEFAULT_CATEGORIES.forEach { category ->
                val status = repository.checkBudgetStatus(category)
                if (status is BudgetStatus.Warning && !isNotificationSent(category)) {
                    repository.markNotificationSent(category)
                }
            }
        }
    }

    private suspend fun isNotificationSent(category: String): Boolean {
        val budgets = repository.allBudgetTargets.first()
        return budgets.find { it.category == category }?.notificationSentAt80 ?: false
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}

sealed class SaveState {
    object Idle : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}