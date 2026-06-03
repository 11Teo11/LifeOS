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
    fun initialState_hasDefaultValues() {
        val state = viewModel.uiState.value
        assertEquals(7f, state.sleepHours)
        assertEquals(5, state.energyLevel)
        assertEquals(5, state.stressLevel)
        assertTrue(state.selectedSymptoms.isEmpty())
        assertFalse(state.saved)
        assertFalse(state.duplicateError)
    }

    @Test
    fun setSleepHours_updatesSleepHoursInState() {
        viewModel.setSleepHours(6.5f)
        assertEquals(6.5f, viewModel.uiState.value.sleepHours)
    }

    @Test
    fun setEnergyLevel_updatesEnergyLevelInState() {
        viewModel.setEnergyLevel(8)
        assertEquals(8, viewModel.uiState.value.energyLevel)
    }

    @Test
    fun setStressLevel_updatesStressLevelInState() {
        viewModel.setStressLevel(3)
        assertEquals(3, viewModel.uiState.value.stressLevel)
    }

    @Test
    fun setDate_updatesSelectedDateAndClearsErrors() {
        val newDate = LocalDate.of(2024, 3, 15)
        viewModel.setDate(newDate)

        val state = viewModel.uiState.value
        assertEquals(newDate, state.selectedDate)
        assertFalse(state.duplicateError)
        assertFalse(state.saved)
    }

    @Test
    fun toggleSymptom_addsSymptomWhenNotPresent() {
        viewModel.toggleSymptom("headache")
        assertTrue(viewModel.uiState.value.selectedSymptoms.contains("headache"))
    }

    @Test
    fun toggleSymptom_removesSymptomWhenAlreadyPresent() {
        viewModel.toggleSymptom("headache")
        viewModel.toggleSymptom("headache")
        assertFalse(viewModel.uiState.value.selectedSymptoms.contains("headache"))
    }

    @Test
    fun toggleSymptomCanTrackMultipleSymptomsIndependently() {
        viewModel.toggleSymptom("headache")
        viewModel.toggleSymptom("fatigue")
        viewModel.toggleSymptom("headache")  // remove headache

        val symptoms = viewModel.uiState.value.selectedSymptoms
        assertFalse(symptoms.contains("headache"))
        assertTrue(symptoms.contains("fatigue"))
    }

    @Test
    fun saveCheckIn_setsDuplicateErrorWhenEntryAlreadyExistsForThatDate() = runTest {
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