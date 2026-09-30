package com.domsslchecker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domsslchecker.app.ServiceLocator
import com.domsslchecker.data.db.DomainExpiryCandidateEntity
import com.domsslchecker.data.db.DomainRecordEntity
import com.domsslchecker.data.db.SslCertEntryEntity
import com.domsslchecker.data.repo.CreateDomainResult
import com.domsslchecker.data.repo.DomainRepository
import com.domsslchecker.data.repo.SslRepository
import com.domsslchecker.domain.DomainParseError
import com.domsslchecker.notif.NotifRepository
import com.domsslchecker.work.DomainMaintenance
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface DomainsUiEvent {
    data object NavBackAfterAdd : DomainsUiEvent
}

class DomainsViewModel(
    private val repo: DomainRepository = ServiceLocator.domainRepository,
    private val expiryRepo: com.domsslchecker.data.repo.DomainExpiryRepository =
        ServiceLocator.domainExpiryRepository,
    private val sslRepo: SslRepository = ServiceLocator.sslRepository,
) : ViewModel() {
    private val _domains = MutableStateFlow<List<DomainRecordEntity>>(emptyList())
    val domains: StateFlow<List<DomainRecordEntity>> = _domains

    private val _sslLeafExpiryUtc = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val sslLeafExpiryUtc: StateFlow<Map<Long, Long>> = _sslLeafExpiryUtc

    private val _selected = MutableStateFlow<DomainRecordEntity?>(null)
    val selected: StateFlow<DomainRecordEntity?> = _selected

    /** Подсветка строки в списке (последний открытый домен). */
    private val _listHighlightId = MutableStateFlow<Long?>(null)
    val listHighlightId: StateFlow<Long?> = _listHighlightId

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _expiryCandidates = MutableStateFlow<List<DomainExpiryCandidateEntity>>(emptyList())
    val expiryCandidates: StateFlow<List<DomainExpiryCandidateEntity>> = _expiryCandidates

    private val _whoisLoading = MutableStateFlow(false)
    val whoisLoading: StateFlow<Boolean> = _whoisLoading

    private val _sslCerts = MutableStateFlow<List<SslCertEntryEntity>>(emptyList())
    val sslCerts: StateFlow<List<SslCertEntryEntity>> = _sslCerts

    private val _sslLoading = MutableStateFlow(false)
    val sslLoading: StateFlow<Boolean> = _sslLoading

    private val _bulkRefreshRunning = MutableStateFlow(false)
    val bulkRefreshRunning: StateFlow<Boolean> = _bulkRefreshRunning

    private val _events = MutableSharedFlow<DomainsUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DomainsUiEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun clearError() {
        _lastError.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            val list = repo.listAll()
            _domains.value = list
            _sslLeafExpiryUtc.value = sslRepo.leafExpiryUtcByDomainIds(list.map { it.id })
        }
    }

    fun loadById(id: Long) {
        viewModelScope.launch {
            _listHighlightId.value = id
            _selected.value = repo.getById(id)
            _expiryCandidates.value = expiryRepo.listCandidates(id)
            _sslCerts.value = sslRepo.listCerts(id)
        }
    }

    fun setListHighlightId(id: Long?) {
        _listHighlightId.value = id
    }

    fun refreshDomainTls(id: Long) {
        viewModelScope.launch {
            _sslLoading.value = true
            try {
                sslRepo.probeTls443AndStore(id)
                notifyIfEnabled()
            } finally {
                _sslLoading.value = false
                loadById(id)
                refresh()
            }
        }
    }

    fun refreshDomainWhois(id: Long) {
        viewModelScope.launch {
            _whoisLoading.value = true
            try {
                expiryRepo.refreshFromWhois(id)
                notifyIfEnabled()
            } finally {
                _whoisLoading.value = false
                loadById(id)
                refresh()
            }
        }
    }

    fun refreshAllDomains() {
        viewModelScope.launch {
            if (_bulkRefreshRunning.value) return@launch
            _bulkRefreshRunning.value = true
            try {
                DomainMaintenance.refreshAllDomainsAndNotify(ServiceLocator.requireAppContext())
            } finally {
                _bulkRefreshRunning.value = false
                refresh()
            }
        }
    }

    private suspend fun notifyIfEnabled() {
        NotifRepository(ServiceLocator.requireAppContext()).scanAndNotifyIfEnabled()
    }

    fun confirmExpiryCandidate(domainId: Long, candidateId: Long) {
        viewModelScope.launch {
            _whoisLoading.value = true
            try {
                expiryRepo.confirmCandidate(domainId, candidateId)
            } finally {
                _whoisLoading.value = false
                loadById(domainId)
                refresh()
            }
        }
    }

    fun addDomain(input: String) {
        viewModelScope.launch {
            when (val res = repo.createFromUserInput(input)) {
                is CreateDomainResult.Success -> {
                    _lastError.value = null
                    val id = res.id
                    _listHighlightId.value = id

                    _whoisLoading.value = true
                    try {
                        expiryRepo.refreshFromWhois(id)
                    } catch (e: Exception) {
                        _lastError.value = e.message?.take(500) ?: "Ошибка WHOIS"
                    } finally {
                        _whoisLoading.value = false
                    }

                    _sslLoading.value = true
                    try {
                        sslRepo.probeTls443AndStore(id)
                    } catch (e: Exception) {
                        _lastError.value = e.message?.take(500) ?: "Ошибка SSL"
                    } finally {
                        _sslLoading.value = false
                    }

                    refresh()
                    _events.emit(DomainsUiEvent.NavBackAfterAdd)
                }
                is CreateDomainResult.Duplicate -> {
                    _lastError.value = "Домен уже добавлен: ${res.domain}"
                }
                is CreateDomainResult.Invalid -> {
                    _lastError.value = when (res.error) {
                        DomainParseError.EMPTY -> "Введите домен"
                        DomainParseError.INVALID -> "Некорректный домен"
                    }
                }
            }
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            repo.delete(id)
            if (_selected.value?.id == id) {
                _selected.value = null
            }
            if (_listHighlightId.value == id) {
                _listHighlightId.value = null
            }
            refresh()
        }
    }

    fun updateLabelNotes(
        id: Long,
        label: String,
        notes: String,
        thresholdsOverrideCsv: String,
    ) {
        viewModelScope.launch {
            repo.updateLabelNotes(id, label, notes, thresholdsOverrideCsv)
            loadById(id)
            refresh()
        }
    }
}
