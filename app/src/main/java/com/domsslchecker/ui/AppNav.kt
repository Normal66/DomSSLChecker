package com.domsslchecker.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import android.os.Build
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.filterIsInstance
import com.domsslchecker.data.db.UpdateStatus
import com.domsslchecker.data.db.SslStatus
import com.domsslchecker.data.db.ScheduleMode
import com.domsslchecker.data.db.DomainListSort
import com.domsslchecker.data.db.DomainRecordEntity
import com.domsslchecker.data.db.DomainExpirySource
import com.domsslchecker.domain.DomainRenewalDeadline
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.net.IDN
import java.util.Locale

@Composable
fun DomSslCheckerAppRoot(
    vm: DomainsViewModel,
    settingsVm: SettingsViewModel,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "list",
    ) {
        composable("list") {
            DomainListScreen(
                vm = vm,
                settingsVm = settingsVm,
                onOpenAdd = { navController.navigate("add") },
                onOpenDomain = { id -> navController.navigate("domain/$id") },
                onOpenSettings = { navController.navigate("settings") },
            )
        }

        composable("settings") {
            SettingsScreen(
                vm = settingsVm,
                onBack = { navController.popBackStack() },
            )
        }

        composable("add") {
            AddDomainScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "domain/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            DomainDetailScreen(
                vm = vm,
                id = id,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DomainListScreen(
    vm: DomainsViewModel,
    settingsVm: SettingsViewModel,
    onOpenAdd: () -> Unit,
    onOpenDomain: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val domains by vm.domains.collectAsState()
    val sslLeafExpiryUtc by vm.sslLeafExpiryUtc.collectAsState()
    val settings by settingsVm.settings.collectAsState()
    val listHighlightId by vm.listHighlightId.collectAsState()
    val bulkRefreshRunning by vm.bulkRefreshRunning.collectAsState()
    val sort = settings?.domainListSort ?: DomainListSort.BY_NAME
    val sorted = remember(sort, domains) { sortedDomainList(domains, sort) }
    val dateFmt = remember {
        DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneOffset.UTC)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DomSSLChecker") },
                actions = {
                    TextButton(onClick = onOpenSettings) { Text("Настройки") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onOpenAdd,
                modifier = Modifier.fillMaxWidth(),
                enabled = !bulkRefreshRunning,
            ) {
                Text("Добавить домен")
            }

            Button(
                onClick = { vm.refreshAllDomains() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !bulkRefreshRunning && sorted.isNotEmpty(),
            ) {
                Text(if (bulkRefreshRunning) "Обновление…" else "Обновить всё (WHOIS + SSL)")
            }
            if (bulkRefreshRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = "Ваши домены",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = when (sort) {
                    DomainListSort.BY_NAME -> "Сортировка: по имени (настройки)"
                    DomainListSort.BY_DOMAIN_EXPIRY -> "Сортировка: по сроку окончания (настройки)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (sorted.isEmpty()) {
                Text(
                    text = "Список пуст. Добавьте первый домен.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 560.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(sorted, key = { it.id }) { item ->
                        val isCurrent = listHighlightId == item.id
                        val title = IDN.toUnicode(item.domain)
                        val exp = DomainRenewalDeadline.renewalDeadlineUtcMillis(item)
                        val expText = if (exp != null) {
                            "Оплатить домен до: ${dateFmt.format(Instant.ofEpochMilli(exp))} UTC"
                        } else {
                            "Оплатить домен до: —"
                        }
                        val sslExp = sslLeafExpiryUtc[item.id]
                        val sslText = if (sslExp != null) {
                            "Срок SSL: ${dateFmt.format(Instant.ofEpochMilli(sslExp))} UTC"
                        } else {
                            "Срок SSL: отсутствует сайт"
                        }
                        Surface(
                            onClick = {
                                vm.setListHighlightId(item.id)
                                onOpenDomain(item.id)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shadowElevation = if (isCurrent) 3.dp else 0.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = expText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = sslText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsState()

    var thresholds by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ScheduleMode.DAILY) }
    var enabled by remember { mutableStateOf(true) }
    var listSort by remember { mutableStateOf(DomainListSort.BY_NAME) }

    LaunchedEffect(settings) {
        val s = settings ?: return@LaunchedEffect
        thresholds = s.globalThresholdsDaysCsv
        mode = s.scheduleMode
        enabled = s.notificationsEnabled
        listSort = s.domainListSort
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Пороги уведомлений (дни, через запятую)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = thresholds,
                onValueChange = { thresholds = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text("Фоновое обновление (WHOIS + SSL)", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { mode = ScheduleMode.OFF }) { Text("Выключено") }
                TextButton(onClick = { mode = ScheduleMode.DAILY }) { Text("Раз в сутки") }
                TextButton(onClick = { mode = ScheduleMode.WEEKLY }) { Text("Раз в неделю") }
                Text("Текущее: $mode", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "После обновления проверяются пороги уведомлений (если включены).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Уведомления в приложении", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { enabled = !enabled }) {
                Text(if (enabled) "Включены (нажмите чтобы выключить)" else "Выключены (нажмите чтобы включить)")
            }
            val systemNotifOk = if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED &&
                    NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            } else {
                NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            }
            Text(
                text = if (systemNotifOk) {
                    "Системные push: разрешены"
                } else {
                    "Системные push: запрещены (Настройки Android → приложение → уведомления)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (systemNotifOk) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )

            Text("Сортировка списка на главной", style = MaterialTheme.typography.titleMedium)
            val sortOptions = listOf(
                DomainListSort.BY_NAME to "По имени",
                DomainListSort.BY_DOMAIN_EXPIRY to "По сроку",
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                sortOptions.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = listSort == value,
                        onClick = { listSort = value },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = sortOptions.size,
                        ),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            Text(
                "Без срока домена в конце при сортировке по дате.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = {
                    vm.save(
                        context = ctx,
                        globalThresholdsDaysCsv = thresholds,
                        mode = mode,
                        enabled = enabled,
                        domainListSort = listSort,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "© by Constantin Sidorov, 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDomainScreen(
    vm: DomainsViewModel,
    onBack: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val error by vm.lastError.collectAsState()
    val whoisLoading by vm.whoisLoading.collectAsState()
    val sslLoading by vm.sslLoading.collectAsState()
    val busy = whoisLoading || sslLoading

    LaunchedEffect(vm) {
        vm.events
            .filterIsInstance<DomainsUiEvent.NavBackAfterAdd>()
            .collect {
                onBack()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый домен") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = when {
                        whoisLoading && sslLoading -> "Проверяем WHOIS и SSL…"
                        whoisLoading -> "Проверяем WHOIS…"
                        else -> "Проверяем SSL…"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    vm.clearError()
                },
                label = { Text("Домен (например example.com)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !busy,
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { vm.addDomain(input) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            ) {
                Text(if (busy) "Проверяем…" else "Сохранить")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DomainDetailScreen(
    vm: DomainsViewModel,
    id: Long,
    onBack: () -> Unit,
) {
    val selected by vm.selected.collectAsState()
    val candidates by vm.expiryCandidates.collectAsState()
    val whoisLoading by vm.whoisLoading.collectAsState()
    val sslCerts by vm.sslCerts.collectAsState()
    val sslLoading by vm.sslLoading.collectAsState()
    var labelText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var thresholdsText by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }
    val dateFmt = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC)
    }

    LaunchedEffect(id) {
        vm.loadById(id)
    }

    LaunchedEffect(selected) {
        val d = selected ?: return@LaunchedEffect
        if (d.id != id) return@LaunchedEffect
        labelText = d.label.orEmpty()
        notesText = d.notes.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Домен") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val domain = selected
            if (domain == null || domain.id != id) {
                Text("Загрузка…", style = MaterialTheme.typography.bodyLarge)
                return@Column
            }

            val unicodeDomain = remember(domain.domain) { IDN.toUnicode(domain.domain) }
            Text(unicodeDomain, style = MaterialTheme.typography.headlineSmall)
            if (unicodeDomain.lowercase(Locale.ROOT) != domain.domain.lowercase(Locale.ROOT)) {
                Text(
                    text = domain.domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "ID: ${domain.id}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            domain.registrar?.let {
                Text(
                    text = "Регистратор: $it",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            domain.hoster?.let {
                Text(
                    text = "Хостер/NS: $it",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (whoisLoading) {
                CircularProgressIndicator()
            }

            Text(
                text = "Статус WHOIS: ${domain.domainUpdateStatus}",
                style = MaterialTheme.typography.bodyMedium,
            )

            domain.domainUpdateLastError?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            val auto = domain.domainExpiryUtcAuto
            if (auto != null) {
                val registry = Instant.ofEpochMilli(auto)
                val payBy = Instant.ofEpochMilli(DomainRenewalDeadline.minusOneCalendarMonthUtc(auto))
                Text(
                    text = "Оплатить до (UTC): ${dateFmt.format(payBy)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Окончание регистрации WHOIS (UTC): ${dateFmt.format(registry)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Срок (авто): —",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Button(
                onClick = { vm.refreshDomainWhois(domain.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Обновить срок (WHOIS)")
            }

            if (domain.domainUpdateStatus == UpdateStatus.NEEDS_CONFIRMATION && candidates.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Нужно подтверждение: выберите дату",
                    style = MaterialTheme.typography.titleMedium,
                )
                for (c in candidates) {
                    val inst = Instant.ofEpochMilli(c.candidateExpiryUtc)
                    Text(
                        text = c.line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { vm.confirmExpiryCandidate(domain.id, c.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Выбрать: ${dateFmt.format(inst)} (${c.serverHost})")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("SSL/TLS (443, SNI)", style = MaterialTheme.typography.titleMedium)

            if (sslLoading) {
                CircularProgressIndicator()
            }

            Text("Статус SSL: ${domain.sslStatus}", style = MaterialTheme.typography.bodyMedium)
            val sslChecked = domain.sslLastCheckedUtc
            if (sslChecked != null) {
                val inst = Instant.ofEpochMilli(sslChecked)
                Text(
                    text = "Проверено (UTC): ${dateFmt.format(inst)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Проверено: —",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            domain.sslLastError?.let { err ->
                val color = if (domain.sslStatus == SslStatus.ERROR) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = err,
                    color = color,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { vm.refreshDomainTls(domain.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Проверить SSL (443)")
            }

            for (c in sslCerts) {
                val after = Instant.ofEpochMilli(c.notAfterUtc)
                Text(
                    text = "#${c.position}: NotAfter(UTC) ${dateFmt.format(after)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = c.subject,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "fp256=${c.sha256Fingerprint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = labelText,
                onValueChange = { labelText = it },
                label = { Text("Название (опционально)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Заметка") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                minLines = 3,
            )

            OutlinedTextField(
                value = thresholdsText,
                onValueChange = { thresholdsText = it },
                label = { Text("Пороги уведомлений (дни, CSV)") },
                supportingText = {
                    Text("Пусто = как в настройках приложения. Пример: 30, 14, 7, 1")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                singleLine = true,
            )

            Button(
                onClick = {
                    vm.updateLabelNotes(
                        domain.id,
                        labelText,
                        notesText,
                        thresholdsText,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }

            Button(
                onClick = { showDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Удалить")
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Удалить домен?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        vm.deleteById(id)
                        onBack()
                    },
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Отмена") }
            },
        )
    }
}

private fun effectiveDomainExpiryUtc(d: DomainRecordEntity): Long? =
    DomainRenewalDeadline.renewalDeadlineUtcMillis(d)

private fun sortedDomainList(
    items: List<DomainRecordEntity>,
    sort: DomainListSort,
): List<DomainRecordEntity> = when (sort) {
    DomainListSort.BY_NAME -> items.sortedWith(
        compareBy { it.domain.lowercase(Locale.ROOT) },
    )
    DomainListSort.BY_DOMAIN_EXPIRY -> items.sortedWith(
        compareBy<DomainRecordEntity> { d ->
            effectiveDomainExpiryUtc(d) ?: Long.MAX_VALUE
        }.thenBy { it.domain.lowercase(Locale.ROOT) },
    )
}
