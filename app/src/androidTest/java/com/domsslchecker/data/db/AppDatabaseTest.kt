package com.domsslchecker.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndListDomainRecords() = runBlocking {
        val dao = db.domainRecordDao()
        val now = 1_700_000_000_000L
        dao.insert(
            DomainRecordEntity(
                domain = "example.com",
                createdAtUtc = now,
                updatedAtUtc = now,
                domainExpiryUtcAuto = null,
                domainExpiryUtcManual = null,
                domainExpirySource = DomainExpirySource.AUTO,
                domainUpdateStatus = UpdateStatus.OK,
                domainUpdateLastRunUtc = null,
                domainUpdateLastError = null,
                sslLastCheckedUtc = null,
                sslStatus = SslStatus.OK,
                sslLastError = null,
                thresholdsOverrideDaysCsv = null,
                label = null,
                notes = null,
            ),
        )

        val all = dao.listAll()
        assertEquals(1, all.size)
        assertEquals("example.com", all.first().domain)
    }
}

