package com.mahmutalperenunal.adaptivehz.core.engine.strategy

import com.mahmutalperenunal.adaptivehz.core.engine.model.RomSkin
import com.mahmutalperenunal.adaptivehz.core.engine.model.RomSkinDetector
import com.mahmutalperenunal.adaptivehz.core.engine.model.VendorStrategy

// Provides the refresh-rate strategy that matches the running skin/ROM.
object VendorStrategyProvider {

    // OEM skins keep their vendor mechanism; AOSP-derived skins use platform limits.
    fun provide(): VendorStrategy {
        return when (RomSkinDetector.detect()) {
            RomSkin.ONE_UI -> SamsungStrategy()
            RomSkin.HYPER_OS -> XiaomiStrategy()
            RomSkin.NOTHING_OS,
            RomSkin.LINEAGE_OS,
            RomSkin.AOSP -> AospStrategy()
        }
    }
}
