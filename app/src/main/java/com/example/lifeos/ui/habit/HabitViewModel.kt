package com.example.lifeos.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeos.data.habit.Habit
import com.example.lifeos.data.habit.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    init {
        loadHabits()
    }

    private fun loadHabits() {
        viewModelScope.launch {
            repository.getActiveHabits().collect { _habits.value = it }
        }
    }

    fun addHabit(name: String, frequency: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertHabit(Habit(name = name, frequency = frequency, colorHex = colorHex))
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    fun logHabitDone(habitId: Int) {
        viewModelScope.launch {
            repository.logHabitDone(habitId)
        }
    }

    fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000
        return Pair(startOfDay, endOfDay)
    }
}