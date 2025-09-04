package com.joshwalter.staywithme.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshwalter.staywithme.data.database.StayWithMeDatabase
import com.joshwalter.staywithme.data.model.EmergencyContact
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StayWithMeDatabase.getDatabase(application)
    private val emergencyContactDao = database.emergencyContactDao()
    
    private val _text = MutableLiveData<String>().apply {
        value = "Emergency Contacts"
    }
    val text: LiveData<String> = _text
    
    val emergencyContacts = emergencyContactDao.getAllActiveContacts()
    
    fun addEmergencyContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            // Get current max priority and add 1
            val contacts = emergencyContactDao.getActiveContactsList()
            val maxPriority = contacts.maxOfOrNull { it.priority } ?: 0
            
            val newContact = EmergencyContact(
                name = name,
                phoneNumber = phoneNumber,
                priority = maxPriority + 1
            )
            
            emergencyContactDao.insert(newContact)
        }
    }
    
    fun updateEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            emergencyContactDao.update(contact)
        }
    }
    
    fun deleteEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            emergencyContactDao.delete(contact)
        }
    }
}