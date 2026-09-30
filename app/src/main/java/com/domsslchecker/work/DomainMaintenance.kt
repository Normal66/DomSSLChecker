package com.domsslchecker.work

import android.content.Context
import com.domsslchecker.app.ServiceLocator
import com.domsslchecker.notif.NotifRepository

object DomainMaintenance {
    suspend fun refreshAllDomainsAndNotify(appContext: Context) {
        val domains = ServiceLocator.domainRepository.listAll()
        for (d in domains) {
            try {
                ServiceLocator.domainExpiryRepository.refreshFromWhois(d.id)
            } catch (_: Exception) {
                // сохраняем последние известные значения
            }
            try {
                ServiceLocator.sslRepository.probeTls443AndStore(d.id)
            } catch (_: Exception) {
            }
        }
        NotifRepository(appContext).scanAndNotifyIfEnabled()
    }
}
