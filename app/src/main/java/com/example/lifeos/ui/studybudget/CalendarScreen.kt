package com.example.lifeos.ui.studybudget

import android.accounts.AccountManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifeos.data.db.entity.AcademicEvent

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendarState by viewModel.calendarState.collectAsState()
    val events by viewModel.academicEvents.getAllEvents().collectAsState(initial = emptyList())

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (accountName != null) {
            viewModel.syncCalendar(accountName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Academic Calendar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val intent = viewModel.getAccountPickerIntent()
                accountPickerLauncher.launch(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect Google Calendar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = calendarState) {
            is CalendarState.Idle -> {
                if (events.isEmpty()) {
                    Text(
                        text = "No events yet. Connect your Google Calendar to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is CalendarState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is CalendarState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sync successful!", fontWeight = FontWeight.Bold)
                        Text("Events imported: ${state.eventCount}")
                        Text("High pressure events: ${state.highPressureCount}")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.resetState() }) {
                    Text("Sync again")
                }
            }
            is CalendarState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = state.message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.resetState() }) {
                    Text("Try again")
                }
            }
            is CalendarState.NeedsConsent -> {
                LaunchedEffect(state) {
                    accountPickerLauncher.launch(state.intent)
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (events.isNotEmpty()) {
            Text(
                text = "Upcoming Events (${events.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(events) { event ->
                    AcademicEventItem(event = event)
                }
            }
        }
    }
}

@Composable
fun AcademicEventItem(event: AcademicEvent) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isHighPressure)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = event.startDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (event.isHighPressure) {
                Text(
                    text = "⚠ High pressure",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}