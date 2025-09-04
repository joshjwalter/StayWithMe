package com.joshwalter.staywithme.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.joshwalter.staywithme.data.model.CheckInSession
import com.joshwalter.staywithme.data.model.EmergencyContact
import com.joshwalter.staywithme.data.model.NotificationLog

@Database(
    entities = [EmergencyContact::class, CheckInSession::class, NotificationLog::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StayWithMeDatabase : RoomDatabase() {
    
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun checkInSessionDao(): CheckInSessionDao
    abstract fun notificationLogDao(): NotificationLogDao
    
    companion object {
        @Volatile
        private var INSTANCE: StayWithMeDatabase? = null
        
        fun getDatabase(context: Context): StayWithMeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StayWithMeDatabase::class.java,
                    "staywithme_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}