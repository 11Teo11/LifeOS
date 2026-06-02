package com.example.lifeos.ui.habit

import com.example.lifeos.data.agent.PlanSuggestion

sealed class DayPlanState {
    object Idle : DayPlanState()
    data class InsufficientData(val checkInsAvailable: Int, val checkInsRequired: Int) : DayPlanState()
    object Loading : DayPlanState()
    data class Draft(
        val targetDate: String,
        val suggestions: List<PlanSuggestion>,
        val energyAvg: Float,
        val source: String
    ) : DayPlanState()
    data class Saved(
        val targetDate: String,
        val suggestions: List<PlanSuggestion>,
        val source: String
    ) : DayPlanState()
    data class Error(val message: String) : DayPlanState()
}
