package com.domsslchecker.data.repo

import androidx.room.withTransaction
import com.domsslchecker.data.db.AppDatabase
import com.domsslchecker.data.db.SslCertEntryEntity
import com.domsslchecker.data.db.SslStatus
import com.domsslchecker.net.ip.AsnLookup
import com.domsslchecker.net.tls.TlsProber
import com.domsslchecker.net.tls.TlsProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.IDN
import java.net.InetAddress
import java.util.Locale

class SslRepository(
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val nowUtcMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val domainDao = db.domainRecordDao()
    private val certDao = db.sslCertEntryDao()

    suspend fun listCerts(domainId: Long): List<SslCertEntryEntity> =
        withContext(Dispatchers.IO) {
            certDao.listByDomainId(domainId)
        }

    suspend fun leafExpiryUtcByDomainIds(domainIds: List<Long>): Map<Long, Long> =
        withContext(Dispatchers.IO) {
            if (domainIds.isEmpty()) return@withContext emptyMap()
            certDao.leafExpiryUtcByDomainIds(domainIds).associate { it.domainId to it.notAfterUtc }
        }

    suspend fun probeTls443AndStore(domainId: Long) = withContext(Dispatchers.IO) {
        val record = domainDao.getById(domainId) ?: return@withContext
        val ascii = toAsciiDomain(record.domain) ?: run {
            patchSslError(
                domainId = domainId,
                message = "Некорректный домен",
            )
            return@withContext
        }

        val now = nowUtcMillis()
        val global = settingsRepository.getGlobalThresholdDays()
        val effectiveThresholds = settingsRepository.thresholdsForDomain(
            record.thresholdsOverrideDaysCsv,
            global,
        )
        val warnDays = effectiveThresholds.minOrNull() ?: 30
        val warnWindowMs: Long = warnDays * 24L * 60L * 60L * 1000L
        val result: TlsProbeResult = try {
            TlsProber.probe(hostAscii = ascii, port = 443)
        } catch (e: Exception) {
            patchSslHandshakeError(
                domainId = domainId,
                now = now,
                message = e.message?.take(500) ?: "Ошибка TLS",
            )
            return@withContext
        }

        val hosterByIp = deriveHosterByIpOrNull(ascii)

        db.withTransaction {
            certDao.deleteByDomainId(domainId)
            val rows = result.certs.map {
                SslCertEntryEntity(
                    domainId = domainId,
                    position = it.position,
                    subject = it.subject,
                    issuer = it.issuer,
                    notBeforeUtc = it.notBeforeUtc,
                    notAfterUtc = it.notAfterUtc,
                    serialNumber = it.serialNumber,
                    sha256Fingerprint = it.sha256Fingerprint,
                )
            }
            if (rows.isNotEmpty()) {
                certDao.insertAll(rows)
            }

            val base = domainDao.getById(domainId) ?: return@withTransaction
            val leafNotAfter = rows.minByOrNull { it.position }?.notAfterUtc
            val status = when {
                result.validationError != null -> SslStatus.ERROR
                leafNotAfter != null && (leafNotAfter - now) <= warnWindowMs -> SslStatus.WARN
                else -> SslStatus.OK
            }

            domainDao.update(
                base.copy(
                    sslLastCheckedUtc = now,
                    sslStatus = status,
                    sslLastError = result.validationError,
                    hoster = hosterByIp ?: base.hoster,
                ),
            )
        }
    }

    private suspend fun patchSslHandshakeError(domainId: Long, now: Long, message: String) {
        db.withTransaction {
            val base = domainDao.getById(domainId) ?: return@withTransaction
            domainDao.update(
                base.copy(
                    sslLastCheckedUtc = now,
                    sslStatus = SslStatus.ERROR,
                    sslLastError = message,
                ),
            )
        }
    }

    private suspend fun patchSslError(domainId: Long, message: String) {
        val now = nowUtcMillis()
        patchSslHandshakeError(domainId, now, message)
    }

    private fun toAsciiDomain(domain: String): String? {
        return try {
            IDN.toASCII(domain.trim(), IDN.ALLOW_UNASSIGNED).lowercase(Locale.ROOT)
        } catch (_: Exception) {
            null
        }
    }

    private fun deriveHosterByIpOrNull(hostAscii: String): String? {
        val ip = try {
            InetAddress.getByName(hostAscii).hostAddress
        } catch (_: Exception) {
            return null
        } ?: return null

        val asn = try {
            AsnLookup.tryLookup(ip)
        } catch (_: Exception) {
            null
        }

        val label = asn?.let { deriveCdnOrProviderLabel(it.name, it.description) }
        if (asn != null) {
            val base = label ?: (asn.description ?: asn.name) ?: "AS${asn.asn}"
            return "$base (AS${asn.asn})"
        }

        // Fallback: reverse DNS (often hints provider/CDN).
        val ptr = try {
            InetAddress.getByName(ip).canonicalHostName
        } catch (_: Exception) {
            null
        }
        return ptr?.takeIf { it.isNotBlank() && it != ip }?.let { "rDNS: $it" }
    }

    private fun deriveCdnOrProviderLabel(name: String?, description: String?): String? {
        val s = buildString {
            if (!name.isNullOrBlank()) append(name)
            if (!description.isNullOrBlank()) {
                if (isNotEmpty()) append(' ')
                append(description)
            }
        }.lowercase(Locale.ROOT)

        return when {
            "cloudflare" in s -> "Cloudflare"
            "fastly" in s -> "Fastly"
            "akamai" in s -> "Akamai"
            "amazon" in s || "aws" in s -> "AWS"
            "google" in s || "gcp" in s -> "Google Cloud"
            "microsoft" in s || "azure" in s -> "Azure"
            "digitalocean" in s -> "DigitalOcean"
            "ovh" in s -> "OVH"
            "hetzner" in s -> "Hetzner"
            else -> null
        }
    }
}
