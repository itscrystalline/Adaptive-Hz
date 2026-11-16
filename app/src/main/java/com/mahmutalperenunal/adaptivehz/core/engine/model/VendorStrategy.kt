package com.mahmutalperenunal.adaptivehz.core.engine.model

import android.content.Context

/**
 * Defines how each skin maps logical refresh-rate requests
 * to concrete system setting writes.
 *
 * Strategies return a list because some mechanisms need more than one key:
 * AOSP skins write both `min_refresh_rate` and `peak_refresh_rate`.
 */
interface VendorStrategy {

    val name: String

    /**
     * LOW state used by Adaptive mode and FORCE_MIN.
     */
    fun desiredLow(context: Context): List<SettingWrite>

    /**
     * Temporary HIGH state used by Adaptive mode after user interaction.
     */
    fun desiredHigh(context: Context): List<SettingWrite>

    /**
     * Persistent minimum mode.
     *
     * Defaults to desiredLow() so existing Samsung/Other strategies
     * keep their current behavior without requiring any changes.
     */
    fun desiredForceMinimum(context: Context): List<SettingWrite> {
        return desiredLow(context)
    }

    /**
     * Persistent maximum mode.
     *
     * Defaults to desiredHigh() so existing Samsung/Other strategies
     * keep their current behavior without requiring any changes.
     */
    fun desiredForceMaximum(context: Context): List<SettingWrite> {
        return desiredHigh(context)
    }

    /**
     * Restores skin/system-controlled refresh-rate behavior.
     * An empty list means no write is required.
     */
    fun desiredSystemControlled(context: Context): List<SettingWrite>

    fun tuning(): VendorTuning
}
