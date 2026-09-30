package com.domsslchecker.notif

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.domsslchecker.MainActivity
import com.domsslchecker.app.ServiceLocator
import com.domsslchecker.data.db.AppDatabase
import com.domsslchecker.domain.DomainRenewalDeadline
import com.domsslchecker.data.db.NotifMarkEntity
import com.domsslchecker.data.repo.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class NotifRepository(
    private val appContext: Context,
) {
    private val db: AppDatabase get() = ServiceLocator.db
    private val settings = SettingsRepository(db = ServiceLocator.db)

    suspend fun scanAndNotifyIfEnabled() = withContext(Dispatchers.IO) {
        val settingsEntity = settings.getOrCreateSettings()
        if (!settingsEntity.notificationsEnabled) return@withContext

        if (Build.VERSION.SDK_INT >= 33) {
            if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
                return@withContext
            }
        }

        val globalThresholds = settings.getGlobalThresholdDays()
        if (globalThresholds.isEmpty()) return@withContext

        val now = Instant.now()
        val domains = db.domainRecordDao().listAll()
        val notifDao = db.notifMarkDao()

        for (d in domains) {
            val label = d.label?.takeIf { it.isNotBlank() } ?: d.domain
            val thresholds =
                settings.thresholdsForDomain(d.thresholdsOverrideDaysCsv, globalThresholds)
            if (thresholds.isEmpty()) continue

            val domainRenewalDeadline = DomainRenewalDeadline.renewalDeadlineUtcMillis(d)
            if (domainRenewalDeadline != null) {
                val days = ExpiryNotifyLogic.daysLeftUtcFloor(now, Instant.ofEpochMilli(domainRenewalDeadline))
                maybeNotify(
                    title = "Домен: оплатить продление",
                    label = label,
                    domainId = d.id,
                    itemKind = "DOMAIN",
                    itemIndex = 0,
                    expiryUtc = domainRenewalDeadline,
                    daysLeft = days,
                    thresholds = thresholds,
                    notifDao = notifDao,
                    nowMillis = now.toEpochMilli(),
                )
            }

            val certs = db.sslCertEntryDao().listByDomainId(d.id)
            for (c in certs) {
                val days = ExpiryNotifyLogic.daysLeftUtcFloor(now, Instant.ofEpochMilli(c.notAfterUtc))
                maybeNotify(
                    title = "SSL: сертификат",
                    label = label,
                    domainId = d.id,
                    itemKind = "SSL",
                    itemIndex = c.position,
                    expiryUtc = c.notAfterUtc,
                    daysLeft = days,
                    thresholds = thresholds,
                    notifDao = notifDao,
                    nowMillis = now.toEpochMilli(),
                )
            }
        }
    }

    private suspend fun maybeNotify(
        title: String,
        label: String,
        domainId: Long,
        itemKind: String,
        itemIndex: Int,
        expiryUtc: Long,
        daysLeft: Long,
        thresholds: List<Int>,
        notifDao: com.domsslchecker.data.db.NotifMarkDao,
        nowMillis: Long,
    ) {
        val applicable = thresholds.filter { daysLeft <= it }
        if (applicable.isEmpty()) return

        val notifyAt = ExpiryNotifyLogic.largestApplicableThreshold(daysLeft, thresholds) ?: return
        var notifyAtNewlyMarked = false

        for (t in applicable) {
            val mark = NotifMarkEntity(
                domainId = domainId,
                itemKind = itemKind,
                itemIndex = itemIndex,
                expiryUtc = expiryUtc,
                thresholdDays = t,
                firedAtUtc = nowMillis,
            )
            val inserted = try {
                notifDao.insert(mark)
            } catch (_: SQLiteConstraintException) {
                -1L
            }
            if (inserted > 0 && t == notifyAt) {
                notifyAtNewlyMarked = true
            }
        }

        if (!notifyAtNewlyMarked) return

        val key = "dom:$domainId:$itemKind:$itemIndex:$expiryUtc:$notifyAt"
        val notificationId = (key.hashCode() and 0x7fff_ffff)

        val daysText = if (daysLeft < 0) "истёк" else "осталось ~$daysLeft дн."
        val body = if (itemKind == "SSL") {
            "$label — cert#$itemIndex — $daysText (порог ${notifyAt}д)"
        } else {
            "$label — $daysText (порог ${notifyAt}д)"
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val n = NotificationCompat.Builder(appContext, DomNotifications.CHANNEL_ID)
            .setSmallIcon(com.domsslchecker.R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, n)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied
        }
    }
}
