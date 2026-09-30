package com.domsslchecker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DomainExpiryCandidateDao {
    @Query("SELECT * FROM domain_expiry_candidates WHERE domain_id = :domainId ORDER BY candidate_expiry_utc ASC")
    suspend fun listByDomainId(domainId: Long): List<DomainExpiryCandidateEntity>

    @Query("DELETE FROM domain_expiry_candidates WHERE domain_id = :domainId")
    suspend fun deleteByDomainId(domainId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DomainExpiryCandidateEntity>)
}
