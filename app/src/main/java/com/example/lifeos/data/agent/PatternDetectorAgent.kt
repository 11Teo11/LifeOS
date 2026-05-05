package com.example.lifeos.data.agent

import com.example.lifeos.data.db.entity.DailyCheckIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PatternResult(
    val hasPattern: Boolean,
    val patternType: String,
    val severity: String,
    val description: String
)

object PatternDetectorAgent {

    suspend fun analyze(checkIns: List<DailyCheckIn>): PatternResult {
        if (checkIns.size < 3) return noPattern()
        val sorted = checkIns.sortedBy { it.date }
        return tryOllama(sorted) ?: ruleBasedFallback(sorted)
    }

    // ── Ollama path ────────────────────────────────────────────────────────────

    private suspend fun tryOllama(sorted: List<DailyCheckIn>): PatternResult? {
        return null
    }

    private fun buildPrompt(sorted: List<DailyCheckIn>): String {
        val dataLines = sorted.joinToString("\n") { c ->
            "- ${c.date}: sleep=${c.sleepHours}h, energy=${c.energyLevel}/10, stress=${c.stressLevel}/10"
        }
        return """
You are a wellness pattern detector. Analyze the 14-day health data below and detect negative patterns.

Data:
$dataLines

Detection rules:
- sleep: 3+ consecutive days with sleepHours < 6
- energy: 3+ consecutive days with energyLevel <= 3
- stress: 3+ consecutive days with stressLevel >= 8

Severity: low=3 days, medium=4-5 days, high=6+ days.

Respond ONLY with a single valid JSON object, no extra text:
{"hasPattern":true,"patternType":"sleep","severity":"medium","description":"You slept under 6h for 3 consecutive days."}

If no pattern: {"hasPattern":false,"patternType":"none","severity":"low","description":""}
        """.trimIndent()
    }

    private fun parseJson(raw: String): PatternResult? {
        return try {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start == -1 || end == -1) return null
            val json = JSONObject(raw.substring(start, end + 1))
            PatternResult(
                hasPattern = json.getBoolean("hasPattern"),
                patternType = json.getString("patternType"),
                severity = json.getString("severity"),
                description = json.getString("description").take(100)
            )
        } catch (e: Exception) {
            null
        }
    }

    // ── Rule-based fallback ────────────────────────────────────────────────────

    private fun ruleBasedFallback(sorted: List<DailyCheckIn>): PatternResult {
        val sleepStreak = longestStreak(sorted) { it.sleepHours < 6f }
        if (sleepStreak >= 3) {
            return PatternResult(
                hasPattern = true,
                patternType = "sleep",
                severity = severityFor(sleepStreak),
                description = "You slept under 6h for $sleepStreak consecutive days."
            )
        }
        val energyStreak = longestStreak(sorted) { it.energyLevel <= 3 }
        if (energyStreak >= 3) {
            return PatternResult(
                hasPattern = true,
                patternType = "energy",
                severity = severityFor(energyStreak),
                description = "Your energy was low (\u22643/10) for $energyStreak consecutive days."
            )
        }
        val stressStreak = longestStreak(sorted) { it.stressLevel >= 8 }
        if (stressStreak >= 3) {
            return PatternResult(
                hasPattern = true,
                patternType = "stress",
                severity = severityFor(stressStreak),
                description = "Your stress was high (\u22658/10) for $stressStreak consecutive days."
            )
        }
        return noPattern()
    }

    private fun longestStreak(
        sorted: List<DailyCheckIn>,
        condition: (DailyCheckIn) -> Boolean
    ): Int {
        var max = 0
        var current = 0
        for (entry in sorted) {
            if (condition(entry)) { current++; if (current > max) max = current }
            else current = 0
        }
        return max
    }

    private fun severityFor(streak: Int): String = when {
        streak >= 6 -> "high"
        streak >= 4 -> "medium"
        else -> "low"
    }

    private fun noPattern() = PatternResult(
        hasPattern = false, patternType = "none", severity = "low", description = ""
    )
}