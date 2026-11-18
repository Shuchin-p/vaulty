# Vaultaire Android — UI Surface Inventory for New Design System

Audited sources: `plan/{ease-of-use, hidden-vaults, plausible-deniability, duress-mode, pattern-encryption, secret-phrase, secure-camera, secure-sharing, encrypted-cloud-backup, device-sync, app-icons, security-architecture}` (README + planning). `plan/ui-design` intentionally excluded (deprecated). Only behaviors stated in the docs are reflected; contradictions between docs are flagged, not resolved.

---

## 1. Feature Matrix

| Feature | Pro gate | Primary UI surfaces | Required states | Security / visual constraints |
|---|---|---|---|---|
| **Ease of Use** (zero-config core) | None | PatternLockScreen (first launch = creation), VaultHomeScreen, RecoveryPhraseScreen | Empty vault; cold→lock <800ms; unlock <500ms; wrong-pattern shake + "Try again"; 5 fails → 30s lockout; auto-lock on background/screen-off | No accounts, no setup wizard, no tutorials/tips/badges. Encryption as gravity (no lock/encrypt buttons). 56dp dot targets, 4.5:1 contrast, reduced-motion, TalkBack dot announcements. Settings only: backup toggle, analytics toggle, disguise (Pro) |
| **Hidden Vaults** | None | Pattern lock grid as sole entry point; Vault open view; empty-vault view; Security Info / Help (forensic transparency) | Unknown pattern → auto-create + empty vault (no "wrong" signal); no vault picker ever; generic app-bar title ("Vault") | No vault list/catalog in UI. Key-derived locator naming, random arena names, 64-slot padded pool + cover files. Examiner can still estimate vault count — must be stated in Help. 48dp targets, 3-fail cooldown, "Forgot pattern?" → recovery, never a vault list |
| **Plausible Deniability** | Core (Duress Mode is the Pro part) | Decoy vault (identical to primary); Setup → Vaults → "Create New Vault" (pattern draw + confirm ×2); decoy believability guidance (US-6) | Decoy opens seamlessly under coercion; no "Real"/"Decoy" label, metadata field, or UI indicator; identical feature set across vaults | No plaintext cross-vault catalog. Same storage format, same encryption, same UI for all vaults. 600k PBKDF2 per guess; honest forensic limits documented in Help |
| **Duress Mode** (`Entitlement.DURESS_MODE`) | **Pro** | PatternLockScreen (duress pattern verified here, before discovery); Settings → designate decoy vault; confirmation/wipe is invisible | Silent multi-pass wipe (DoD 5220.22-M) of non-decoy vault + `.dat` files; decoy opens; sync paused/isolated; no sound/notification/progress/confirmation; AttemptThrottler persists across restarts/config changes | Duress pattern must be visually indistinguishable from normal unlock (same grid, same feedback, same navigation). Constant-time SHA-256 pattern-hash check. Wipe artifacts acknowledged (flash residue). Recovery only via backup + phrase |
| **Pattern Encryption** (5×5) | None | PatternLockScreen (unlock + create + confirm); pattern strength guidance; error states | ≥6 dots + ≥1 direction change enforced; shake on invalid; red flash + heavy haptic on wrong; GCM-tag-fail → "Incorrect pattern", failed counter++; "Vault not found" → suggest recovery; lockout after threshold | FLAG_SECURE claimed for shoulder-surfing/screen-recording protection (stated iOS→Android adaptation; not yet implemented). Memory hygiene on lock. PBKDF2 600k, device-bound salt (KeyStore target). Error strings: "Pattern must connect at least 6 dots", "Pattern needs at least one turn" |
| **Secret Phrase** (recovery) | None | One-time Phrase Display (creation); Settings → Recovery Phrase (post-auth re-display); Recovery phrase entry (9 fields, auto-advance); new-pattern draw + new-phrase display after reset; cloud restore entry | Generated: exactly 9 words; custom: 6–24 + strength estimator; "I've saved it" enabled after 10s (creation); auto-lock 30s inactivity; invalid → "Phrase not recognized" (no timing leak); total-loss warning copy | **FLAG_SECURE** on phrase display (blocks screenshot/MediaProjection/remote view). Clipboard copies auto-clear after 60s. `importantForAutofill=no`, `textNoSuggestions`, phrase TextView hidden from accessibility. Monospace 24sp, 3 words/line, "write on paper, do not store digitally" |
| **Secure Camera** | None (in-vault) | Camera preview (Photo/Video modes, lens switch, tap-to-focus, flash/torch); Photo Review; Video Review; permission rationale; gallery import (P1/P2) | Review is **mandatory**: Retake (discard, nothing written) / Keep (encrypt + thumbnail → vault grid). Capturing spinner, recording timer, "Encrypting…" processing, audio-off badge, storage-full, camera-in-use errors | **FLAG_SECURE** on preview + review (no screenshot, blank recents thumbnail). Nothing hits MediaStore/gallery/Photos; all to `filesDir`. Zero unencrypted temp files; buffers wiped on cancel/background/crash; max sensor resolution; orientation preserved; captures land in the currently unlocked vault (decoy under duress) |
| **Secure Sharing** (`E2EE_SHARED_VAULTS`) | **Pro to create, Free to open** | Share Creation (Pro badge, phrase, policy controls); phrase handoff (Copy / Share-via / QR); "Open Shared Vault" phrase entry; Recipient shared-vault view; "My Shares" list; Revoke confirm; Upgrade-to-Pro dialog | Phrase states: unclaimed / already claimed / invalid; share states: active / expired / access-limit / revoked; owner offline → "Revoking…" pending; recipient next sync → "Access revoked by owner"; update publish + pull-to-refresh | One-time 12-word phrase, independent key hierarchy (250k PBKDF2, per-share salt). Policy bound as GCM AAD (tamper = decrypt failure). Read-only enforced (no write path). No-export policy: UI actions hidden + **FLAG_SECURE on recipient Activity**; screenshot attempt → vault locks. Shared vaults survive local duress purge |
| **Encrypted Cloud Backup** | None (Free + Pro, **on by default**) | Settings → Encrypted Cloud Backup toggle + last-backup status; backup status indicators (cloud states); Restore flow: "Restore from backup?" → phrase entry → "Restoring…" progress → "Vault restored!" | Toggle OFF deletes remote copy, local vault intact; 24h cadence, Wi-Fi only, resumable; restore gate on recovery phrase | Encryption **before** upload; separate backup key (pattern-derived, distinct context); fixed 10MB padded chunks + decoy records; Google Drive AppData only; zero-knowledge (no plaintext key/pattern/phrase leaves device); `allowBackup=false` |
| **Device Sync** (`CROSS_DEVICE_SYNC`) | **Pro** | Per-vault "Sync This Vault" settings; sync status row ("This device + N other devices"); "Disconnect This Device"; activity log (optional, Pro) | Sync states: Synced / Syncing… / Offline-queued / Not syncing / Isolated (duress). First-sync: "Preparing first sync…". Errors: wrong Google account, backup disabled, outdated app, Drive quota, offline | Metadata inside encrypted vault (count only — no device names/IDs, nothing leaks before unlock). Per-vault opt-in, incremental chunks. Duress wipe is local-only — never propagates; isolated device never auto-rejoins. No conflict-resolution UI (automatic add-wins merge) |
| **App Icons** (`CUSTOM_APP_ICONS`) | **Pro** (Free = preview only) | Settings → App Icon selection/preview grid (8 icons); confirmation dialog; launcher-refresh feedback | Free: tap → "App Icons is a Vaultaire Pro feature" dialog, no change; Pro: switch + persistence + post-update reconciliation; launcher refresh ~2–5s + toast | Discreet utility icons (Calculator, Printer, Units, Device Info, Level, Sound Meter, Network) + default. `PackageManager.setComponentEnabledSetting`; re-applied in `Application.onCreate()` after Play update. Alias label decision open (utility name vs "Vaultaire") |
| **Security Architecture** (cross-cutting) | Mixed | Security Info / Help (forensic transparency + what-is-logged); Settings → Privacy (analytics/crash opt-out); root/debugger/tamper warning surfaces | FLAG_SECURE scope; tamper detection; certificate pinning; audit logging; analytics opt-out | FLAG_SECURE as house standard (currently unimplemented). KeyStore/EncryptedSharedPreferences for salt + persisted secrets; StrongBox target for MVK wrap; memory zeroing on lock/crash; no plaintext vault names, file names, or labels at rest |

---

## 2. Screen Inventory (distinct screens implied by the docs)

| # | Screen | Suggested route | FLAG_SECURE | Pro-gated | Owning feature |
|---|---|---|---|---|---|
| 1 | Pattern Lock (unlock **and** first-run creation; single grid) | `lock` | Yes (doc-claimed) | No | pattern-encryption / ease-of-use / hidden-vaults |
| 2 | Lockout cooldown overlay (30s after failure threshold) | `lock` (state) | Inherits | No | pattern-encryption / hidden-vaults |
| 3 | Pattern confirm + strength guidance (create flow) | `lock/confirm` | Yes (doc-claimed) | No | pattern-encryption / plausible-deniability |
| 4 | Vault Home (grid/list, generic "Vault" title, FAB +, three-dot settings) | `vault` | Not specified | No | ease-of-use |
| 5 | Vault empty state ("No files in this vault", Add Files) | `vault` (state) | Not specified | No | hidden-vaults |
| 6 | Recovery phrase display — one-time at creation | `recovery/display` | **Yes** | No | secret-phrase / ease-of-use |
| 7 | Recovery phrase entry ("Forgot Pattern?" path, 9 fields) | `recovery/entry` | Not specified (no-autofill/no-suggestions) | No | secret-phrase |
| 8 | New pattern + new phrase after recovery reset | `recovery/reset-pattern` | **Yes** (phrase step) | No | secret-phrase |
| 9 | Welcome / Restore-from-backup entry (new device) | `welcome` | Not specified | No | encrypted-cloud-backup / secret-phrase |
| 10 | Restore progress ("Restoring vault…" → "Vault restored!") | `restore/progress` | Not specified | No | encrypted-cloud-backup |
| 11 | Settings root (backup toggle, analytics toggle, privacy, about) | `settings` | Not specified | No | ease-of-use |
| 12 | Settings → App Icon (preview/selection grid + confirm dialog) | `settings/app-icon` | Not specified | **Yes** (preview free) | app-icons |
| 13 | Settings → Duress: designate decoy vault | `settings/duress` | Not specified | **Yes** | duress-mode |
| 14 | ~~Settings → Vaults → "Create New Vault"~~ — **removed by decision 2**: no vault list, no create button. A new pattern is the only way to create a vault. | `lock` | — | — | plausible-deniability |
| 15 | Decoy believability guidance (how to make decoy convincing) | `settings/security/duress/guidance` | **Yes** | **Yes** (duress) | plausible-deniability |
| 16 | Security Info / Help (forensic transparency, what-is-logged) | `settings/security-info` | Not specified | No | hidden-vaults / security-architecture |
| 17 | Privacy / analytics + crash opt-out | `settings/privacy` | Not specified | No | security-architecture |
| 18 | Settings → Recovery Phrase (post-auth re-display) | `settings/recovery-phrase` | **Yes** | No | secret-phrase |
| 19 | Upgrade to Pro (paywall dialog/sheet) | `paywall` | Not specified | — (gate) | secure-sharing / device-sync / app-icons / duress-mode |
| 20 | Camera preview (Photo/Video, lens, focus, flash/torch, shutter) | `vault/camera` | **Yes** | No | secure-camera |
| 21 | Photo review (Keep / Retake) | `vault/camera/review` | **Yes** | No | secure-camera |
| 22 | Video review (playback, Keep / Retake, audio-off badge) | `vault/camera/review/video` | **Yes** | No | secure-camera |
| 23 | Camera permission rationale / permanently-denied settings link | `vault/camera/permission` | **Yes** | No | secure-camera |
| 24 | Gallery import into vault (+ delete-originals reminder) | system picker → `vault` | Not specified | No | secure-camera / ease-of-use |
| 25 | Share creation (policy: expiry, max opens, no-export) | `vault/share/create` | Not specified | **Yes** | secure-sharing |
| 26 | Sharing-phrase handoff (Copy / Share-via / QR, trusted-channel warning) | `vault/share/phrase` | Not specified | **Yes** | secure-sharing |
| 27 | Open Shared Vault (12-word entry, Paste / Scan QR) | `shared/open` | Not specified | No (free may open) | secure-sharing |
| 28 | Recipient shared-vault view (read-only badge, policy line, view-only actions) | `shared/{id}` | **Yes** (no-export policy) | No | secure-sharing |
| 29 | My Shares list (status, Revoke, Publish Update) | `shares` | Not specified | **Yes** | secure-sharing |
| 30 | Revoke confirmation + "Revoking…" pending state | `shares` (dialog) | Not specified | **Yes** | secure-sharing |
| 31 | Settings → Encrypted Cloud Backup (toggle, last backup, cadence) | `settings/backup` | Not specified | No | encrypted-cloud-backup |
| 32 | Backup status indicator (cloud states, on vault/settings rows) | `settings` / `vault` (component) | Not specified | No | encrypted-cloud-backup |
| 33 | Vault settings → Sync ("Sync This Vault", device count) | `vault/settings/sync` | Not specified | **Yes** | device-sync |
| 34 | "Disconnect This Device" (local-only, no revoke) | `vault/settings/sync` (dialog) | Not specified | **Yes** | device-sync |
| 35 | Sync activity log (optional, encrypted, local-only) | `vault/settings/sync/log` | Not specified | **Yes** | device-sync |

---

## 3. Non-Negotiable UX Invariants

1. **Duress pattern is indistinguishable from normal unlock.** Same 5×5 grid, same dot/line/animation/haptic language, same navigation timing. No extra affordance, badge, or delay when the duress pattern is drawn; the decoy opens exactly as a primary vault would. Trigger resolution happens before vault discovery, silently.
2. **A wrong/unknown pattern must reveal nothing.** Empty-vault state for an unmatched pattern must not signal "wrong" — it must read identically to a genuinely new/empty vault. No error copy, counter, cooldown, or recovery nudge may distinguish "this pattern opens another vault" from "this vault is empty". No plaintext or implied vault list exists anywhere in the UI.
3. **Decoy vault looks identical to a primary vault.** Same UI, same feature set, same encryption and storage format. No "Real"/"Decoy" label, metadata field, or behavioral tell at rest or on screen; one shared generic title ("Vault").
4. **No vault list, catalog, or picker on any launch path.** App opens to the pattern grid. No count of vaults, no switch-vault affordance; "Forgot pattern?" leads to recovery, never to a vault list.
5. **No plaintext vault names.** No vault label, name, or pattern↔vault mapping is stored or displayed; on-disk naming is key-derived/random. App bar shows a generic title.
6. **Recovery phrase is shown once, with screenshot prevention.** One-time full-screen display under FLAG_SECURE (blocks screenshots, screen recording, remote view). "I've saved it" gated by a 10s minimum delay. No re-display except post-authentication in settings; a reset generates a new phrase and invalidates the old one. RecoveryPhrase entry fields: no autofill, no suggestions.
7. **No clipboard persistence on secrets.** Phrase copy auto-clears after 60s with explicit user-visible feedback; phrase text hidden from accessibility services; the display auto-locks after 30s inactivity.
8. **Camera review is mandatory before encryption.** Every capture lands on an explicit Keep/Retake decision; nothing is encrypted or written to the vault before "Keep", and "Retake" leaves no file, no partial `.vlt`, and no unencrypted buffer. No capture may exist in MediaStore/gallery at any point.
9. **Duress is silent.** No confirmation dialog, warning sound, notification, or progress bar during or after wipe; the app must behave exactly as a normal unlock from the outside.
10. **Recovery truthfulness.** Every recovery entry point states that losing both pattern and phrase means unrecoverable data — no account reset, no backdoor.

---

## 4. Open Design Questions

1. **Welcome screen vs. direct-to-pattern.** `pattern-encryption/planning.md` Flow 1 routes a device with no configured vault to a "Welcome Screen"; `ease-of-use` and `hidden-vaults` mandate launch directly to the pattern grid with zero onboarding. Which wins?
2. **"Create New Vault" vs. no-create-button.** `plausible-deniability/planning.md` §4.1 specifies Settings → Vaults → "Create New Vault" (pattern draw + confirm ×2); `hidden-vaults` AC-3 and `ease-of-use` state there is never a "Create New" affordance (first pattern auto-creates). Does a settings creation path exist, and if so how does it avoid leaking vault existence?
3. **Wrong-pattern feedback.** `pattern-encryption` error matrix specifies "Incorrect pattern" + failed-counter increment on GCM tag mismatch; `hidden-vaults`/`plausible-deniability` require the silent empty state (Invariant 2). Reconcile: is there any user-visible failure feedback at all, and what does the AttemptThrottler display?
4. **Recovery-phrase spec conflict.** `ease-of-use` says 12-word BIP39 (300k iterations); `secret-phrase` says 9-word grammatical sentence, custom ≥6 words, 800k iterations, phrase-derived salt. Which drives the phrase UI (word count, fields, copy format)?
5. **Lockout thresholds differ.** `ease-of-use` AC-10 (5 failures → 30s lockout) vs `hidden-vaults` AC-6 (3 failures → 30s cooldown). Canonical throttling policy for the lock screen and overlay copy?
6. **FLAG_SECURE scope.** Docs claim it for pattern grid (shoulder surfing), phrase display, camera preview/review, and recipient shared vault; nothing specifies vault home, file viewer, settings, or share screens. Where does the design system set it as default-on vs. opt-in?
7. **App icon alias label.** `app-icons/planning.md` leaves "label under icon = utility name (e.g. Calculator)" vs. "Vaultaire" as an explicit open decision, directly affecting launcher-level deniability.
8. **Ownerless screens.** Decoy believability guidance (plausible-deniability US-6) and the analytics/crash opt-out UI (security-architecture claim 15) have user stories but no specified screen or route — confirm owners before the design system assigns components.

---

## 5. Aurora decisions on the open questions

These are design-system decisions, not silent product changes. Each is reversible at the noted seam.

| # | Question | Aurora decision | Reversal seam |
|---|---|---|---|
| 1 | Welcome vs. direct-to-pattern | **Direct to pattern.** `/welcome` exists only on a device with no local vault, and only as two honest choices: restore with phrase or start fresh. No carousel, no account. | Remove the `Restore` link in `EntryScreen` |
| 2 | "Create New Vault" | **No create button and no vault list.** A pattern that matches nothing resolves to a neutral empty vault. Additional vaults exist only as different patterns. Cost: a typo can open an empty vault; accepted for deniability. | `VaultResolutionPolicy.Strict` (not recommended) |
| 3 | Wrong-pattern feedback | **No wrong-pattern state on unlock.** Glow and resolve identically; rate-limit copy is neutral. Error styling only in pattern creation. See `screens.md §3.1`. | `PatternFeedback.LegacyError` |
| 4 | Recovery phrase spec | **UI supports 9, 12, and custom (≥ 6).** Canonical generated phrase is **9 words** for recovery; shares use **12**. Entry accepts all three. The crypto spec must pick one; the design does not force it. | `RecoveryPhraseSpec.wordCount` |
| 5 | Lockout threshold | **5 failures → 30s**, escalating (30s, 2m, 10m, 30m), persisted. Copy is neutral. Docs conflict 3 vs 5; 5 is the shipped default. | `ThrottlePolicy` |
| 6 | FLAG_SECURE scope | **Secure by default on lock, setup, phrase, camera, review, viewer, shared vault, duress confirm.** Home, timeline, settings and shares remain capturable (user can enable "Hide previews"). Broader than the docs, narrower than "everything". | `SecureSurfacePolicy` per destination |
| 7 | App icon alias label | **Alias label = utility name** (e.g. "Calculator"); the in-app UI never changes. Play Store policy review required before shipping. | manifest `activity-alias` label |
| 8 | Ownerless screens | Decoy guidance is a Pro content screen under Duress setup; privacy/analytics is a first-class Settings screen. Neither is reachable from home. | remove routes |

> The three pixel-diff guardians in `accessibility.md §10.3` are the acceptance tests for rows 2, 3, and duress indistinguishability.
