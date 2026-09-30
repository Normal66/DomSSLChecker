package com.domsslchecker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSingleton(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AppSettingsEntity)

    @Update
    suspend fun update(entity: AppSettingsEntity)
}
