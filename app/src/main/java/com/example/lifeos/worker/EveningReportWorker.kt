package com.example.lifeos.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lifeos.MainActivity
import com.example.lifeos.data.agent.EveningReportAgent
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.HabitLog
import com.example.lifeos.data.report.ReportPreferences
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class EveningReportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "evening_report"
        private const val CHANNEL_NAME = "Evening Report"
        const val EXTRA_OPEN_TAB = "open_tab"
        const val TAB_BUDGET = 0
        const val TAB_CHECKIN = 3
        const val TAB_REPORT = 5
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val today = LocalDate.now().toString()

            val checkIn = db.dailyCheckInDao().getCheckInForDate(today)
            if (checkIn == null) {
                sendNotification(
                    title = "Daily Check-In Reminder",
                    message = "You haven't checked in today — no evening report can be generated. Tap to log your wellness.",
                    targetTab = TAB_CHECKIN
                )
                return Result.success()
            }

            val todayTransactions = db.transactionDao()
                .getAllTransactionsOnce()
                .filter { it.date.startsWith(today) }

            if (todayTransactions.isEmpty()) {
                sendNotification(
                    title = "No Transactions Today",
                    message = "You haven't logged any transactions today — no evening report can be generated. Tap to add your spending.",
                    targetTab = TAB_BUDGET
                )
                return Result.success()
            }

            val startOfDay = LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
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

            ReportPreferences(applicationContext).saveReport(
                text = report.text,
                date = today
            )

            sendNotification(
                title = "Your Evening Report",
                message = report.text,
                targetTab = TAB_REPORT
            )
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun sendNotification(title: String, message: String, targetTab: Int) {
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, targetTab)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, targetTab, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(2001, notification)
    }
}