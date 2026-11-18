# About / Privacy — Test Cases

## Scope
`screens.md §19` (About/Privacy) + `flows/04-security-settings.md §4.7–4.8` (Privacy & About screens) + `screens.md §30` (Privacy & Security Info details) + `security-architecture/planning.md` (invariants) + `plausible-deniability/planning.md` (forensic limits)

## Preconditions
- App installed, vault unlocked, session valid
- Version row visible: `Version 1.0.0 (build 42)` format
- Licenses list rendered (open-source dependencies)
- Threat model summary present (plain language, honest limits)
- Privacy policy link → local markdown, 44dp close button
- Security architecture link → local markdown, 44dp close button
- Privacy screen: Crash reports (off), Diagnostics (off), Local activity log (on, encrypted), Clipboard auto-clear (30/60/off), Hide previews in recents (on)
- Top copy: "Vaultaire has no server that can read your vault. There is no account to delete."
- No analytics SDK, no advertising ID, no third-party trackers stated in one sentence
- Export my data → encrypted archive only (AES-256-GCM, user phrase), never plaintext ZIP
- Diagnostics opt-in: local, encrypted, never includes keys/phrases/file names
- Security info screen: threat model, honest limits (forensic vault-count, flash residue, offline guessing, permanent loss), FLAG_SECURE blocks/doesn't block, no "military-grade" claims
- Security info action: `Read the full threat model` → local markdown, 44dp close
- Long-press version 3× within 1s → diagnostics toast (build, vault size, sync state, **never keys**)

---

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T19-01 | Open About from Settings | Functional | P0 | 1. Open `/settings`<br>2. Tap About row | Push `/settings/about`; title "About"; version, build, licenses, threat model summary, Privacy policy & Security architecture links visible |
| T19-02 | Open Privacy from Settings | Functional | P0 | 1. Open `/settings`<br>2. Tap Privacy row | Push `/settings/privacy`; title "Privacy"; top copy "Vaultaire has no server that can read your vault. There is no account to delete."; rows rendered |
| T19-03 | Open Security info from Security settings | Functional | P0 | 1. Open `/settings/security`<br>2. Tap Security info row | Push `/settings/security-info`; title "Security info"; threat model, honest limits, FLAG_SECURE info, `Read the full threat model` link |
| T19-04 | Open Privacy policy from About | Functional | P0 | 1. Open `/settings/about`<br>2. Tap Privacy policy link | Push local privacy policy markdown; 44dp close button top-right; content readable; close returns to About |
| T19-05 | Open Security architecture from About | Functional | P0 | 1. Open `/settings/about`<br>2. Tap Security architecture link | Push local security architecture markdown; 44dp close button top-right; content readable; close returns to About |
| T19-06 | Read full threat model from Security info | Functional | P0 | 1. Open `/settings/security-info`<br>2. Tap `Read the full threat model` | Push local threat model markdown; 44dp close button; content includes forensic limits, offline guessing, permanent loss; close returns |
| T19-07 | Toggle Crash reports off→on | Functional | P1 | 1. Open `/settings/privacy`<br>2. Tap Crash reports toggle (off) | Toggle switches on; state persists; no analytics SDK implied by copy |
| T19-08 | Toggle Crash reports on→off | Functional | P1 | 1. Open `/settings/privacy` (Crash reports on)<br>2. Tap Crash reports toggle | Toggle switches off; state persists; zero telemetry sent |
| T19-09 | Toggle Diagnostics off→on | Functional | P1 | 1. Open `/settings/privacy`<br>2. Tap Diagnostics toggle (off) | Toggle switches on; local encrypted diagnostics enabled; toast confirms "Diagnostics enabled locally" |
| T19-10 | Toggle Diagnostics on→off | Functional | P1 | 1. Open `/settings/privacy` (Diagnostics on)<br>2. Tap Diagnostics toggle | Toggle switches off; local diagnostics disabled; no data retained |
| T19-11 | Toggle Local activity log on→off | Functional | P1 | 1. Open `/settings/privacy`<br>2. Tap Local activity log toggle (on) | Confirm dialog "Clear local activity log? This can't be undone."; Cancel keeps on; Confirm clears log, toggle off |
| T19-12 | Change Clipboard auto-clear | Functional | P1 | 1. Open `/settings/privacy`<br>2. Tap Clipboard auto-clear row<br>3. Select 60s | Value sheet shows 30s/60s/off; selection saves; `S16` success state |
| T19-13 | Toggle Hide previews in recents | Functional | P1 | 1. Open `/settings/privacy`<br>2. Tap Hide previews toggle (on) | Toggle switches off; recents snapshot changes from solid to blurred; persists |
| T19-14 | Export my data success | Functional | P0 | 1. Open `/settings/privacy`<br>2. Tap Export my data<br>3. Confirm "Export creates an encrypted archive. Save to Files?" | Iris progress "Creating encrypted archive…"; mint toast "Archive saved to Files"; archive is AES-256-GCM encrypted with user phrase |
| T19-15 | Long-press version for diagnostics | Functional | P0 | 1. Open `/settings/about`<br>2. Long-press version 3× within 1s | Toast shows: build, vault size (e.g., "2.1 GB"), sync state (e.g., "Synced 2m ago"); **no keys, phrases, file names** |
| T19-16 | Scroll About content | Functional | P1 | 1. Open `/settings/about`<br>2. Scroll through licenses, threat model summary | All content accessible; no truncation; licenses list complete |
| T19-17 | Scroll Privacy content | Functional | P1 | 1. Open `/settings/privacy`<br>2. Scroll through all rows | All rows accessible; top copy visible; no analytics badge wall |
| T19-18 | Scroll Security info content | Functional | P1 | 1. Open `/settings/security-info`<br>2. Scroll through threat model, honest limits, FLAG_SECURE | All content accessible; honest limits include: forensic vault-count estimation, flash residue after duress wipe, offline pattern guessing, loss of pattern+phrase=permanent loss |
| T19-19 | Back navigation from About | Functional | P0 | 1. Open `/settings/about`<br>2. Press back gesture | Returns to `/settings`; no side effects |
| T19-20 | Back navigation from Privacy | Functional | P0 | 1. Open `/settings/privacy`<br>2. Press back gesture | Returns to `/settings`; no side effects |
| T19-21 | Back navigation from Security info | Functional | P0 | 1. Open `/settings/security-info`<br>2. Press back gesture | Returns to `/settings/security`; no side effects |
| T19-22 | Deep link to About | Functional | P1 | 1. Launch `vaultaire://settings/about` | Opens `/settings/about` directly; same content as nav |
| T19-23 | Deep link to Privacy | Functional | P1 | 1. Launch `vaultaire://settings/privacy` | Opens `/settings/privacy` directly; same content as nav |
| T19-24 | Verify no analytics toggle exists | Functional | P0 | 1. Open `/settings/privacy`<br>2. Scan all rows | No "Usage Analytics" or "Analytics" toggle; copy states "No analytics SDK, no advertising ID, no third-party trackers" in one sentence |

---

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T19-25 | Export my data — storage full (`D3`) | Error | P0 | 1. Fill device to ≤50 MB free<br>2. Open `/settings/privacy`<br>3. Tap Export my data → Confirm | Butter banner "Free up space to export"; Retry/Cancel; no partial archive left |
| T19-26 | Export my data — process death mid-export | Error | P1 | 1. Start export<br>2. Kill app process during "Creating encrypted archive…" | On reopen: partial archive deleted; user can retry; no corrupt state |
| T19-27 | Clear activity log — cancel | Edge | P1 | 1. Open `/settings/privacy`<br>2. Tap Local activity log (on)<br>3. Tap Cancel in confirm dialog | Log remains on; toggle stays on; no data cleared |
| T19-28 | Privacy policy link — missing file | Error | P1 | 1. Delete local privacy policy markdown (test hook)<br>2. Tap Privacy policy link | Fatal screen: "This screen couldn't open. Your vault is safe." + `Back to vault` |
| T19-29 | Security architecture link — missing file | Error | P1 | 1. Delete local security architecture markdown<br>2. Tap Security architecture link | Fatal screen: "This screen couldn't open. Your vault is safe." + `Back to vault` |
| T19-30 | Threat model link — missing file | Error | P1 | 1. Delete local threat model markdown<br>2. Tap `Read the full threat model` | Fatal screen: "This screen couldn't open. Your vault is safe." + `Back to vault` |
| T19-31 | Long-press version — <3 taps | Edge | P1 | 1. Open `/settings/about`<br>2. Tap version 2× within 1s | No toast; no diagnostics shown |
| T19-32 | Long-press version — >1s between taps | Edge | P1 | 1. Open `/settings/about`<br>2. Tap version, wait 2s, tap 2× | No toast; counter resets |
| T19-33 | Diagnostics toggle — verify no secrets in export | Security | P0 | 1. Enable Diagnostics<br>2. Trigger local diagnostics export (test hook)<br>3. Inspect output | Output contains build, vault size, sync state; **no keys, no phrases, no file names, no vault labels, no pattern coordinates** |
| T19-34 | Crash reports toggle — verify no network on off | Security | P0 | 1. Ensure Crash reports off<br>2. Force crash (test hook)<br>3. Monitor network | Zero outbound crash report requests; local only |
| T19-35 | Export archive — verify encryption | Security | P0 | 1. Export my data<br>2. Inspect archive in Files app | Archive encrypted with AES-256-GCM; requires user phrase to open; never plaintext ZIP |
| T19-36 | Local activity log — verify encrypted at rest | Security | P1 | 1. Enable Local activity log<br>2. Inspect app storage (test hook) | Log stored encrypted; plaintext not recoverable without app |
| T19-37 | Hide previews — verify recents cover | Functional | P1 | 1. Toggle Hide previews on<br>2. Background app<br>3. Open recents | Solid brand cover (not blurred) |
| T19-38 | Hide previews off — verify recents blur | Functional | P1 | 1. Toggle Hide previews off<br>2. Background app<br>3. Open recents | Blurred snapshot (not solid) |
| T19-39 | Clipboard auto-clear — 30s default | Functional | P1 | 1. Copy text in vault<br>2. Wait 30s<br>3. Paste in external app | Clipboard cleared; paste yields empty/previous |
| T19-40 | Clipboard auto-clear — off disables | Functional | P1 | 1. Set Clipboard auto-clear to off<br>2. Copy text in vault<br>3. Wait 60s<br>4. Paste in external app | Clipboard retains text; not auto-cleared |

---

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T19-41 | No "military-grade" claims anywhere | Security | P0 | 1. Open `/settings/about`, `/settings/privacy`, `/settings/security-info`<br>2. Search all text | Zero occurrences of "military-grade", "unbreakable", "100% secure", "bank-level" |
| T19-42 | No analytics SDK present | Security | P0 | 1. Inspect build.gradle / dependencies<br>2. Verify no PostHog, Firebase Analytics, etc. | No analytics SDK compiled; copy confirms "No analytics SDK" |
| T19-43 | No advertising ID collected | Security | P0 | 1. Monitor network on app launch<br>2. Check for advertising ID requests | Zero requests for advertising ID; copy confirms "no advertising ID" |
| T19-44 | No third-party trackers | Security | P0 | 1. Inspect network traffic on all screens<br>2. Check for tracker domains | Zero tracker domains; copy confirms "no third-party trackers" |
| T19-45 | Threat model — honest limits listed | Security | P0 | 1. Open `/settings/security-info`<br>2. Verify honest limits section | Lists: forensic vault-count estimation; flash residue after duress wipe; offline pattern guessing against copied indexes; loss of pattern + phrase = permanent loss |
| T19-46 | FLAG_SECURE — blocks screenshots on secure screens | Security | P0 | 1. Open `/lock`, `/vault/camera`, `/recovery/display`<br>2. Attempt screenshot | Screenshot blocked; system shows "Couldn't capture screenshot" |
| T19-47 | FLAG_SECURE — blocks screen recording | Security | P0 | 1. Start screen recording<br>2. Navigate to secure screens | Secure screens show black in recording; non-secure show content |
| T19-48 | FLAG_SECURE — doesn't block camera photo | Security | P0 | 1. Point another camera at screen on `/lock`<br>2. Photograph | Photo captures content; copy in Security info states this limitation |
| T19-49 | Diagnostics never include secrets | Security | P0 | 1. Enable Diagnostics<br>2. Unlock vault, perform actions<br>3. Export diagnostics<br>4. Inspect | No pattern coordinates, no recovery phrase, no keys, no file names, no vault labels, no vault IDs, no sync record IDs, no IP addresses |
| T19-50 | Export archive uses separate key | Security | P0 | 1. Export my data<br>2. Verify encryption key derivation | Archive encrypted with key derived from recovery phrase (or user phrase), **not** MVK or VDK |
| T19-51 | Privacy screen — no vault metadata exposed | Security | P0 | 1. Open `/settings/privacy` with multiple vaults<br>2. Inspect all rows and copy | No vault count, no vault labels, no vault IDs, no file names, no collection names |
| T19-52 | About screen — no vault metadata exposed | Security | P0 | 1. Open `/settings/about` with multiple vaults<br>2. Inspect version, build, licenses, threat model summary | No vault count, no vault labels, no vault IDs, no file names |
| T19-53 | Security info — no vault metadata exposed | Security | P0 | 1. Open `/settings/security-info`<br>2. Inspect threat model, honest limits | No vault count, no vault labels, no vault IDs, no forensic estimates as exact numbers |
| T19-54 | Local activity log — no file names/vault labels | Security | P1 | 1. Enable Local activity log<br>2. Perform file operations across vaults<br>3. View log (test hook) | Log entries show generic actions only (e.g., "Imported 5 items"); no file names, no vault labels |
| T19-55 | Crash reports — scrubbed of secrets | Security | P1 | 1. Enable Crash reports<br>2. Force crash with keys in memory<br>3. Inspect crash dump (test hook) | MemoryHygiene zeroed keys before dump; no pattern, phrase, MVK, VDK in crash data |

---

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T19-56 | `A4` 200% font — About | Accessibility | P0 | 1. Set system font scale 200%<br>2. Open `/settings/about` | Text wraps; licenses scroll; no truncation; touch targets ≥48dp; 44dp close button still 44dp |
| T19-57 | `A4` 200% font — Privacy | Accessibility | P0 | 1. Set system font scale 200%<br>2. Open `/settings/privacy` | Row labels wrap; value chips stack; top copy scrolls; all toggles accessible |
| T19-58 | `A4` 200% font — Security info | Accessibility | P0 | 1. Set system font scale 200%<br>2. Open `/settings/security-info` | Text wraps; no truncation; `Read the full threat model` link accessible |
| T19-59 | `A2` TalkBack — About | Accessibility | P1 | 1. Enable TalkBack<br>2. Navigate `/settings/about` | Version, build, licenses announced; links announced as "Privacy policy, link", "Security architecture, link"; long-press hint announced |
| T19-60 | `A2` TalkBack — Privacy | Accessibility | P1 | 1. Enable TalkBack<br>2. Navigate `/settings/privacy` | Top copy announced; each toggle announced with state "Crash reports, off, switch"; Export my data announced as button |
| T19-61 | `A2` TalkBack — Security info | Accessibility | P1 | 1. Enable TalkBack<br>2. Navigate `/settings/security-info` | Threat model announced; honest limits announced; `Read the full threat model` announced as link |
| T19-62 | `A3` Switch access — linear focus | Accessibility | P1 | 1. Enable Switch access<br>2. Navigate all three screens | Linear focus order; no drag-only gestures required; all actions reachable |
| T19-63 | `V1` phone portrait — single column | Adaptive | P0 | 1. Width <600dp<br>2. Open all three screens | Single column; floating nav; content centered; 320dp max width for About |
| T19-64 | `V4` tablet — two-pane | Adaptive | P0 | 1. Width 600–839dp<br>2. Open all three screens | Two-pane: left rail (groups), right detail; About/Privacy/Security info in detail pane |
| T19-65 | `V5` desktop — three-pane | Adaptive | P1 | 1. Width ≥840dp<br>2. Open all three screens | Rail + detail; content capped at 1040dp; keyboard + pointer navigation works |
| T19-66 | `V6` multi-window — minimum size | Adaptive | P1 | 1. Resize window to 320×480dp<br>2. Open all three screens | Content reflows; no horizontal scroll; touch targets maintained |
| T19-67 | `A5` Reduced motion — no springs | Accessibility | P1 | 1. Enable reduced motion<br>2. Navigate between screens | Transitions 120ms fades; no spring animations; duress instant unaffected |
| T19-68 | `A6` RTL — mirrored layout | Accessibility | P1 | 1. Set RTL locale<br>2. Open all three screens | Chevrons mirrored; grid column order mirrored; numbers stay LTR; text RTL |

---

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T19-69 | Process death (`G4`) — About | Lifecycle | P1 | 1. Open `/settings/about`<br>2. Kill process<br>3. Relaunch app | Reopens at `/settings/about` if session valid; else `/lock`; scroll position restored |
| T19-70 | Process death (`G4`) — Privacy | Lifecycle | P1 | 1. Open `/settings/privacy`<br>2. Kill process<br>3. Relaunch app | Reopens at `/settings/privacy` if session valid; else `/lock`; toggle states preserved |
| T19-71 | Process death (`G4`) — Security info | Lifecycle | P1 | 1. Open `/settings/security-info`<br>2. Kill process<br>3. Relaunch app | Reopens at `/settings/security-info` if session valid; else `/lock` |
| T19-72 | Auto-lock (`S9`) during About | Lifecycle | P0 | 1. Open `/settings/about`<br>2. Wait past auto-lock threshold | Brand cover shown; unlock returns to About; scroll position restored |
| T19-73 | Auto-lock (`S9`) during Privacy | Lifecycle | P0 | 1. Open `/settings/privacy`<br>2. Wait past auto-lock threshold | Brand cover shown; unlock returns to Privacy; toggle states preserved |
| T19-74 | Auto-lock (`S9`) during Security info | Lifecycle | P0 | 1. Open `/settings/security-info`<br>2. Wait past auto-lock threshold | Brand cover shown; unlock returns to Security info |
| T19-75 | Background (`N2` offline) — no banner | Lifecycle | P1 | 1. Disable network<br>2. Open all three screens | **No offline banner** (local-only screens); content fully functional |
| T19-76 | Pro lapse (`E3`) — no gated rows here | Lifecycle | P1 | 1. Let Pro expire while on Privacy screen<br>2. Observe | No Pro gate on Privacy/About/Security info; all rows remain accessible |
| T19-77 | Storage full (`D3`) — Export blocked | Lifecycle | P0 | 1. Fill storage to `D3`<br>2. Tap Export my data | Export blocked at action: "Free up space to export"; reads unaffected |
| T19-78 | Battery saver (`X2`) — Solid mode | Lifecycle | P1 | 1. Enable battery saver<br>2. Open all three screens | Appearance shows "Solid (set by battery saver)"; no nag; preference preserved |
| T19-79 | Thermal throttling (`X4`) — no effect | Lifecycle | P1 | 1. Simulate thermal throttle<br>2. Open all three screens | No UI change (non-camera screens); no thermal copy shown |
| T19-80 | Screen capture (`X5`) — About/Privacy/Security info | Lifecycle | P1 | 1. Start screen recording<br>2. Open all three screens | Blurred snapshot in recording (not secure screens); no capture blocked notice |

---

## Security invariants to assert

- [ ] **No analytics toggle exists** — there is no analytics SDK; copy states "No analytics SDK, no advertising ID, no third-party trackers" in one sentence
- [ ] **Diagnostics never include keys/phrases/file names** — local, encrypted, opt-in only; export contains only build, vault size, sync state
- [ ] **In-app documents use 44dp close button** — Privacy policy, Security architecture, Threat model all have 44dp close top-right
- [ ] **No "military-grade" claims** — zero occurrences of "military-grade", "unbreakable", "100% secure", "bank-level" in any screen
- [ ] **Export my data produces encrypted archive only** — AES-256-GCM with user phrase; never plaintext ZIP
- [ ] **Privacy top copy exact** — "Vaultaire has no server that can read your vault. There is no account to delete."
- [ ] **Honest limits listed in Security info** — forensic vault-count estimation; flash residue after duress wipe; offline pattern guessing against copied indexes; loss of pattern + phrase = permanent loss
- [ ] **FLAG_SECURE documented honestly** — blocks screenshots, screen recording, casting; does not block another camera pointed at screen
- [ ] **Crash reports off by default** — opt-in only; separate toggle from Diagnostics
- [ ] **Diagnostics off by default** — opt-in only; local, encrypted, no secrets
- [ ] **Local activity log on by default, encrypted** — clear requires confirmation; clearing is local only
- [ ] **Long-press version diagnostics toast** — shows build, vault size, sync state; **never keys, phrases, file names**
- [ ] **No vault metadata in any screen** — no vault count, labels, IDs, file names, collection names
- [ ] **Recents cover respects Hide previews** — on=solid brand cover, off=blurred snapshot

---

## Gaps & contradictions

- **`screens.md §19`** says "No analytics toggle (there is no analytics)" but **`security-architecture/planning.md §5`** describes "Two toggles: 'Usage Analytics' + 'Crash Diagnostics'" and "PostHog SDK integrated with opt-out flag" — direct contradiction on whether analytics SDK exists
- **`flows/04-security-settings.md §4.7`** Privacy screen shows "Crash reports (off by default), Diagnostics (off)" but **`security-architecture/planning.md`** implies analytics toggle should exist — plan unclear on whether crash reports = analytics or separate
- **`screens.md §30.1`** says "State it in one sentence, not a badge wall" for no analytics claim, but **`security-architecture/planning.md`** markets "Analytics opt-out (granular)" as competitive differentiator — mixed messaging
- **`flows/04-security-settings.md §4.8`** About screen long-press shows "build, vault size, sync state" but **`screens.md §19`** says "long-press version for local diagnostics (never exposes keys)" — consistent but "diagnostics" term overloaded with Privacy screen Diagnostics toggle
- **`flows/04-security-settings.md §4.7`** Export my data "produces encrypted archive (AES-256-GCM, user phrase)" but no spec on whether phrase = recovery phrase or separate export phrase — key derivation unspecified
- **`screens.md §30.2`** Security info includes "What FLAG_SECURE blocks" but **`screens.md §33`** screen-level matrix shows Settings (which includes Privacy/About/Security info) as `FLAG_SECURE: —` — Settings screens not protected by FLAG_SECURE per matrix, but Security info documents FLAG_SECURE behavior for other screens
- **`plausible-deniability/planning.md §5`** honest limits table includes "Examiner can guess patterns offline and verify them against index tags" but **`screens.md §30.2`** Security info says "offline pattern guessing against copied indexes" — consistent but could be more specific about what "copied indexes" means
- **`flows/07-system-states.md §10`** bans "military-grade" but **`security-architecture/planning.md §7`** marketing statement says "mathematically prevents us from decrypting your data" — not a banned phrase but marketing language that could be challenged