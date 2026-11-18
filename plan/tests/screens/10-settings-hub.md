# Settings Hub — Test Cases
## Scope — screens.md §10 "Settings Hub" + flows/04-security-settings.md §4.1 (Settings Hub), §4.2 (Security Settings), §4.7 (Privacy), §4.8 (About) + flows/06-appearance-and-monetization.md §1.1 (Appearance), §1.2 (App Icons), §1.3 (Paywall) + components.md §3.5 (AuroraListRow), §3.6 (AuroraSectionHeader), §3.7 (AuroraBadge), §8.2 (AuroraProGate), §7.1 (AuroraSnackbar) + security-architecture/planning.md (no account/email, zero-knowledge, disguise invariant) + duress-mode/planning.md (duress AC-02, AC-03)
## Preconditions
- Resolved session (G2): vault unlocked, pattern verified, session alive
- Pro entitlement active (E2): Duress, Device sync, App icon change, Sharing create available
- Pro entitlement free (E1): Pro rows show `[PRO]` badge, chevron intact, tap opens AuroraProGate sheet
- Pro entitlement expired (E3): Pro rows show `[PRO ended]` badge, tap opens gate with lapsed copy
- Pro entitlement pending (E4): Pro rows show `Finishing purchase…` inline, `Check again` after 60 s
- Encrypted backup: enabled (On) or disabled (Off); Google Drive connected or not
- Device sync: active (3 devices synced) or inactive (gate shows Pro)
- Auto-lock setting: one of immediately / 30s / 1m / 5m (default 30s)
- Version long-press: 3× tap within 1 s triggers AuroraDiagnosticsToast
- Accessibility: TalkBack on, display size 200% (A4), RTL locale, reduced motion (A5)
- Form factors: phone portrait (V1 < 600dp), tablet (V4 ≥ 600dp two-pane), foldable (V3), desktop (V5), multi-window (V6)

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T10-01 | Settings Hub loads with vault size and Pro status | functional | P0 | 1. Unlock vault. 2. Open Settings via nav drawer. | S0 → S3 (< 300 ms); profile card shows `Vault` + `Pro · 2.1 GB` (E2) or `Free · 2.1 GB` (E1); groups: Security, Cloud, Appearance, About/Privacy/Licenses |
| T10-02 | Tap Security / Cloud / Appearance / About rows navigate correctly | functional | P0 | 1. On Settings Hub. 2. Tap each group row. | Security → `/settings/security`; Cloud rows: Encrypted backup → `/settings/cloud/backup` (ring shows mint/butter/danger), Device sync (E2) → `/settings/cloud/sync` (`3 devices · Synced 2m ago`), Device sync (E1/E3) → AuroraProGate: "Device sync keeps your vault in sync across devices" / "Your Pro access has ended…"; Appearance → `/settings/appearance`; Privacy → `/settings/privacy` ("Vaultaire has no server…"); About → `/settings/about` (version, build, licenses, links); back returns to Hub |
| T10-03 | Tap Pro row opens paywall sheet (E1/E3/E4) | functional | P0 | 1. Free/expired/pending tier. 2. On Settings Hub. 3. Tap `Pro` row. | Opens `/pro` as sheet (half-height phone, centered tablet); hero + two plan cards; `Continue`, `Restore`, legal text; E3 shows lapsed copy; E4 shows `Finishing purchase…` / `Check again` |
| T10-04 | Back gesture from Settings Hub returns to Vault | functional | P0 | 1. On Settings Hub. 2. Back gesture / predictive back. | Returns to `/vault`; session preserved |
| T10-05 | Profile card shows aggregated vault size only; never vault count/label/ID | functional | P0 | 1. Multiple vaults exist. 2. Open Settings Hub. | Profile shows `Vault · 1.5 GB` (aggregate); **never** vault count, vault label, or vault ID |
| T10-06 | Pro row keeps chevron and opens value sheet (E1/E3) | functional | P0 | 1. Free tier. 2. On Settings Hub. 3. Tap `Pro` row. | Row has trailing chevron; opens `/pro` sheet; **never** a disabled/greyed row |
| T10-07 | Duress row (E2) navigates to Duress Setup; (E1) opens AuroraProGate | functional | P0 | 1. Pro active (E2). 2. On Security screen. 3. Tap `Duress mode` row. 4. Repeat for Free tier. | E2: pushes `/settings/security/duress`; rose-tint card: "If you're forced to unlock, this pattern opens one vault and erases the rest." — E1: AuroraProGate inline: "Duress mode opens a decoy vault and erases others" + `Try Pro` / `Not now` |
| T10-08 | Hidden vaults row opens explainer only; no list/count/manage | functional | P0 | 1. On Security screen. 2. Tap `Hidden vaults` row. | Pushes `/settings/security/hidden-vaults`; butter honesty card: "A forensic examiner with file access may still estimate how many vaults exist. Hidden vaults hide the catalog, not the storage." |
| T10-09 | Security info row opens threat model with honest language | functional | P0 | 1. On Security screen. 2. Tap `Security info` row. | Pushes `/settings/security-info`; threat model: what app protects (coercion, theft, cloud leakage), what it doesn't (forensic vault-count estimation, flash residue, offline pattern guessing, loss of pattern+phrase = permanent loss); no "military-grade" |
| T10-10 | Auto-lock & Clipboard pickers open value sheets with correct options | functional | P0 | 1. On Security screen. 2. Tap each row. | Auto-lock: `immediately` / `30s` / `1m` / `5m` (default 30s); Clipboard: `30s` / `60s` / `off`; save → S16 |
| T10-11 | Biometric unlock toggle (available) saves immediately; pattern always required | functional | P0 | 1. Biometric enrolled. 2. On Security screen. 3. Toggle `Biometric unlock`. | Toggles on/off; pattern always remains required; write → S16 |
| T10-12 | Attempt throttle & Screen capture rows read-only with neutral copy | functional | P0 | 1. On Security screen. 2. Observe both rows. | Throttle: tier/deadline neutrally; Screen capture: "Locked on for secure screens"; no interaction |
| T10-13 | Recovery phrase row requires auth then opens FLAG_SECURE phrase display | functional | P0 | 1. On Security screen. 2. Tap `Recovery phrase` row. 3. Auth via pattern/biometric. | Biometric/pattern re-verify → pushes `/recovery/display` (FLAG_SECURE); phrase never in Hub |
| T10-14 | Change pattern shows interstitial then pattern create with mode=change | functional | P0 | 1. On Security screen. 2. Tap `Change pattern` row. | Full-screen interstitial: "Changing your pattern creates a new recovery phrase. Your files stay encrypted." → Continue → `/onboarding/pattern` with `mode=change` |
| T10-15 | Privacy: Crash/Diagnostics off by default; log on; clear confirms | functional | P0 | 1. Fresh install. 2. Open Privacy. | Crash & Diagnostics off; Local activity log on; Clear → dialog "Clear local activity log? This can't be undone." |
| T10-16 | Privacy: Export my data creates encrypted archive (AES-256-GCM, user phrase) | functional | P0 | 1. On Privacy screen. 2. Tap `Export my data`. 3. Confirm. | S15: "Creating encrypted archive…" → S16: "Archive saved to Files"; AES-256-GCM with user phrase; never plaintext |
| T10-17 | Privacy: Hide previews in recents on by default; toggle → S16 | functional | P0 | 1. On Privacy screen. 2. Observe `Hide previews in recents` toggle. | On by default; toggle → S16; recents snapshot solid |
| T10-18 | About: Privacy policy & Security architecture links open local markdown (no network) | functional | P0 | 1. On About screen. 2. Tap links. | Pushes local markdown viewer; 44dp close; no network |
| T10-19 | Version long-press (3× within 1s) shows diagnostics toast; never keys/phrases | functional | P0 | 1. On Settings Hub. 2. Long-press version row 3× fast. | AuroraDiagnosticsToast: build, vault size, sync state; **never keys/phrases/file names** |
| T10-20 | Adaptive layouts: tablet two-pane (V4), phone single-column (V1), foldable tabletop (V3), RTL (A6) | adaptive | P0 | 1. Tablet ≥ 600dp: Open Hub. 2. Phone < 600dp: Open Hub. 3. Foldable tabletop: Open Hub. 4. RTL locale (ar/he): Open Hub. | V4: left rail (groups + profile), right detail (selected rows); V1: single column, floating nav, profile max 320dp; V3: content centered in bottom half, no hinge cross; RTL: rows/preview mirror, version number stays LTR |
| T10-21 | 200% font (A4) wraps labels, stacks chips, no truncation, 48dp targets | adaptive | P0 | 1. Set display size 200%. 2. Open Settings Hub. | Row labels wrap to 2 lines; value chips stack vertically; profile card grows; touch targets stay 48dp |
| T10-22 | Reduced motion (A5) disables crossfade (120ms fade); no bloom drift | adaptive | P0 | 1. Enable reduced motion. 2. On Appearance, change Theme. | Crossfade = 120 ms fade; no bloom drift in preview |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T10-23 | Probe slow > 300 ms shows skeleton then content; no spinner | performance | P0 | 1. Inject I/O delay on vault size read. 2. Open Settings Hub. | S1 skeleton rows (same geometry) → S3 when ready; no spinner |
| T10-24 | Zero vaults shows `Vault · 0 B` in profile; no "No vault found" | functional | P0 | 1. Delete all vaults. 2. Open Settings Hub. | Profile shows `Vault · 0 B`; no "No vault found" copy |
| T10-25 | Pro status changes mid-screen (E2→E3) rows update in place; no reload | functional | P0 | 1. Pro active. 2. On Settings Hub. 3. Simulate expiry (E3). | Device Sync row badge updates to `[PRO ended]`; gate copy updates on next tap; **no reload** |
| T10-26 | Process death (G4) reopens at /settings if session valid; scroll restored | lifecycle | P0 | 1. On Settings Hub. 2. Kill process. 3. Relaunch. | Cold start → `/lock` → unlock → `/settings`; scroll position restored |
| T10-27 | Export fails on storage full (D3) shows butter banner; partial deleted | error | P1 | 1. Fill storage to D3. 2. On Privacy. 3. Tap `Export my data`. 4. Confirm. | S6: "Free up space to export" + `Retry` / `Cancel`; partial archive deleted |
| T10-28 | App language row (API ≥ 33) opens sheet, recreates activity; session intact | functional | P0 | 1. API ≥ 33. 2. On Appearance. 3. Tap `App language`. Pick new locale. | Activity recreated; layout re-laid; session intact; unsaved name dropped after confirm |
| T10-29 | Surface row Glass unavailable (API < 31) shows reason; preview renders Solid | functional | P0 | 1. API < 31 device. 2. On Appearance. 3. Observe Surface row. | S12: row disabled, explained "Blur needs Android 12 or newer."; preview renders Solid |
| T10-30 | App icon grid absent tiles on missing drawable/alias; grid reflows | functional | P0 | 1. Build without Printer alias. 2. On App icons. | That tile **absent**, not disabled; grid reflows |
| T10-31 | App icon change SecurityException reverts to last good alias; retry copy | error | P1 | 1. Inject SecurityException on alias enable. 2. Tap `Change icon`. 3. Confirm. | S6: "The icon didn't change." + `Try again`; active alias unchanged; second failure adds "If it keeps happening, restart the app." |
| T10-32 | Paywall offline (N2) shows "Connect to see plans"; Restore still runs | functional | P0 | 1. Airplane mode. 2. Open `/pro` from gate. | S8: price line "Connect to see plans."; `Continue` disabled with reason; `Restore` still runs |
| T10-33 | Paywall already owned reconciles silently; no double charge | functional | P0 | 1. Purchase already owned. 2. Tap `Continue`. | Silent reconcile → E2 → S16 "Pro is active."; never second billing launch |
| T10-34 | Notification bodies use neutral strings under disguise; never "Vaultaire" | security | P0 | 1. Disguise active (Calculator). 2. Trigger backup notification. | Notification body **never** says "Vaultaire"; uses neutral string |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T10-35 | No account row, no sign-out, no email anywhere in Settings | security | P0 | 1. Inspect all Settings screens (Hub, Security, Privacy, About, Appearance). | **Never** an account row, sign-out button, email field, or "forgot password" |
| T10-36 | Pro rows keep chevron; never greyed/disabled rows | security | P0 | 1. Free tier. 2. Inspect Device sync, Duress, App icon, Share rows. | All show `[PRO]` badge + chevron; tap opens gate; **never** disabled/greyed row |
| T10-37 | Duress row weight indistinguishable when enabled; no red, no highlight | security | P0 | 1. Pro active. 2. Enable Duress. 3. Return to Security screen. | Duress row visual weight identical to other rows; no red, no special highlight |
| T10-38 | Version long-press diagnostics never exposes keys/phrases/file names/pattern | security | P0 | 1. On Settings Hub. 2. Long-press version 3×. 3. Inspect toast content. | Shows build, vault size, sync state only; **no** keys, phrases, file names, pattern, vault labels |
| T10-39 | Hidden vaults explainer never lists vaults, count, or manage/rename | security | P0 | 1. Open `/settings/security/hidden-vaults`. | **No** vault list, **no** count, **no** "manage" button, **no** rename action |
| T10-40 | Duress setup never lists vaults; decoy by pattern only | security | P0 | 1. Open `/settings/security/duress`. | Compact 5×5 grid, **no** vault names, **no** count, **no** picker; match by pattern only |
| T10-41 | Security info threat model uses honest language; no fear copy | security | P0 | 1. Open `/settings/security-info`. | Lists: what app protects (coercion, theft, cloud leakage), what it doesn't (forensic vault-count estimation, flash residue, offline pattern guessing, loss of pattern+phrase = permanent loss) |
| T10-42 | Privacy top copy declares no server, no account | security | P0 | 1. Open `/settings/privacy`. | "Vaultaire has no server that can read your vault. There is no account to delete." |
| T10-43 | Export archive encrypted with user phrase, never plaintext | security | P0 | 1. Export data. 2. Inspect archive in Files. | AES-256-GCM encrypted; requires phrase to open; no plaintext ZIP |
| T10-44 | Crash reports and diagnostics off by default, opt-in only | security | P0 | 1. Fresh install. 2. Open Privacy. | Both toggles off; no telemetry uploaded unless user enables |
| T10-45 | Zero analytics events on any Settings toggle/navigation/Pro gate | security | P0 | 1. Toggle all settings. 2. Navigate all screens. 3. Check PostHog/network. | **Zero** analytics events; no conversion, no paywall-view, no icon-change metrics |
| T10-46 | Attempt throttle read-only; never implies specific pattern wrong | security | P0 | 1. Trigger throttle tier. 2. Observe Security screen. | Shows tier/deadline neutrally; no "incorrect pattern", no shake, no red |
| T10-47 | Screen capture locked on for secure screens; read-only row | security | P0 | 1. Open Security screen. 2. Observe Screen capture row. | "Locked on for secure screens"; no toggle; FLAG_SECURE enforced on lock, recovery, duress, phrase display |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T10-48 | TalkBack announces screen title on each Settings screen | accessibility | P0 | 1. TalkBack on. 2. Navigate each Settings screen. | "Settings", "Security settings", "Appearance", "Privacy", "About" announced |
| T10-49 | TalkBack reads Pro badge and gate copy on gated rows | accessibility | P0 | 1. TalkBack on, Free tier. 2. Focus Device sync row. | "Device sync, Pro, button" → tap → gate sheet: "Device sync keeps your vault in sync across devices, Try Pro button, Not now button" |
| T10-50 | Two-pane (V4): rail highlights selected group, detail scrolls; foldable tabletop no hinge cross | adaptive | P0 | 1. Tablet. 2. Tap Security in rail. 3. Foldable tabletop: Open Hub. | Rail highlights Security; right pane shows Security rows; profile in rail; V3: content centered in bottom half, no hinge cross |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T10-51 | Auto-lock during Settings Hub shows brand cover; unlock returns to Hub | lifecycle | P0 | 1. Set auto-lock "immediately". 2. Open Settings Hub. 3. Wait threshold. | S9: Brand cover; unlock → returns to Settings Hub |
| T10-52 | Screen off during Settings Hub locks immediately; relaunch shows /lock | lifecycle | P0 | 1. Open Settings Hub. 2. Screen off. 3. Screen on. | App locks; relaunch shows `/lock` |
| T10-53 | Process death mid-export (G4) deletes partial archive; user retries | lifecycle | P0 | 1. Start export. 2. Kill process mid-write. 3. Relaunch. | Partial archive deleted; user retries export |
| T10-54 | Process death during app icon change (G4) reconciles on relaunch | lifecycle | P0 | 1. Tap `Change icon`. 2. Kill at S15. 3. Relaunch. | Application.onCreate() re-applies stored alias; screen re-reads enabled component |
| T10-55 | Play update resets aliases; stored alias reapplied even E1/E3 | lifecycle | P0 | 1. Disguise active (Calculator). 2. Play update installs. 3. Launch. | Stored alias reapplied; disguise persists even if Pro lapsed (07 §3.1) |
| T10-56 | Language switch with name sheet open drops unsaved text | lifecycle | P0 | 1. Open Profile name sheet, type name. 2. Change App language. | Activity recreated; sheet closed; unsaved name dropped after confirm |
| T10-57 | Prefs write atomic (temp+rename); G4 during write → last committed wins | lifecycle | P0 | 1. Toggle setting. 2. Kill during write. 3. Relaunch. | Last committed value; no torn state |
| T10-58 | S9 while name sheet open dismisses sheet, nothing written | lifecycle | P0 | 1. Open Profile name sheet. 2. Auto-lock triggers. | Sheet dismissed with session; old name stands |
| T10-59 | Throttle tier persisted across death; monotonic floor prevents bypass | lifecycle | P0 | 1. Reach tier 2 wait. 2. Kill process. 3. Relaunch. | Wait resumes from persisted deadline; clock rollback re-arms full wait |
| T10-60 | Pending destination from deep link resolves after unlock | lifecycle | P0 | 1. Kill process. 2. Open `vaultaire://settings`. 3. Unlock. | Lands on `/settings`; opaqueId consumed |

## Security invariants to assert
- [ ] **No account row, no sign-out, no email** anywhere in Settings (Hub, Security, Privacy, About, Appearance)
- [ ] **Pro rows keep chevron** and open value sheet (AuroraProGate); never greyed/disabled rows
- [ ] **Duress row weight indistinguishable** when enabled; no red, no special highlight
- [ ] **Version long-press diagnostics** (3× tap) never exposes keys, phrases, file names, pattern, vault labels
- [ ] **Hidden vaults explainer**: no vault list, no count, no "manage", no rename
- [ ] **Duress setup**: no vault list, no count, no picker; decoy designated by pattern only
- [ ] **Security info**: honest threat model; no "military-grade", no fear copy, no lock illustrations
- [ ] **Privacy top copy**: "Vaultaire has no server that can read your vault. There is no account to delete."
- [ ] **Export archive**: AES-256-GCM encrypted with user phrase; never plaintext
- [ ] **Crash reports & diagnostics**: off by default; opt-in only
- [ ] **Zero analytics events** on any Settings toggle, navigation, or Pro gate interaction
- [ ] **Attempt throttle**: read-only, neutral copy; never implies specific pattern was wrong
- [ ] **Screen capture**: locked on for secure screens; read-only row
- [ ] **Disguise alias**: notification bodies use neutral strings (never "Vaultaire")
- [ ] **Biometric toggle off**: does not remove enrolled biometrics
- [ ] **Wipe is silent**: no progress UI; residue acknowledged only in Security info

## Gaps & contradictions
- **screens.md §10** profile card shows `Pro · 2.1 GB` but **flows/04 §4.1** Data contract says "Pro entitlement, alias setting, pending destination" — no mention of how Free tier renders. **Assumed**: `Free · 2.1 GB` but not explicitly specified.
- **screens.md §10** line 369: "About · Privacy · Licenses" as bottom group. **flows/04 §4.1** Exit points table lists separate `/settings/privacy` and `/settings/about` rows. **Consistent** but Licenses not a separate route — likely part of About.
- **flows/04 §4.1** Actions table: Pro row / `Try Pro` — **flows/06 §1.3** Paywall entry points show Settings Pro rows open `/pro` sheet. **Consistent**.
- **screens.md §11** Auto-lock options: "immediately / 30s / 1m / 5m (default 30s)". **flows/04 §4.2** Auto-lock picker value sheet matches. **Consistent**.
- **flows/04 §4.2** Biometric unlock toggle: "Hardware fail → off". **screens.md §11** doesn't specify failure behavior. **Gap**: hardware fail behavior only in flows.
- **flows/04 §4.7** Privacy: "Crash reports toggle — Off by default; opt-in only". **security-architecture/planning.md §5** says "Anonymous usage and crash data go to PostHog by default". **Contradiction**: security-architecture says analytics on by default; flows/04 says off by default. **Resolution**: flows/04 is canonical for Settings UI — both toggles off by default.
- **flows/04 §4.7** Export: "Encrypted archive (AES-256-GCM, user phrase); never plaintext ZIP". **screens.md §14** Backup key reveal gate mentioned but no screen spec. **Gap**: Backup key reveal flow needs separate screen spec.
- **flows/04 §4.8** About: "Long-press version → diagnostics toast". **screens.md §10** line 376: "The version row is long-pressable for diagnostics (never exposes keys)". **Consistent**.
- **flows/06 §1.1** Apply matrix: "there is no vault label… Aurora ships no such row". **screens.md §18** lists vault label row. **Contradiction resolved**: flows/06 is canonical — no vault label row.
- **flows/06 §1.1** Data contract: "Must never appear: vault count, vault label, collection names, file names, pattern data, purchase token, account email." **screens.md §10** profile shows aggregated size only. **Consistent**.
- **flows/06 §1.2** App icons: "The alias label also drives the Recents task title, notification channel names, and the uninstall prompt". **flows/06 Gaps #1**: "Notification copy contradicts a complete disguise… Needs a neutral string set". **Gap**: Not implemented; test T10-34 asserts expected behavior.
- **flows/06 §1.3** Paywall: "No analytics: the only record of a purchase is Play's token". **Consistent** with security invariants.
- **duress-mode/planning.md AC-02**: "Duress pattern checked before vault index lookup". **flows/04 §4.3** Duress setup: pattern grid matches against vault indexes. **Consistent**.
- **duress-mode/planning.md AC-03**: "Silent, multi-pass wipe". **flows/04 §4.3** "Wipe is silent: no progress UI". **Consistent**.
- **security-architecture/planning.md §4**: "No signup flow… No 'forgot pattern' via email… No cloud identity". **Settings Hub** has no account row. **Consistent**.
- **Gap**: **Auto (Android Automotive)** and **TV (V6)** form factors not explicitly covered in Settings screen specs. **Deferred**.
- **Gap**: **Encrypted trash setting** mentioned in flows/04 Gaps #5 ("opt-in 7-day encrypted trash… needs a Settings → Privacy row") but not in screens.md or flows. **Not testable until specified**.
- **Gap**: **Backup key reveal gate** (flows/04 Gaps #10) needs screen spec; not testable here.
- **Gap**: **Sync review list route** (flows/04 Gaps #8) undefined; conflict state mentions "→ review list" but no route.