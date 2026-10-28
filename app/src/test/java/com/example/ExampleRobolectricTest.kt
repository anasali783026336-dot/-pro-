package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DataUsageStats
import com.example.data.model.FirewallPolicy
import com.example.data.model.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
