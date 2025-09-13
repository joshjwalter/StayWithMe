package com.joshwalter.staywithme.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.joshwalter.staywithme.data.model.UserInfo

@Dao
interface UserInfoDao {
    
    @Query("SELECT * FROM user_info WHERE id = 1")
    fun getUserInfo(): LiveData<UserInfo?>
    
    @Query("SELECT * FROM user_info WHERE id = 1")
    suspend fun getUserInfoSync(): UserInfo?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userInfo: UserInfo)
    
    @Update
    suspend fun update(userInfo: UserInfo)
}