package com.example.lifeos.ui.studybudget

import android.content.Context
import android.content.Intent
import android.util.Log
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
    data class Success(
        val eventCount: Int,
        val highPressureCount: Int,
        val mediumPressureCount: Int
    ) : CalendarState()
    data class Error(val message: String) : CalendarState()
    object NeedsAccountPicker : CalendarState()
    data class NeedsConsent(val intent: Intent) : CalendarState()
}

class CalendarViewModel(private val context: Context) : ViewModel() {

    private val calendarService = GoogleCalendarService(context)
    private val db = AppDatabase.getDatabase(context)

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _calendarState = MutableStateFlow<CalendarState>(CalendarState.Idle)
    val calendarState: StateFlow<CalendarState> = _calendarState.asStateFlow()

    val academicEvents = db.academicEventDao()

    fun getAccountPickerIntent(): Intent {
        return com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
            .usingOAuth2(context, listOf(com.google.api.services.calendar.CalendarScopes.CALENDAR_READONLY))
            .newChooseAccountIntent()
    }

    fun syncCalendar(accountName: String) {
        rememberAccount(accountName)
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
                        pressureLevel = event.pressureLevel
                    )
                }

                withContext(Dispatchers.IO) {
                    db.academicEventDao().insertAll(entities)
                }

                _calendarState.value = CalendarState.Success(
                    eventCount = events.size,
                    highPressureCount = events.count { it.pressureLevel == "high" },
                    mediumPressureCount = events.count { it.pressureLevel == "medium" }
                )
            } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                _calendarState.value = CalendarState.NeedsConsent(e.intent)
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "syncCalendar failed", e)
                val detail = e.message ?: e.cause?.message ?: e::class.java.simpleName
                _calendarState.value = CalendarState.Error("Failed to sync calendar: $detail")
            }
        }
    }

    fun onConsentLaunched() {
        // Flip out of NeedsConsent so the LaunchedEffect (keyed on state) can re-fire
        // if consent is needed again after a denial+retry.
        _calendarState.value = CalendarState.Loading
    }

    fun onConsentResult(granted: Boolean) {
        if (!granted) {
            _calendarState.value = CalendarState.Error("Calendar permission denied. Tap Connect to retry.")
            return
        }
        val acct = lastAccount()
        if (acct == null) {
            _calendarState.value = CalendarState.Error("Account not remembered. Tap Connect to retry.")
        } else {
            syncCalendar(acct)
        }
    }

    fun onAccountPickerDismissed() {
        if (_calendarState.value is CalendarState.Loading) _calendarState.value = CalendarState.Idle
    }

    private fun rememberAccount(name: String) {
        prefs.edit().putString(KEY_LAST_ACCOUNT, name).apply()
    }

    private fun lastAccount(): String? = prefs.getString(KEY_LAST_ACCOUNT, null)

    companion object {
        private const val PREFS_NAME = "calendar_oauth"
        private const val KEY_LAST_ACCOUNT = "last_account"
    }

    fun addManualEvent(
        title: String,
        startDate: String,
        endDate: String,
        pressureLevel: String
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.academicEventDao().insertAll(
                    listOf(
                        AcademicEvent(
                            googleEventId = "manual_${System.currentTimeMillis()}",
                            title = title,
                            startDate = startDate,
                            endDate = endDate,
                            pressureLevel = pressureLevel,
                            isManuallyAdded = true
                        )
                    )
                )
            }
        }
    }

    fun resetState() {
        _calendarState.value = CalendarState.Idle
    }

    fun updatePressureLevel(event: AcademicEvent, newLevel: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.academicEventDao().insertAll(
                    listOf(event.copy(pressureLevel = newLevel))
                )
            }
        }
    }

    fun deleteEvent(event: AcademicEvent) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.academicEventDao().deleteEvent(event)
            }
        }
    }

    fun updateEvent(event: AcademicEvent, title: String, startDate: String, pressureLevel: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.academicEventDao().insertAll(
                    listOf(event.copy(
                        title = title,
                        startDate = startDate,
                        endDate = startDate,
                        pressureLevel = pressureLevel
                    ))
                )
            }
        }
    }
}