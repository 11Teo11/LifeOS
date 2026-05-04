package com.example.lifeos.ui.habit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifeos.data.db.entity.Habit

@Composable
fun HabitScreen(viewModel: HabitViewModel, modifier: Modifier = Modifier) {
    val habits by viewModel.habits.collectAsState()
    val completedHabitIds by viewModel.completedHabitIds.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(bottom = 64.dp)
            ) {
                Text("+")
            }
        }
    ){ padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "My Habits",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (habits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No habits yet. Press + to add one.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(habits) { habit ->
                        HabitItem(
                            habit = habit,
                            isChecked = habit.id in completedHabitIds,
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
fun HabitItem(habit: Habit, isChecked: Boolean, onCheck: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isChecked, onCheckedChange = { if (!isChecked) onCheck() })
        Text(
            text = habit.name,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        TextButton(onClick = onDelete) {
            Text("Delete")
        }
    }
}

@Composable
fun AddHabitDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Habit") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(name, "daily", "#FF6B9D")
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}