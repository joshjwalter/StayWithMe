package com.joshwalter.staywithme.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val notificationType: String, // "gentle", "urgent", "emergency", "sms", "call"
    val timestamp: Date,
    val success: Boolean,
    val contactId: Long? = null,
    val message: String? = null
)