package com.domsslchecker.notif

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.domsslchecker.data.db.ScheduleMode
import com.domsslchecker.work.MaintenanceWorker
import java.util.concurrent.TimeUnit

object NotifScheduler {
    private const val UNIQUE_NAME = "maintenance"

    fun applyMode(context: Context, mode: ScheduleMode) {
        val appContext = context.applicationContext
        val wm = WorkManager.getInstance(appContext)
        if (mode == ScheduleMode.OFF) {
            wm.cancelUniqueWork(UNIQUE_NAME)
            return
        }

        val interval = when (mode) {
            ScheduleMode.DAILY -> 1L to TimeUnit.DAYS
            ScheduleMode.WEEKLY -> 7L to TimeUnit.DAYS
            ScheduleMode.OFF -> 1L to TimeUnit.DAYS
        }

        val (repeat, unit) = interval
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val req = PeriodicWorkRequest.Builder(
            MaintenanceWorker::class.java,
            repeat,
            unit,
        )
            .setConstraints(constraints)
            .build()

        wm.enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            req,
        )
    }
}
