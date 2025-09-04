package com.joshwalter.staywithme.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.joshwalter.staywithme.data.model.CheckInSession

@Dao
interface CheckInSessionDao {
    
    @Query("SELECT * FROM checkin_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    fun getCurrentSession(): LiveData<CheckInSession?>
    
    @Query("SELECT * FROM checkin_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun getCurrentSessionSync(): CheckInSession?
    
    @Query("SELECT * FROM checkin_sessions ORDER BY startTime DESC")
    fun getAllSessions(): LiveData<List<CheckInSession>>
    
    @Insert
    suspend fun insert(session: CheckInSession): Long
    
    @Update
    suspend fun update(session: CheckInSession)
    
    @Query("UPDATE checkin_sessions SET isActive = 0 WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long)
    
    @Query("UPDATE checkin_sessions SET isActive = 0 WHERE isActive = 1")
    suspend fun endAllActiveSessions()
}