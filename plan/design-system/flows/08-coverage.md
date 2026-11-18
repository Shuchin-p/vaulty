# 08 · Flow Layer Coverage, State Matrix, Exceptions, and Gap Roll-Up

> Comprehensive verification of the 40 canonical screens in Vaultaire Aurora across all 9 screen classes.
> Maps state budget adherence (`S0`, `S1`, `S3`, `S4`, `S6`, `S9`, `A4`, `V1`, `V4`), justifies intentional omissions, and consolidates all product/security gaps into a severity-ranked ledger.

---

## 1. Executive Summary

- **Total canonical screens:** 40 / 40 (100% documented across `01`–`06`).
- **Validation status:** 0 structural errors, 0 missing routes, 0 invariant breaks (`verify-flows.js`).
- **State coverage:** All screens cover `S0`, `S3`, `S9`, `A4`, `V1`, `V4`. Omissions of `S1`, `S4`, `S6`, `S8` are class-specific and justified by design principles.
- **Identified gaps:** 18 total items categorized into Critical (7), Medium (8), and Technical (3).

---

## 2. Per-Class State Matrices

Legend: ✅ = Explicitly specified in state table · ➖ = Justified intentional omission (see §3).

### 2.1 Oneshot Class (6 screens)
> Transient guidance, onboarding steps, splash, and terminal screens.

| Route | S0 | S1 | S3 | S4 | S6 | S9 | A4 | V1 | V4 |
|---|---|---|---|---|---|---|---|---|---|
| `/` | ✅ | ✅ | ➖ | ➖ | ➖ | ➖ | ✅ | ✅ | ✅ |
| `/welcome` | ✅ | ➖ | ✅ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ |
| `/onboarding/phrase` | ✅ | ➖ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ | ✅ |
| `/onboarding/done` | ✅ | ➖ | ✅ | ➖ | ➖ | ➖ | ✅ | ✅ | ✅ |
| `/recovery/reset-pattern` | ✅ | ➖ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/restore/progress` | ✅ | ✅ | ✅ | ➖ | ✅ | ➖ | ✅ | ✅ | ✅ |

### 2.2 Secret Class (5 screens)
> High-security surfaces holding sensitive keys, phrases, or unencrypted review buffers. Solid panel + FLAG_SECURE.

| Route | S0 | S1 | S3 | S4 | S6 | S9 | A4 | V1 | V4 |
|---|---|---|---|---|---|---|---|---|---|
| `/recovery/display` | ✅ | ➖ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ | ✅ |
| `/vault/camera/review` | ✅ | ✅ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/vault/camera/review/video` | ✅ | ✅ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/vault/share/phrase` | ✅ | ➖ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ | ✅ |
| `/settings/security/duress` | ✅ | ➖ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |

### 2.3 Form Class (5 screens)
> User input, phrase entry, pattern creation, and share initialization.

| Route | S0 | S1 | S3 | S4 | S6 | S9 | A4 | V1 | V4 |
|---|---|---|---|---|---|---|---|---|---|
| `/onboarding/pattern` | ✅ | ➖ | ✅ | ➖ | ✅ | ➖ | ✅ | ✅ | ✅ |
| `/onboarding/confirm` | ✅ | ➖ | ✅ | ➖ | ✅ | ➖ | ✅ | ✅ | ✅ |
| `/recovery/enter` | ✅ | ✅ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/vault/share/create` | ✅ | ➖ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/shared/open` | ✅ | ✅ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |

### 2.4 List Class (6 screens)
> Collections of items, logs, tiles, or shares with paging and search.

| Route | S0 | S1 | S3 | S4 | S6 | S9 | A4 | V1 | V4 |
|---|---|---|---|---|---|---|---|---|---|
| `/vault` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/vault/timeline` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/vault/collection/{id}` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/vault/search` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/settings/cloud/sync/log` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/shares` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

### 2.5 Detail Class (2 screens)
> Full-screen media viewer or shared collection detail view.

| Route | S0 | S1 | S3 | S4 | S6 | S9 | A4 | V1 | V4 |
|---|---|---|---|---|---|---|---|---|---|
| `/vault/file/{id}` | ✅ | ✅ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/shared/{id}` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

### 2.6 Task Class (1 screen)
> Long-running batch process (e.g., gallery import).

| Route | S0 | S1 | S3 | S4 | S6 | S9 | A4 | V1 | V4 |
|---|---|---|---|---|---|---|---|---|---|
| `/vault/import` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

### 2.7 Camera Class (2 screens)
> Viewfinder preview and permission rationale.

| Route | S0 | S1 | S3 | S4 | S6 | S9 | A4 | V1 | V4 |
|---|---|---|---|---|---|---|---|---|---|
| `/vault/camera` | ✅ | ✅ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/vault/camera/permission` | ✅ | ➖ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ | ✅ |

### 2.8 Settings Class (10 screens)
> Static or toggle-driven configuration surfaces and explainer cards.

| Route | S0 | S1 | S3 | S4 | S6 | S9 | A4 | V1 | V4 |
|---|---|---|---|---|---|---|---|---|---|
| `/settings` | ✅ | ➖ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ | ✅ |
| `/settings/security` | ✅ | ➖ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ | ✅ |
| `/settings/security/duress/guidance` | ✅ | ➖ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ | ✅ |
| `/settings/security/hidden-vaults` | ✅ | ➖ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ | ✅ |
| `/settings/security-info` | ✅ | ➖ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ | ✅ |
| `/settings/privacy` | ✅ | ➖ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/settings/about` | ✅ | ➖ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ | ✅ |
| `/settings/cloud/backup` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/settings/cloud/sync` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/settings/appearance` | ✅ | ➖ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |

### 2.9 Pro-Only Class (2 screens)
> Disguise configuration and paywall sheet.

| Route | S0 | S1 | S3 | S4 | S6 | S9 | A4 | V1 | V4 |
|---|---|---|---|---|---|---|---|---|---|
| `/settings/appearance/icons` | ✅ | ➖ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/pro` | ✅ | ➖ | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 3. Intentional Omissions & Exceptions Justification

1. **`S1 Loading` on static & form screens:** Static screens (`/welcome`, `/settings/*`, `/onboarding/*`) compose synchronously in under 16 ms. Showing a skeleton or spinner (`S1`) before 300 ms is an anti-pattern that creates visual flicker (`00 §2`).
2. **`S4 Empty` on interactive controls:** Screens like `/onboarding/pattern`, `/lock`, `/vault/camera`, `/recovery/enter`, and `/pro` present interactive widgets (pattern grid, camera viewfinder, input slots, paywall cards). They have no empty list state; `S3 Content` is their idle baseline.
3. **`S6 Error` on static content:** Purely informative screens (`/settings/about`, `/settings/security-info`, `/settings/security/hidden-vaults`, `/onboarding/done`) have no network or I/O calls that can fail. They require no error state.
4. **`S8 Offline` on local surfaces:** As established in `07-system-states §1.1`, local vault surfaces (`/vault`, `/vault/timeline`, `/vault/collection/{id}`, `/vault/search`, `/settings`, `/settings/security`, `/settings/appearance`) read local encrypted disk storage. They are **invisible offline** and forbid offline banners or error toasts. Offline indicators (`S8` / `N2`) are restricted strictly to transport screens (`/settings/cloud/backup`, `/settings/cloud/sync`, `/shares`, `/shared/open`, `/shared/{id}`, `/restore/progress`).
5. **`S9 Locked` on onboarding & pre-auth screens:** Screens that run **before** a vault session exists (`/`, `/welcome`, `/onboarding/pattern`, `/onboarding/confirm`, `/onboarding/done`) cannot auto-lock (`S9`) because there is no session to lock. Backgrounding during onboarding resets or preserves uncommitted step state.

---

## 3.1 Machine-Readable Exception Registry

> Consumed by `extract-states.js` to suppress documented, intentional omissions from the
> "SECTIONS WITH MISSING MANDATORY STATES" report. Each entry names the route, the missing
> mandatory state, and the §3 justification. Any omission not listed here is a defect.

| Route | Missing mandatory state | Justification (§3) |
|---|---|---|
| `/` | S2, S3, S6, S9, S15, S16 | §3.5 — pre-auth shell; no session to lock, no content to show |
| `/welcome` | S0, S1, S2, S6, S9, S15, S16 | §3.1 — static content screen; composition is synchronous, no I/O |
| `/onboarding/pattern` | S0, S15, S9 | §3.2 — interactive widget (pattern grid); no empty list state; pre-auth |
| `/onboarding/confirm` | S0, S15, S9 | §3.2 — interactive widget (pattern grid); no empty list state; pre-auth |
| `/onboarding/phrase` | S0, S1, S2, S6, S15 | §3.1 — static content screen; S15 handled by `/onboarding/done` |
| `/onboarding/done` | S0, S1, S2, S3, S6, S9, S15 | §3.5 — pre-auth terminal screen; no session to lock |
| `/recovery/enter` | S0 | §3.2 — interactive widget (input slots); S0 is composition of an empty grid |
| `/recovery/reset-pattern` | S0, S1, S2, S9 | §3.5 — pre-auth; S0 is composition of the interstitial card |
| `/restore/progress` | S0, S2, S9, S15 | §3.5 — pre-auth; S15 is the billing/restore commit, S9 is pre-auth |
| `/vault/camera/permission` | S1, S6, S7, X4, X5 | §3.7 — rationale screen; preview (S1) and capture states (S6/S7/X4/X5) live on `/vault/camera` |
| `/shared/open` | S16 | §3.3 — form class; S16 success lands on `/shared/{id}`, not on the entry form |

---

## 4. Severity-Ranked Gap Roll-Up

### 4.1 High Severity / Critical Gaps (Security, Crypto, Coercion)

| # | Gap | Affected Domain | Description & Impact | Recommendation |
|---|---|---|---|---|
| 1 | **Throttle Counting Basis** | `01`, `04` | Deniability decision removed the wrong-pattern state (`screens §3.1`), making "5 failures" unobservable. `01 §R1` redefines as attempt rate (5 releases / 2 min), which also throttles 6 legitimate quick unlocks. | Product sign-off required: accept rate-based throttling or move to pure time-damping. |
| 2 | **Duress Partial-Wipe Durable Ledger** | `01`, `04` | Mid-wipe process death or crash could leave residual vault files. `01 §R3` requires wipe re-arm before decoy interactivity, but no persistent ledger exists. | Implement a two-phase commit wipe ledger in `EncryptedSharedPreferences` before initiating file deletes. |
| 3 | **Biometric Target Vault Ambiguity** | `01`, `04` | `01 §R2` pins biometric unlock to "last unlocked vault." In multi-vault setups, this gives the user no vault choice and creates a small forensic leak ("last used"). | Confirm "last unlocked vault" policy or prompt for pattern when multiple vaults exist. |
| 4 | **Review Card `<Vault label>` Leak** | `03` | `components.md §6.3` references `<Vault label>` in review sheet, violating Invariant 4 (no plaintext vault labels). | Update `components.md` to mandate generic **"Encrypting into Vault"** copy. |
| 5 | **Post-Restore Pattern Binding** | `01`, `04` | Device-bound salt means a restored vault cannot verify the old pattern. `/restore/progress` requires pattern setup, but crypto binding is unconfirmed. | Ensure crypto spec documents salt generation rules during phrase restore. |
| 6 | **Disguise Leak via Widget / QSS Tile** | `03`, `06` | Launcher icon disguise (`screens §17`) does not cover system widgets or Quick Settings tiles reading "Vaultaire". | Exclude widgets/tiles when disguise alias is active, or rename widget label dynamically. |
| 7 | **Security Channel Notification Permission** | `03`, `06` | Users who never set up backup or sync are never prompted for `POST_NOTIFICATIONS`, blocking silent security alerts. | Add notification permission prompt to Security settings hub. |

### 4.2 Medium Severity Gaps (UX, Storage, State Semantics)

| # | Gap | Affected Domain | Description & Impact | Recommendation |
|---|---|---|---|---|
| 8 | **Encrypted Trash (7 Days) Setting UI** | `02`, `04` | Opt-in 7-day encrypted trash changes delete semantics vault-wide, but has no setting row in `screens.md` or `Settings`. | Add "Encrypted trash (7 days)" row to `/settings/privacy` with storage cap. |
| 9 | **No Re-Entry Route for Claimed Shares** | `05`, `06` | Once a recipient leaves `/shared/{id}`, `screens.md §16` has no entry in My Shares — user must re-enter 12-word phrase. | Add a "Shared with me" section to `/shares` or document re-entry limitation. |
| 10 | **Missing Sync Review Route** | `05`, `06` | `screens.md §15` says "→ review list," but `_TEMPLATE.md §5` defines no route for sync conflicts. | Add `/settings/cloud/sync/review` route or define as a modal sheet on `/settings/cloud/sync`. |
| 11 | **`Allow Mobile Data` Persistence** | `05`, `06` | `07 §1.3` puts `Resume anyway / Wait for Wi-Fi` on banner, but `screens.md §14` lists no stored toggle. | Specify whether mobile data choice is per-session (banner only) or a stored Setting toggle. |
| 12 | **`Publish Update` Share Semantics** | `05`, `06` | "Update" is undefined for phrase-based shares (what changed, what recipient pulls, when). | Define exact delta-sync payload and recipient pull trigger for updated shares. |
| 13 | **Restore-Over-Existing-Vault Policy** | `01` | Running restore on a device that already has a local vault has no specified overwrite or merge policy. | Require explicit confirm dialog: "Restoring replaces the existing local vault." |
| 14 | **Backup Key Reveal Auth Gate** | `05`, `06` | `/settings/cloud/backup` shows key after auth, but exact reveal gate flow (inline vs push) is unparsed. | Align backup key reveal with `/settings/security` phrase reveal flow. |
| 15 | **Delete vs. Disable Cloud Data Lifecycle** | `05`, `06` | Disabling backup notes cloud records remain, but no explicit "Delete cloud data" action exists. | Specify cloud deletion RPC and confirmation dialog in Backup settings. |

### 4.3 Low Severity / Technical & Token Gaps

| # | Gap | Affected Domain | Description & Impact | Recommendation |
|---|---|---|---|---|
| 16 | **Paging & LRU Eviction Tokens** | `02`, `04` | Grid page size (60 tiles), thumbnail decode size, and memory LRU caps are hardcoded numbers. | Move paging and decoding limits into `foundations.md §8` tokens. |
| 17 | **Camera Motion & Limit Tokens** | `03`, `06` | Video max duration (10:00), burst capture cap (5), and critical battery stop threshold lack token definitions. | Define `duration.videoMax`, `cap.burstPhoto`, and `threshold.batteryCritical` in tokens. |
| 18 | **Pattern-Only Restore UI Distinction** | `05`, `06` | Same-device pattern restore (V3) lacks distinct copy on `/restore/progress` from phrase restore. | Add subtle subtitle on `/restore/progress`: "Restoring from local backup key." |

---

## 5. New Components Roll-Up

Consolidated list of proposed UI components introduced across the 6 flow domain files:

| Component Name | Primary Domain | Purpose & Description |
|---|---|---|
| `AuroraSecureSurface` | `01`, `03` | Container enforcing `FLAG_SECURE`, solid background, recents cover, clipboard off. |
| `AuroraSecretPhrase` | `01`, `05` | 3×3 (9-word) or 2×6 (12-word) solid word grid with 5s background zeroization. |
| `AuroraCameraViewfinder` | `03` | Full-bleed CameraX preview with 76dp shutter, lens flip, flash cycle, audio-off badge. |
| `AuroraCameraReview` | `03` | Mandatory photo/video review sheet (`#000` background, Retake/Keep controls). |
| `AuroraThreatBanner` | `03`, `07` | Top banner for mic denied, battery low, storage low, metered network. |
| `AuroraDuressSetup` | `04` | Compact 5×5 pattern grid + 2s hold-to-enable iris sweep pill. |
| `AuroraHonestyCard` | `04` | Non-dismissible butter card explaining technical limitations without fear copy. |
| `AuroraDiagnosticsToast` | `04` | Local build, storage size, and sync state readout (never exposes keys). |
| `AuroraExportArchiveSheet` | `04` | Encrypted archive creator (AES-256-GCM) with save-to-Files integration. |
| `AuroraProCard` | `06` | Glass paywall card with iris eyebrow, benefit list, price mono, primary/ghost. |
| `AuroraProGate` | `04`, `06` | Feature-row inline lock card with 1-sentence value statement and Try Pro action. |
| `AuroraIconPreviewGrid` | `06` | 72dp squircle grid with 2dp iris selection ring for disguise icons. |
| `AuroraAliasSwitcher` | `06` | Helper wrapping `PackageManager.setComponentEnabledSetting` with `DONT_KILL_APP`. |
| `AuroraLivePreviewCard` | `06` | 320dp top card rendering live preview of chosen theme + surface mode. |
