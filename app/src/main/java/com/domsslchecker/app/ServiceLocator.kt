package com.domsslchecker.app

import android.content.Context
import androidx.room.Room
import com.domsslchecker.data.db.AppDatabase
import com.domsslchecker.data.db.MIGRATION_1_2
import com.domsslchecker.data.db.MIGRATION_2_3
import com.domsslchecker.data.db.MIGRATION_3_4
import com.domsslchecker.data.db.MIGRATION_4_5
import com.domsslchecker.data.repo.DomainRepository
import com.domsslchecker.data.repo.DomainExpiryRepository
import com.domsslchecker.data.repo.SettingsRepository
import com.domsslchecker.data.repo.SslRepository

object ServiceLocator {
    private lateinit var appContext: Context

    lateinit var db: AppDatabase
        private set

    lateinit var domainRepository: DomainRepository
        private set

    lateinit var domainExpiryRepository: DomainExpiryRepository
        private set

    lateinit var sslRepository: SslRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    fun requireAppContext(): Context = appContext

    fun init(context: Context) {
        appContext = context.applicationContext
        db = Room.databaseBuilder(appContext, AppDatabase::class.java, "domsslchecker.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
        domainRepository = DomainRepository(
            dao = db.domainRecordDao(),
            nowUtcMillis = { System.currentTimeMillis() },
        )
        domainExpiryRepository = DomainExpiryRepository(
            db = db,
        )
        settingsRepository = SettingsRepository(db = db)
        sslRepository = SslRepository(db = db, settingsRepository = settingsRepository)
    }
}

