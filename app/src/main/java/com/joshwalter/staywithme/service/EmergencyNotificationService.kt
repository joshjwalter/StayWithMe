package com.joshwalter.staywithme.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.joshwalter.staywithme.MainActivity
import com.joshwalter.staywithme.R
import com.joshwalter.staywithme.data.database.StayWithMeDatabase
import com.joshwalter.staywithme.data.model.EmergencyContact
import com.joshwalter.staywithme.data.model.NotificationLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class EmergencyNotificationService(private val context: Context) {
    
    private val database = StayWithMeDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        const val CHANNEL_ID = "emergency_notifications"
        const val URGENT_CHANNEL_ID = "urgent_emergency"
        const val NOTIFICATION_ID_BASE = 1000
        
        // Notification escalation levels
        const val LEVEL_GENTLE = 1
        const val LEVEL_URGENT = 2
        const val LEVEL_EMERGENCY = 3
        const val LEVEL_SMS_CALL = 4
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Gentle reminder channel
        val gentleChannel = NotificationChannel(
            CHANNEL_ID,
            "Emergency Check-ins",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Gentle reminders to check in"
        }
        
        // Urgent emergency channel
        val urgentChannel = NotificationChannel(
            URGENT_CHANNEL_ID,
            "Emergency Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Urgent emergency notifications"
            enableVibration(true)
            vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
        }
        
        notificationManager.createNotificationChannels(listOf(gentleChannel, urgentChannel))
    }
    
    suspend fun executeNotificationLevel(level: Int, sessionId: Long) {
        when (level) {
            LEVEL_GENTLE -> showGentleReminder(sessionId)
            LEVEL_URGENT -> showUrgentNotification(sessionId)
            LEVEL_EMERGENCY -> showEmergencyNotification(sessionId)
            LEVEL_SMS_CALL -> sendEmergencySMS(sessionId)
        }
        
        // Log the notification
        database.notificationLogDao().insert(
            NotificationLog(
                sessionId = sessionId,
                notificationType = when (level) {
                    LEVEL_GENTLE -> "gentle"
                    LEVEL_URGENT -> "urgent"
                    LEVEL_EMERGENCY -> "emergency"
                    LEVEL_SMS_CALL -> "sms_call"
                    else -> "unknown"
                },
                timestamp = Date(),
                success = true
            )
        )
    }
    
    private fun showGentleReminder(sessionId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to Check In")
            .setContentText("Please confirm you're okay")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + 1, notification)
        }
    }
    
    private fun showUrgentNotification(sessionId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, URGENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("URGENT: Check In Required")
            .setContentText("Please respond immediately to confirm your safety")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .build()
            
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + 2, notification)
        }
    }
    
    private fun showEmergencyNotification(sessionId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, URGENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("EMERGENCY: Immediate Response Required")
            .setContentText("Emergency contacts will be notified if no response")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .build()
            
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + 3, notification)
        }
    }
    
    private suspend fun sendEmergencySMS(sessionId: Long) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val contacts = database.emergencyContactDao().getActiveContactsList()
        val userInfo = database.userInfoDao().getUserInfoSync()
        val session = database.checkInSessionDao().getCurrentSessionSync()
        val smsManager = context.getSystemService(SmsManager::class.java)
        
        contacts.forEach { contact ->
            try {
                val message = buildEmergencyMessage(userInfo, session)
                
                smsManager.sendTextMessage(
                    contact.phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
                
                // Log successful SMS
                database.notificationLogDao().insert(
                    NotificationLog(
                        sessionId = sessionId,
                        notificationType = "sms",
                        timestamp = Date(),
                        success = true,
                        contactId = contact.id,
                        message = message
                    )
                )
            } catch (e: Exception) {
                // Log failed SMS
                database.notificationLogDao().insert(
                    NotificationLog(
                        sessionId = sessionId,
                        notificationType = "sms",
                        timestamp = Date(),
                        success = false,
                        contactId = contact.id,
                        message = e.message
                    )
                )
            }
        }
    }
    
    private fun buildEmergencyMessage(userInfo: com.joshwalter.staywithme.data.model.UserInfo?, session: com.joshwalter.staywithme.data.model.CheckInSession?): String {
        val userName = userInfo?.name ?: "StayWithMe user"
        val medicalInfo = userInfo?.medicalInfo ?: ""
        val customMessage = userInfo?.customAlertMessage ?: ""
        val location = session?.location
        val substances = session?.substances ?: ""
        
        // Use custom message if provided, otherwise use default
        var message = if (customMessage.isNotEmpty()) {
            customMessage
        } else {
            "EMERGENCY: This is an automated alert from $userName's safety app. Their timer has expired and they are unresponsive."
        }
        
        // Replace placeholders in custom message
        message = message.replace("[Your Name]", userName)
        message = message.replace("[Medical Info text here]", medicalInfo)
        
        // Add medical information if not already included
        if (medicalInfo.isNotEmpty() && !message.contains(medicalInfo)) {
            message += "\n\nMedical Information: $medicalInfo"
        }
        
        // Add substance information if available
        if (substances.isNotEmpty()) {
            message += "\n\nSubstances: $substances"
        }
        
        // Add location if available
        if (location != null && location.isNotEmpty()) {
            val locationLink = "https://maps.google.com/?q=$location"
            message += "\n\nLast known location: $locationLink"
        }
        
        message += "\n\nPlease check on them immediately."
        
        return message
    }
}