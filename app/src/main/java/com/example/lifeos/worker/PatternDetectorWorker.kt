package com.example.lifeos.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lifeos.data.agent.PatternDetectorAgent
import com.example.lifeos.data.db.AppDatabase
import com.example.lifeos.data.db.entity.PatternAlert
import java.time.LocalDate

class PatternDetectorWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)

        val checkIns = db.dailyCheckInDao()
            .getCheckInsFromOnce(LocalDate.now().minusDays(13).toString())

        val result = PatternDetectorAgent.analyze(checkIns)

        android.util.Log.d("PatternDetector", "checkIns=${checkIns.size}, hasPattern=${result.hasPattern}, type=${result.patternType}, severity=${result.severity}")

        db.patternAlertDao().insert(
            PatternAlert(
                detectedAt = System.currentTimeMillis(),
                hasPattern = result.hasPattern,
                patternType = result.patternType,
                severity = result.severity,
                description = result.description
            )
        )

        return Result.success()
    }
}