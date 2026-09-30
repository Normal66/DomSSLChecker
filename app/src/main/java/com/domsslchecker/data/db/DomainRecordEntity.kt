package com.domsslchecker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "domain_records",
    indices = [
        Index(value = ["domain"], unique = true),
    ],
)
data class DomainRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "domain")
    val domain: String,

    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: Long,

    @ColumnInfo(name = "updated_at_utc")
    val updatedAtUtc: Long,

    @ColumnInfo(name = "domain_expiry_utc_auto")
    val domainExpiryUtcAuto: Long?,

    @ColumnInfo(name = "domain_expiry_utc_manual")
    val domainExpiryUtcManual: Long?,

    @ColumnInfo(name = "domain_expiry_source")
    val domainExpirySource: DomainExpirySource,

    @ColumnInfo(name = "domain_update_status")
    val domainUpdateStatus: UpdateStatus,

    @ColumnInfo(name = "domain_update_last_run_utc")
    val domainUpdateLastRunUtc: Long?,

    @ColumnInfo(name = "domain_update_last_error")
    val domainUpdateLastError: String?,

    @ColumnInfo(name = "ssl_last_checked_utc")
    val sslLastCheckedUtc: Long?,

    @ColumnInfo(name = "ssl_status")
    val sslStatus: SslStatus,

    @ColumnInfo(name = "ssl_last_error")
    val sslLastError: String?,

    @ColumnInfo(name = "thresholds_override_days_csv")
    val thresholdsOverrideDaysCsv: String?,

    @ColumnInfo(name = "label")
    val label: String?,

    @ColumnInfo(name = "notes")
    val notes: String?,

    @ColumnInfo(name = "registrar")
    val registrar: String?,

    @ColumnInfo(name = "hoster")
    val hoster: String?,

    @ColumnInfo(name = "name_servers_csv")
    val nameServersCsv: String?,
)

