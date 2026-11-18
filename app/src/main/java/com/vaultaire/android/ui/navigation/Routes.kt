package com.vaultaire.android.ui.navigation

/**
 * Canonical route table for Vaultaire.
 *
 * Mirrors `plan/design-system/flows/verify-flows.js`. Every route declared
 * here must resolve in [VaultaireNavHost]; the flow verifier treats this list
 * as the source of truth.
 */
object Routes {
    // Access & recovery
    const val Splash = "/"
    const val Welcome = "/welcome"
    const val OnboardingPattern = "/onboarding/pattern"
    const val OnboardingConfirm = "/onboarding/confirm"
    const val OnboardingPhrase = "/onboarding/phrase"
    const val OnboardingDone = "/onboarding/done"
    const val Lock = "/lock"
    const val RecoveryEnter = "/recovery/enter"
    const val RecoveryDisplay = "/recovery/display"
    const val RecoveryResetPattern = "/recovery/reset-pattern"

    // Restore
    const val RestoreProgress = "/restore/progress"

    // Vault core
    const val Vault = "/vault"
    const val VaultTimeline = "/vault/timeline"
    const val VaultCollectionPattern = "/vault/collection/{id}"
    const val VaultSearch = "/vault/search"
    const val VaultFilePattern = "/vault/file/{id}"
    const val VaultImport = "/vault/import"

    // Camera
    const val Camera = "/vault/camera"
    const val CameraReview = "/vault/camera/review"
    const val CameraReviewVideo = "/vault/camera/review/video"
    const val CameraPermission = "/vault/camera/permission"

    // Settings
    const val Settings = "/settings"
    const val SecuritySettings = "/settings/security"
    const val DuressSettings = "/settings/security/duress"
    const val DuressGuidance = "/settings/security/duress/guidance"
    const val HiddenVaults = "/settings/security/hidden-vaults"
    const val SecurityInfo = "/settings/security-info"
    const val Privacy = "/settings/privacy"
    const val About = "/settings/about"
    const val BackupSettings = "/settings/cloud/backup"
    const val CloudSync = "/settings/cloud/sync"
    const val SyncLog = "/settings/cloud/sync/log"
    const val Appearance = "/settings/appearance"
    const val AppearanceIcons = "/settings/appearance/icons"
    const val Pro = "/pro"

    // Sharing
    const val Shares = "/shares"
    const val ShareCreate = "/vault/share/create"
    const val SharePhrase = "/vault/share/phrase"
    const val SharedOpen = "/shared/open"
    const val SharedVaultPattern = "/shared/{id}"

    // Parameter route helpers
    fun collection(id: String): String = "/vault/collection/$id"
    fun file(id: String): String = "/vault/file/$id"
    fun sharedVault(id: String): String = "/shared/$id"

    // Navigation argument keys
    const val ArgId = "id"
}
