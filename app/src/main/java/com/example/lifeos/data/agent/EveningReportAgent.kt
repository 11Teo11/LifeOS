package com.example.lifeos.data.agent

import com.example.lifeos.data.db.entity.DailyCheckIn
import com.example.lifeos.data.db.entity.HabitLog
import com.example.lifeos.data.db.entity.PatternAlert
import com.example.lifeos.data.db.entity.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EveningReport(val text: String)

object EveningReportAgent {

    suspend fun generate(
        checkIn: DailyCheckIn,
        todayTransactions: List<Transaction>,
        todayHabitLogs: List<HabitLog>,
        totalActiveHabits: Int,
        latestAlert: PatternAlert?
    ): EveningReport {
        return tryOllama(checkIn, todayTransactions, todayHabitLogs, totalActiveHabits, latestAlert)
            ?: ruleBasedFallback(checkIn, todayTransactions, todayHabitLogs, totalActiveHabits, latestAlert)
    }

    // ── Ollama path ────────────────────────────────────────────────────────────

    private suspend fun tryOllama(
        checkIn: DailyCheckIn,
        todayTransactions: List<Transaction>,
        todayHabitLogs: List<HabitLog>,
        totalActiveHabits: Int,
        latestAlert: PatternAlert?
    ): EveningReport? {
        return try {
            val prompt = buildPrompt(checkIn, todayTransactions, todayHabitLogs, totalActiveHabits, latestAlert)
            val raw = withContext(Dispatchers.IO) { OllamaClient.generate(prompt) }
                ?: return null
            val trimmed = raw.trim()
            if (trimmed.isBlank() || trimmed.length < 30) return null
            EveningReport(trimmed)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildPrompt(
        checkIn: DailyCheckIn,
        todayTransactions: List<Transaction>,
        todayHabitLogs: List<HabitLog>,
        totalActiveHabits: Int,
        latestAlert: PatternAlert?
    ): String {
        val totalSpent = todayTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
        val topCategory = todayTransactions
            .filter { it.amount < 0 }
            .groupBy { it.category }
            .maxByOrNull { it.value.size }?.key ?: "general"
        val habitsCompleted = todayHabitLogs.map { it.habitId }.distinct().size
        val alertLine = if (latestAlert != null && latestAlert.hasPattern)
            "Wellness pattern detected: ${latestAlert.description} (severity: ${latestAlert.severity})"
        else
            "No wellness pattern detected."

        return """
You are an Accountability Coach summarising a user's day. Use ONLY the data below. Be concise and direct.

--- Today's Data ---
Wellness: sleep=${checkIn.sleepHours}h, energy=${checkIn.energyLevel}/10, stress=${checkIn.stressLevel}/10, symptoms="${checkIn.symptoms}"
Habits: $habitsCompleted out of $totalActiveHabits completed today
Finance: ${todayTransactions.size} transaction(s), ${"%.2f".format(totalSpent)} RON spent, top category="$topCategory"
Pattern analysis: $alertLine

--- Instructions ---
Write an evening report in plain English with exactly these three parts:
1. Summary: 2–3 sentences describing the user's day across wellness, habits, and spending.
2. Insights: at most 2 cross-domain insights (e.g. how sleep affects spending, how stress relates to habit completion).
3. Recommendation: one concrete action for tomorrow.

Rules:
- Total response must be under 150 words.
- Do not use bullet points, headers, or markdown.
- Do not repeat the raw numbers verbatim — interpret them.
- Write in second person ("You...").
    """.trimIndent()
    }

    // ── Rule-based fallback ────────────────────────────────────────────────────

    private fun ruleBasedFallback(
        checkIn: DailyCheckIn,
        todayTransactions: List<Transaction>,
        todayHabitLogs: List<HabitLog>,
        totalActiveHabits: Int,
        latestAlert: PatternAlert?
    ): EveningReport {
        val totalSpent = todayTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
        val topCategory = todayTransactions
            .filter { it.amount < 0 }
            .groupBy { it.category }
            .maxByOrNull { it.value.size }?.key ?: "general"
        val habitsCompleted = todayHabitLogs
            .map { it.habitId }
            .distinct()
            .size

        val summary = "Today: energy ${checkIn.energyLevel}/10, stress ${checkIn.stressLevel}/10, " +
                "sleep ${checkIn.sleepHours}h. " +
                "You completed $habitsCompleted/$totalActiveHabits habits " +
                "and logged ${todayTransactions.size} transaction(s) (%.2f RON spent).".format(totalSpent)

        val insight1 = if (checkIn.energyLevel <= 4 && checkIn.sleepHours < 7f)
            "Low energy often follows poor sleep — your ${checkIn.sleepHours}h may be affecting your productivity."
        else if (habitsCompleted == totalActiveHabits && checkIn.energyLevel >= 7)
            "High energy correlated with completing all habits today — good momentum."
        else
            "Your energy (${checkIn.energyLevel}/10) and habit completion ($habitsCompleted/$totalActiveHabits) are linked — consistent routines help."

        val insight2 = if (latestAlert != null && latestAlert.hasPattern)
            "Pattern detected: ${latestAlert.description} — this may be affecting your spending decisions."
        else if (checkIn.stressLevel >= 7 && totalSpent > 0)
            "High stress (${checkIn.stressLevel}/10) combined with spending in \"$topCategory\" — check if this is stress-driven."
        else
            "Your top spending category today was \"$topCategory\" — verify it fits your monthly budget."

        val recommendation = buildRecommendation(checkIn, habitsCompleted, totalActiveHabits, topCategory)

        val report = "$summary $insight1 $insight2 Tomorrow: $recommendation"
        return EveningReport(report.take(750))
    }

    private fun buildRecommendation(
        checkIn: DailyCheckIn,
        habitsCompleted: Int,
        totalActiveHabits: Int,
        topCategory: String
    ): String {
        return when {
            checkIn.sleepHours < 6f -> "prioritise at least 7h sleep to recover energy."
            checkIn.stressLevel >= 8 -> "try a 10-minute wind-down routine before bed to lower stress."
            habitsCompleted < totalActiveHabits -> "focus on completing all $totalActiveHabits habits."
            else -> "review your \"$topCategory\" spending against your monthly target."
        }
    }
}