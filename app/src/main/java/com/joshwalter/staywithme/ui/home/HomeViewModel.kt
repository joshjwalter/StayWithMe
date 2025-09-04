package com.joshwalter.staywithme.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.joshwalter.staywithme.data.database.StayWithMeDatabase
import com.joshwalter.staywithme.data.model.CheckInSession

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StayWithMeDatabase.getDatabase(application)
    
    private val _text = MutableLiveData<String>().apply {
        value = "Welcome to StayWithMe - Your safety companion"
    }
    val text: LiveData<String> = _text
    
    val currentSession = database.checkInSessionDao().getCurrentSession()
    
    private val _selectedDuration = MutableLiveData<Int>().apply {
        value = 120 // Default 2 hours
    }
    val selectedDuration: LiveData<Int> = _selectedDuration
    
    private val _substanceInfo = MutableLiveData<String>().apply {
        value = ""
    }
    val substanceInfo: LiveData<String> = _substanceInfo
    
    private val _notes = MutableLiveData<String>().apply {
        value = ""
    }
    val notes: LiveData<String> = _notes
    
    private val _isSessionActive = MutableLiveData<Boolean>().apply {
        value = false
    }
    val isSessionActive: LiveData<Boolean> = _isSessionActive
    
    // Update text based on session status
    val dynamicText = currentSession.switchMap { session ->
        val textLiveData = MutableLiveData<String>()
        if (session != null && session.isActive) {
            textLiveData.value = "Safety session active since ${formatTime(session.startTime)}"
        } else {
            textLiveData.value = "Ready to start a new safety session"
        }
        textLiveData
    }
    
    fun setDuration(minutes: Int) {
        _selectedDuration.value = minutes
    }
    
    fun setSubstanceInfo(info: String) {
        _substanceInfo.value = info
    }
    
    fun setNotes(notes: String) {
        _notes.value = notes
    }
    
    fun getSelectedDuration(): Int = _selectedDuration.value ?: 120
    
    fun getSubstanceInfo(): String = _substanceInfo.value ?: ""
    
    fun getNotes(): String = _notes.value ?: ""
    
    fun onSessionStarted() {
        _isSessionActive.value = true
        _text.value = "Safety session started. Check in regularly to stay safe!"
    }
    
    fun onCheckInPerformed() {
        _text.value = "Check-in recorded. Stay safe!"
    }
    
    fun onSessionEnded() {
        _isSessionActive.value = false
        _text.value = "Safety session ended. Take care!"
    }
    
    private fun formatTime(date: java.util.Date): String {
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return formatter.format(date)
    }
}