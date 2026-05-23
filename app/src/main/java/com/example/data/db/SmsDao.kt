package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SmsLog
import com.example.data.model.SmsTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_templates ORDER BY timestamp DESC")
    fun getAllTemplates(): Flow<List<SmsTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: SmsTemplate)

    @Query("DELETE FROM sms_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Int)

    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<SmsLog>>

    @Query("SELECT COUNT(*) FROM sms_logs WHERE timestamp >= :sinceTime AND status IN ('SENT', 'SIMULATED_SENT')")
    suspend fun getSentCountSince(sinceTime: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SmsLog)

    @Query("DELETE FROM sms_logs")
    suspend fun clearLogs()

    @Query("DELETE FROM sms_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)
}
