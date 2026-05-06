package com.example.lifeos.ui.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeos.data.preferences.ReportPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReportUiState(
    val reportText: String = "",
    val reportDate: String = ""
)

class ReportViewModel(context: Context) : ViewModel() {

    private val prefs = ReportPreferences(context)

    val uiState: StateFlow<ReportUiState> = combine(
        prefs.reportText,
        prefs.reportDate
    ) { text, date ->
        ReportUiState(reportText = text, reportDate = date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportUiState())
}