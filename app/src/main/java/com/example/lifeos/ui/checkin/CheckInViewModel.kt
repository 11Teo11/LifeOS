package com.example.lifeos.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeos.data.db.entity.DailyCheckIn
import com.example.lifeos.data.repository.DailyCheckInRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CheckInUiState(
    val sleepHours: Float = 7f,
    val energyLevel: Int = 5,
    val stressLevel: Int = 5,
    val selectedSymptoms: Set<String> = emptySet(),
    val selectedDate: LocalDate = LocalDate.now(),
    val saved: Boolean = false,
    val duplicateError: Boolean = false
)

class CheckInViewModel(private val repository: DailyCheckInRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<DailyCheckIn>> = repository
        .getLast14Days(LocalDate.now().minusDays(13).toString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chartData: StateFlow<List<DailyCheckIn>> = repository
        .getLast14Days(LocalDate.now().minusDays(6).toString())
        .map { list -> list.sortedBy { it.date } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSleepHours(hours: Float) = _uiState.update { it.copy(sleepHours = hours) }
    fun setEnergyLevel(level: Int) = _uiState.update { it.copy(energyLevel = level) }
    fun setStressLevel(level: Int) = _uiState.update { it.copy(stressLevel = level) }

    fun setDate(date: LocalDate) =
        _uiState.update { it.copy(selectedDate = date, duplicateError = false, saved = false) }

    fun toggleSymptom(symptom: String) {
        _uiState.update { state ->
            val updated = state.selectedSymptoms.toMutableSet()
            if (symptom in updated) updated.remove(symptom) else updated.add(symptom)
            state.copy(selectedSymptoms = updated)
        }
    }

    fun saveCheckIn() {
        viewModelScope.launch {
            val s = _uiState.value
            val dateStr = s.selectedDate.toString()

            if (repository.getCheckInForDate(dateStr) != null) {
                _uiState.update { it.copy(duplicateError = true) }
                return@launch
            }

            repository.insert(
                DailyCheckIn(
                    date = dateStr,
                    timestamp = System.currentTimeMillis(),
                    sleepHours = s.sleepHours,
                    energyLevel = s.energyLevel,
                    stressLevel = s.stressLevel,
                    symptoms = s.selectedSymptoms.joinToString(",")
                )
            )
            _uiState.update { it.copy(saved = true, duplicateError = false) }
        }
    }
}