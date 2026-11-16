package com.mahmutalperenunal.adaptivehz.core.engine.model

// Android settings namespaces that may contain refresh-rate related keys.
enum class SettingsNamespace {
    SECURE,
    SYSTEM,
    GLOBAL
}

// Represents a single write operation targeting an Android settings namespace.
//
// AOSP-based skins store refresh-rate limits as floats in Settings.System
// (peak_refresh_rate / min_refresh_rate) and express "system controlled" by
// removing the keys entirely, hence the float and clear modes.
data class SettingWrite(
    val namespace: SettingsNamespace,
    val key: String,
    val intValue: Int,

    // Human-readable representation used in logs and diagnostics.
    val label: String = "${namespace.name.lowercase()}/$key=$intValue",

    // Writes the value with putFloat instead of putInt (AOSP min/peak limits).
    val writeAsFloat: Boolean = false,

    // Deletes the setting, returning the key to system control.
    val clear: Boolean = false
)

// Represents the possible outcomes of a refresh-rate settings operation.
sealed interface RefreshRateApplyResult {

    // The attempted to write operation, or null when no write was required.
    val requestedWrite: SettingWrite?

    // The requested value was written and successfully verified.
    data class AppliedAndVerified(
        override val requestedWrite: SettingWrite,
        val readBackValue: Int,
        val genericOverrideApplied: Boolean = false
    ) : RefreshRateApplyResult

    // The requested value was written, but reliable verification was unavailable.
    data class WrittenButUnverified(
        override val requestedWrite: SettingWrite,
        val genericOverrideApplied: Boolean = false
    ) : RefreshRateApplyResult

    // The written value did not match the value returned during verification.
    data class VerificationMismatch(
        override val requestedWrite: SettingWrite,
        val readBackValue: Int?,
        val genericOverrideApplied: Boolean = false
    ) : RefreshRateApplyResult

    // The platform rejected the requested settings write.
    data class WriteRejected(
        override val requestedWrite: SettingWrite
    ) : RefreshRateApplyResult

    // An unexpected error occurred while applying the requested value.
    data class Failure(
        override val requestedWrite: SettingWrite?,
        val throwable: Throwable
    ) : RefreshRateApplyResult

    // A requested custom rate was unavailable, but the existing vendor strategy was restored.
    data class CustomFallbackApplied(
        override val requestedWrite: SettingWrite?,
        val requestedRefreshRateHz: Int
    ) : RefreshRateApplyResult

    // No write was required for the requested state.
    data object NoOperation : RefreshRateApplyResult {
        override val requestedWrite: SettingWrite? = null
    }
}

// Indicates whether the result can be treated as a successful engine operation.
val RefreshRateApplyResult.isOperationalSuccess: Boolean
    get() = when (this) {
        is RefreshRateApplyResult.AppliedAndVerified,
        is RefreshRateApplyResult.WrittenButUnverified,
        is RefreshRateApplyResult.CustomFallbackApplied -> true

        else -> false
    }

// Defines the global operating modes supported by the refresh-rate engine.
enum class AdaptiveHzMode {
    OFF,
    ADAPTIVE,
    FORCE_MIN,
    FORCE_MAX
}

// Defines the refresh-rate behavior applied to an individual app profile.
enum class AppRefreshProfileMode {
    DEFAULT,
    SYSTEM_CONTROLLED,
    FORCE_MIN,
    FORCE_MAX
}
