package com.mahmutalperenunal.adaptivehz.core.system

import com.mahmutalperenunal.adaptivehz.core.engine.model.RomSkin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HyperOsCustomRefreshRateContractTest {
    @Test
    fun `observed active value wins over nominal HyperOS key`() {
        val ordered = HyperOsCustomRefreshRateContract.orderCandidates(
            candidates = listOf(
                HyperOsCustomRefreshRateContract.Candidate("miui_refresh_rate", 60),
                HyperOsCustomRefreshRateContract.Candidate("user_refresh_rate", 120)
            ),
            activeRefreshRate = 120
        )

        assertEquals("user_refresh_rate", ordered.first().key)
    }

    @Test
    fun `active lease wins to avoid cross-key orphan`() {
        val ordered = HyperOsCustomRefreshRateContract.orderCandidates(
            candidates = listOf(
                HyperOsCustomRefreshRateContract.Candidate("miui_refresh_rate", 60),
                HyperOsCustomRefreshRateContract.Candidate("user_refresh_rate", 120)
            ),
            activeRefreshRate = 120,
            activeLeaseKey = "miui_refresh_rate"
        )

        assertEquals("miui_refresh_rate", ordered.first().key)
    }

    @Test
    fun `custom selection covers One UI HyperOS 3 and AOSP skins`() {
        assertTrue(CustomRefreshRateController.isCustomSelectionSupported(RomSkin.ONE_UI, null, 2))
        assertTrue(CustomRefreshRateController.isCustomSelectionSupported(RomSkin.HYPER_OS, 3, 4))
        assertFalse(CustomRefreshRateController.isCustomSelectionSupported(RomSkin.HYPER_OS, 2, 4))
        assertTrue(CustomRefreshRateController.isCustomSelectionSupported(RomSkin.NOTHING_OS, null, 4))
        assertTrue(CustomRefreshRateController.isCustomSelectionSupported(RomSkin.LINEAGE_OS, null, 4))
        assertTrue(CustomRefreshRateController.isCustomSelectionSupported(RomSkin.AOSP, null, 4))
        assertFalse(CustomRefreshRateController.isCustomSelectionSupported(RomSkin.NOTHING_OS, null, 1))
        assertFalse(CustomRefreshRateController.isCustomSelectionSupported(RomSkin.HYPER_OS, 3, 1))
    }
}
