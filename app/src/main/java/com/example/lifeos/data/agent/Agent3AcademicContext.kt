package com.example.lifeos.data.agent

import com.example.lifeos.data.db.entity.AcademicEvent
import com.example.lifeos.data.db.entity.Transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Agent3Result(
    val avgNormalDay: Double,
    val avgExamDay: Double,
    val avgMediumDay: Double,
    val insightSentence: String,
    val hasData: Boolean
)

object Agent3AcademicContext {

    suspend fun analyze(
        transactions: List<Transaction>,
        academicEvents: List<AcademicEvent>,
        host: String = "10.0.2.2"
    ): Agent3Result {
        if (transactions.isEmpty() || academicEvents.isEmpty()) {
            return Agent3Result(
                avgNormalDay = 0.0,
                avgExamDay = 0.0,
                avgMediumDay = 0.0,
                insightSentence = "Import transactions and sync your calendar to see academic spending insights.",
                hasData = false
            )
        }

        val spentPerDay = mutableMapOf<String, Double>()
        for (transaction in transactions) {
            if (transaction.amount >= 0) continue
            val date = extractDate(transaction.date) ?: continue
            spentPerDay[date] = (spentPerDay[date] ?: 0.0) + Math.abs(transaction.amount)
        }

        if (spentPerDay.isEmpty()) {
            return Agent3Result(0.0, 0.0, 0.0, "No spending data found.", false)
        }

        val highPressureDates = academicEvents
            .filter { it.pressureLevel == "high" }
            .mapNotNull { extractDate(it.startDate) }
            .toSet()

        val mediumPressureDates = academicEvents
            .filter { it.pressureLevel == "medium" }
            .mapNotNull { extractDate(it.startDate) }
            .toSet()

        val examDaySpending = spentPerDay.filter { it.key in highPressureDates }.values
        val mediumDaySpending = spentPerDay.filter { it.key in mediumPressureDates }.values
        val normalDaySpending = spentPerDay.filter {
            it.key !in highPressureDates && it.key !in mediumPressureDates
        }.values

        val avgExam = if (examDaySpending.isNotEmpty()) examDaySpending.average() else 0.0
        val avgMedium = if (mediumDaySpending.isNotEmpty()) mediumDaySpending.average() else 0.0
        val avgNormal = if (normalDaySpending.isNotEmpty()) normalDaySpending.average() else 0.0

        val insight = generateInsight(avgNormal, avgExam, avgMedium, host)

        return Agent3Result(
            avgNormalDay = avgNormal,
            avgExamDay = avgExam,
            avgMediumDay = avgMedium,
            insightSentence = insight,
            hasData = true
        )
    }

    private suspend fun generateInsight(
        avgNormal: Double,
        avgExam: Double,
        avgMedium: Double,
        host: String
    ): String {
        return try {
            val prompt = """
You are a financial advisor for students. Based on the spending data below, write ONE concise insight sentence (max 20 words) about the student's spending pattern during academic pressure periods.

Average daily spending:
- Normal days: ${"%.2f".format(avgNormal)} RON
- Deadline days: ${"%.2f".format(avgMedium)} RON  
- Exam days: ${"%.2f".format(avgExam)} RON

Write only the insight sentence, nothing else.
            """.trimIndent()

            val raw = OllamaClient.generate(prompt, host)
            if (!raw.isNullOrBlank() && raw.length > 10) raw.trim()
            else fallbackInsight(avgNormal, avgExam, avgMedium)
        } catch (e: Exception) {
            fallbackInsight(avgNormal, avgExam, avgMedium)
        }
    }

    private fun fallbackInsight(avgNormal: Double, avgExam: Double, avgMedium: Double): String {
        val examDiff = avgExam - avgNormal
        return when {
            avgExam == 0.0 && avgMedium == 0.0 ->
                "No academic pressure days found in your transaction history yet."
            avgExam > 0 && examDiff > 5 ->
                "You spend ${"%.2f".format(examDiff)} RON more per day during exam periods."
            avgExam > 0 && examDiff < -5 ->
                "You spend ${"%.2f".format(-examDiff)} RON less per day during exams — you stay focused!"
            else ->
                "Your spending is consistent across normal and academic pressure days."
        }
    }

    private fun extractDate(dateString: String): String? {
        return try {
            LocalDate.parse(
                dateString.substring(0, minOf(10, dateString.length)),
                DateTimeFormatter.ISO_DATE
            ).toString()
        } catch (e: Exception) {
            null
        }
    }
}