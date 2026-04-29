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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifeos.data.db.entity.Transaction

@Composable
fun StudyBudgetScreen(viewModel: StudyBudgetViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val importState by viewModel.importState.collectAsState()
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "StudyBudget",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { filePickerLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Import Revolut CSV")
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        if (transactions.isNotEmpty()) {
            Text(
                text = "Transactions (${transactions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(transactions) { transaction ->
                    TransactionItem(transaction = transaction)
                }
            }
        }
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
                TransactionItem(transaction = transaction)
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
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
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
            }
            Text(
                text = "${transaction.amount} ${transaction.currency}",
                fontWeight = FontWeight.Bold,
                color = if (transaction.amount < 0)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}