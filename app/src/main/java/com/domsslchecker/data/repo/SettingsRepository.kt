package com.domsslchecker.data.repo

import androidx.room.withTransaction
import com.domsslchecker.data.db.AppDatabase
import com.domsslchecker.data.db.AppSettingsEntity
import com.domsslchecker.data.db.DomainListSort
import com.domsslchecker.data.db.ScheduleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsRepository(
    private val db: AppDatabase,
    private val nowUtcMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val dao = db.appSettingsDao()

    suspend fun getOrCreateSettings(): AppSettingsEntity = withContext(Dispatchers.IO) {
        val existing = dao.getSingleton()
        if (existing != null) return@withContext existing
        val defaults = AppSettingsEntity(
            id = 1,
            globalThresholdsDaysCsv = "30,14,7,3,1",
            scheduleMode = ScheduleMode.DAILY,
            notificationsEnabled = true,
            domainListSort = DomainListSort.BY_NAME,
        )
        db.withTransaction {
            val again = dao.getSingleton()
            if (again != null) return@withTransaction
            dao.insert(defaults)
        }
        dao.getSingleton() ?: defaults
    }

    suspend fun update(
        globalThresholdsDaysCsv: String,
        scheduleMode: ScheduleMode,
        notificationsEnabled: Boolean,
        domainListSort: DomainListSort,
    ) = withContext(Dispatchers.IO) {
        val current = getOrCreateSettings()
        dao.update(
            current.copy(
                globalThresholdsDaysCsv = globalThresholdsDaysCsv.trim(),
                scheduleMode = scheduleMode,
                notificationsEnabled = notificationsEnabled,
                domainListSort = domainListSort,
            ),
        )
    }

    suspend fun getGlobalThresholdDays(): List<Int> {
        val csv = getOrCreateSettings().globalThresholdsDaysCsv
        return parseDaysCsv(csv)
    }

    /**
     * CSV порогов домена; пустой/невалидный override → глобальные.
     */
    fun thresholdsForDomain(overrideCsv: String?, globalThresholds: List<Int>): List<Int> {
        val o = overrideCsv?.trim().orEmpty()
        if (o.isBlank()) return globalThresholds
        val parsed = parseDaysCsv(o)
        return if (parsed.isEmpty()) globalThresholds else parsed
    }

    private fun parseDaysCsv(csv: String): List<Int> {
        return csv.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .distinct()
            .sorted()
    }
}
