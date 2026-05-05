package com.example.lifeos.data.repository

import com.example.lifeos.data.db.dao.PatternAlertDao
import com.example.lifeos.data.db.entity.PatternAlert
import kotlinx.coroutines.flow.Flow

class PatternAlertRepository(private val dao: PatternAlertDao) {

    suspend fun insert(alert: PatternAlert) = dao.insert(alert)

    fun getLatest(): Flow<PatternAlert?> = dao.getLatest()
}