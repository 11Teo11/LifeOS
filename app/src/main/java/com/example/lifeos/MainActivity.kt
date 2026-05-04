package com.example.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.repository.HabitRepository
import com.example.lifeos.ui.habit.HabitScreen
import com.example.lifeos.ui.habit.HabitViewModel
import com.example.lifeos.ui.onboarding.OnboardingScreen
import com.example.lifeos.ui.onboarding.OnboardingViewModel
import com.example.lifeos.ui.studybudget.BudgetSettingsScreen
import com.example.lifeos.ui.studybudget.BudgetSettingsViewModel
import com.example.lifeos.ui.studybudget.CalendarScreen
import com.example.lifeos.ui.studybudget.CalendarViewModel
import com.example.lifeos.ui.studybudget.StudyBudgetScreen
import com.example.lifeos.ui.studybudget.StudyBudgetViewModel
import com.example.lifeos.ui.theme.LifeOSTheme
import com.example.lifeos.util.NotificationHelper
import com.example.lifeos.worker.BudgetCheckWorker
import com.example.lifeos.worker.HabitResetWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var habitViewModel: HabitViewModel

    private fun scheduleHabitReset() {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val delay = midnight.timeInMillis - now.timeInMillis

        val resetRequest = PeriodicWorkRequestBuilder<HabitResetWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "habit_reset",
            ExistingPeriodicWorkPolicy.KEEP,
            resetRequest
        )
    }

    private fun scheduleBudgetCheck() {
        val budgetCheckRequest = PeriodicWorkRequestBuilder<BudgetCheckWorker>(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "budget_check",
            ExistingPeriodicWorkPolicy.KEEP,
            budgetCheckRequest
        )
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleHabitReset()
        NotificationHelper.createNotificationChannel(this)
        scheduleBudgetCheck()
        requestNotificationPermission()

        val database = AppDatabase.getDatabase(this)
        val repository = HabitRepository(database.habitDao())
        habitViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HabitViewModel(repository) as T
            }
        })[HabitViewModel::class.java]

        setContent {
            LifeOSTheme {
                val context = LocalContext.current
                val onboardingViewModel = remember { OnboardingViewModel(context) }
                val isOnboardingCompleted by onboardingViewModel.isCompleted.collectAsState()
                val isOnboardingFullyCompleted by onboardingViewModel.isFullyCompleted.collectAsState()
                val studyBudgetViewModel = remember { StudyBudgetViewModel(context) }
                val budgetSettingsViewModel = remember { BudgetSettingsViewModel(context) }
                val calendarViewModel = remember { CalendarViewModel(context) }
                var selectedTab by remember { mutableIntStateOf(0) }
                var showOnboardingFromSettings by remember { mutableStateOf(false) }

                if (!isOnboardingCompleted || showOnboardingFromSettings) {
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                        calendarViewModel = calendarViewModel,
                        onOnboardingComplete = { showOnboardingFromSettings = false },
                        isReEntry = showOnboardingFromSettings
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Budget") },
                                    label = { Text("Budget") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Habits") },
                                    label = { Text("Habits") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                                    label = { Text("Calendar") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") }
                                )
                            }
                        }
                    ) { innerPadding ->
                        when (selectedTab) {
                            0 -> StudyBudgetScreen(
                                viewModel = studyBudgetViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            1 -> HabitScreen(
                                viewModel = habitViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            2 -> CalendarScreen(
                                viewModel = calendarViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            3 -> BudgetSettingsScreen(
                                viewModel = budgetSettingsViewModel,
                                isOnboardingFullyCompleted = isOnboardingFullyCompleted,
                                onCompleteOnboarding = {
                                    onboardingViewModel.resetForReEntry()
                                    showOnboardingFromSettings = true
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}