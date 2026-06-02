package com.example.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.lifeos.data.db.entity.DayPlan
import com.example.lifeos.data.db.entity.DayPlanSuggestion
import kotlinx.coroutines.flow.Flow

@Dao
interface DayPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: DayPlan)

    @Insert
    suspend fun insertSuggestions(suggestions: List<DayPlanSuggestion>)

    @Update
    suspend fun updateSuggestion(suggestion: DayPlanSuggestion)

    @Query("DELETE FROM day_plan_suggestions WHERE planTargetDate = :targetDate")
    suspend fun deleteSuggestionsForPlan(targetDate: String)

    @Query("DELETE FROM day_plans WHERE targetDate = :targetDate")
    suspend fun deletePlan(targetDate: String)

    @Query("DELETE FROM day_plans WHERE targetDate < :keepFromDate")
    suspend fun deletePlansOlderThan(keepFromDate: String)

    @Query("SELECT * FROM day_plans WHERE targetDate = :targetDate LIMIT 1")
    suspend fun getPlanForDate(targetDate: String): DayPlan?

    @Query("SELECT * FROM day_plans WHERE targetDate = :targetDate LIMIT 1")
    fun observePlanForDate(targetDate: String): Flow<DayPlan?>

    @Query("SELECT * FROM day_plan_suggestions WHERE planTargetDate = :targetDate ORDER BY orderIndex ASC")
    suspend fun getSuggestionsForDate(targetDate: String): List<DayPlanSuggestion>

    @Query("SELECT * FROM day_plan_suggestions WHERE planTargetDate = :targetDate ORDER BY orderIndex ASC")
    fun observeSuggestionsForDate(targetDate: String): Flow<List<DayPlanSuggestion>>

    @Transaction
    suspend fun replacePlan(plan: DayPlan, suggestions: List<DayPlanSuggestion>) {
        deleteSuggestionsForPlan(plan.targetDate)
        deletePlan(plan.targetDate)
        insertPlan(plan)
        insertSuggestions(suggestions)
    }
}
