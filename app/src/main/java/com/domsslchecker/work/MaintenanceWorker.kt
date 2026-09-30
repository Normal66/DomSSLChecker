package com.domsslchecker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.domsslchecker.data.db.ScheduleMode
import com.domsslchecker.data.repo.SettingsRepository
import com.domsslchecker.app.ServiceLocator

class MaintenanceWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val settings = SettingsRepository(ServiceLocator.db).getOrCreateSettings()
            if (settings.scheduleMode == ScheduleMode.OFF) {
                return Result.success()
            }
            DomainMaintenance.refreshAllDomainsAndNotify(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
