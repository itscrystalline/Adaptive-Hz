package com.mahmutalperenunal.adaptivehz.core.engine.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RomSkinDetectorTest {

    private fun detect(
        manufacturer: String,
        brand: String = manufacturer,
        fingerprint: String = "",
        display: String = "",
        properties: Map<String, String> = emptyMap()
    ): RomSkin {
        return RomSkinDetector.detect(
            manufacturer = manufacturer,
            brand = brand,
            fingerprint = fingerprint,
            display = display,
            property = { properties[it] }
        )
    }

    @Test
    fun `nothing os is routed to the aosp mechanism`() {
        val skin = detect(
            manufacturer = "Nothing",
            properties = mapOf("ro.nothing.version.id" to "5.0")
        )

        assertEquals(RomSkin.NOTHING_OS, skin)
        assertEquals(true, skin.usesAospRefreshRateMechanism)
    }

    @Test
    fun `stock samsung stays on one ui`() {
        val skin = detect(
            manufacturer = "samsung",
            properties = mapOf("ro.build.version.sem" to "1601")
        )

        assertEquals(RomSkin.ONE_UI, skin)
        assertEquals(false, skin.usesAospRefreshRateMechanism)
    }

    @Test
    fun `samsung running lineage uses platform limits instead of one ui`() {
        val skin = detect(
            manufacturer = "samsung",
            properties = mapOf("ro.lineage.build.version" to "23.2")
        )

        assertEquals(RomSkin.LINEAGE_OS, skin)
        assertEquals(true, skin.usesAospRefreshRateMechanism)
    }

    @Test
    fun `lineage display string is detected without system properties`() {
        val skin = detect(
            manufacturer = "samsung",
            display = "lineage_a52sxq-userdebug 14 UQ1A.240205.004"
        )

        assertEquals(RomSkin.LINEAGE_OS, skin)
    }

    @Test
    fun `xiaomi hyperos keeps its vendor mechanism`() {
        val skin = detect(
            manufacturer = "Xiaomi",
            properties = mapOf("ro.mi.os.version.name" to "HyperOS 2.0")
        )

        assertEquals(RomSkin.HYPER_OS, skin)
        assertEquals(false, skin.usesAospRefreshRateMechanism)
    }

    @Test
    fun `xiaomi running lineage uses platform limits instead of hyperos`() {
        val skin = detect(
            manufacturer = "Xiaomi",
            properties = mapOf("ro.lineage.version" to "21-20240101")
        )

        assertEquals(RomSkin.LINEAGE_OS, skin)
    }

    @Test
    fun `unrecognized hardware falls back to aosp limits`() {
        val skin = detect(manufacturer = "Longcheer", brand = "generic")

        assertEquals(RomSkin.AOSP, skin)
    }
}
