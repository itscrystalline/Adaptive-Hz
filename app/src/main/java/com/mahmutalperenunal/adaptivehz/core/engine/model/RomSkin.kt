package com.mahmutalperenunal.adaptivehz.core.engine.model

import android.annotation.SuppressLint
import android.os.Build
import java.util.Locale

/**
 * Android skin/ROM family that decides which refresh-rate mechanism is authoritative.
 *
 * OEM skins (One UI, HyperOS) expose vendor-specific settings, while AOSP-derived
 * skins (Nothing OS, LineageOS, plain AOSP/GSI) honor the platform
 * `peak_refresh_rate` / `min_refresh_rate` limits in Settings.System.
 */
enum class RomSkin(val displayName: String) {
    ONE_UI("Samsung One UI"),
    HYPER_OS("Xiaomi HyperOS/MIUI"),
    NOTHING_OS("Nothing OS"),
    LINEAGE_OS("LineageOS"),
    AOSP("AOSP");

    val usesAospRefreshRateMechanism: Boolean
        get() = this != ONE_UI && this != HYPER_OS
}

/**
 * Detects the running skin/ROM family.
 *
 * ROM markers are checked before manufacturer fields so that OEM hardware running an
 * AOSP-based ROM (for example a Samsung or Xiaomi device on LineageOS) is routed to the
 * AOSP mechanism instead of the stock vendor mechanism.
 */
object RomSkinDetector {

    fun detect(): RomSkin = detect(
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        fingerprint = Build.FINGERPRINT,
        display = Build.DISPLAY,
        property = SystemPropertyReader::get
    )

    internal fun detect(
        manufacturer: String,
        brand: String,
        fingerprint: String,
        display: String,
        property: (String) -> String?
    ): RomSkin {
        val normalizedManufacturer = manufacturer.lowercase(Locale.ROOT)
        val normalizedBrand = brand.lowercase(Locale.ROOT)
        val normalizedFingerprint = fingerprint.lowercase(Locale.ROOT)
        val normalizedDisplay = display.lowercase(Locale.ROOT)

        fun hasProperty(vararg keys: String): Boolean =
            keys.any { !property(it).isNullOrBlank() }

        val isLineageRom = hasProperty(
            "ro.lineage.version",
            "ro.lineage.build.version",
            "ro.lineage.device",
            "ro.modversion"
        ) || normalizedFingerprint.contains("lineage") ||
            normalizedDisplay.contains("lineage")

        val isNothing = normalizedManufacturer.contains("nothing") ||
            normalizedBrand.contains("nothing") ||
            hasProperty("ro.nothing.version.id")

        val isXiaomiFamily = listOf(
            normalizedManufacturer,
            normalizedBrand
        ).any { value ->
            value.contains("xiaomi") || value.contains("redmi") || value.contains("poco")
        }

        val isHyperOs = hasProperty(
            "ro.mi.os.version.name",
            "ro.mi.os.version.incremental",
            "ro.miui.ui.version.name",
            "ro.miui.ui.version.code"
        )

        val isSamsung = normalizedManufacturer.contains("samsung") ||
            normalizedBrand.contains("samsung")

        return when {
            isLineageRom -> RomSkin.LINEAGE_OS
            isNothing -> RomSkin.NOTHING_OS
            isHyperOs || isXiaomiFamily -> RomSkin.HYPER_OS
            isSamsung -> RomSkin.ONE_UI
            else -> RomSkin.AOSP
        }
    }
}

/** Reads Android system properties through the hidden framework API when available. */
internal object SystemPropertyReader {

    @SuppressLint("PrivateApi")
    fun get(key: String): String? = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getMethod(
            "get",
            String::class.java,
            String::class.java
        )

        method.invoke(null, key, "") as? String
    }.getOrNull()
}
