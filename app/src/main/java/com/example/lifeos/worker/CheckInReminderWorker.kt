package com.example.lifeos.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lifeos.data.db.AppDatabase
import java.time.LocalDate

class CheckInReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now().toString()
        val dao = AppDatabase.getDatabase(applicationContext).dailyCheckInDao()
        if (dao.getCheckInForDate(today) == null) {
            sendNotification()
        }
        return Result.success()
    }

    private fun sendNotification() {
        val channelId = "checkin_reminder"
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Daily Check-In Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily Check-In")
            .setContentText("Don't forget to log today's check-in!")
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }
}