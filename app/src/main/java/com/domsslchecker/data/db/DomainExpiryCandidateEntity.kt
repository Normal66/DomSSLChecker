package com.domsslchecker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "domain_expiry_candidates",
    foreignKeys = [
        ForeignKey(
            entity = DomainRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["domain_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["domain_id"]),
    ],
)
data class DomainExpiryCandidateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "domain_id")
    val domainId: Long,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "server_host")
    val serverHost: String,

    @ColumnInfo(name = "candidate_expiry_utc")
    val candidateExpiryUtc: Long,

    @ColumnInfo(name = "line")
    val line: String,
)
