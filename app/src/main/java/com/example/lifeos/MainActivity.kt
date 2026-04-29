package com.example.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.lifeos.ui.studybudget.StudyBudgetScreen
import com.example.lifeos.ui.studybudget.StudyBudgetViewModel
import com.example.lifeos.ui.theme.LifeOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeOSTheme {
                val context = LocalContext.current
                val viewModel = remember { StudyBudgetViewModel(context) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StudyBudgetScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}