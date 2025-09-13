package com.joshwalter.staywithme.ui.myinfo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshwalter.staywithme.data.database.StayWithMeDatabase
import com.joshwalter.staywithme.data.model.EmergencyContact
import com.joshwalter.staywithme.data.model.UserInfo
import kotlinx.coroutines.launch

class MyInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StayWithMeDatabase.getDatabase(application)
    private val userInfoDao = database.userInfoDao()
    private val emergencyContactDao = database.emergencyContactDao()
    
    val userInfo: LiveData<UserInfo?> = userInfoDao.getUserInfo()
    val emergencyContacts: LiveData<List<EmergencyContact>> = emergencyContactDao.getAllActiveContacts()
    
    fun saveUserInfo(name: String, medicalInfo: String, notes: String, customAlertMessage: String) {
        viewModelScope.launch {
            val userInfo = UserInfo(
                name = name,
                medicalInfo = medicalInfo,
                notes = notes,
                customAlertMessage = customAlertMessage
            )
            userInfoDao.insertOrUpdate(userInfo)
        }
    }
    
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
    
    fun deleteEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            emergencyContactDao.delete(contact)
        }
    }
    
    fun updateEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            emergencyContactDao.update(contact)
        }
    }
}