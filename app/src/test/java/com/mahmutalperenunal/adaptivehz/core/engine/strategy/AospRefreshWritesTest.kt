package com.mahmutalperenunal.adaptivehz.core.engine.strategy

import com.mahmutalperenunal.adaptivehz.core.engine.model.SettingWrite
import com.mahmutalperenunal.adaptivehz.core.engine.model.SettingsNamespace
import com.mahmutalperenunal.adaptivehz.core.system.RefreshRateController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AospRefreshWritesTest {

    @Test
    fun `low expands the lower bound before lowering the peak`() {
        val writes = AospRefreshWrites.low(60)

        assertEquals(
            listOf(
                RefreshRateController.KEY_MIN_REFRESH_RATE,
                RefreshRateController.KEY_PEAK_REFRESH_RATE
            ),
            writes.map { it.key }
        )
        assertTrue(writes.all { it.namespace == SettingsNamespace.SYSTEM })
        assertTrue(writes.all { it.writeAsFloat })
        assertEquals(listOf(60, 60), writes.map { it.intValue })
    }

    @Test
    fun `high raises the peak before raising the minimum`() {
        val writes = AospRefreshWrites.high(120)

        assertEquals(
            listOf(
                RefreshRateController.KEY_PEAK_REFRESH_RATE,
                RefreshRateController.KEY_MIN_REFRESH_RATE
            ),
            writes.map { it.key }
        )
        assertTrue(writes.all { it.writeAsFloat })
        assertEquals(listOf(120, 120), writes.map { it.intValue })
    }

    @Test
    fun `system controlled clears both platform limits`() {
        val writes = AospRefreshWrites.systemControlled()

        assertEquals(2, writes.size)
        assertTrue(writes.all { it.clear })
        assertTrue(writes.all { it.namespace == SettingsNamespace.SYSTEM })
        assertEquals(
            setOf(
                RefreshRateController.KEY_PEAK_REFRESH_RATE,
                RefreshRateController.KEY_MIN_REFRESH_RATE
            ),
            writes.map(SettingWrite::key).toSet()
        )
    }
}
