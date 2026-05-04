package com.example.lifeos.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifeos.data.db.entity.DailyCheckIn
import kotlin.math.roundToInt

private val SYMPTOMS = listOf("fatigue", "headache", "anxiety", "low_focus")
private val SYMPTOM_LABELS = mapOf(
    "fatigue" to "Fatigue",
    "headache" to "Headache",
    "anxiety" to "Anxiety",
    "low_focus" to "Low Focus"
)

@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.history.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Today's Check-In", style = MaterialTheme.typography.headlineSmall)
        }

        if (uiState.saved) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        "Check-in saved!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            item {
                SliderRow(
                    label = "Sleep",
                    value = uiState.sleepHours,
                    valueRange = 0f..12f,
                    displayValue = "%.1fh".format(uiState.sleepHours),
                    onValueChange = { viewModel.setSleepHours((it * 2).roundToInt() / 2f) }
                )
            }
            item {
                SliderRow(
                    label = "Energy",
                    value = uiState.energyLevel.toFloat(),
                    valueRange = 1f..10f,
                    displayValue = "${uiState.energyLevel}/10",
                    onValueChange = { viewModel.setEnergyLevel(it.roundToInt()) }
                )
            }
            item {
                SliderRow(
                    label = "Stress",
                    value = uiState.stressLevel.toFloat(),
                    valueRange = 1f..10f,
                    displayValue = "${uiState.stressLevel}/10",
                    onValueChange = { viewModel.setStressLevel(it.roundToInt()) }
                )
            }
            item {
                Text("Symptoms", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Column {
                    SYMPTOMS.forEach { symptom ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = symptom in uiState.selectedSymptoms,
                                onCheckedChange = { viewModel.toggleSymptom(symptom) }
                            )
                            Text(SYMPTOM_LABELS[symptom] ?: symptom)
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { viewModel.saveCheckIn() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Last 14 Days", style = MaterialTheme.typography.titleMedium)
            }
            items(history) { entry ->
                CheckInHistoryCard(entry)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(displayValue, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CheckInHistoryCard(entry: DailyCheckIn) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(entry.date, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Sleep: %.1fh".format(entry.sleepHours))
                Text("Energy: ${entry.energyLevel}/10")
                Text("Stress: ${entry.stressLevel}/10")
            }
            if (entry.symptoms.isNotBlank()) {
                val labels = entry.symptoms.split(",")
                    .mapNotNull { SYMPTOM_LABELS[it.trim()] }
                    .joinToString(", ")
                Text("Symptoms: $labels", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}