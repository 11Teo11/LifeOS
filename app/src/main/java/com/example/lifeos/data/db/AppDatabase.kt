package com.example.lifeos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.lifeos.data.db.dao.AcademicEventDao
import com.example.lifeos.data.db.dao.BudgetTargetDao
import com.example.lifeos.data.db.dao.TransactionDao
import com.example.lifeos.data.db.entity.Transaction
import com.example.lifeos.data.db.entity.Habit
import com.example.lifeos.data.db.dao.HabitDao
import com.example.lifeos.data.db.dao.TransactionCorrectionDao
import com.example.lifeos.data.db.entity.HabitLog
import com.example.lifeos.data.db.entity.BudgetTarget
import com.example.lifeos.data.db.entity.DailyCheckIn
import com.example.lifeos.data.db.dao.DailyCheckInDao
import com.example.lifeos.data.db.entity.AcademicEvent
import com.example.lifeos.data.db.entity.TransactionCorrection
import com.example.lifeos.data.db.entity.AgentInsight
import com.example.lifeos.data.db.dao.AgentInsightDao

@Database(
    entities = [
        Transaction::class,
        BudgetTarget::class,
        Habit::class,
        HabitLog::class,
        DailyCheckIn::class,
        AcademicEvent::class,
        TransactionCorrection::class,
        AgentInsight::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun habitDao(): HabitDao
    abstract fun budgetTargetDao(): BudgetTargetDao
    abstract fun academicEventDao(): AcademicEventDao
    abstract fun dailyCheckInDao(): DailyCheckInDao

    abstract fun transactionCorrectionDao(): TransactionCorrectionDao
    abstract fun agentInsightDao(): AgentInsightDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifeos_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}