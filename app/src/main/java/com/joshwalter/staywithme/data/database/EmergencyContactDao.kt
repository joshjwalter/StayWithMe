package com.joshwalter.staywithme.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.joshwalter.staywithme.data.model.EmergencyContact

@Dao
interface EmergencyContactDao {
    
    @Query("SELECT * FROM emergency_contacts WHERE isActive = 1 ORDER BY priority")
    fun getAllActiveContacts(): LiveData<List<EmergencyContact>>
    
    @Query("SELECT * FROM emergency_contacts WHERE isActive = 1 ORDER BY priority")
    suspend fun getActiveContactsList(): List<EmergencyContact>
    
    @Insert
    suspend fun insert(contact: EmergencyContact): Long
    
    @Update
    suspend fun update(contact: EmergencyContact)
    
    @Delete
    suspend fun delete(contact: EmergencyContact)
    
    @Query("DELETE FROM emergency_contacts WHERE id = :contactId")
    suspend fun deleteById(contactId: Long)
}