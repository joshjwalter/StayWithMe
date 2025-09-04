package com.joshwalter.staywithme.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.joshwalter.staywithme.data.model.NotificationLog

@Dao
interface NotificationLogDao {
    
    @Query("SELECT * FROM notification_logs WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getLogsForSession(sessionId: Long): LiveData<List<NotificationLog>>
    
    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): LiveData<List<NotificationLog>>
    
    @Insert
    suspend fun insert(log: NotificationLog): Long
    
    @Query("DELETE FROM notification_logs WHERE timestamp < :cutoffDate")
    suspend fun deleteOldLogs(cutoffDate: java.util.Date)
}