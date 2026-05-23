package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val targetNumber: String,
    val messageText: String,
    val status: String, // "SENT", "FAILED", "SIMULATED_SENT", "SIMULATED_FAILED"
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
