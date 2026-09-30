package com.domsslchecker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class SslLeafExpiryRow(
    val domainId: Long,
    val notAfterUtc: Long,
)

@Dao
interface SslCertEntryDao {
    @Query("SELECT * FROM ssl_cert_entries WHERE domain_id = :domainId ORDER BY position ASC")
    suspend fun listByDomainId(domainId: Long): List<SslCertEntryEntity>

    @Query(
        "SELECT domain_id AS domainId, not_after_utc AS notAfterUtc " +
            "FROM ssl_cert_entries " +
            "WHERE position = 0 AND domain_id IN (:domainIds)",
    )
    suspend fun leafExpiryUtcByDomainIds(domainIds: List<Long>): List<SslLeafExpiryRow>

    @Query("DELETE FROM ssl_cert_entries WHERE domain_id = :domainId")
    suspend fun deleteByDomainId(domainId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SslCertEntryEntity>)
}

