package com.example.lifeos.ui.studybudget

import android.accounts.AccountManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeos.data.db.entity.AcademicEvent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val calendarState by viewModel.calendarState.collectAsState()
    val events by viewModel.academicEvents.getAllEvents().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<AcademicEvent?>(null) }
    var showCalendarView by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (accountName != null) viewModel.syncCalendar(accountName)
    }

    val eventsByDate = remember(events) {
        events.groupBy { event ->
            try {
                LocalDate.parse(event.startDate.substring(0, 10), DateTimeFormatter.ISO_DATE)
            } catch (e: Exception) { null }
        }.filterKeys { it != null }.mapKeys { it.key!! }
    }

    val selectedDayEvents = remember(selectedDay, events) {
        if (selectedDay == null) emptyList()
        else eventsByDate[selectedDay] ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Academic Calendar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showCalendarView = !showCalendarView }) {
                Icon(
                    imageVector = if (showCalendarView) Icons.Default.List else Icons.Default.CalendarMonth,
                    contentDescription = if (showCalendarView) "Switch to list" else "Switch to calendar"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { accountPickerLauncher.launch(viewModel.getAccountPickerIntent()) },
                modifier = Modifier.weight(1f)
            ) { Text("Sync Google") }
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f)
            ) { Text("Add manually") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = calendarState) {
            is CalendarState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is CalendarState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Sync successful!", fontWeight = FontWeight.Bold)
                        Text("Imported: ${state.eventCount} | High: ${state.highPressureCount} | Medium: ${state.mediumPressureCount}")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { viewModel.resetState() }) { Text("Sync again") }
            }
            is CalendarState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(state.message, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
                TextButton(onClick = { viewModel.resetState() }) { Text("Try again") }
            }
            is CalendarState.NeedsConsent -> {
                LaunchedEffect(state) { accountPickerLauncher.launch(state.intent) }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showCalendarView) {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Text("< Prev")
                }
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Text("Next >")
                }
            }

            // Days of week header
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calendar grid
            CalendarGrid(
                yearMonth = currentMonth,
                eventsByDate = eventsByDate,
                selectedDay = selectedDay,
                onDayClick = { day ->
                    selectedDay = if (selectedDay == day) null else day
                }
            )

            // Selected day events
            if (selectedDay != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedDay!!.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (selectedDayEvents.isEmpty()) {
                    Text(
                        text = "No events on this day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    selectedDayEvents.forEach { event ->
                        AcademicEventItem(
                            event = event,
                            onEdit = { eventToEdit = event },
                            onDelete = { viewModel.deleteEvent(event) }
                        )
                    }
                }
            }

        } else {
            // List View
            if (events.isEmpty()) {
                Text(
                    text = "No events yet. Sync Google Calendar or add manually.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Upcoming Events (${events.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(events) { event ->
                        AcademicEventItem(
                            event = event,
                            onEdit = { eventToEdit = event },
                            onDelete = { viewModel.deleteEvent(event) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, startDate, endDate, pressureLevel ->
                viewModel.addManualEvent(title, startDate, endDate, pressureLevel)
                showAddDialog = false
            }
        )
    }

    eventToEdit?.let { event ->
        EditEventDialog(
            event = event,
            onDismiss = { eventToEdit = null },
            onConfirm = { title, startDate, pressureLevel ->
                viewModel.updateEvent(event, title, startDate, pressureLevel)
                eventToEdit = null
            },
            onDelete = {
                viewModel.deleteEvent(event)
                eventToEdit = null
            }
        )
    }
}

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    eventsByDate: Map<LocalDate, List<AcademicEvent>>,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDay = yearMonth.atDay(1)
    val lastDay = yearMonth.atEndOfMonth()
    // Monday = 1, so offset for first day
    val firstDayOfWeek = firstDay.dayOfWeek.value - 1

    val days = mutableListOf<LocalDate?>()
    repeat(firstDayOfWeek) { days.add(null) }
    var current = firstDay
    while (!current.isAfter(lastDay)) {
        days.add(current)
        current = current.plusDays(1)
    }
    // Fill to complete last row
    while (days.size % 7 != 0) days.add(null)

    val weeks = days.chunked(7)

    Column {
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    day == null -> Color.Transparent
                                    day == selectedDay -> MaterialTheme.colorScheme.primary
                                    day == LocalDate.now() -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                }
                            )
                            .clickable(enabled = day != null) {
                                day?.let { onDayClick(it) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val dayEvents = eventsByDate[day] ?: emptyList()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    fontSize = 12.sp,
                                    color = if (day == selectedDay)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
                                )
                                if (dayEvents.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                        dayEvents.take(3).forEach { event ->
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (event.pressureLevel) {
                                                            "high" -> Color(0xFFB71C1C)
                                                            "medium" -> Color(0xFFF57F17)
                                                            else -> Color(0xFF2E7D32)
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AcademicEventItem(
    event: AcademicEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = when (event.pressureLevel) {
        "high" -> Color(0xFFFFCDD2)
        "medium" -> Color(0xFFFFF9C4)
        else -> Color(0xFFE8F5E9)
    }
    val pressureLabel = when (event.pressureLevel) {
        "high" -> "⚠ High pressure"
        "medium" -> "● Medium pressure"
        else -> "✓ Low pressure"
    }
    val pressureColor = when (event.pressureLevel) {
        "high" -> Color(0xFFB71C1C)
        "medium" -> Color(0xFFF57F17)
        else -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(text = formatEventDate(event.startDate), style = MaterialTheme.typography.bodySmall, color = Color(0xFF555555))
                Text(text = pressureLabel, style = MaterialTheme.typography.labelSmall, color = pressureColor)
                if (event.isManuallyAdded) {
                    Text(text = "✎ Manually added", style = MaterialTheme.typography.labelSmall, color = Color(0xFF555555))
                }
            }
            TextButton(onClick = onEdit) {
                Text("Edit", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun EditEventDialog(
    event: AcademicEvent,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var pressureLevel by remember { mutableStateOf(event.pressureLevel) }

    val existingDate = try {
        val parts = formatEventDate(event.startDate).split("/", ",", " ").filter { it.isNotBlank() }
        Triple(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" })
    } catch (e: Exception) { Triple("", "", java.time.LocalDate.now().year.toString()) }

    var day by remember { mutableStateOf(existingDate.first) }
    var month by remember { mutableStateOf(existingDate.second) }
    var year by remember { mutableStateOf(existingDate.third) }
    var hour by remember { mutableStateOf("") }
    var minute by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete event?") },
            text = { Text("Are you sure you want to delete \"${event.title}\"?") },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event title") }, modifier = Modifier.fillMaxWidth())
                Text("Date:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = day, onValueChange = { if (it.length <= 2) day = it }, label = { Text("DD") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = month, onValueChange = { if (it.length <= 2) month = it }, label = { Text("MM") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = year, onValueChange = { if (it.length <= 4) year = it }, label = { Text("YYYY") }, modifier = Modifier.weight(1.2f))
                }
                Text("Time (optional):", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = hour, onValueChange = { if (it.length <= 2) hour = it }, label = { Text("HH") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minute, onValueChange = { if (it.length <= 2) minute = it }, label = { Text("MM") }, modifier = Modifier.weight(1f))
                }
                Text("Pressure level:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach { level ->
                        FilterChip(selected = pressureLevel == level, onClick = { pressureLevel = level }, label = { Text(level.replaceFirstChar { it.uppercase() }) })
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete event", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && day.isNotBlank() && month.isNotBlank()) {
                    val h = hour.padStart(2, '0').ifBlank { "00" }
                    val m = minute.padStart(2, '0').ifBlank { "00" }
                    val dateString = "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}T$h:$m:00"
                    onConfirm(title, dateString, pressureLevel)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf(java.time.LocalDate.now().year.toString()) }
    var hour by remember { mutableStateOf("") }
    var minute by remember { mutableStateOf("") }
    var pressureLevel by remember { mutableStateOf("low") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Academic Event") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event title") }, modifier = Modifier.fillMaxWidth())
                Text("Date:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = day, onValueChange = { if (it.length <= 2) day = it }, label = { Text("DD") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = month, onValueChange = { if (it.length <= 2) month = it }, label = { Text("MM") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = year, onValueChange = { if (it.length <= 4) year = it }, label = { Text("YYYY") }, modifier = Modifier.weight(1.2f))
                }
                Text("Time (optional):", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = hour, onValueChange = { if (it.length <= 2) hour = it }, label = { Text("HH") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minute, onValueChange = { if (it.length <= 2) minute = it }, label = { Text("MM") }, modifier = Modifier.weight(1f))
                }
                Text("Pressure level:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach { level ->
                        FilterChip(selected = pressureLevel == level, onClick = { pressureLevel = level }, label = { Text(level.replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && day.isNotBlank() && month.isNotBlank()) {
                    val h = hour.padStart(2, '0').ifBlank { "00" }
                    val m = minute.padStart(2, '0').ifBlank { "00" }
                    val dateString = "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}T$h:$m:00"
                    onConfirm(title, dateString, dateString, pressureLevel)
                }
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun formatEventDate(dateString: String): String {
    return try {
        val inputFormats = listOf(
            java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            java.time.format.DateTimeFormatter.ISO_DATE_TIME,
            java.time.format.DateTimeFormatter.ISO_DATE
        )
        var result: String? = null
        for (format in inputFormats) {
            try {
                val date = java.time.OffsetDateTime.parse(dateString, format)
                result = date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm"))
                break
            } catch (e: Exception) {
                try {
                    val date = java.time.LocalDateTime.parse(dateString, format)
                    result = date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm"))
                    break
                } catch (e2: Exception) {
                    try {
                        val date = java.time.LocalDate.parse(dateString, format)
                        result = date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        break
                    } catch (e3: Exception) { continue }
                }
            }
        }
        result ?: dateString
    } catch (e: Exception) {
        dateString
    }
}