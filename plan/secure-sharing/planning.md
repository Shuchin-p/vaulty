# Secure Sharing — Product Planning Document

## 1. User Stories

### As a Vault Owner (Pro Subscriber)
| ID | Story | Priority |
|----|-------|----------|
| US-01 | I want to generate a one-time 12-word sharing phrase so I can securely share my vault with a trusted person. | P0 |
| US-02 | I want to set an expiration date on the share so access automatically stops after a deadline. | P0 |
| US-03 | I want to limit the number of times the recipient can open the shared vault. | P0 |
| US-04 | I want to disable export/screenshot for the recipient so files stay inside Vaultaire. | P0 |
| US-05 | I want to revoke access at any time so future opens are blocked immediately. | P0 |
| US-06 | I want to see a list of active shares with their status (active, expired, revoked, claimed). | P1 |
| US-07 | I want to publish encrypted updates to a shared vault so the recipient sees my latest files. | P1 |
| US-08 | I want the sharing phrase to be completely separate from my pattern and recovery phrase. | P0 |
| US-09 | I want to send the sharing phrase via any channel I trust (QR code, encrypted message, in person). | P0 |

### As a Recipient (Free or Pro)
| ID | Story | Priority |
|----|-------|----------|
| UR-01 | I want to enter a sharing phrase and open a shared vault without creating an account. | P0 |
| UR-02 | I want to view files in the shared vault with the same UI as my own vaults. | P0 |
| UR-03 | I want to be blocked from exporting, saving, or screenshotting when the owner disabled it. | P0 |
| UR-04 | I want to see a clear indicator that this is a read-only shared vault. | P0 |
| UR-05 | I want to receive updates automatically when the owner publishes changes. | P1 |
| UR-06 | I want to be notified if the share is revoked or expired. | P1 |
| UR-07 | I want the sharing phrase to be spent after I claim it so no one else can use it. | P0 |

---

## 2. User Flows

### Flow A: Owner Creates a Share
```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Owner opens Vault → taps "Share" (Pro-gated)                │
├─────────────────────────────────────────────────────────────────┤
│ 2. Share Creation Screen:                                      │
│    • Auto-generates 12-word phrase (regenerate button)         │
│    • Expiration date picker (optional)                         │
│    • Max opens input (optional, default: unlimited)            │
│    • Toggle: "Disable export & screenshots"                    │
│    • "Create Share" button                                     │
├─────────────────────────────────────────────────────────────────┤
│ 3. On Create:                                                  │
│    • Derive share key from phrase + random salt (PBKDF2 250k)  │
│    • Serialize vault payload (file index + encrypted content)  │
│    • Encrypt payload with AES-256-GCM, policy as AAD           │
│    • Upload EncryptedSharePackage to Google Drive App Folder   │
│    • Store share metadata locally (phrase hash, policy, driveID)│
├─────────────────────────────────────────────────────────────────┤
│ 4. Display phrase + "Copy" / "Share via..." / "Show QR"        │
│    • Warning: "Send through a trusted channel"                 │
└─────────────────────────────────────────────────────────────────┘
```

### Flow B: Recipient Opens a Share
```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Recipient opens Vaultaire → "Open Shared Vault"             │
├─────────────────────────────────────────────────────────────────┤
│ 2. Enter 12-word phrase screen (with paste/QR scan)            │
├─────────────────────────────────────────────────────────────────┤
│ 3. On Submit:                                                  │
│    • Derive share key from phrase + salt from package          │
│    • Download EncryptedSharePackage from Drive                 │
│    • Decrypt with AES-256-GCM, verify policy AAD               │
│    • Check expiration & open count (enforce locally)           │
│    • Mark phrase as spent (locally + cloud flag)               │
│    • Create local read-only vault instance                     │
├─────────────────────────────────────────────────────────────────┤
│ 4. Open shared vault (read-only UI):                           │
│    • Header: "Shared Vault • [Owner's vault name]"             │
│    • Badge: "Read-only" + policy indicators                    │
│    • Files viewable; export/share/screenshot blocked per policy│
│    • Pull-to-refresh checks for updates/revocation             │
└─────────────────────────────────────────────────────────────────┘
```

### Flow C: Owner Revokes Access
```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Owner opens "My Shares" list → taps share → "Revoke"        │
├─────────────────────────────────────────────────────────────────┤
│ 2. Confirm dialog: "Recipient will lose access on next sync"   │
├─────────────────────────────────────────────────────────────────┤
│ 3. Write revocation flag to Drive (share metadata file)        │
│    • Retry with exponential backoff if offline                 │
│    • Local state updated immediately                           │
├─────────────────────────────────────────────────────────────────┤
│ 4. Recipient's next online check:                              │
│    • Detects revocation flag                                   │
│    • Locks shared vault, shows "Access revoked by owner"       │
│    • Local cached files remain but cannot be opened            │
└─────────────────────────────────────────────────────────────────┘
```

### Flow D: Owner Publishes Updates
```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Owner modifies vault (add/remove/edit files)                │
├─────────────────────────────────────────────────────────────────┤
│ 2. "Publish to Shares" button (or auto on unlock)              │
├─────────────────────────────────────────────────────────────────┤
│ 3. For each active share:                                      │
│    • Re-encrypt updated payload with same share key            │
│    • Upload new EncryptedSharePackage (versioned)              │
│    • Update share metadata timestamp                           │
├─────────────────────────────────────────────────────────────────┤
│ 4. Recipient's next sync:                                      │
│    • Downloads new package                                     │
│    • Decrypts, replaces local read-only vault                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Acceptance Criteria (Given/When/Then)

### AC-01: Share Creation
| Scenario | Given | When | Then |
|----------|-------|------|------|
| AC-01.1 | Owner has Pro entitlement | Taps "Share" on a vault | Share creation screen opens |
| AC-01.2 | Owner on share screen | Does not modify defaults | 12-word phrase generated; no expiration; unlimited opens; export enabled |
| AC-01.3 | Owner sets expiration date | Creates share | Share expires after date; recipient cannot open after |
| AC-01.4 | Owner sets max opens = 3 | Creates share | Recipient can open exactly 3 times; 4th open fails |
| AC-01.5 | Owner toggles "Disable export" | Creates share | Recipient cannot export/screenshot; UI actions hidden |
| AC-01.6 | Owner has Free tier | Taps "Share" | Blocked with "Upgrade to Pro" dialog |

### AC-02: Recipient Opens Share
| Scenario | Given | When | Then |
|----------|-------|------|------|
| AC-02.1 | Recipient has Vaultaire + Google sign-in | Enters valid unused phrase | Shared vault opens read-only |
| AC-02.2 | Recipient enters phrase | Phrase already claimed | Error: "This share has already been claimed" |
| AC-02.3 | Recipient enters phrase | Phrase tampered/wrong | Error: "Invalid sharing phrase" (GCM auth fails) |
| AC-02.4 | Share expired | Recipient opens | Error: "This shared vault has expired" |
| AC-02.5 | Max opens exceeded | Recipient opens | Error: "Access limit exceeded" |
| AC-02.4 | Export disabled | Recipient tries export | Action hidden; toast "Export disabled by owner" |

### AC-03: Revocation
| Scenario | Given | When | Then |
|----------|-------|------|------|
| AC-03.1 | Owner revokes share | Recipient online & syncs | Vault locks immediately; "Access revoked" shown |
| AC-03.2 | Owner revokes share | Recipient offline | On next online sync, vault locks |
| AC-03.3 | Owner revokes share | Owner offline | Queued; sent when online; local UI shows "Revoking..." |

### AC-04: Updates & Sync
| Scenario | Given | When | Then |
|----------|-------|------|------|
| AC-04.1 | Owner publishes update | Recipient syncs | Recipient sees new files; old files removed |
| AC-04.2 | Owner adds file to vault | Publishes to shares | Recipient sees file on next sync |
| AC-04.3 | Owner deletes file | Publishes to shares | Recipient no longer sees file on next sync |

### AC-05: Security & Trust Model
| Scenario | Given | When | Then |
|----------|-------|------|------|
| AC-05.1 | Attacker intercepts phrase | Tries to claim from another device/account | Fails — phrase spent on first claim |
| AC-05.2 | Attacker modifies policy in transit | Recipient opens | GCM auth fails; decryption throws exception |
| AC-05.3 | Recipient tries to write to shared vault | Any write attempt | Blocked; read-only enforcement |
| AC-05.4 | Owner changes pattern | Existing shares | Unaffected — separate key hierarchy |

---

## 4. UX Design

### 4.1 Share Creation Screen
```
┌────────────────────────────────────────────┐
│ ←  Share Vault              [Pro Badge]    │
├────────────────────────────────────────────┤
│                                            │
│  Your Sharing Phrase                       │
│  ────────────────────                      │
│  abandon ability able about above absent   │
│  absorb abstract absurd abuse access       │
│  accident account accuse achieve           │
│  [Regenerate]          [Copy]              │
│                                            │
│  ────────────────────────────────────────  │
│                                            │
│  Access Controls                           │
│  ┌ Expiration Date          [Never ▼]     │
│  ┌ Max Opens                [Unlimited ▼] │
│  ┌ ☑ Disable Export & Screenshots         │
│                                            │
│  ────────────────────────────────────────  │
│                                            │
│  [Create Share]  (Primary, Pro color)      │
│                                            │
│  ⚠ Send the phrase through a trusted      │
│     channel (in person, phone, E2E msg).  │
│                                            │
└────────────────────────────────────────────┘
```

### 4.2 Recipient: Open Shared Vault Screen
```
┌────────────────────────────────────────────┐
│ ←  Open Shared Vault                       │
├────────────────────────────────────────────┤
│                                            │
│  Enter the 12-word sharing phrase          │
│  you received from the vault owner.        │
│                                            │
│  ┌──────────────────────────────────────┐  │
│  │ abandon ability able about above ... │  │
│  └──────────────────────────────────────┘  │
│         [Paste]        [Scan QR]           │
│                                            │
│  [Open Vault]  (Primary, disabled until    │
│                  12 valid words entered)   │
│                                            │
└────────────────────────────────────────────┘
```

### 4.3 Shared Vault View (Recipient)
```
┌────────────────────────────────────────────┐
│ ←  Shared Vault • Personal Docs      🔒    │
│     Read-only • Expires in 7 days • 3/5    │
├────────────────────────────────────────────┤
│                                            │
│  📁 Tax Returns          3 files  12 MB   │
│  📁 Medical Records      1 file   4 MB    │
│  📄 passport-scan.pdf    2.1 MB           │
│                                            │
│  [Pull to refresh for updates]            │
│                                            │
└────────────────────────────────────────────┘
Long-press file → "View" only (no Export/Share)
Screenshot attempt → Vault locks + "Screenshots disabled"
```

### 4.4 Owner: My Shares List
```
┌────────────────────────────────────────────┐
│ ←  My Shares                               │
├────────────────────────────────────────────┤
│                                            │
│  ┌ Personal Docs → john@...          ●●●  │
│  │  Active • Expires 2026-11-23 • 2/5    │
│  │  [Revoke]  [Publish Update]            │
│  ├──────────────────────────────────────┤  │
│  │ Work Project → sarah@...           ●●● │
│  │  Revoked • 2026-11-10                 │
│  │  [View Details]                        │
│  └──────────────────────────────────────┘  │
│                                            │
│  [+ New Share]                             │
│                                            │
└────────────────────────────────────────────┘
```

---

## 5. Trust Model

| Principle | Enforcement |
|-----------|-------------|
| **Owner is sole writer** | Share payload encrypted with owner-derived key; recipient gets read-only decrypted copy; no write path exists |
| **Phrase is one-time** | Cloud marks phrase "spent" on claim; subsequent claims rejected |
| **Phrase independent of credentials** | Separate PBKDF2 derivation (250k iterations, unique salt); no key overlap with pattern (600k) or recovery (300k/800k) |
| **Policy tamper-evident** | Policy serialized as JSON → bound as AAD in AES-GCM; any modification breaks auth tag |
| **Revocation is eventual** | Cloud flag checked on recipient sync; cannot erase already-viewed data |
| **No export = app-enforced** | UI actions hidden; `FLAG_SECURE` on Activity; cannot prevent external camera |
| **Pro creates, Free opens** | `E2EE_SHARED_VAULTS` entitlement gated on share creation only |
| **Duress mode: shared vaults survive** | Shared vaults stored in cloud; local purge only affects owner's local indices |

---

## 6. Edge Cases & Error Handling

| Case | Handling |
|------|----------|
| Phrase entered with extra spaces/caps | Normalize: `trim().lowercase()` before derivation |
| Recipient uninstalls/reinstalls | Phrase spent locally; cannot re-claim — owner must create new share |
| Owner deletes local vault but share exists | Share metadata persists in cloud; owner can still revoke/publish |
| Drive quota exceeded | Graceful error; retry with backoff; show "Sync failed" |
| Network offline during create/revoke | Queue operation; retry on connectivity; show pending state |
| Recipient on different Android version | Minimum API 24 (Android 7.0); crypto APIs available |
| Large vault (>100MB) | Chunked upload; streaming encryption (ref `ZeroKnowledgeBackupEngine`) |
| Concurrent share creation | Each gets unique salt/IV; independent packages |

---

## 7. Dependencies & Prerequisites

| Dependency | Purpose | Status |
|------------|---------|--------|
| Google Play Services Auth | Google Sign-In for Drive | ✅ Available |
| Google Drive API (v3) | Transport for share packages | ❌ Not integrated |
| Google Drive Changes API | Poll for updates/revocation | ❌ Not integrated |
| `E2EE_SHARED_VAULTS` entitlement | Pro gating | ✅ Defined |
| BiometricPrompt / Keystore | Protect local share metadata | ⚠️ Partially used |
| WorkManager | Background sync/retry | ❌ Not used for sharing |

---

*Document Version: 1.0*  
*Author: Product/Architecture Subagent*  
*Date: 2026-11-16*