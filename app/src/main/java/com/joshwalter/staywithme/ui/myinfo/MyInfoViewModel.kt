package com.joshwalter.staywithme.ui.myinfo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshwalter.staywithme.data.database.StayWithMeDatabase
import com.joshwalter.staywithme.data.model.UserInfo
import kotlinx.coroutines.launch

class MyInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StayWithMeDatabase.getDatabase(application)
    private val userInfoDao = database.userInfoDao()
    
    private val _userInfo = MutableLiveData<UserInfo?>()
    val userInfo: LiveData<UserInfo?> = _userInfo
    
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess
    
    suspend fun loadUserInfo() {
        val info = userInfoDao.getUserInfo()
        _userInfo.value = info ?: UserInfo(
            customAlertMessage = "EMERGENCY: I may need assistance. I haven't checked in as scheduled. Please check on me immediately."
        ) // Return default if none exists
    }
    
    fun saveUserInfo(name: String, medicalInfo: String, additionalNotes: String, customAlertMessage: String, checkInIntervalMinutes: Int) {
        viewModelScope.launch {
            try {
                val userInfo = UserInfo(
                    name = name,
                    medicalInfo = medicalInfo,
                    additionalNotes = additionalNotes,
                    customAlertMessage = customAlertMessage,
                    checkInIntervalMinutes = checkInIntervalMinutes
                )
                
                userInfoDao.insertOrUpdate(userInfo)
                // Reload the user info to ensure it's properly saved
                loadUserInfo()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _saveSuccess.value = false
            }
        }
    }
}
