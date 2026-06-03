package com.example.lifeos.ui.habit

import com.example.lifeos.data.repository.HabitRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: HabitViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val mockRepo = mockk<HabitRepository> {
            every { getActiveHabits() } returns flowOf(emptyList())
            every { getLogsForToday(any(), any()) } returns flowOf(emptyList())
        }
        viewModel = HabitViewModel(mockRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getTodayRange start is at midnight`() {
        val (start, _) = viewModel.getTodayRange()
        val cal = Calendar.getInstance()
        cal.timeInMillis = start

        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `getTodayRange spans exactly 24 hours`() {
        val (start, end) = viewModel.getTodayRange()
        assertEquals(24 * 60 * 60 * 1000L, end - start)
    }

    @Test
    fun `getTodayRange end is start plus one day`() {
        val (start, end) = viewModel.getTodayRange()
        assertTrue(end > start)
        assertEquals(start + 86_400_000L, end)
    }
}