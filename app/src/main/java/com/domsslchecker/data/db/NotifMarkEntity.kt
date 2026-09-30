package com.domsslchecker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notif_marks",
    indices = [
        Index(
            value = ["domain_id", "item_kind", "item_index", "expiry_utc", "threshold_days"],
            unique = true,
        ),
    ],
)
data class NotifMarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "domain_id")
    val domainId: Long,

    // DOMAIN | SSL
    @ColumnInfo(name = "item_kind")
    val itemKind: String,

    // for SSL, certificate position in chain; 0 for DOMAIN
    @ColumnInfo(name = "item_index")
    val itemIndex: Int,

    @ColumnInfo(name = "expiry_utc")
    val expiryUtc: Long,

    @ColumnInfo(name = "threshold_days")
    val thresholdDays: Int,

    @ColumnInfo(name = "fired_at_utc")
    val firedAtUtc: Long,
)
