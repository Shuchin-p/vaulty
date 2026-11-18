# Secret Phrase Feature — Planning Package

## Feature Overview

The **Secret Phrase** is Vaultaire's recovery lifeline: a human-readable credential that allows users to recover their vault when they forget their pattern. Unlike traditional BIP-39 mnemonics, Vaultaire uses a **custom nine-word grammatical sentence** derived via **PBKDF2-HMAC-SHA512 at 800,000 iterations** to unlock an **AES-256-GCM encrypted recovery envelope** containing the vault key.

> **Source**: [vaultaire.app/features/secret-phrase/](https://vaultaire.app/features/secret-phrase/) and [vaultaire.app/features/security-features/](https://vaultaire.app/features/security-features/)

---

## Website Claims Summary

| Aspect | Website Claim |
|--------|---------------|
| **Word Count** | 9 words (custom grammatical sentence) — NOT 12-word BIP-39 |
| **Custom Phrases** | User can supply own phrase (≥ 6 words, passes strength check) |
| **KDF Iterations** | 800,000 PBKDF2-HMAC-SHA512 iterations |
| **Salt** | Deterministic phrase-derived salt (not device salt) |
| **Recovery Key** | Separate 256-bit key derived from phrase (does NOT encode vault key) |
| **Envelope** | AES-256-GCM encrypted envelope containing vault key (local) or vault key + backup key (iCloud restore) |
| **Dual Access** | Pattern envelope + Recovery envelope both wrap the same Master Vault Key (MVK) |
| **Phrase Storage** | Encrypted in iOS Keychain (`WhenUnlockedThisDeviceOnly`) — Android: Keystore/EncryptedSharedPreferences |
| **Display** | Shown at creation; available later in vault settings |
| **Screenshot Prevention** | Implied (sensitive credential) |
| **Pattern Change** | Generates NEW recovery phrase & envelope; file master key unchanged |
| **Backup/Restore** | CloudKit recovery manifest located by deterministic phrase locator |

---

## Status Dashboard

| Component | Status | Notes |
|-----------|--------|-------|
| **Phrase Generation (9-word grammatical)** | ❌ Missing | Current: 12-word BIP-39 style list |
| **Custom Phrase Support (≥6 words)** | ❌ Missing | Current: requires exactly 12 words |
| **PBKDF2 800k Iterations** | ❌ Wrong | Current: 300,000 iterations |
| **Deterministic Phrase-Derived Salt** | ❌ Missing | Current: uses device salt |
| **Phrase Strength Estimator** | ❌ Missing | Not implemented |
| **Recovery Envelope (dual-access)** | ⚠️ Partial | `EnvelopeEncryptionEngine` exists but not dual-wrapped |
| **Phrase Storage (encrypted, retrievable)** | ❌ Missing | No Android Keystore integration |
| **Screenshot Prevention (FLAG_SECURE)** | ❌ Missing | Not implemented |
| **Clipboard Handling** | ❌ Missing | Not implemented |
| **Pattern Reset via Phrase Flow** | ❌ Missing | No integration with vault creation/reset |
| **Vault Restore via Phrase (Cloud)** | ❌ Missing | No cloud backup integration yet |
| **Phrase Display in Settings** | ❌ Missing | No UI for viewing saved phrase |
| **Memory Hygiene for Phrase** | ⚠️ Partial | `MemoryHygiene` exists but not applied to phrase |
| **Tests** | ❌ Missing | No test coverage |

---

## ⚠️ CRITICAL CONTRADICTIONS (Website vs Current Code)

| Parameter | Website Claim | Current Android Code | Severity |
|-----------|---------------|---------------------|----------|
| **Word Count** | **9 words** (grammatical sentence) | **12 words** (BIP-39 style) | 🔴 **BLOCKER** |
| **PBKDF2 Iterations** | **800,000** | **300,000** | 🔴 **BLOCKER** |
| **Salt Source** | Deterministic phrase-derived | Device-wide random salt | 🔴 **BLOCKER** |
| **Phrase Format** | Grammatical sentence template | Space-separated word list | 🟡 High |
| **Custom Phrases** | Supported (≥6 words, validated) | Not supported | 🟡 High |

> **Action Required**: These are not optional improvements — they are fundamental architecture mismatches. The Android implementation must be redesigned to match the iOS specification exactly for cross-platform compatibility.

---

## Quick Navigation

- [Product Planning (planning.md)](planning.md) — User stories, flows, acceptance criteria, UX
- [Technical Implementation (implementation.md)](implementation.md) — Architecture, algorithms, class interfaces, security
- [Test Specifications (tests.md)](tests.md) — Unit, integration, security, edge case tests