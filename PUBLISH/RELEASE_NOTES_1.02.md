# DomSSLChecker 1.02

## Русский

### Новое
- **Дедлайн оплаты домена**: в списке и уведомлениях — дата «оплатить до» (WHOIS − 1 календарный месяц); на экране домена также показана сырая дата окончания регистрации.
- **«Обновить всё»** на главном экране: WHOIS + SSL для всех доменов, затем проверка порогов уведомлений.
- **Фоновое расписание** теперь реально обновляет WHOIS и SSL (`MaintenanceWorker`), а не только читает старые данные из БД.
- **Уведомления**: одно push-сообщение за проход на домен/сертификат (без пачки по всем порогам); проверка порогов после ручного обновления WHOIS/SSL.
- В **настройках** — статус системных push-разрешений.

### Исправлено
- Расписание «раз в сутки/неделю» больше не привязано только к включённым уведомлениям (обновление данных идёт по расписанию; push — отдельный переключатель).

---

## English

### Added
- **Domain pay-by deadline**: list and notifications use registry expiry minus one calendar month; detail screen shows both pay-by and raw WHOIS expiry.
- **Refresh all** on the main screen (WHOIS + SSL for every domain, then threshold scan).
- **Scheduled background job** refreshes WHOIS/TLS, not only DB scan.
- **Notifications**: one push per entity per scan; threshold check after manual WHOIS/SSL refresh.
- Settings show **system notification permission** status.

### Fixed
- Schedule runs independently of the in-app notifications toggle (data refresh vs push).
