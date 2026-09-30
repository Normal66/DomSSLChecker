package com.domsslchecker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ssl_cert_entries",
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
        Index(value = ["domain_id", "position"], unique = true),
        Index(value = ["sha256_fingerprint"], unique = false),
    ],
)
data class SslCertEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "domain_id")
    val domainId: Long,

    @ColumnInfo(name = "position")
    val position: Int,

    @ColumnInfo(name = "subject")
    val subject: String,

    @ColumnInfo(name = "issuer")
    val issuer: String,

    @ColumnInfo(name = "not_before_utc")
    val notBeforeUtc: Long,

    @ColumnInfo(name = "not_after_utc")
    val notAfterUtc: Long,

    @ColumnInfo(name = "serial_number")
    val serialNumber: String,

    @ColumnInfo(name = "sha256_fingerprint")
    val sha256Fingerprint: String,
)

