# DomSSLChecker

**Android‑приложение для контроля сроков доменов и SSL‑сертификатов.**  
**Android app to track domain and SSL certificate expiry dates.**

- **WHOIS**: срок окончания домена
- **SSL/TLS**: срок окончания leaf‑сертификата (порт 443, SNI=домен)
- **Фоновые проверки**: по расписанию через WorkManager + уведомления

---

## Русский

### Зачем
Чтобы не пропускать:
- окончание регистрации домена
- окончание SSL‑сертификата сайта

### Возможности
- **Список доменов**:
  - `имя_домена`
  - `Срок домена: ...`
  - `Срок SSL: ...` или `Срок SSL: отсутствует сайт`
- **Проверка по кнопке**: WHOIS и SSL для выбранного домена
- **Автопроверка**: по расписанию (выключено/день/неделя) + уведомления
- **Сортировка**: по имени / по сроку домена

### Установка
1. Откройте **Releases**
2. Скачайте `DomSSLChecker-*.apk`
3. Установите (Android может попросить разрешение “из неизвестных источников”)

### Конфиденциальность
- Приложение хранит список доменов **локально** на устройстве (Room DB).
- Для проверок выполняются сетевые запросы к WHOIS/SSL endpoint’ам.

### Поддержка
Если что-то не работает — создайте Issue в этом репозитории и приложите:
- домен
- что нажимали
- текст ошибки (если есть)

---

## English

### Why
To avoid missing:
- domain registration expiration
- website SSL certificate expiration

### Features
- **Domain list**:
  - `domain_name`
  - `Domain expiry: ...`
  - `SSL expiry: ...` or `SSL expiry: site unavailable`
- **Manual refresh** per domain (WHOIS + SSL)
- **Scheduled background checks** (WorkManager) + notifications
- **Sorting**: by name / by domain expiry

### Install
1. Open **Releases**
2. Download `DomSSLChecker-*.apk`
3. Install (Android may ask to allow “unknown sources”)

### Privacy
- The domain list is stored **locally** on the device (Room DB).
- Network requests are performed to fetch WHOIS/SSL data.

---

## License / Лицензия
This repository does **not** publish source code. Releases contain compiled APKs.  
See [`LICENSE.md`](./LICENSE.md).

---

© by Constantin Sidorov, 2026

