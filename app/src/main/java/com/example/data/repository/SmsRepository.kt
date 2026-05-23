package com.example.data.repository

import com.example.data.db.SmsDao
import com.example.data.model.SmsLog
import com.example.data.model.SmsTemplate
import kotlinx.coroutines.flow.Flow

class SmsRepository(private val smsDao: SmsDao) {
    val allTemplates: Flow<List<SmsTemplate>> = smsDao.getAllTemplates()
    val allLogs: Flow<List<SmsLog>> = smsDao.getAllLogs()

    suspend fun insertTemplate(template: SmsTemplate) {
        smsDao.insertTemplate(template)
    }

    suspend fun deleteTemplateById(id: Int) {
        smsDao.deleteTemplateById(id)
    }

    suspend fun insertLog(log: SmsLog) {
        smsDao.insertLog(log)
    }

    suspend fun getSentCountSince(sinceTime: Long): Int {
        return smsDao.getSentCountSince(sinceTime)
    }

    suspend fun clearLogs() {
        smsDao.clearLogs()
    }

    suspend fun deleteLogById(id: Int) {
        smsDao.deleteLogById(id)
    }
}
