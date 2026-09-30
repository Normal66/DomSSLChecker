package com.domsslchecker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `domain_expiry_candidates` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `domain_id` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                `server_host` TEXT NOT NULL,
                `candidate_expiry_utc` INTEGER NOT NULL,
                `line` TEXT NOT NULL,
                FOREIGN KEY(`domain_id`) REFERENCES `domain_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_domain_expiry_candidates_domain_id`
            ON `domain_expiry_candidates` (`domain_id`)
            """.trimIndent(),
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `app_settings` (
                `id` INTEGER NOT NULL,
                `global_thresholds_days_csv` TEXT NOT NULL,
                `schedule_mode` TEXT NOT NULL,
                `notifications_enabled` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO `app_settings` (
                `id`, `global_thresholds_days_csv`, `schedule_mode`, `notifications_enabled`
            ) VALUES (
                1, '30,14,7,3,1', 'DAILY', 1
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notif_marks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `domain_id` INTEGER NOT NULL,
                `item_kind` TEXT NOT NULL,
                `item_index` INTEGER NOT NULL,
                `expiry_utc` INTEGER NOT NULL,
                `threshold_days` INTEGER NOT NULL,
                `fired_at_utc` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_notif_marks_domain_id_item_kind_item_index_expiry_utc_threshold_days`
            ON `notif_marks` (
                `domain_id`,
                `item_kind`,
                `item_index`,
                `expiry_utc`,
                `threshold_days`
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_settings ADD COLUMN domain_list_sort TEXT NOT NULL DEFAULT 'BY_NAME'")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE domain_records ADD COLUMN registrar TEXT")
        db.execSQL("ALTER TABLE domain_records ADD COLUMN hoster TEXT")
        db.execSQL("ALTER TABLE domain_records ADD COLUMN name_servers_csv TEXT")
    }
}
