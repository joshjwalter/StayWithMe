package com.joshwalter.staywithme.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_info")
data class UserInfo(
    @PrimaryKey
    val id: Int = 1, // Single record
    val name: String = "",
    val medicalInfo: String = "",
    val notes: String = "",
    val customAlertMessage: String = ""
)