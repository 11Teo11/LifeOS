package com.example.lifeos.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeos.data.agent.DayPlannerAgent
import com.example.lifeos.data.agent.DayPlannerResult
import com.example.lifeos.data.agent.PlanSuggestion
import com.example.lifeos.data.db.dao.AcademicEventDao
import com.example.lifeos.data.db.dao.DailyCheckInDao
import com.example.lifeos.data.db.entity.DayPlan
import com.example.lifeos.data.db.entity.DayPlanSuggestion
import com.example.lifeos.data.preferences.OllamaPreferences
import com.example.lifeos.data.repository.DayPlanRepository
import com.example.lifeos.data.repository.HabitRepository
import com.example.lifeos.data.repository.PatternAlertRepository
import com.example.lifeos.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class DayPlanViewModel(
    private val checkInDao: DailyCheckInDao,
    private val patternAlertRepository: PatternAlertRepository,
    private val transactionRepository: TransactionRepository,
    private val academicEventDao: AcademicEventDao,
    private val habitRepository: HabitRepository,
    private val dayPlanRepository: DayPlanRepository,
    private val ollamaPreferences: OllamaPreferences
) : ViewModel() {

    private val _planState = MutableStateFlow<DayPlanState>(DayPlanState.Idle)
    val planState: StateFlow<DayPlanState> = _planState.asStateFlow()

    init {
        viewModelScope.launch { loadSavedPlanIfAny() }
    }

    private suspend fun loadSavedPlanIfAny() {
        val targetDate = tomorrowIso()
        val plan = dayPlanRepository.getPlanForDate(targetDate) ?: return
        val rows = dayPlanRepository.getSuggestionsForDate(targetDate)
        if (rows.isEmpty()) return
        _planState.value = DayPlanState.Saved(
            targetDate = targetDate,
            suggestions = rows.map { it.toDomain() },
            source = plan.source
        )
    }

    fun generatePlan() {
        viewModelScope.launch {
            _planState.value = DayPlanState.Loading
            try {
                val sevenDaysAgoIso = LocalDate.now().minusDays(7).toString()
                val sevenDaysAgoMs = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                val tomorrowIso = tomorrowIso()
                val sevenDaysAheadIso = LocalDate.now().plusDays(7).toString()

                val checkIns = checkInDao.getCheckInsFromOnce(sevenDaysAgoIso)
                if (checkIns.size < 3) {
                    _planState.value = DayPlanState.InsufficientData(
                        checkInsAvailable = checkIns.size,
                        checkInsRequired = 3
                    )
                    return@launch
                }

                val latestAlert = patternAlertRepository.getLatest().first()
                val transactions = transactionRepository
                    .getTransactionsBetweenDates(sevenDaysAgoIso, LocalDate.now().toString())
                    .first()
                val upcomingEvents = academicEventDao
                    .getEventsBetweenDates(LocalDate.now().toString(), sevenDaysAheadIso)
                    .first()
                val activeHabits = habitRepository.getActiveHabits().first()
                val recentLogs = habitRepository.getLogsSince(sevenDaysAgoMs)

                val host = ollamaPreferences.ollamaHost.first()

                val result = DayPlannerAgent.generate(
                    recentCheckIns = checkIns,
                    latestAlert = latestAlert,
                    recentTransactions = transactions,
                    upcomingEvents = upcomingEvents,
                    activeHabits = activeHabits,
                    recentHabitLogs = recentLogs,
                    host = host
                )

                _planState.value = when (result) {
                    is DayPlannerResult.InsufficientData -> DayPlanState.InsufficientData(
                        checkInsAvailable = checkIns.size,
                        checkInsRequired = 3
                    )
                    is DayPlannerResult.Ready -> DayPlanState.Draft(
                        targetDate = tomorrowIso,
                        suggestions = result.suggestions,
                        energyAvg = result.energyAvg,
                        source = result.source
                    )
                }
            } catch (e: Exception) {
                _planState.value = DayPlanState.Error(
                    "Could not generate plan: ${e.message ?: e::class.java.simpleName}"
                )
            }
        }
    }

    fun updateDraftSuggestion(index: Int, suggestion: String, justification: String) {
        val current = _planState.value as? DayPlanState.Draft ?: return
        if (index !in current.suggestions.indices) return
        val updated = current.suggestions.toMutableList().also {
            it[index] = it[index].copy(suggestion = suggestion, justification = justification)
        }
        _planState.value = current.copy(suggestions = updated)
    }

    fun removeDraftSuggestion(index: Int) {
        val current = _planState.value as? DayPlanState.Draft ?: return
        if (index !in current.suggestions.indices) return
        val updated = current.suggestions.toMutableList().also { it.removeAt(index) }
        _planState.value = current.copy(suggestions = updated)
    }

    fun saveDraft() {
        val draft = _planState.value as? DayPlanState.Draft ?: return
        if (draft.suggestions.isEmpty()) return
        viewModelScope.launch {
            val plan = DayPlan(
                targetDate = draft.targetDate,
                generatedAt = System.currentTimeMillis(),
                source = draft.source,
                energyAvg = draft.energyAvg
            )
            val rows = draft.suggestions.mapIndexed { idx, s ->
                DayPlanSuggestion(
                    planTargetDate = draft.targetDate,
                    orderIndex = idx,
                    suggestion = s.suggestion,
                    justification = s.justification,
                    priority = s.priority,
                    category = s.category,
                    effort = s.effort
                )
            }
            dayPlanRepository.savePlan(plan, rows)
            _planState.value = DayPlanState.Saved(
                targetDate = draft.targetDate,
                suggestions = draft.suggestions,
                source = draft.source
            )
        }
    }

    fun discardDraft() {
        val target = tomorrowIso()
        viewModelScope.launch {
            val existing = dayPlanRepository.getPlanForDate(target)
            if (existing != null) {
                val rows = dayPlanRepository.getSuggestionsForDate(target)
                _planState.value = DayPlanState.Saved(
                    targetDate = target,
                    suggestions = rows.map { it.toDomain() },
                    source = existing.source
                )
            } else {
                _planState.value = DayPlanState.Idle
            }
        }
    }

    fun clearSavedAndRegenerate() {
        viewModelScope.launch {
            dayPlanRepository.deletePlan(tomorrowIso())
            _planState.value = DayPlanState.Idle
            generatePlan()
        }
    }

    private fun tomorrowIso(): String = LocalDate.now().plusDays(1).toString()

    private fun DayPlanSuggestion.toDomain() = PlanSuggestion(
        suggestion = suggestion,
        justification = justification,
        priority = priority,
        category = category,
        effort = effort
    )
}
