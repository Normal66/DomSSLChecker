package com.domsslchecker.data.repo

import android.database.sqlite.SQLiteConstraintException
import com.domsslchecker.data.db.DomainExpirySource
import com.domsslchecker.data.db.DomainRecordDao
import com.domsslchecker.data.db.DomainRecordEntity
import com.domsslchecker.data.db.SslStatus
import com.domsslchecker.data.db.UpdateStatus
import com.domsslchecker.domain.DomainName
import com.domsslchecker.domain.DomainParseError
import com.domsslchecker.domain.NormalizedDomainName

sealed class CreateDomainResult {
    data class Success(val id: Long) : CreateDomainResult()
    data class Duplicate(val domain: String) : CreateDomainResult()
    data class Invalid(val error: DomainParseError) : CreateDomainResult()
}

class DomainRepository(
    private val dao: DomainRecordDao,
    private val nowUtcMillis: () -> Long,
) {
    suspend fun listAll(): List<DomainRecordEntity> = dao.listAll()
    suspend fun getById(id: Long): DomainRecordEntity? = dao.getById(id)

    suspend fun createFromUserInput(input: String): CreateDomainResult {
        return when (val parsed = DomainName.parseUserInput(input)) {
            is DomainName.ParseResult.Invalid ->
                CreateDomainResult.Invalid(parsed.error)

            is DomainName.ParseResult.Ok ->
                create(parsed.domain)
        }
    }

    private suspend fun create(normalized: NormalizedDomainName, label: String? = null, notes: String? = null): CreateDomainResult {
        val now = nowUtcMillis()
        val entity = DomainRecordEntity(
            domain = normalized.value,
            createdAtUtc = now,
            updatedAtUtc = now,
            domainExpiryUtcAuto = null,
            domainExpiryUtcManual = null,
            domainExpirySource = DomainExpirySource.AUTO,
            domainUpdateStatus = UpdateStatus.OK,
            domainUpdateLastRunUtc = null,
            domainUpdateLastError = null,
            sslLastCheckedUtc = null,
            sslStatus = SslStatus.OK,
            sslLastError = null,
            thresholdsOverrideDaysCsv = null,
            label = label,
            notes = notes,
            registrar = null,
            hoster = null,
            nameServersCsv = null,
        )
        return try {
            val id = dao.insert(entity)
            CreateDomainResult.Success(id)
        } catch (_: SQLiteConstraintException) {
            CreateDomainResult.Duplicate(entity.domain)
        }
    }

    suspend fun delete(id: Long) {
        val current = dao.getById(id) ?: return
        dao.delete(current)
    }

    suspend fun updateLabelNotes(
        id: Long,
        label: String?,
        notes: String?,
        thresholdsOverrideCsv: String,
    ) {
        val current = dao.getById(id) ?: return
        val now = nowUtcMillis()
        val thr = thresholdsOverrideCsv.trim()
        val thresholdsFinal = thr.takeIf { it.isNotEmpty() }
        dao.update(
            current.copy(
                label = label?.trim()?.takeIf { it.isNotEmpty() },
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                thresholdsOverrideDaysCsv = thresholdsFinal,
                updatedAtUtc = now,
            ),
        )
    }
}

