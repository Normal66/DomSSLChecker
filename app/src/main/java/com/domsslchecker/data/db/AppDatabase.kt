package com.domsslchecker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DomainRecordEntity::class,
        SslCertEntryEntity::class,
        DomainExpiryCandidateEntity::class,
        AppSettingsEntity::class,
        NotifMarkEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun domainRecordDao(): DomainRecordDao
    abstract fun sslCertEntryDao(): SslCertEntryDao
    abstract fun domainExpiryCandidateDao(): DomainExpiryCandidateDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun notifMarkDao(): NotifMarkDao
}

