package com.joshwalter.staywithme.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.joshwalter.staywithme.MainActivity
import com.joshwalter.staywithme.R
import com.joshwalter.staywithme.data.database.StayWithMeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

class CheckInMonitoringService : Service() {
    
    private lateinit var database: StayWithMeDatabase
    private lateinit var notificationService: EmergencyNotificationService
    
    companion object {
        const val FOREGROUND_SERVICE_ID = 1001
        const val CHANNEL_ID = "monitoring_service"
        
        fun startService(context: Context) {
            val intent = Intent(context, CheckInMonitoringService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, CheckInMonitoringService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        database = StayWithMeDatabase.getDatabase(this)
        notificationService = EmergencyNotificationService(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_SERVICE_ID, createForegroundNotification())
        
        // Schedule periodic check-ins
        scheduleCheckInMonitoring()
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Safety Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background safety monitoring service"
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("StayWithMe Active")
            .setContentText("Monitoring your safety")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun scheduleCheckInMonitoring() {
        val workRequest = PeriodicWorkRequestBuilder<CheckInWorker>(
            15, TimeUnit.MINUTES
        ).build()
        
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "check_in_monitoring",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
    }
}

class CheckInWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        val database = StayWithMeDatabase.getDatabase(applicationContext)
        val currentSession = database.checkInSessionDao().getCurrentSessionSync()
        
        android.util.Log.d("CheckInWorker", "Worker running - session active: ${currentSession?.isActive}")
        
        if (currentSession != null && currentSession.isActive) {
            val timeSinceLastCheckIn = currentSession.lastCheckIn?.let { 
                Date().time - it.time 
            } ?: (Date().time - currentSession.startTime.time)
            
            // Get user's custom check-in interval, default to 30 minutes
            val userInfo = database.userInfoDao().getUserInfo()
            val checkInIntervalMinutes = userInfo?.checkInIntervalMinutes ?: 30
            
            // Calculate escalation thresholds based on user's check-in interval
            val urgentThresholdMinutes = (checkInIntervalMinutes * 1.5).toInt() // Urgent after 1.5x interval
            val emergencyThresholdMinutes = checkInIntervalMinutes * 2 // Emergency after 2x interval
            val smsThresholdMinutes = checkInIntervalMinutes * 3 // SMS after 3x interval
            
            val checkInIntervalMs = checkInIntervalMinutes * 60 * 1000L
            val urgentThresholdMs = urgentThresholdMinutes * 60 * 1000L
            val emergencyThresholdMs = emergencyThresholdMinutes * 60 * 1000L
            val smsThresholdMs = smsThresholdMinutes * 60 * 1000L
            
            val notificationService = EmergencyNotificationService(applicationContext)
            
            when {
                timeSinceLastCheckIn >= smsThresholdMs && currentSession.notificationLevel < 4 -> {
                    database.checkInSessionDao().update(currentSession.copy(notificationLevel = 4))
                    notificationService.executeNotificationLevel(4, currentSession.id)
                }
                timeSinceLastCheckIn >= emergencyThresholdMs && currentSession.notificationLevel < 3 -> {
                    database.checkInSessionDao().update(currentSession.copy(notificationLevel = 3))
                    notificationService.executeNotificationLevel(3, currentSession.id)
                }
                timeSinceLastCheckIn >= urgentThresholdMs && currentSession.notificationLevel < 2 -> {
                    database.checkInSessionDao().update(currentSession.copy(notificationLevel = 2))
                    notificationService.executeNotificationLevel(2, currentSession.id)
                }
                timeSinceLastCheckIn >= checkInIntervalMs && currentSession.notificationLevel < 1 -> {
                    database.checkInSessionDao().update(currentSession.copy(notificationLevel = 1))
                    notificationService.executeNotificationLevel(1, currentSession.id)
                }
            }
        }
        
        return Result.success()
    }
}