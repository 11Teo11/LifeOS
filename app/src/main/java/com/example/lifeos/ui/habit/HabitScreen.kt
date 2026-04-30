package com.example.lifeos.ui.habit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifeos.data.habit.Habit

@Composable
fun HabitScreen(viewModel: HabitViewModel, modifier: Modifier = Modifier) {
    val habits by viewModel.habits.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "Obiceiurile mele",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (habits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nu ai obiceiuri încă. Apasă + pentru a adăuga.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(habits) { habit ->
                        HabitItem(
                            habit = habit,
                            onCheck = { viewModel.logHabitDone(habit.id) },
                            onDelete = { viewModel.deleteHabit(habit) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, frequency, color ->
                viewModel.addHabit(name, frequency, color)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun HabitItem(habit: Habit, onCheck: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = false, onCheckedChange = { onCheck() })
        Text(
            text = habit.name,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        TextButton(onClick = onDelete) {
            Text("Șterge")
        }
    }
}

@Composable
fun AddHabitDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Habit nou") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nume obicei") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(name, "daily", "#FF6B9D")
            }) { Text("Adaugă") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anulează") }
        }
    )
}