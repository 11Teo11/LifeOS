package com.example.lifeos.data.agent

import com.example.lifeos.data.db.entity.AcademicEvent
import com.example.lifeos.data.db.entity.DailyCheckIn
import com.example.lifeos.data.db.entity.Habit
import com.example.lifeos.data.db.entity.HabitLog
import com.example.lifeos.data.db.entity.PatternAlert
import com.example.lifeos.data.db.entity.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class PlanSuggestion(
    val suggestion: String,
    val justification: String,
    val priority: String,
    val category: String,
    val effort: String
)

sealed class DayPlannerResult {
    data class Ready(
        val suggestions: List<PlanSuggestion>,
        val energyAvg: Float,
        val source: String
    ) : DayPlannerResult()
    object InsufficientData : DayPlannerResult()
}

object DayPlannerAgent {

    private const val MIN_CHECKINS_REQUIRED = 3
    private const val LOW_ENERGY_THRESHOLD = 5
    private const val MIN_SUGGESTIONS = 3
    private const val MAX_SUGGESTIONS = 5

    val ALLOWED_CATEGORIES = setOf("recovery", "wellness", "habit", "study", "budget")
    val ALLOWED_PRIORITIES = setOf("high", "medium", "low")
    val ALLOWED_EFFORTS = setOf("low", "medium", "high")
    val BUDGET_CATEGORIES = listOf(
        "Food", "Transport", "Entertainment",
        "Shopping", "Health", "Education", "Other"
    )

    suspend fun generate(
        recentCheckIns: List<DailyCheckIn>,
        latestAlert: PatternAlert?,
        recentTransactions: List<Transaction>,
        upcomingEvents: List<AcademicEvent>,
        activeHabits: List<Habit>,
        recentHabitLogs: List<HabitLog>,
        host: String = "10.0.2.2"
    ): DayPlannerResult {
        val last3 = recentCheckIns.sortedByDescending { it.timestamp }.take(3)
        if (last3.size < MIN_CHECKINS_REQUIRED) return DayPlannerResult.InsufficientData

        val energyAvg = last3.map { it.energyLevel.toFloat() }.average().toFloat()
        val lowEnergy = energyAvg < LOW_ENERGY_THRESHOLD

        val context = buildContext(
            last3 = last3,
            latestAlert = latestAlert,
            recentTransactions = recentTransactions,
            upcomingEvents = upcomingEvents,
            activeHabits = activeHabits,
            recentHabitLogs = recentHabitLogs,
            energyAvg = energyAvg
        )

        val (suggestions, source) = tryOllama(context, host)?.let { it to "ollama" }
            ?: (ruleBasedFallback(context) to "rule_based")

        val guarded = applyGuardrail(suggestions, context)

        return DayPlannerResult.Ready(
            suggestions = guarded,
            energyAvg = energyAvg,
            source = source
        )
    }

    // ── Context summary ────────────────────────────────────────────────────────

    internal data class PlannerContext(
        val last3: List<DailyCheckIn>,
        val energyAvg: Float,
        val sleepAvg: Float,
        val stressAvg: Float,
        val latestAlert: PatternAlert?,
        val totalSpentLast7d: Double,
        val topCategoryLast7d: String,
        val upcomingHighPressure: List<AcademicEvent>,
        val activeHabits: List<Habit>,
        val staleHabitNames: List<String>,
        val lowEnergy: Boolean
    )

    private fun buildContext(
        last3: List<DailyCheckIn>,
        latestAlert: PatternAlert?,
        recentTransactions: List<Transaction>,
        upcomingEvents: List<AcademicEvent>,
        activeHabits: List<Habit>,
        recentHabitLogs: List<HabitLog>,
        energyAvg: Float
    ): PlannerContext {
        val sleepAvg = last3.map { it.sleepHours }.average().toFloat()
        val stressAvg = last3.map { it.stressLevel.toFloat() }.average().toFloat()

        val outflow = recentTransactions.filter { it.amount < 0 }
        val totalSpent = outflow.sumOf { -it.amount }
        val topCategory = outflow.groupBy { it.category.ifBlank { "uncategorized" } }
            .maxByOrNull { it.value.size }?.key ?: "general"

        val upcomingHighPressure = upcomingEvents.filter { it.pressureLevel == "high" }

        val recentHabitIds = recentHabitLogs.map { it.habitId }.toSet()
        val staleHabits = activeHabits.filter { it.id !in recentHabitIds }.map { it.name }

        return PlannerContext(
            last3 = last3,
            energyAvg = energyAvg,
            sleepAvg = sleepAvg,
            stressAvg = stressAvg,
            latestAlert = latestAlert,
            totalSpentLast7d = totalSpent,
            topCategoryLast7d = topCategory,
            upcomingHighPressure = upcomingHighPressure,
            activeHabits = activeHabits,
            staleHabitNames = staleHabits,
            lowEnergy = energyAvg < LOW_ENERGY_THRESHOLD
        )
    }

    // ── Ollama path ────────────────────────────────────────────────────────────

    private suspend fun tryOllama(ctx: PlannerContext, host: String): List<PlanSuggestion>? {
        return try {
            val prompt = buildPrompt(ctx)
            val raw = withContext(Dispatchers.IO) { OllamaClient.generate(prompt, host) }
                ?: return null
            parseJson(raw)?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    internal fun buildPrompt(ctx: PlannerContext): String {
        val alertLine = if (ctx.latestAlert != null && ctx.latestAlert.hasPattern)
            "Wellness pattern: ${ctx.latestAlert.description} (severity: ${ctx.latestAlert.severity})"
        else
            "No recent wellness pattern detected."
        val eventsLine = if (ctx.upcomingHighPressure.isEmpty())
            "No high-pressure academic events in the next 7 days."
        else
            "Upcoming high-pressure events: " +
                ctx.upcomingHighPressure.take(3).joinToString("; ") { "${it.title} (${it.startDate})" }
        val staleLine = if (ctx.staleHabitNames.isEmpty())
            "All active habits were completed in the last 7 days."
        else
            "Habits not completed recently: ${ctx.staleHabitNames.joinToString(", ")}"
        val energyRule = if (ctx.lowEnergy)
            "Average energy is ${"%.1f".format(ctx.energyAvg)}/10 (LOW). " +
                "You MUST exclude high-effort activities (set effort=\"low\" or \"medium\" only) " +
                "and include at least one suggestion with category=\"recovery\"."
        else
            "Average energy is ${"%.1f".format(ctx.energyAvg)}/10. Mix effort levels reasonably."

        return """
You are Agent 4, a Day Planner. Generate a plan for TOMORROW using ONLY the data below.

--- User data (last 3 days) ---
Energy avg: ${"%.1f".format(ctx.energyAvg)}/10
Sleep avg: ${"%.1f".format(ctx.sleepAvg)}h
Stress avg: ${"%.1f".format(ctx.stressAvg)}/10
$alertLine

--- Finance (last 7 days) ---
Total spent: ${"%.2f".format(ctx.totalSpentLast7d)} RON
Top category: ${ctx.topCategoryLast7d}
Budget category options: ${BUDGET_CATEGORIES.joinToString(", ")}

--- Academic (next 7 days) ---
$eventsLine

--- Habits ---
Active habits: ${ctx.activeHabits.joinToString(", ") { it.name }.ifBlank { "none" }}
$staleLine

--- Rules ---
$energyRule
- Return exactly between $MIN_SUGGESTIONS and $MAX_SUGGESTIONS suggestions.
- Each suggestion must reference the user's actual data (not generic advice).
- "justification" must be one short sentence that names the data point.
- Allowed values:
  category: ${ALLOWED_CATEGORIES.joinToString(" | ")}
  priority: ${ALLOWED_PRIORITIES.joinToString(" | ")}
  effort:   ${ALLOWED_EFFORTS.joinToString(" | ")}

--- Output format ---
Return ONLY a JSON array, no prose, no markdown fences. Schema:
[
  {"suggestion":"...","justification":"...","priority":"...","category":"...","effort":"..."}
]
        """.trimIndent()
    }

    internal fun parseJson(raw: String): List<PlanSuggestion>? {
        val trimmed = raw.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = trimmed.indexOf('[')
        val end = trimmed.lastIndexOf(']')
        if (start < 0 || end <= start) return null
        return try {
            val arr = JSONArray(trimmed.substring(start, end + 1))
            val out = mutableListOf<PlanSuggestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val suggestion = obj.optString("suggestion").trim()
                val justification = obj.optString("justification").trim()
                if (suggestion.isBlank() || justification.isBlank()) continue
                out += PlanSuggestion(
                    suggestion = suggestion,
                    justification = justification,
                    priority = obj.optString("priority").lowercase()
                        .takeIf { it in ALLOWED_PRIORITIES } ?: "medium",
                    category = obj.optString("category").lowercase()
                        .takeIf { it in ALLOWED_CATEGORIES } ?: "wellness",
                    effort = obj.optString("effort").lowercase()
                        .takeIf { it in ALLOWED_EFFORTS } ?: "medium"
                )
            }
            out.take(MAX_SUGGESTIONS).takeIf { it.size >= MIN_SUGGESTIONS }
        } catch (e: Exception) {
            null
        }
    }

    // ── Rule-based fallback ────────────────────────────────────────────────────

    internal fun ruleBasedFallback(ctx: PlannerContext): List<PlanSuggestion> {
        val out = mutableListOf<PlanSuggestion>()

        if (ctx.lowEnergy) {
            out += PlanSuggestion(
                suggestion = "Take a recovery day — short walk plus 20 minutes of rest after lunch",
                justification = "Energy averaged ${"%.1f".format(ctx.energyAvg)}/10 over the last 3 days, below 5/10.",
                priority = "high",
                category = "recovery",
                effort = "low"
            )
        }

        if (ctx.sleepAvg < 7f) {
            out += PlanSuggestion(
                suggestion = "Aim for lights-out by 23:00 to hit 7+ hours of sleep",
                justification = "Sleep averaged ${"%.1f".format(ctx.sleepAvg)}h over the last 3 days.",
                priority = "high",
                category = "wellness",
                effort = "low"
            )
        }

        if (ctx.stressAvg >= 7f) {
            out += PlanSuggestion(
                suggestion = "Do a 10-minute wind-down (breathing or stretching) before bed",
                justification = "Stress averaged ${"%.1f".format(ctx.stressAvg)}/10 over the last 3 days.",
                priority = "medium",
                category = "wellness",
                effort = "low"
            )
        }

        ctx.staleHabitNames.firstOrNull()?.let { habit ->
            out += PlanSuggestion(
                suggestion = "Complete your \"$habit\" habit tomorrow",
                justification = "\"$habit\" has not been logged in the last 7 days.",
                priority = "medium",
                category = "habit",
                effort = if (ctx.lowEnergy) "low" else "medium"
            )
        }

        ctx.upcomingHighPressure.firstOrNull()?.let { event ->
            out += PlanSuggestion(
                suggestion = "Block 60 minutes to prepare for \"${event.title}\"",
                justification = "High-pressure event on ${event.startDate}.",
                priority = "high",
                category = "study",
                effort = if (ctx.lowEnergy) "medium" else "high"
            )
        }

        if (ctx.totalSpentLast7d > 0 && ctx.topCategoryLast7d != "general") {
            out += PlanSuggestion(
                suggestion = "Review your \"${ctx.topCategoryLast7d}\" spending against your monthly budget",
                justification = "${ctx.topCategoryLast7d} was your top spending category in the last 7 days (${"%.2f".format(ctx.totalSpentLast7d)} RON total).",
                priority = "low",
                category = "budget",
                effort = "low"
            )
        }

        if (out.size < MIN_SUGGESTIONS) {
            out += PlanSuggestion(
                suggestion = "Plan three priorities for tomorrow in the morning",
                justification = "Set a clear focus before the day starts.",
                priority = "low",
                category = "wellness",
                effort = "low"
            )
        }
        if (out.size < MIN_SUGGESTIONS) {
            out += PlanSuggestion(
                suggestion = "Drink water immediately after waking up",
                justification = "Hydration supports the energy level reported in recent check-ins.",
                priority = "low",
                category = "wellness",
                effort = "low"
            )
        }
        if (out.size < MIN_SUGGESTIONS) {
            out += PlanSuggestion(
                suggestion = "Take a 5-minute break every hour during focused work",
                justification = "Short breaks help sustain attention across the day.",
                priority = "low",
                category = "wellness",
                effort = "low"
            )
        }

        return out.take(MAX_SUGGESTIONS)
    }

    // ── Guardrail (applies to both LLM and fallback output) ───────────────────

    internal fun applyGuardrail(
        raw: List<PlanSuggestion>,
        ctx: PlannerContext
    ): List<PlanSuggestion> {
        if (!ctx.lowEnergy) return raw.take(MAX_SUGGESTIONS)

        val filtered = raw.filter { it.effort != "high" }.toMutableList()
        if (filtered.none { it.category == "recovery" }) {
            filtered.add(
                0,
                PlanSuggestion(
                    suggestion = "Take a recovery day — short walk plus 20 minutes of rest after lunch",
                    justification = "Energy averaged ${"%.1f".format(ctx.energyAvg)}/10 over the last 3 days, below 5/10.",
                    priority = "high",
                    category = "recovery",
                    effort = "low"
                )
            )
        }

        var result = filtered.take(MAX_SUGGESTIONS).toMutableList()
        if (result.size < MIN_SUGGESTIONS) {
            val recoveryFiller = ruleBasedFallback(ctx)
                .filter { it.effort != "high" }
                .filter { existing -> result.none { it.suggestion == existing.suggestion } }
            for (filler in recoveryFiller) {
                if (result.size >= MIN_SUGGESTIONS) break
                result += filler
            }
        }
        return result.take(MAX_SUGGESTIONS)
    }
}
