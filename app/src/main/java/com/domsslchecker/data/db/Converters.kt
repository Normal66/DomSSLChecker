package com.domsslchecker.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun domainExpirySourceToString(value: DomainExpirySource): String = value.name

    @TypeConverter
    fun domainExpirySourceFromString(value: String): DomainExpirySource =
        DomainExpirySource.valueOf(value)

    @TypeConverter
    fun updateStatusToString(value: UpdateStatus): String = value.name

    @TypeConverter
    fun updateStatusFromString(value: String): UpdateStatus =
        UpdateStatus.valueOf(value)

    @TypeConverter
    fun sslStatusToString(value: SslStatus): String = value.name

    @TypeConverter
    fun sslStatusFromString(value: String): SslStatus =
        SslStatus.valueOf(value)

    @TypeConverter
    fun scheduleModeToString(value: ScheduleMode): String = value.name

    @TypeConverter
    fun scheduleModeFromString(value: String): ScheduleMode =
        ScheduleMode.valueOf(value)

    @TypeConverter
    fun domainListSortToString(value: DomainListSort): String = value.name

    @TypeConverter
    fun domainListSortFromString(value: String): DomainListSort =
        DomainListSort.valueOf(value)
}

