package com.joshwalter.staywithme.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.joshwalter.staywithme.data.database.StayWithMeDatabase

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StayWithMeDatabase.getDatabase(application)
    
    private val _text = MutableLiveData<String>().apply {
        value = "Session History & Notifications"
    }
    val text: LiveData<String> = _text
    
    val allSessions = database.checkInSessionDao().getAllSessions()
    val recentNotifications = database.notificationLogDao().getRecentLogs()
}