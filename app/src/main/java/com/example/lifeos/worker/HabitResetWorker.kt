package com.example.lifeos.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class HabitResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}