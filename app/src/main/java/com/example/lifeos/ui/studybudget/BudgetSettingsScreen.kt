package com.example.lifeos.ui.studybudget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifeos.data.db.entity.BudgetTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    viewModel: BudgetSettingsViewModel,
    isOnboardingFullyCompleted: Boolean,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgetTargets by viewModel.budgetTargets.collectAsState(initial = emptyList())
    val saveState by viewModel.saveState.collectAsState()

    var selectedCategory by remember { mutableStateOf(ALL_CATEGORIES.first()) }
    var budgetInput by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Budget Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (!isOnboardingFullyCompleted) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Setup not complete",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You skipped the initial setup. Complete it to set your name and monthly budget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCompleteOnboarding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Complete Setup")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Budget Limit")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (budgetTargets.isEmpty()) {
            Text(
                text = "No budget limits set yet. Add one to start tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Monthly Limits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(budgetTargets) { budget ->
                    BudgetTargetItem(
                        budget = budget,
                        onDelete = { viewModel.deleteBudget(budget) }
                    )
                }
            }
        }

        when (val state = saveState) {
            is SaveState.Success -> {
                LaunchedEffect(state) {
                    viewModel.resetSaveState()
                }
            }
            is SaveState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            else -> {}
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            categories = ALL_CATEGORIES,
            selectedCategory = selectedCategory,
            budgetInput = budgetInput,
            onCategorySelected = { selectedCategory = it },
            onBudgetInputChanged = { budgetInput = it },
            onConfirm = {
                val amount = budgetInput.toDoubleOrNull()
                if (amount != null) {
                    viewModel.saveBudget(selectedCategory, amount)
                    showAddDialog = false
                    budgetInput = ""
                }
            },
            onDismiss = {
                showAddDialog = false
                budgetInput = ""
            }
        )
    }
}

@Composable
fun BudgetTargetItem(
    budget: BudgetTarget,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (budget.category == "Total") "💰 Total Budget" else budget.category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Limit: ${budget.monthlyLimit} ${budget.currency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    categories: List<String>,
    selectedCategory: String,
    budgetInput: String,
    onCategorySelected: (String) -> Unit,
    onBudgetInputChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Budget Limit") },
        text = {
            Column {
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedCategory == "Total") "💰 Total Budget" else selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(if (category == "Total") "💰 Total Budget" else category) },
                                onClick = {
                                    onCategorySelected(category)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = onBudgetInputChanged,
                    label = { Text("Monthly limit (RON)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}