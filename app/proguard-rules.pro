# Release hardening (R8/ProGuard).
# Keep WorkManager workers (instantiated via reflection).
-keep class com.domsslchecker.work.** extends androidx.work.ListenableWorker { *; }


