package com.vaultaire.android.data.pricing.model

/**
 * Product entitlements for Vaultaire Pro subscription gating.
 * 
 * Pro users can CREATE shares; Free users can OPEN shares.
 * This matches the website claim: "Pro to create, Free to open"
 */
enum class Entitlement {
    /**
     * Allows creating end-to-end encrypted shared vaults.
     * Pro subscription required.
     */
    E2EE_SHARED_VAULTS,

    /**
     * Allows encrypted cloud backup to Google Drive.
     * Pro subscription required.
     */
    ENCRYPTED_CLOUD_BACKUP,

    /**
     * Allows device-to-device sync.
     * Pro subscription required.
     */
    DEVICE_SYNC,

    /**
     * Allows hidden vaults (plausible deniability).
     * Pro subscription required.
     */
    HIDDEN_VAULTS,

    /**
     * Allows custom app icons.
     * Pro subscription required.
     */
    CUSTOM_APP_ICONS,

    /**
     * Allows duress mode (plausible deniability wipe).
     * Pro subscription required.
     */
    DURESS_MODE,
}
