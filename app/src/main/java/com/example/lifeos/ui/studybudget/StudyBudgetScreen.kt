package com.example.lifeos.ui.studybudget

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifeos.data.db.entity.BudgetTarget
import com.example.lifeos.data.db.entity.Transaction

@Composable
fun StudyBudgetScreen(viewModel: StudyBudgetViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val importState by viewModel.importState.collectAsState()
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val allBudgetTargets by viewModel.allBudgetTargets.collectAsState(initial = emptyList())
    val spentPerCategory by viewModel.spentPerCategory.collectAsState(initial = emptyMap())
    var transactionToCorrect by remember { mutableStateOf<Transaction?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            val inputStream = context.contentResolver.openInputStream(it)
            inputStream?.let { stream -> viewModel.previewCsv(stream) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "StudyBudget",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            if (allBudgetTargets.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No budgets set. Go to Settings to add one.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                allBudgetTargets.forEach { target ->
                    val spent = spentPerCategory[target.category] ?: 0.0
                    CategoryBudgetCard(target = target, spent = spent)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import Revolut CSV")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            when (val state = importState) {
                is ImportState.Idle -> {
                    if (transactions.isEmpty()) {
                        Text(
                            text = "No transactions yet. Import a Revolut CSV to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is ImportState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ImportState.Preview -> {
                    PreviewSection(
                        result = state.result,
                        onConfirm = {
                            selectedUri?.let {
                                val inputStream = context.contentResolver.openInputStream(it)
                                inputStream?.let { stream -> viewModel.confirmImport(stream) }
                            }
                        },
                        onCancel = { viewModel.resetState() }
                    )
                }
                is ImportState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Import successful!", fontWeight = FontWeight.Bold)
                            Text("Imported: ${state.result.imported} transactions")
                            Text("Skipped (duplicates): ${state.result.skipped}")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.resetState() }) {
                        Text("Import another file")
                    }
                }
                is ImportState.Error -> {
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
            }
        }

        if (transactions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Transactions (${transactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(transactions) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onCorrect = { transactionToCorrect = transaction }
                )
            }
        }
    }

    transactionToCorrect?.let { transaction ->
        CorrectCategoryDialog(
            transaction = transaction,
            onDismiss = { transactionToCorrect = null },
            onConfirm = { newCategory ->
                viewModel.correctCategory(transaction, newCategory)
                transactionToCorrect = null
            }
        )
    }
}

@Composable
fun PreviewSection(
    result: ImportResult,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Preview", fontWeight = FontWeight.Bold)
            Text("Found ${result.imported} transactions. First 5:")
            Spacer(modifier = Modifier.height(8.dp))
            result.preview.forEach { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onCorrect = {},
                    showFixButton = false
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = onConfirm) { Text("Import all") }
            }
        }
    }
}

@Composable
fun CategoryBudgetCard(target: BudgetTarget, spent: Double) {
    val progress = (spent / target.monthlyLimit).coerceIn(0.0, 1.0).toFloat()
    val remaining = target.monthlyLimit - spent
    val isOverBudget = remaining < 0

    val progressColor = when {
        progress >= 1.0f -> MaterialTheme.colorScheme.error
        progress >= 0.8f -> Color(0xFFF9A825)
        else -> Color(0xFF2E7D32)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = target.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${"%.2f".format(spent)} / ${"%.2f".format(target.monthlyLimit)} RON",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isOverBudget)
                    "${"%.2f".format(-remaining)} RON over budget"
                else
                    "${"%.2f".format(remaining)} RON remaining",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOverBudget) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onCorrect: () -> Unit,
    showFixButton: Boolean = true
) {
    val categoryColor = when {
        transaction.category == "uncategorized" -> MaterialTheme.colorScheme.onSurfaceVariant
        transaction.isManuallyCorrected -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.primary
    }

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = transaction.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (transaction.isManuallyCorrected)
                        "${transaction.category} ✓"
                    else
                        transaction.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = categoryColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${transaction.amount} ${transaction.currency}",
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.amount < 0)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
                if (showFixButton) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onCorrect,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Fix category",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CorrectCategoryDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(transaction.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fix category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                DEFAULT_CATEGORIES.forEach { category ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                        Text(
                            text = category,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedCategory) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}