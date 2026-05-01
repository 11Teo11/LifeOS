package com.example.lifeos.ui.studybudget

import android.Manifest
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeos.data.calendar.GoogleCalendarService
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.AcademicEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class CalendarState {
    object Idle : CalendarState()
    object Loading : CalendarState()
    data class Success(val eventCount: Int, val highPressureCount: Int) : CalendarState()
    data class Error(val message: String) : CalendarState()
    object NeedsAccountPicker : CalendarState()
    data class NeedsConsent(val intent: android.content.Intent) : CalendarState()
}
class CalendarViewModel(private val context: Context) : ViewModel() {

    private val calendarService = GoogleCalendarService(context)
    private val db = AppDatabase.getDatabase(context)

    private val _calendarState = MutableStateFlow<CalendarState>(CalendarState.Idle)
    val calendarState: StateFlow<CalendarState> = _calendarState.asStateFlow()

    val academicEvents = db.academicEventDao()

    fun getAccountPickerIntent(): Intent {
        return com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
            .usingOAuth2(context, listOf(com.google.api.services.calendar.CalendarScopes.CALENDAR_READONLY))
            .newChooseAccountIntent()
    }

    fun syncCalendar(accountName: String) {
        viewModelScope.launch {
            _calendarState.value = CalendarState.Loading
            try {
                val events = withContext(Dispatchers.IO) {
                    calendarService.fetchEvents(accountName)
                }

                val entities = events.map { event ->
                    AcademicEvent(
                        googleEventId = event.googleEventId,
                        title = event.title,
                        startDate = event.startDate,
                        endDate = event.endDate,
                        isHighPressure = event.isHighPressure
                    )
                }

                withContext(Dispatchers.IO) {
                    db.academicEventDao().insertAll(entities)
                }

                _calendarState.value = CalendarState.Success(
                    eventCount = events.size,
                    highPressureCount = events.count { it.isHighPressure }
                )
            } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                _calendarState.value = CalendarState.NeedsConsent(e.intent)
            } catch (e: Exception) {
                _calendarState.value = CalendarState.Error("Failed to sync calendar: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun resetState() {
        _calendarState.value = CalendarState.Idle
    }
}