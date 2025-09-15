package com.joshwalter.staywithme.data.database

import androidx.room.*
import com.joshwalter.staywithme.data.model.UserInfo

@Dao
interface UserInfoDao {
    
    @Query("SELECT * FROM user_info WHERE id = 1")
    suspend fun getUserInfo(): UserInfo?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userInfo: UserInfo): Long
    
    @Update
    suspend fun update(userInfo: UserInfo)
    
    @Query("DELETE FROM user_info")
    suspend fun deleteAll()
}
