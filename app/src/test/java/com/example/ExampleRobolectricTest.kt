package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.DailyUsageRecord
import com.example.data.db.FirewallRuleEntity
import com.example.data.model.AlertConfig
import com.example.data.model.ComparisonResult
import com.example.data.model.DataUsageStats
import com.example.data.model.FirewallPolicy
import com.example.data.model.NetworkType
import com.example.data.repository.AlertRepository
import com.example.data.repository.DataExportManager
import com.example.data.repository.QuotaConfig
import com.example.data.repository.QuotaRepository
import com.example.service.PacketParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("حارس البيانات PRO", appName)
    }

    @Test
    fun `data usage bytes formatting`() {
        val oneKb = 1024L
        val (valKb, unitKb) = DataUsageStats.formatBytes(oneKb)
        assertEquals("1.0", valKb)
        assertTrue(unitKb.contains("KB"))

        val oneMb = 1024L * 1024L
        val (valMb, unitMb) = DataUsageStats.formatBytes(oneMb)
        assertEquals("1.0", valMb)
        assertTrue(unitMb.contains("MB"))

        val twoGb = 2L * 1024L * 1024L * 1024L
        val (valGb, unitGb) = DataUsageStats.formatBytes(twoGb)
        assertEquals("2.00", valGb)
        assertTrue(unitGb.contains("GB"))
    }

    @Test
    fun `firewall policy evaluation`() {
        // ALLOW_ALL allows both
        assertTrue(FirewallPolicy.ALLOW_ALL.isAllowedOn(NetworkType.WIFI))
        assertTrue(FirewallPolicy.ALLOW_ALL.isAllowedOn(NetworkType.CELLULAR))

        // BLOCK_ALL blocks both
        assertFalse(FirewallPolicy.BLOCK_ALL.isAllowedOn(NetworkType.WIFI))
        assertFalse(FirewallPolicy.BLOCK_ALL.isAllowedOn(NetworkType.CELLULAR))

        // WIFI_ONLY allows Wi-Fi, blocks Cellular
        assertTrue(FirewallPolicy.WIFI_ONLY.isAllowedOn(NetworkType.WIFI))
        assertFalse(FirewallPolicy.WIFI_ONLY.isAllowedOn(NetworkType.CELLULAR))

        // CELLULAR_ONLY allows Cellular, blocks Wi-Fi
        assertFalse(FirewallPolicy.CELLULAR_ONLY.isAllowedOn(NetworkType.WIFI))
        assertTrue(FirewallPolicy.CELLULAR_ONLY.isAllowedOn(NetworkType.CELLULAR))
    }

    @Test
    fun `comparison result calculations`() {
        val current = 1500L
        val previous = 1000L
        val comp = ComparisonResult(current, previous)
        assertTrue(comp.isIncrease)
        assertEquals(500L, comp.diffBytes)
        assertEquals(50.0, comp.percentChange, 0.01)
        assertEquals("+50.0%", comp.formattedPercent())
    }

    @Test
    fun `alert repository deduplication`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alertRepo = AlertRepository(context)
        alertRepo.resetThresholdIfCycleRenewed()

        // First trigger for 75% should succeed
        assertTrue(alertRepo.shouldFireThresholdAlert(75))

        // Subsequent trigger for 75% or 50% in the same cycle should be suppressed
        assertFalse(alertRepo.shouldFireThresholdAlert(75))
        assertFalse(alertRepo.shouldFireThresholdAlert(50))

        // Higher threshold (90%) should fire
        assertTrue(alertRepo.shouldFireThresholdAlert(90))
    }

    @Test
    fun `packet parser test`() {
        // Construct a synthetic IPv4 TCP packet
        val packet = ByteArray(40)
        packet[0] = 0x45.toByte() // IPv4, IHL = 5 (20 bytes)
        packet[9] = 6.toByte() // TCP Protocol
        // Source IP 192.168.1.50
        packet[12] = 192.toByte(); packet[13] = 168.toByte(); packet[14] = 1.toByte(); packet[15] = 50.toByte()
        // Destination IP 1.1.1.1
        packet[16] = 1.toByte(); packet[17] = 1.toByte(); packet[18] = 1.toByte(); packet[19] = 1.toByte()
        // Dst Port 443 (HTTPS) -> 0x01BB
        packet[20] = 0x1F.toByte(); packet[21] = 0x90.toByte() // src port 8080
        packet[22] = 0x01.toByte(); packet[23] = 0xBB.toByte() // dst port 443

        val parsed = PacketParser.parse(packet, packet.size)
        assertNotNull(parsed)
        assertEquals(4, parsed?.ipVersion)
        assertEquals(443, parsed?.dstPort)
        assertTrue(parsed?.summaryAr?.contains("HTTPS") == true)
    }

    @Test
    fun `database and export manager test`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val quotaRepo = QuotaRepository(context)
        val alertRepo = AlertRepository(context)
        val exportManager = DataExportManager(db, quotaRepo, alertRepo)

        // Insert dummy record
        db.dailyUsageDao().insertOrUpdate(
            DailyUsageRecord("2026-10-28", 1000L, 2000L, 3000L)
        )

        val records = db.dailyUsageDao().getAllRecords().first()
        assertTrue(records.isNotEmpty())
        assertEquals(3000L, records.first().totalBytes)

        // Export JSON
        val json = exportManager.exportSettingsAndRulesJson()
        assertTrue(json.contains("Data Guardian PRO"))

        // Import test
        val result = exportManager.importSettingsAndRulesFromJson(json)
        assertTrue(result.isSuccess)
    }
}
