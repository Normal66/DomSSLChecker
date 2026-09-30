# DomSSLChecker

**Android‑приложение для контроля сроков доменов и SSL‑сертификатов.**  
**Android app to track domain and SSL certificate expiry dates.**

- **Current release / Текущий релиз**: **1.02**

- **WHOIS**: срок окончания домена (через `whois.ru`), в UI — **дедлайн оплаты** (минус 1 месяц от даты реестра)
- **SSL/TLS**: срок окончания leaf‑сертификата (порт 443, SNI=домен)
- **Фоновое обновление**: WHOIS + SSL по расписанию (WorkManager) + уведомления по порогам
- **Хостер/провайдер**: определяется по NS и/или по A/ASN (часто CDN)

Подробнее для пользователей: [PUBLISH/README.md](PUBLISH/README.md)

---

## Русский

### Установка
1. Откройте **Releases**
2. Скачайте `DomSSLChecker-1.02.apk` (собирается CI при теге `1.02`)
3. Установите APK

### Сборка из исходников

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Release APK: push тега `X.YY` → GitHub Actions (секреты: [doc/github-release-secrets.md](doc/github-release-secrets.md)).

### Документация
- [doc/tz.md](doc/tz.md) — ТЗ
- [PUBLISH/RELEASE_NOTES_1.02.md](PUBLISH/RELEASE_NOTES_1.02.md) — что нового

---

## English

Install from **Releases** (`DomSSLChecker-1.02.apk`). Build with `./gradlew :app:assembleDebug`.

---

## License / Лицензия

See [LICENSE.md](LICENSE.md) / [PUBLISH/LICENSE.md](PUBLISH/LICENSE.md).

© by Constantin Sidorov, 2026
