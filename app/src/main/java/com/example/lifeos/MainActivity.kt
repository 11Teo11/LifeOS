package com.example.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lifeos.data.habit.HabitDatabase
import com.example.lifeos.data.habit.HabitRepository
import com.example.lifeos.ui.habit.HabitScreen
import com.example.lifeos.ui.habit.HabitViewModel
import com.example.lifeos.ui.studybudget.StudyBudgetScreen
import com.example.lifeos.ui.studybudget.StudyBudgetViewModel
import com.example.lifeos.ui.theme.LifeOSTheme

class MainActivity : ComponentActivity() {

    private lateinit var habitViewModel: HabitViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = HabitDatabase.getDatabase(this)
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
                val studyBudgetViewModel = remember { StudyBudgetViewModel(context) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        StudyBudgetScreen(viewModel = studyBudgetViewModel)
                        HabitScreen(viewModel = habitViewModel)
                    }
                }
            }
        }
    }
}