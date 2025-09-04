package com.joshwalter.staywithme.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "checkin_sessions")
data class CheckInSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Date,
    val durationMinutes: Int,
    val isActive: Boolean = true,
    val lastCheckIn: Date? = null,
    val notificationLevel: Int = 0, // 0=none, 1=gentle, 2=urgent, 3=emergency, 4=sms/call
    val location: String? = null,
    val substances: String? = null,
    val notes: String? = null
)