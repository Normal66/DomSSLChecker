package com.domsslchecker.data.repo

import com.domsslchecker.data.db.AppDatabase
import com.domsslchecker.data.db.DomainExpiryCandidateEntity
import com.domsslchecker.data.db.UpdateStatus
import com.domsslchecker.net.whois.WhoisDateParser
import com.domsslchecker.net.whois.WhoisRuWebClient
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.IDN
import java.util.Locale

class DomainExpiryRepository(
    private val db: AppDatabase,
    private val nowUtcMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val domainDao = db.domainRecordDao()
    private val candidateDao = db.domainExpiryCandidateDao()

    suspend fun listCandidates(domainId: Long): List<DomainExpiryCandidateEntity> =
        withContext(Dispatchers.IO) {
            candidateDao.listByDomainId(domainId)
        }

    /**
     * WHOIS по [whois.ru](https://whois.ru/) (HTTPS, путь `/{ascii}`).
     * Несколько дат в ответе: берётся **самая поздняя** (максимум по UTC) без вопроса пользователю.
     */
    suspend fun refreshFromWhois(domainId: Long) = withContext(Dispatchers.IO) {
        val now = nowUtcMillis()

        val current = domainDao.getById(domainId) ?: return@withContext
        val asciiDomain = toAsciiDomain(current.domain)
        if (asciiDomain == null) {
            patchDomain(
                id = domainId,
                now = now,
                status = UpdateStatus.ERROR,
                lastError = "Некорректный домен",
            )
            return@withContext
        }

        val aggregated = LinkedHashMap<Long, DomainExpiryCandidateEntity>()
        var gotWhoisText = false
        var registrar: String? = null
        var hoster: String? = null
        var nameServers: List<String> = emptyList()

        try {
            val plain = try {
                WhoisRuWebClient.fetchWhoisPlainText(asciiDomain)
            } catch (_: Exception) {
                null
            }
            if (plain != null && plain.isNotBlank()) {
                gotWhoisText = true
                registrar = parseRegistrar(plain)
                nameServers = parseNameServers(plain)
                hoster = deriveHosterFromNameServers(nameServers)
                for (c in WhoisDateParser.parse(plain)) {
                    val key = c.instantUtc
                    if (!aggregated.containsKey(key)) {
                        aggregated[key] = DomainExpiryCandidateEntity(
                            domainId = domainId,
                            source = "whois",
                            serverHost = "whois.ru (https)",
                            candidateExpiryUtc = c.instantUtc,
                            line = c.line,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            patchDomain(
                id = domainId,
                now = now,
                status = UpdateStatus.ERROR,
                lastError = e.message?.take(500) ?: "Ошибка WHOIS",
            )
            return@withContext
        }

        val uniqueInstants = aggregated.keys.sorted()
        if (uniqueInstants.isEmpty()) {
            val noDataMsg = if (!gotWhoisText) {
                "WHOIS: нет ответа с whois.ru. Проверьте сеть и домен."
            } else {
                "Не удалось извлечь дату истечения из ответа whois.ru"
            }
            db.withTransaction {
                candidateDao.deleteByDomainId(domainId)
                val base = domainDao.getById(domainId) ?: return@withTransaction
                domainDao.update(
                    base.copy(
                        domainUpdateStatus = UpdateStatus.ERROR,
                        domainUpdateLastRunUtc = now,
                        domainUpdateLastError = noDataMsg,
                    ),
                )
            }
            return@withContext
        }

        // Несколько дат: автоматически самая поздняя
        val chosenInstant = uniqueInstants.max()
        val row = aggregated.getValue(chosenInstant)
        db.withTransaction {
            candidateDao.deleteByDomainId(domainId)
            val base = domainDao.getById(domainId) ?: return@withTransaction
            domainDao.update(
                base.copy(
                    domainExpiryUtcAuto = row.candidateExpiryUtc,
                    domainUpdateStatus = UpdateStatus.OK,
                    domainUpdateLastRunUtc = now,
                    domainUpdateLastError = null,
                    registrar = registrar,
                    hoster = hoster,
                    nameServersCsv = nameServers.takeIf { it.isNotEmpty() }?.joinToString(","),
                ),
            )
        }
    }

    suspend fun confirmCandidate(domainId: Long, candidateId: Long) = withContext(Dispatchers.IO) {
        val c = candidateDao.listByDomainId(domainId).firstOrNull { it.id == candidateId }
            ?: return@withContext
        val now = nowUtcMillis()
        db.withTransaction {
            candidateDao.deleteByDomainId(domainId)
            val base = domainDao.getById(domainId) ?: return@withTransaction
            domainDao.update(
                base.copy(
                    domainExpiryUtcAuto = c.candidateExpiryUtc,
                    domainUpdateStatus = UpdateStatus.OK,
                    domainUpdateLastRunUtc = now,
                    domainUpdateLastError = null,
                ),
            )
        }
    }

    private suspend fun patchDomain(
        id: Long,
        now: Long,
        status: UpdateStatus,
        lastError: String,
    ) {
        db.withTransaction {
            val base = domainDao.getById(id) ?: return@withTransaction
            domainDao.update(
                base.copy(
                    domainUpdateStatus = status,
                    domainUpdateLastRunUtc = now,
                    domainUpdateLastError = lastError,
                ),
            )
        }
    }

    private fun toAsciiDomain(domain: String): String? {
        return try {
            IDN.toASCII(domain.trim(), IDN.ALLOW_UNASSIGNED).lowercase(Locale.ROOT)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRegistrar(text: String): String? {
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val low = line.lowercase(Locale.ROOT)
            val key = when {
                low.startsWith("registrar:") -> "registrar:"
                low.startsWith("регистратор:") -> "регистратор:"
                else -> null
            } ?: continue
            val v = line.substring(key.length).trim().trimEnd('.')
            if (v.isNotEmpty()) return v.take(200)
        }
        return null
    }

    private fun parseNameServers(text: String): List<String> {
        val out = ArrayList<String>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val low = line.lowercase(Locale.ROOT)
            val key = when {
                low.startsWith("name server:") -> "name server:"
                low.startsWith("nserver:") -> "nserver:"
                low.startsWith("сервер:") -> "сервер:"
                else -> null
            } ?: continue
            val v = line.substring(key.length).trim().trimEnd('.').lowercase(Locale.ROOT)
            if (v.isNotEmpty() && v !in out) out.add(v.take(253))
        }
        return out
    }

    private fun deriveHosterFromNameServers(nameServers: List<String>): String? {
        val first = nameServers.firstOrNull() ?: return null
        val parts = first.split('.').filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val provider = parts.takeLast(2).joinToString(".")
        return when (provider) {
            "he.net" -> "Hurricane Electric (he.net)"
            "cloudflare.com" -> "Cloudflare"
            "nic.ru" -> "RU-CENTER (nic.ru)"
            "reg.ru" -> "REG.RU"
            "timeweb.ru" -> "Timeweb"
            else -> provider
        }
    }
}
