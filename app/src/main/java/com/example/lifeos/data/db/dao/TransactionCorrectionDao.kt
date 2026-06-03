package com.example.lifeos.data.db.dao

import androidx.room.*
import com.example.lifeos.data.db.entity.TransactionCorrection
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionCorrectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(correction: TransactionCorrection)

    @Query("SELECT * FROM transaction_corrections ORDER BY createdAt DESC")
    fun getAllCorrections(): Flow<List<TransactionCorrection>>

    @Query("SELECT * FROM transaction_corrections")
    suspend fun getAllCorrectionsOnce(): List<TransactionCorrection>

    @Delete
    suspend fun delete(correction: TransactionCorrection)
}