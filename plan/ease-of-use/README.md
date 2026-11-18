# Ease of Use Feature Planning

## Overview

This document defines the complete planning package for the Vaultaire Android app's Ease of Use feature. The goal is to implement a security-first, zero-configuration user experience that feels like unlocking a phone.

**Key Principle**: *Real security should feel effortless.* Security and usability multiply, not oppose each other.

---

## Website Claims Summary

From https://vaultaire.app/features/ease-of-use/:

### Core Claims
- **No accounts** - No server, no email, no personal information required
- **No setup** - No setup wizards, no configuration screens
- **No learning curve** - Three steps: draw pattern, add files, vault opens
- **AES-256-GCM encryption** - Industry-standard authenticated encryption
- **PBKDF2 key derivation** - 600,000 iterations for pattern-based authentication
- **Feels like unlocking your phone** - Sub-second pattern unlock, muscle memory

### Design Philosophy
- Most secure app is useless if nobody can use it
- Security that people actually use beats stronger security that people abandon
- The easiest thing to do should be the most secure thing
- Accessibility is not a bolt-on feature
- Defaults are the best choices - no need to configure

### Technical Details
- **Key Derivation**: PBKDF2WithHmacSHA512, 600,000 iterations, 256-bit output
- **Encryption**: AES/GCM/NoPadding, 128-bit auth tag, 12-byte IV
- **Recovery**: 12-word mnemonic seed phrase (300,000 iterations for derivation)
- **Pattern minimum**: 6 points on 5×5 grid
- **Pattern validation**: Minimum 6 points, interpolation for intermediate nodes

---

## Status Dashboard

| Priority | Component | Status | Owner |
|----------|-----------|--------|-------|
| 🔴 Critical | MainActivity design | Pending | - |
| 🔴 Critical | PatternLockScreen | Pending | - |
| 🔴 Critical | First launch flow | Pending | - |
| 🟡 High | VaultHomeScreen | Pending | - |
| 🟡 High | Navigation structure | Pending | - |
| 🟢 Medium | RecoveryPhraseScreen | Pending | - |
| 🟢 Medium | Error handling strategy | Pending | - |
| 🟢 Medium | Accessibility support | Pending | - |
| 🔵 Info | Theme & styling | Pending | - |

Legend: 🔴 Critical (must deliver), 🟡 High (core feature), 🟢 Medium (supporting), 🔵 Info (documentation)

---

## Quick Navigation

- [**`planning.md`**](planning.md) — Product Owner perspective: UX principles, user stories, onboarding flow
- [**`implementation.md`**](implementation.md) — Tech Lead perspective: architecture, code structure, technical specs
- [**`tests.md`**](tests.md) — Test specification: UI tests, performance tests, edge cases