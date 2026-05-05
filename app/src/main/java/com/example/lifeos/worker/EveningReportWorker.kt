package com.example.lifeos.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lifeos.data.agent.EveningReportAgent
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.HabitLog
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class EveningReportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "evening_report"
        private const val CHANNEL_NAME = "Evening Report"
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val today = LocalDate.now().toString()

            val checkIn = db.dailyCheckInDao().getCheckInForDate(today)
            if (checkIn == null) {
                sendNotification(
                    title = "Daily Check-In Reminder",
                    message = "You haven't checked in today — no evening report can be generated. Take a moment to log your wellness."
                )
                return Result.success()
            }

            val todayTransactions = db.transactionDao()
                .getAllTransactionsOnce()
                .filter { it.date == today }

            if (todayTransactions.isEmpty()) {
                return Result.success()
            }

            val startOfDay = LocalDate.now().atStartOfDay()
                .toEpochSecond(java.time.ZoneOffset.UTC) * 1000
            val endOfDay = startOfDay + 86_400_000L

            val todayLogs: List<HabitLog> = db.habitDao()
                .getLogsForToday(startOfDay, endOfDay)
                .first()

            val totalActiveHabits = db.habitDao()
                .getActiveHabits()
                .first()
                .size

            val latestAlert = db.patternAlertDao().getLatestOnce()

            val report = EveningReportAgent.generate(
                checkIn = checkIn,
                todayTransactions = todayTransactions,
                todayHabitLogs = todayLogs,
                totalActiveHabits = totalActiveHabits,
                latestAlert = latestAlert
            )

            sendNotification(title = "Your Evening Report", message = report.text)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun sendNotification(title: String, message: String) {
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()

        manager.notify(2001, notification)
    }
}