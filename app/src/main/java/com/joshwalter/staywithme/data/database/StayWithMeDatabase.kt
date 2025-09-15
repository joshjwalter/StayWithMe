package com.joshwalter.staywithme.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.joshwalter.staywithme.data.model.CheckInSession
import com.joshwalter.staywithme.data.model.EmergencyContact
import com.joshwalter.staywithme.data.model.NotificationLog
import com.joshwalter.staywithme.data.model.UserInfo

@Database(
    entities = [EmergencyContact::class, CheckInSession::class, NotificationLog::class, UserInfo::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StayWithMeDatabase : RoomDatabase() {
    
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun checkInSessionDao(): CheckInSessionDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun userInfoDao(): UserInfoDao
    
    companion object {
        @Volatile
        private var INSTANCE: StayWithMeDatabase? = null
        
        fun getDatabase(context: Context): StayWithMeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StayWithMeDatabase::class.java,
                    "staywithme_database"
                )
                .fallbackToDestructiveMigration() // For development - in production, add proper migrations
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}