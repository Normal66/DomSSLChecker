package com.domsslchecker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Long = 1,

    @ColumnInfo(name = "global_thresholds_days_csv")
    val globalThresholdsDaysCsv: String,

    @ColumnInfo(name = "schedule_mode")
    val scheduleMode: ScheduleMode,

    @ColumnInfo(name = "notifications_enabled")
    val notificationsEnabled: Boolean,

    @ColumnInfo(name = "domain_list_sort")
    val domainListSort: DomainListSort = DomainListSort.BY_NAME,
)

enum class ScheduleMode {
    OFF,
    DAILY,
    WEEKLY,
}
