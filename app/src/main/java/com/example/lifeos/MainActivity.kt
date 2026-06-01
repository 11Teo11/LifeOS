package com.example.lifeos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.example.lifeos.data.preferences.OllamaPreferences
import com.example.lifeos.data.repository.DailyCheckInRepository
import com.example.lifeos.data.repository.DayPlanRepository
import com.example.lifeos.data.repository.HabitRepository
import com.example.lifeos.data.repository.PatternAlertRepository
import com.example.lifeos.data.repository.TransactionRepository
import com.example.lifeos.ui.checkin.CheckInScreen
import com.example.lifeos.ui.checkin.CheckInViewModel
import com.example.lifeos.ui.habit.DayPlanViewModel
import com.example.lifeos.ui.habit.HabitScreen
import com.example.lifeos.ui.habit.HabitViewModel
import com.example.lifeos.ui.onboarding.OnboardingScreen
import com.example.lifeos.ui.onboarding.OnboardingViewModel
import com.example.lifeos.ui.report.ReportScreen
import com.example.lifeos.ui.report.ReportViewModel
import com.example.lifeos.ui.studybudget.BudgetSettingsScreen
import com.example.lifeos.ui.studybudget.BudgetSettingsViewModel
import com.example.lifeos.ui.studybudget.CalendarScreen
import com.example.lifeos.ui.studybudget.CalendarViewModel
import com.example.lifeos.ui.studybudget.StudyBudgetScreen
import com.example.lifeos.ui.studybudget.StudyBudgetViewModel
import com.example.lifeos.ui.theme.LifeOSTheme
import com.example.lifeos.util.NotificationHelper
import com.example.lifeos.worker.BudgetCheckWorker
import com.example.lifeos.worker.CheckInReminderWorker
import com.example.lifeos.worker.EveningReportWorker
import com.example.lifeos.worker.HabitResetWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var habitViewModel: HabitViewModel
    private lateinit var dayPlanViewModel: DayPlanViewModel
    private lateinit var checkInViewModel: CheckInViewModel
    private val tabToOpen = mutableStateOf(0)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tab = intent.getIntExtra(EveningReportWorker.EXTRA_OPEN_TAB, 0)
        if (tab != 0) tabToOpen.value = tab
    }

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
            "habit_reset", ExistingPeriodicWorkPolicy.KEEP, resetRequest
        )
    }

    private fun scheduleBudgetCheck() {
        val budgetCheckRequest = PeriodicWorkRequestBuilder<BudgetCheckWorker>(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "budget_check", ExistingPeriodicWorkPolicy.KEEP, budgetCheckRequest
        )
    }

    private fun scheduleCheckInReminder() {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis

        val reminderRequest = PeriodicWorkRequestBuilder<CheckInReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "checkin_reminder", ExistingPeriodicWorkPolicy.KEEP, reminderRequest
        )
    }

    private fun scheduleEveningReport() {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis

        val reportRequest = PeriodicWorkRequestBuilder<EveningReportWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "evening_report", ExistingPeriodicWorkPolicy.KEEP, reportRequest
        )
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleHabitReset()
        NotificationHelper.createNotificationChannel(this)
        scheduleBudgetCheck()
        scheduleCheckInReminder()
        scheduleEveningReport()
        requestNotificationPermission()

        val tab = intent?.getIntExtra(EveningReportWorker.EXTRA_OPEN_TAB, 0) ?: 0
        if (tab != 0) tabToOpen.value = tab

        val database = AppDatabase.getDatabase(this)

        val repository = HabitRepository(database.habitDao())
        habitViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HabitViewModel(repository) as T
            }
        })[HabitViewModel::class.java]

        val checkInRepository = DailyCheckInRepository(database.dailyCheckInDao())
        val patternAlertRepository = PatternAlertRepository(database.patternAlertDao())
        val transactionRepository = TransactionRepository(database.transactionDao())
        val dayPlanRepository = DayPlanRepository(database.dayPlanDao())
        val ollamaPreferences = OllamaPreferences(applicationContext)
        dayPlanViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DayPlanViewModel(
                    checkInDao = database.dailyCheckInDao(),
                    patternAlertRepository = patternAlertRepository,
                    transactionRepository = transactionRepository,
                    academicEventDao = database.academicEventDao(),
                    habitRepository = repository,
                    dayPlanRepository = dayPlanRepository,
                    ollamaPreferences = ollamaPreferences
                ) as T
            }
        })[DayPlanViewModel::class.java]
        checkInViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CheckInViewModel(checkInRepository, patternAlertRepository, applicationContext) as T
            }
        })[CheckInViewModel::class.java]

        setContent {
            LifeOSTheme {
                val context = LocalContext.current
                val onboardingViewModel = remember { OnboardingViewModel(context) }
                val isOnboardingCompleted by onboardingViewModel.isCompleted.collectAsState()
                val isOnboardingFullyCompleted by onboardingViewModel.isFullyCompleted.collectAsState()
                val studyBudgetViewModel = remember { StudyBudgetViewModel(context) }
                val budgetSettingsViewModel = remember { BudgetSettingsViewModel(context) }
                val calendarViewModel = remember { CalendarViewModel(context) }
                val reportViewModel = remember { ReportViewModel(context) }
                var selectedTab by remember { mutableIntStateOf(tabToOpen.value) }

                LaunchedEffect(tabToOpen.value) {
                    selectedTab = tabToOpen.value
                }

                if (!isOnboardingCompleted) {
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                        calendarViewModel = calendarViewModel,
                        onOnboardingComplete = { },
                        isReEntry = false
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
                                    icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = "Check-In") },
                                    label = { Text("Check-In") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 4,
                                    onClick = { selectedTab = 4 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 5,
                                    onClick = { selectedTab = 5 },
                                    icon = { Icon(Icons.Default.Description, contentDescription = "Report") },
                                    label = { Text("Report") }
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
                                dayPlanViewModel = dayPlanViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            2 -> CalendarScreen(
                                viewModel = calendarViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            3 -> CheckInScreen(
                                viewModel = checkInViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            4 -> BudgetSettingsScreen(
                                viewModel = budgetSettingsViewModel,
                                isOnboardingFullyCompleted = isOnboardingFullyCompleted,
                                onCompleteOnboarding = {
                                    onboardingViewModel.resetForReEntry()
                                    calendarViewModel.resetState()
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                            5 -> ReportScreen(
                                viewModel = reportViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}