package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_templates")
data class SmsTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetNumber: String,
    val messagePattern: String,
    val repeatCount: Int,
    val delayMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
