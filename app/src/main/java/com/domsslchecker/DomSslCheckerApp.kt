package com.domsslchecker

import android.app.Application
import com.domsslchecker.app.ServiceLocator
import com.domsslchecker.notif.DomNotifications
import androidx.work.WorkManager
import com.domsslchecker.notif.NotifScheduler
import kotlinx.coroutines.runBlocking

class DomSslCheckerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        DomNotifications.init(this)
        WorkManager.getInstance(this).cancelUniqueWork("notif_scan")

        // Apply WorkManager schedule based on DB settings (avoid blocking main thread)
        Thread {
            runBlocking {
                val s = ServiceLocator.settingsRepository.getOrCreateSettings()
                NotifScheduler.applyMode(this@DomSslCheckerApp, s.scheduleMode)
            }
        }.start()
    }
}

