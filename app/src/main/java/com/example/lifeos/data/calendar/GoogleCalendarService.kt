package com.example.lifeos.data.calendar

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class GoogleCalendarService(private val context: Context) {

    private val HIGH_PRESSURE_KEYWORDS = listOf(
        "exam", "examen", "test", "deadline", "colocviu",
        "laborator", "seminar", "proiect", "prezentare"
    )

    fun buildCalendarService(accountName: String): Calendar {
        val credential = GoogleAccountCredential
            .usingOAuth2(context, listOf(CalendarScopes.CALENDAR_READONLY))
        credential.selectedAccountName = accountName

        return Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("LifeOS")
            .build()
    }

    fun fetchEvents(accountName: String): List<AcademicEventData> {
        val service = buildCalendarService(accountName)

        val now = Date()
        val future = Date(System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000)

        val events = service.events().list("primary")
            .setTimeMin(com.google.api.client.util.DateTime(now))
            .setTimeMax(com.google.api.client.util.DateTime(future))
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute()

        return events.items.map { event ->
            AcademicEventData(
                googleEventId = event.id,
                title = event.summary ?: "No title",
                startDate = event.start.dateTime?.toString() ?: event.start.date.toString(),
                endDate = event.end.dateTime?.toString() ?: event.end.date.toString(),
                isHighPressure = isHighPressure(event)
            )
        }
    }

    private fun isHighPressure(event: Event): Boolean {
        val title = event.summary?.lowercase() ?: return false
        val description = event.description?.lowercase() ?: ""
        return HIGH_PRESSURE_KEYWORDS.any { keyword ->
            title.contains(keyword) || description.contains(keyword)
        }
    }
}

data class AcademicEventData(
    val googleEventId: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val isHighPressure: Boolean
)