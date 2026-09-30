# DomSSLChecker

Android app: domain registration and SSL certificate expiry tracking.

User-facing install notes: [PUBLISH/README.md](PUBLISH/README.md)

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Release APK (signed) via GitHub Actions on tag push — secrets: [doc/github-release-secrets.md](doc/github-release-secrets.md).

## Docs

- [doc/tz.md](doc/tz.md) — product spec
- [doc/workflow.md](doc/workflow.md) — dev workflow
