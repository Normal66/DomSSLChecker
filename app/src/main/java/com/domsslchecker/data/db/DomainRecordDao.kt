package com.domsslchecker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DomainRecordDao {
    @Query("SELECT * FROM domain_records ORDER BY domain ASC")
    suspend fun listAll(): List<DomainRecordEntity>

    @Query("SELECT * FROM domain_records WHERE id = :id")
    suspend fun getById(id: Long): DomainRecordEntity?

    @Query("SELECT * FROM domain_records WHERE domain = :domain")
    suspend fun getByDomain(domain: String): DomainRecordEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DomainRecordEntity): Long

    @Update
    suspend fun update(entity: DomainRecordEntity)

    @Delete
    suspend fun delete(entity: DomainRecordEntity)
}

