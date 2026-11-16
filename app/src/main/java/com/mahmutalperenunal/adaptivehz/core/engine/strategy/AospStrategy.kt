package com.mahmutalperenunal.adaptivehz.core.engine.strategy

import android.content.Context
import com.mahmutalperenunal.adaptivehz.core.engine.model.SettingWrite
import com.mahmutalperenunal.adaptivehz.core.engine.model.SettingsNamespace
import com.mahmutalperenunal.adaptivehz.core.engine.model.VendorStrategy
import com.mahmutalperenunal.adaptivehz.core.engine.model.VendorTuning
import com.mahmutalperenunal.adaptivehz.core.system.RefreshRateController

/**
 * Refresh-rate strategy for AOSP-derived skins (Nothing OS, LineageOS, plain AOSP/GSI).
 *
 * These ROMs ignore One UI's `secure/refresh_rate_mode` and control the display through
 * the platform limits in Settings.System:
 *
 * - `peak_refresh_rate` is the upper bound.
 * - `min_refresh_rate` is the lower bound.
 *
 * A fixed rate therefore requires both keys. Writes are ordered so the bound that
 * expands the range is applied first, preventing the framework from clamping the
 * transition (lowering: min then peak, raising: peak then min).
 *
 * Returning to system control deletes both keys so the ROM's own policy applies.
 */
class AospStrategy : VendorStrategy {

    override val name: String = "AOSP (Nothing OS/LineageOS)"

    // Fixed minimum limit used by Adaptive LOW and FORCE_MIN.
    override fun desiredLow(context: Context): List<SettingWrite> {
        val (minHz, _) = RefreshRateController.resolveDisplayMinMax(context.applicationContext)
        return AospRefreshWrites.low(minHz)
    }

    // Fixed maximum limit used by Adaptive HIGH and FORCE_MAX.
    override fun desiredHigh(context: Context): List<SettingWrite> {
        val (_, maxHz) = RefreshRateController.resolveDisplayMinMax(context.applicationContext)
        return AospRefreshWrites.high(maxHz)
    }

    // Deletes both platform limits so the ROM regains control.
    override fun desiredSystemControlled(context: Context): List<SettingWrite> {
        return AospRefreshWrites.systemControlled()
    }

    // Provides AOSP-specific interaction and event tuning for the adaptive engine.
    override fun tuning(): VendorTuning {
        return VendorTuning(
            interactionIdleTimeoutMs = 2_000L,
            eventCoalescingWindowMs = 60L,
            allowContentChangeBoost = true,
            allowScrollBoost = true,
            extendBoostOnWindowChange = true
        )
    }
}

/** Pure write builders, kept separate so transitions are unit-testable without a device. */
internal object AospRefreshWrites {

    fun low(minHz: Int): List<SettingWrite> {
        return listOf(
            SettingWrite(
                namespace = SettingsNamespace.SYSTEM,
                key = RefreshRateController.KEY_MIN_REFRESH_RATE,
                intValue = minHz,
                label = "system/min_refresh_rate=$minHz (Minimum)",
                writeAsFloat = true
            ),
            SettingWrite(
                namespace = SettingsNamespace.SYSTEM,
                key = RefreshRateController.KEY_PEAK_REFRESH_RATE,
                intValue = minHz,
                label = "system/peak_refresh_rate=$minHz (Minimum)",
                writeAsFloat = true
            )
        )
    }

    fun high(maxHz: Int): List<SettingWrite> {
        return listOf(
            SettingWrite(
                namespace = SettingsNamespace.SYSTEM,
                key = RefreshRateController.KEY_PEAK_REFRESH_RATE,
                intValue = maxHz,
                label = "system/peak_refresh_rate=$maxHz (Maximum)",
                writeAsFloat = true
            ),
            SettingWrite(
                namespace = SettingsNamespace.SYSTEM,
                key = RefreshRateController.KEY_MIN_REFRESH_RATE,
                intValue = maxHz,
                label = "system/min_refresh_rate=$maxHz (Maximum)",
                writeAsFloat = true
            )
        )
    }

    fun systemControlled(): List<SettingWrite> {
        return listOf(
            SettingWrite(
                namespace = SettingsNamespace.SYSTEM,
                key = RefreshRateController.KEY_PEAK_REFRESH_RATE,
                intValue = 0,
                label = "system/peak_refresh_rate (System Controlled)",
                clear = true
            ),
            SettingWrite(
                namespace = SettingsNamespace.SYSTEM,
                key = RefreshRateController.KEY_MIN_REFRESH_RATE,
                intValue = 0,
                label = "system/min_refresh_rate (System Controlled)",
                clear = true
            )
        )
    }
}
