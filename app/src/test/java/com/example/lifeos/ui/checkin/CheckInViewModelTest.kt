package com.example.lifeos.ui.checkin

import android.content.Context
import com.example.lifeos.data.db.entity.DailyCheckIn
import com.example.lifeos.data.db.entity.PatternAlert
import com.example.lifeos.data.repository.DailyCheckInRepository
import com.example.lifeos.data.repository.PatternAlertRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CheckInViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: CheckInViewModel
    private lateinit var mockRepo: DailyCheckInRepository
    private lateinit var mockAlertRepo: PatternAlertRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk {
            every { getLast14Days(any()) } returns flowOf(emptyList())
            coEvery { getCheckInForDate(any()) } returns null
        }
        mockAlertRepo = mockk {
            every { getLatest() } returns flowOf(null)
        }
        val mockContext = mockk<Context>(relaxed = true)
        viewModel = CheckInViewModel(mockRepo, mockAlertRepo, mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() {
        val state = viewModel.uiState.value
        assertEquals(7f, state.sleepHours)
        assertEquals(5, state.energyLevel)
        assertEquals(5, state.stressLevel)
        assertTrue(state.selectedSymptoms.isEmpty())
        assertFalse(state.saved)
        assertFalse(state.duplicateError)
    }

    @Test
    fun `setSleepHours updates sleepHours in state`() {
        viewModel.setSleepHours(6.5f)
        assertEquals(6.5f, viewModel.uiState.value.sleepHours)
    }

    @Test
    fun `setEnergyLevel updates energyLevel in state`() {
        viewModel.setEnergyLevel(8)
        assertEquals(8, viewModel.uiState.value.energyLevel)
    }

    @Test
    fun `setStressLevel updates stressLevel in state`() {
        viewModel.setStressLevel(3)
        assertEquals(3, viewModel.uiState.value.stressLevel)
    }

    @Test
    fun `setDate updates selectedDate and clears errors`() {
        val newDate = LocalDate.of(2024, 3, 15)
        viewModel.setDate(newDate)

        val state = viewModel.uiState.value
        assertEquals(newDate, state.selectedDate)
        assertFalse(state.duplicateError)
        assertFalse(state.saved)
    }

    @Test
    fun `toggleSymptom adds symptom when not present`() {
        viewModel.toggleSymptom("headache")
        assertTrue(viewModel.uiState.value.selectedSymptoms.contains("headache"))
    }

    @Test
    fun `toggleSymptom removes symptom when already present`() {
        viewModel.toggleSymptom("headache")
        viewModel.toggleSymptom("headache")
        assertFalse(viewModel.uiState.value.selectedSymptoms.contains("headache"))
    }

    @Test
    fun `toggleSymptom can track multiple symptoms independently`() {
        viewModel.toggleSymptom("headache")
        viewModel.toggleSymptom("fatigue")
        viewModel.toggleSymptom("headache")  // remove headache

        val symptoms = viewModel.uiState.value.selectedSymptoms
        assertFalse(symptoms.contains("headache"))
        assertTrue(symptoms.contains("fatigue"))
    }

    @Test
    fun `saveCheckIn sets duplicateError when entry already exists for that date`() = runTest {
        val existing = DailyCheckIn(
            date = LocalDate.now().toString(),
            timestamp = System.currentTimeMillis(),
            sleepHours = 7f,
            energyLevel = 5,
            stressLevel = 5
        )
        coEvery { mockRepo.getCheckInForDate(any()) } returns existing

        viewModel.saveCheckIn()

        assertTrue(viewModel.uiState.value.duplicateError)
        assertFalse(viewModel.uiState.value.saved)
    }
}