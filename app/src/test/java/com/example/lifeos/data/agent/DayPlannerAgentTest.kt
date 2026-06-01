package com.example.lifeos.data.agent

import com.example.lifeos.data.db.entity.AcademicEvent
import com.example.lifeos.data.db.entity.DailyCheckIn
import com.example.lifeos.data.db.entity.Habit
import com.example.lifeos.data.db.entity.HabitLog
import com.example.lifeos.data.db.entity.PatternAlert
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayPlannerAgentTest {

    private fun checkIn(energy: Int, sleep: Float = 7f, stress: Int = 4, dayOffset: Int = 0): DailyCheckIn {
        val nowMs = System.currentTimeMillis() - dayOffset * 24L * 60 * 60 * 1000
        return DailyCheckIn(
            date = "2026-06-${"%02d".format(1 + dayOffset)}",
            timestamp = nowMs,
            sleepHours = sleep,
            energyLevel = energy,
            stressLevel = stress
        )
    }

    private fun habit(id: Int, name: String) = Habit(id = id, name = name, frequency = "daily", colorHex = "#fff")

    private val noEvents = emptyList<AcademicEvent>()
    private val noTx = emptyList<com.example.lifeos.data.db.entity.Transaction>()
    private val noLogs = emptyList<HabitLog>()
    private val noHabits = emptyList<Habit>()

    // ── Sufficiency gate ───────────────────────────────────────────────────────

    @Test
    fun `fewer than 3 check-ins returns InsufficientData`() = runTest {
        val result = DayPlannerAgent.generate(
            recentCheckIns = listOf(checkIn(7, dayOffset = 0), checkIn(6, dayOffset = 1)),
            latestAlert = null,
            recentTransactions = noTx,
            upcomingEvents = noEvents,
            activeHabits = noHabits,
            recentHabitLogs = noLogs,
            host = "unreachable.invalid"
        )
        assertTrue(result is DayPlannerResult.InsufficientData)
    }

    @Test
    fun `exactly 3 check-ins produces a plan`() = runTest {
        val result = DayPlannerAgent.generate(
            recentCheckIns = listOf(
                checkIn(7, dayOffset = 0),
                checkIn(6, dayOffset = 1),
                checkIn(7, dayOffset = 2)
            ),
            latestAlert = null,
            recentTransactions = noTx,
            upcomingEvents = noEvents,
            activeHabits = noHabits,
            recentHabitLogs = noLogs,
            host = "unreachable.invalid"
        )
        val ready = result as DayPlannerResult.Ready
        assertTrue("Should produce 3-5 suggestions, got ${ready.suggestions.size}",
            ready.suggestions.size in 3..5)
        assertEquals("rule_based", ready.source)
    }

    // ── Energy guardrail ───────────────────────────────────────────────────────

    @Test
    fun `low energy avg excludes high-effort suggestions`() = runTest {
        val result = DayPlannerAgent.generate(
            recentCheckIns = listOf(
                checkIn(3, dayOffset = 0),
                checkIn(4, dayOffset = 1),
                checkIn(2, dayOffset = 2)
            ),
            latestAlert = null,
            recentTransactions = noTx,
            upcomingEvents = listOf(
                AcademicEvent(
                    googleEventId = "evt1",
                    title = "Exam",
                    startDate = "2026-06-05",
                    endDate = "2026-06-05",
                    pressureLevel = "high"
                )
            ),
            activeHabits = noHabits,
            recentHabitLogs = noLogs,
            host = "unreachable.invalid"
        )
        val ready = result as DayPlannerResult.Ready
        assertTrue("Energy avg should be < 5, got ${ready.energyAvg}", ready.energyAvg < 5f)
        assertTrue(
            "No suggestion should be high effort when energy < 5",
            ready.suggestions.none { it.effort == "high" }
        )
    }

    @Test
    fun `low energy avg includes at least one recovery suggestion`() = runTest {
        val result = DayPlannerAgent.generate(
            recentCheckIns = listOf(
                checkIn(3, dayOffset = 0),
                checkIn(4, dayOffset = 1),
                checkIn(2, dayOffset = 2)
            ),
            latestAlert = null,
            recentTransactions = noTx,
            upcomingEvents = noEvents,
            activeHabits = noHabits,
            recentHabitLogs = noLogs,
            host = "unreachable.invalid"
        )
        val ready = result as DayPlannerResult.Ready
        assertTrue(
            "Expected at least one recovery suggestion",
            ready.suggestions.any { it.category == "recovery" }
        )
    }

    @Test
    fun `normal energy avg does not force recovery suggestion`() = runTest {
        val result = DayPlannerAgent.generate(
            recentCheckIns = listOf(
                checkIn(8, dayOffset = 0),
                checkIn(7, dayOffset = 1),
                checkIn(9, dayOffset = 2)
            ),
            latestAlert = null,
            recentTransactions = noTx,
            upcomingEvents = noEvents,
            activeHabits = noHabits,
            recentHabitLogs = noLogs,
            host = "unreachable.invalid"
        )
        val ready = result as DayPlannerResult.Ready
        assertTrue(ready.energyAvg >= 5f)
        // Recovery may still appear, but not required — assert the run doesn't crash and produces output.
        assertTrue(ready.suggestions.isNotEmpty())
    }

    // ── Context-driven suggestions ─────────────────────────────────────────────

    @Test
    fun `stale habit appears as a habit-category suggestion`() = runTest {
        val result = DayPlannerAgent.generate(
            recentCheckIns = listOf(
                checkIn(7, dayOffset = 0),
                checkIn(7, dayOffset = 1),
                checkIn(7, dayOffset = 2)
            ),
            latestAlert = null,
            recentTransactions = noTx,
            upcomingEvents = noEvents,
            activeHabits = listOf(habit(1, "Drink water")),
            recentHabitLogs = emptyList(),
            host = "unreachable.invalid"
        )
        val ready = result as DayPlannerResult.Ready
        assertTrue(
            "Expected a habit suggestion referencing the stale habit",
            ready.suggestions.any { it.category == "habit" && "Drink water" in it.suggestion }
        )
    }

    @Test
    fun `high-pressure upcoming event surfaces as study suggestion`() = runTest {
        val result = DayPlannerAgent.generate(
            recentCheckIns = listOf(
                checkIn(7, dayOffset = 0),
                checkIn(7, dayOffset = 1),
                checkIn(7, dayOffset = 2)
            ),
            latestAlert = null,
            recentTransactions = noTx,
            upcomingEvents = listOf(
                AcademicEvent(
                    googleEventId = "evt1",
                    title = "MDS Exam",
                    startDate = "2026-06-05",
                    endDate = "2026-06-05",
                    pressureLevel = "high"
                )
            ),
            activeHabits = noHabits,
            recentHabitLogs = noLogs,
            host = "unreachable.invalid"
        )
        val ready = result as DayPlannerResult.Ready
        assertTrue(
            "Expected a study suggestion mentioning the upcoming exam",
            ready.suggestions.any { it.category == "study" && "MDS Exam" in it.suggestion }
        )
    }

    // ── parseJson ──────────────────────────────────────────────────────────────

    @Test
    fun `parseJson handles markdown code fences`() {
        val raw = """
            ```json
            [
              {"suggestion":"Sleep early","justification":"Sleep avg 5h","priority":"high","category":"wellness","effort":"low"},
              {"suggestion":"Walk 20m","justification":"Energy low","priority":"medium","category":"recovery","effort":"low"},
              {"suggestion":"Review notes","justification":"Exam soon","priority":"high","category":"study","effort":"medium"}
            ]
            ```
        """.trimIndent()
        val parsed = DayPlannerAgent.parseJson(raw)
        assertNotNull(parsed)
        assertEquals(3, parsed!!.size)
        assertEquals("Sleep early", parsed[0].suggestion)
    }

    @Test
    fun `parseJson rejects fewer than minimum suggestions`() {
        val raw = """[{"suggestion":"x","justification":"y","priority":"low","category":"wellness","effort":"low"}]"""
        val parsed = DayPlannerAgent.parseJson(raw)
        assertNull(parsed)
    }

    @Test
    fun `parseJson coerces unknown enum values to defaults`() {
        val raw = """
            [
              {"suggestion":"a","justification":"b","priority":"URGENT","category":"foo","effort":"extreme"},
              {"suggestion":"c","justification":"d","priority":"high","category":"wellness","effort":"low"},
              {"suggestion":"e","justification":"f","priority":"low","category":"habit","effort":"medium"}
            ]
        """.trimIndent()
        val parsed = DayPlannerAgent.parseJson(raw)
        assertNotNull(parsed)
        assertEquals(3, parsed!!.size)
        assertEquals("medium", parsed[0].priority)
        assertEquals("wellness", parsed[0].category)
        assertEquals("medium", parsed[0].effort)
    }

    @Test
    fun `parseJson returns null on malformed input`() {
        assertNull(DayPlannerAgent.parseJson("not json at all"))
        assertNull(DayPlannerAgent.parseJson(""))
        assertNull(DayPlannerAgent.parseJson("[broken"))
    }
}
