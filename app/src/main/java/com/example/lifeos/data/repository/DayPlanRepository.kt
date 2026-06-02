package com.example.lifeos.data.repository

import com.example.lifeos.data.db.dao.DayPlanDao
import com.example.lifeos.data.db.entity.DayPlan
import com.example.lifeos.data.db.entity.DayPlanSuggestion
import kotlinx.coroutines.flow.Flow

class DayPlanRepository(private val dao: DayPlanDao) {

    suspend fun getPlanForDate(targetDate: String): DayPlan? =
        dao.getPlanForDate(targetDate)

    suspend fun getSuggestionsForDate(targetDate: String): List<DayPlanSuggestion> =
        dao.getSuggestionsForDate(targetDate)

    fun observePlanForDate(targetDate: String): Flow<DayPlan?> =
        dao.observePlanForDate(targetDate)

    fun observeSuggestionsForDate(targetDate: String): Flow<List<DayPlanSuggestion>> =
        dao.observeSuggestionsForDate(targetDate)

    suspend fun savePlan(plan: DayPlan, suggestions: List<DayPlanSuggestion>) {
        dao.replacePlan(plan, suggestions)
    }

    suspend fun updateSuggestion(suggestion: DayPlanSuggestion) {
        dao.updateSuggestion(suggestion)
    }

    suspend fun deletePlan(targetDate: String) {
        dao.deletePlan(targetDate)
    }

    suspend fun pruneOlderThan(keepFromDate: String) {
        dao.deletePlansOlderThan(keepFromDate)
    }
}
