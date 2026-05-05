package com.example.lifeos.ui.checkin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeos.data.db.entity.DailyCheckIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val EnergyTeal  = Color(0xFF009688)
private val SleepPurple = Color(0xFF7C4DFF)
private val WarningRed  = Color(0xFFF44336)
private val GridColour  = Color(0x22000000)

private val SYMPTOMS = listOf("fatigue", "headache", "anxiety", "low_focus")
private val SYMPTOM_LABELS = mapOf(
    "fatigue"   to "Fatigue",
    "headache"  to "Headache",
    "anxiety"   to "Anxiety",
    "low_focus" to "Low Focus"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel,
    modifier: Modifier = Modifier
) {
    val uiState  by viewModel.uiState.collectAsState()
    val history  by viewModel.history.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Text("Wellness Trends", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            if (chartData.size >= 3) {
                WellnessTrendsCard(data = chartData)
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Complete at least 3 check-ins to see your trend chart.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("Today's Check-In", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.selectedDate.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                }
            }
        }

        if (uiState.duplicateError) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "A check-in for ${uiState.selectedDate} already exists. Pick a different date.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
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
            items(history) { entry -> CheckInHistoryCard(entry) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.setDate(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun WellnessTrendsCard(data: List<DailyCheckIn>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(colour = EnergyTeal,  label = "Energy (1–10)")
                LegendDot(colour = SleepPurple, label = "Sleep (hours, 0–12)")
                LegendDot(colour = WarningRed,  label = "Low energy (<4)")
            }
            Spacer(Modifier.height(8.dp))
            WellnessLineChart(
                data = data,
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )
        }
    }
}

@Composable
private fun LegendDot(colour: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).background(colour, shape = CircleShape))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WellnessLineChart(data: List<DailyCheckIn>, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val axisLabelPaint = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(122, 96, 128)
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
    }
    val xLabelPaint = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(122, 96, 128)
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    Canvas(modifier = modifier) {
        val leftPad   = 40.dp.toPx(); val bottomPad = 28.dp.toPx()
        val topPad    = 8.dp.toPx();  val rightPad  = 8.dp.toPx()
        val plotLeft  = leftPad;      val plotRight  = size.width - rightPad
        val plotTop   = topPad;       val plotBottom = size.height - bottomPad
        val plotW     = plotRight - plotLeft; val plotH = plotBottom - plotTop
        val maxY      = 12f
        listOf(3f, 6f, 9f, 12f).forEach { v ->
            val y = plotBottom - (v / maxY) * plotH
            drawLine(color = GridColour, start = Offset(plotLeft, y), end = Offset(plotRight, y), strokeWidth = 1.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(v.toInt().toString(), plotLeft - 4.dp.toPx(), y + 4.dp.toPx(), axisLabelPaint)
        }
        if (data.isEmpty()) return@Canvas
        val n = data.size
        val spacing = if (n > 1) plotW / (n - 1) else plotW / 2f
        fun xFor(i: Int)     = if (n == 1) plotLeft + plotW / 2 else plotLeft + i * spacing
        fun energyY(v: Int)  = plotBottom - (v.toFloat() / maxY) * plotH
        fun sleepY(v: Float) = plotBottom - (v / maxY) * plotH
        for (i in 0 until n - 1) {
            drawLine(color = SleepPurple, start = Offset(xFor(i), sleepY(data[i].sleepHours)), end = Offset(xFor(i+1), sleepY(data[i+1].sleepHours)), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        for (i in 0 until n - 1) {
            drawLine(color = EnergyTeal, start = Offset(xFor(i), energyY(data[i].energyLevel)), end = Offset(xFor(i+1), energyY(data[i+1].energyLevel)), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        data.forEachIndexed { i, entry ->
            val x = xFor(i)
            drawCircle(color = SleepPurple, radius = 4.dp.toPx(), center = Offset(x, sleepY(entry.sleepHours)))
            val energyColour = if (entry.energyLevel < 4) WarningRed else EnergyTeal
            if (entry.energyLevel < 4) drawCircle(color = WarningRed.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = Offset(x, energyY(entry.energyLevel)))
            drawCircle(color = energyColour, radius = 5.dp.toPx(), center = Offset(x, energyY(entry.energyLevel)))
            val dayLabel = try { LocalDate.parse(entry.date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) } catch (e: Exception) { entry.date.takeLast(2) }
            drawContext.canvas.nativeCanvas.drawText(dayLabel, x, size.height - 2.dp.toPx(), xLabelPaint)
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, displayValue: String, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(displayValue, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.fillMaxWidth())
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
                val labels = entry.symptoms.split(",").mapNotNull { SYMPTOM_LABELS[it.trim()] }.joinToString(", ")
                Text("Symptoms: $labels", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}