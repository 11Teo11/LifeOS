# LifeOS — Agent Contracts

This document defines the exact input/output contracts for all 5 AI agents.
All agents must respect these contracts — changes require team agreement.

---

## Agent 1 — Pattern Detector

**Owner:** Colleague A (Roberta)
**Trigger:** After every check-in save (WorkManager one-shot)
**User Story:** WC-03

### Input

Last 14 days of DailyCheckIn records from Room DB:

- date: String (e.g. "2026-05-01")
- energyLevel: Int (1–10)
- sleepHours: Float (0–12)
- mood: Int (1–10)
- notes: String

### Output

- hasPattern: Boolean — true / false
- patternType: String — "sleep" / "energy" / "stress" / "none"
- severity: String — "low" / "medium" / "high"
- description: String — max 100 chars, English

Example (pattern found):

    hasPattern = true
    patternType = "sleep"
    severity = "medium"
    description = "You slept under 6h for 3 consecutive days."

Example (no pattern):

    hasPattern = false
    patternType = "none"
    severity = "low"
    description = ""

---

## Agent 2 — Budget Analyzer

**Owner:** Colleague B (Teo)
**Trigger:** After CSV import or manual transaction add
**User Story:** SB-02

### Input

- Raw transaction list from Room DB (Transaction entities)
- User correction map from Room DB (TransactionCorrection entities)

### Output

List of classifications, one per transaction:

- transactionId: Long — Room DB transaction ID
- category: String — one of the valid categories below
- confidence: Double — 0.0 to 1.0
- wasManuallyOverridden: Boolean — true if user corrected this

Also:

- correctionMapUpdated: Boolean — true if correction map was changed

### Valid categories

"🍔 Food", "🚌 Transport", "🎬 Entertainment", "🛍️ Shopping", "💊 Health", "📚 Education", "📦 Other"

---

## Agent 3 — Academic Context Integrator

**Owner:** Colleague B (Teo)
**Trigger:** Weekly WorkManager + on calendar sync
**User Story:** SB-03, SB-04

### Input

- Agent 2 output (classified transactions)
- AcademicEvent list from Room DB

### Output

- avgDailySpendNormal: Double — RON per day in normal periods
- avgDailySpendExam: Double — RON per day in exam periods
- upcomingHighPressureDays: Int — high pressure days in next 14 days
- insight: String — AI-generated sentence, max 100 chars, English
- pressureFlags: List of upcoming events, each with:
  - date: String
  - pressureLevel: String — "high" / "medium" / "low"
  - eventTitle: String

Example:

    avgDailySpendNormal = 45.20
    avgDailySpendExam = 78.50
    upcomingHighPressureDays = 3
    insight = "You spend 74% more during exam periods."

---

## Agent 4 — Day Planner

**Owner:** You (Erika)
**Trigger:** On-demand — user taps "Generate tomorrow's plan"
**User Story:** HT-02

### Input

- Agent 1 output (pattern detection result)
- Agent 2 output (budget classifications)
- Agent 3 output (academic context)
- Last 7 days of HabitLog from Room DB
- Tomorrow's AcademicEvent list

### Output

List of 3–5 suggestions, each with:

- suggestion: String — max 80 chars, English
- justification: String — max 120 chars, references actual data
- priority: String — "high" / "medium" / "low"

Example:

    suggestion = "Go to bed before 23:00"
    justification = "You slept under 6h for 3 days in a row."
    priority = "high"

---

## Agent 5 — Accountability Coach

**Owner:** You (Erika)
**Trigger:** 21:00 daily (WorkManager periodic) + Sunday weekly
**User Story:** AC-01, AC-02

### Input

- Agent 1 output
- Agent 2 output
- Agent 3 output
- Agent 4 output (if generated today)
- Today's completed habits (HabitLog)
- Today's transactions
- Today's check-in

### Output — Daily Report

- reportType: String — "daily"
- summary: String — max 50 words
- insights: List — max 2 items, each max 50 words
- recommendation: String — max 30 words, actionable
- wordCount: Int — total report must be under 150 words

Example:

    reportType = "daily"
    summary = "Good energy day despite 2 exams coming up."
    insights = ["You spent 40% more on food delivery on low-sleep days."]
    recommendation = "Prep meals tomorrow to reduce food spending."
    wordCount = 42

### Output — Weekly Correlations

- reportType: String — "weekly"
- correlations: List — max 2 items, each with:
  - description: String — max 80 chars
  - occurrences: Int — min 3 to avoid false positives
  - confidence: String — "low" / "medium" / "high"

Example:

    description = "When you sleep under 6h, you spend 40% more on food delivery."
    occurrences = 4
    confidence = "high"

---

## Inter-Agent Dependencies

Agent 1 consumes: DailyCheckIn records
Agent 2 consumes: Transaction records + correction map
Agent 3 consumes: Agent 2 output + AcademicEvent records
Agent 4 consumes: Agent 1 + Agent 2 + Agent 3 outputs + HabitLog + tomorrow's events
Agent 5 consumes: Agent 1 + Agent 2 + Agent 3 + Agent 4 outputs + today's full summary

---

## Implementation Notes

- All agents use Gemini Nano (on-device) or Ollama + Mistral 7B as fallback
- Agent outputs are stored in Room DB as JSON strings
- If an agent fails, the app continues normally — agents are non-blocking
- Agents 1, 2, 3 run automatically; Agent 4 is on-demand; Agent 5 is scheduled
- Hallucinations are acceptable per barem — focus on structure correctness
- Never change output field names without updating this document and notifying the team
