## Release checklist — 1.01 (GitHub, без исходников)

Цель: репозиторий содержит только описание и релизы (APK). Исходный код не публикуем.

---

### 1) Проверить артефакты локально

В папке `F:\Develop\DomSSLChecker\PUBLISH\` должно быть:
- `README.md`
- `LICENSE.md`
- `RELEASE_NOTES_1.01.md`
- `DomSSLChecker-1.01.apk` (**подписанный**)

Не загружать в GitHub:
- `domsslchecker-release.jks`
- `release-signing-credentials.txt`

---

### 2) Обновить файлы в репозитории

Repo: `Normal66/DomSSLChecker`

Загрузить/обновить файлы (через Web → Add file → Upload files):
- `README.md`
- `LICENSE.md`
- `RELEASE_NOTES_1.01.md`

Коммит‑сообщение (пример):
- `Update docs for 1.01`

---

### 3) Создать релиз на GitHub

GitHub → Releases → Draft a new release

- **Tag**: `1.01`
- **Title**: `DomSSLChecker 1.01`
- **Body**: вставить текст из `RELEASE_NOTES_1.01.md`
- **Attach binaries**:
  - `DomSSLChecker-1.01.apk`

Нажать **Publish release**.

---

### 4) Быстрая проверка после публикации

- Открыть релиз `1.01` и скачать APK
- Установить на телефон
- Добавить кириллический домен (например `найдись-ка.рф`) и убедиться:
  - в UI видно Unicode + punycode
  - “Хостер/NS” заполняется после SSL проверки

