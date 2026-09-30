package com.domsslchecker.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domsslchecker.app.ServiceLocator
import com.domsslchecker.data.db.AppSettingsEntity
import com.domsslchecker.data.db.DomainListSort
import com.domsslchecker.data.db.ScheduleMode
import com.domsslchecker.data.repo.SettingsRepository
import com.domsslchecker.notif.NotifScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository = ServiceLocator.settingsRepository,
) : ViewModel() {
    private val _settings = MutableStateFlow<AppSettingsEntity?>(null)
    val settings: StateFlow<AppSettingsEntity?> = _settings

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _settings.value = repo.getOrCreateSettings()
        }
    }

    fun save(
        context: Context,
        globalThresholdsDaysCsv: String,
        mode: ScheduleMode,
        enabled: Boolean,
        domainListSort: DomainListSort,
    ) {
        viewModelScope.launch {
            repo.update(
                globalThresholdsDaysCsv = globalThresholdsDaysCsv,
                scheduleMode = mode,
                notificationsEnabled = enabled,
                domainListSort = domainListSort,
            )
            _settings.value = repo.getOrCreateSettings()
            NotifScheduler.applyMode(context, mode)
        }
    }
}
