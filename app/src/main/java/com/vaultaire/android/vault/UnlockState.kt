package com.vaultaire.android.vault

/**
 * Represents the unlock state of a vault.
 */
sealed class UnlockState {
    /** Vault is locked and requires authentication */
    object Locked : UnlockState()
    
    /** Vault is being unlocked (crypto operation in progress) */
    object Unlocking : UnlockState()
    
    /** Vault is successfully unlocked */
    data class Unlocked(val vault: OpenVault) : UnlockState()
    
    /** Unlock failed due to wrong credentials */
    data class Failed(val error: String) : UnlockState()
    
    /** Vault is in duress mode (silent destruction) */
    object Duress : UnlockState()
    
    /** Check if state represents an active vault */
    val isActive: Boolean
        get() = this is Unlocked && !vault.isDuressDecoy
}

/**
 * State machine for vault unlock operations.
 */
class UnlockStateMachine {
    var state: UnlockState = UnlockState.Locked
        private set
    
    fun transitionToLocking() {
        state = UnlockState.Locked
    }
    
    fun transitionToUnlocking() {
        state = UnlockState.Unlocking
    }
    
    fun transitionToUnlocked(vault: OpenVault) {
        state = UnlockState.Unlocked(vault)
    }
    
    fun transitionToFailed(error: String) {
        state = UnlockState.Failed(error)
    }
    
    fun triggerDuress() {
        state = UnlockState.Duress
    }
}
