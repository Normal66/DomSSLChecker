package com.domsslchecker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface NotifMarkDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: NotifMarkEntity): Long
}
