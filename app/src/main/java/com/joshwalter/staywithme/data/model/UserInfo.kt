package com.joshwalter.staywithme.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_info")
data class UserInfo(
    @PrimaryKey
    val id: Int = 1, // Single user info record
    val name: String = "",
    val medicalInfo: String = "",
    val additionalNotes: String = "",
    val customAlertMessage: String = "",
    val checkInIntervalMinutes: Int = 30 // Default 30 minutes
)
