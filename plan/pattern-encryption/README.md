# Pattern Encryption - Feature Planning Package

## Quick Navigation
- [Feature Overview](#feature-overview)
- [Website Claims Summary](#website-claims-summary)
- [Status Dashboard](#status-dashboard)
- [Planning Documents](#planning-documents)
  - [Product Owner Perspective](./planning.md)
  - [Technical Architecture](./implementation.md)
  - [Testing Strategy](./tests.md)

---

## Feature Overview

Pattern Encryption is a biometric-inspired key derivation mechanism that allows users to unlock their encrypted vault by drawing a gesture on a 5×5 grid of dots. This replaces traditional password/PIN entry with a more intuitive, muscle-memory-based authentication method.

### Core Security Model
```
Pattern → PBKDF2 → Vault Key → Decrypt Index & Unwrap Master Key → AES-256-GCM File Protection
```

**Layered Key Hierarchy:**
1. **Pattern** - User gesture on 5×5 grid
2. **Vault Derivation Key (VDK)** - 256-bit key from PBKDF2-HMAC-SHA512 (600,000 iterations)
3. **Master Vault Key (MVK)** - Random 256-bit key generated at vault creation
4. **Encrypted Files** - Protected with AES-256-GCM under MVK

---

## Website Claims Summary

| Claim | Technical Specification | URL |
|-------|------------------------|-----|
| 5×5 grid interaction | 25 positions for pattern drawing | Pattern Encryption |
| PBKDF2-HMAC-SHA512 | 600,000 iterations, 256-bit output | Pattern Encryption |
| Device-wide salt | Stored in iOS Keychain | Pattern Encryption |
| 256-bit vault key | AES key from KDF output | Pattern Encryption |
| Vault key unwraps master key | AES-256-GCM envelope encryption | Pattern Encryption |
| AES-256-GCM file protection | Authenticated encryption for all data | Security Features |
| No password field | Pattern-only entry | Pattern Encryption |
| Layered key hierarchy | VDK → MVK → Files | Security Features |

**Key Architectural Claims:**
- Pattern must unlock AES-GCM authenticated index
- No server-side key verification (zero-knowledge)
- Recovery via 12-word mnemonic phrase (different iteration count: 800,000)
- Smudge attack mitigation via 5×5 vs 3×3 grid

---

## Status Dashboard

### ✅ Already Implemented (Matches Website Claims)

| Component | Status | Confidence |
|-----------|--------|------------|
| 5×5 Grid serialization (`PatternPoint`) | ✅ Complete | High |
| Pattern normalization (`PatternUtils`) | ✅ Complete | High |
| PBKDF2 with 600,000 iterations | ✅ Complete | High |
| 256-bit key derivation | ✅ Complete | High |
| AES-256-GCM key wrapping | ✅ Complete | High |
| Random master key generation | ✅ Complete | High |
| Domain separation for KDF | ✅ Complete | High |
| Memory hygiene for char arrays | ✅ Complete | Medium |

### ⚠️ Partial Implementation (Needs Work)

| Component | Gap | Risk |
|-----------|-----|------|
| **Device salt storage** | Uses SharedPreferences instead of Android Keystore | **HIGH** |
| **Recovery key derivation** | Uses 300,000 iterations vs claimed 800,000 | **MEDIUM** |
| **Zero-knowledge backup encryption** | Not implemented for pattern-derived backup key | **HIGH** |
| **Memory cleanup** | Only clears char arrays, not byte arrays | **MEDIUM** |
| **Timing attack resistance** | Not verified | **MEDIUM** |
| **Pattern validation** | Minimum 6 points, but no direction change check | **LOW** |
| **Screenshot protection** | Not implemented (iOS-specific) | Low for Android |

### ❌ Missing Features (Not Yet Implemented)

| Component | Reason | Priority |
|-----------|--------|----------|
| Keystore-backed salt storage | Security gap | CRITICAL |
| Pattern-derived backup key | Feature gap | HIGH |
| Recovery envelope encryption | Feature gap | HIGH |
| Plausible deniability vault detection | UX feature | MEDIUM |
| Direction change requirement enforcement | Validation | LOW |
| Brute-force mitigation (lockout/timeout) | Security | MEDIUM |

---

## Planning Documents

- **`planning.md`** — Product Owner perspective: user stories, flows, acceptance criteria, UX considerations
- **`implementation.md`** — Technical architecture: classes, data structures, crypto specs, Android adaptations
- **`tests.md`** — Test strategy: unit tests, integration tests, security tests, mocks/recommendations

---

## Android-Specific Adaptations vs iOS

| Feature | iOS Implementation | Required Android Adaptation |
|---------|-------------------|----------------------------|
| Device salt | iOS Keychain with `WhenUnlockedThisDeviceOnly` | Android Keystore (AES keys) + EncryptedSharedPreferences |
| Screenshot shield | `UIView.windowLevel` + screen capture detection | FLAG_SECURE + `OnUserInteractionListener` |
| Secure key storage | Keycats | Android KeyStore (API 23+) for asymmetric keys |
| Memory zeroing | Built-in types clearing | Requires explicit `Arrays.fill()` for byte[] |

---

## Key Technical Findings

### Cryptography Alignment
The existing implementation correctly follows the documented architecture:
- PBKDF2-HMAC-SHA512 with 600,000 iterations ✓
- 256-bit keys for VDK, MVK, AES-GCM ✓
- Domain separation (`VAULTAIRE_PATTERN_V1`) ✓
- Proper memory cleanup for char arrays ✓

### Critical Security Gaps

1. **Device Salt Storage**: Implementation uses plain SharedPreferences with hex-encoded salt. iOS claims Keychain with device-bound storage. **Must migrate to Android Keystore**.

2. **Recovery Key Iterations**: Website claims 800,000 iterations for recovery. Implementation uses 300,000. **Alignment required**.

3. **Encrypted Recovery Database**: iOS stores pattern/recovery info in encrypted Keychain DB. Android has no equivalent. **New feature needed**.

4. **Nonce Discipline**: Chunking scheme is correct (unique IV per chunk via XOR with index), but header salt is unused. **Consider using salt in header**.

### Recommendations

1. Replace `DeviceSaltManager` to use Android Keystore or EncryptedSharedPreferences
2. Update `RecoveryPhraseEngine` to use 800,000 iterations
3. Create `RecoveryStorageEngine` for encrypted local storage
4. Add pattern validation for minimum direction changes
5. Implement timing-attack resistant comparisons
6. Add brute-force detection (failed attempt counter, progressive delays)