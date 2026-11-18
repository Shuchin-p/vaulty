# Duress Setup — Test Cases
## Scope
screens.md §12 "Duress Setup (Pro)" + §29 "Decoy believability guidance" · flows/04-security-settings.md §4.3–4.4 · duress-mode/planning.md · plausible-deniability/planning.md · hidden-vaults/planning.md

## Preconditions
- Pro entitlement active (E2)
- At least one vault exists on device (pattern can match)
- Duress mode currently disabled
- App unlocked, user at `/settings/security/duress`
- FLAG_SECURE active for confirmation step; clipboard disabled; recents shows brand cover
- Secure panel (solid) applied per security matrix

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T12-01 | Open duress setup from Security settings | Functional | P0 | 1. From `/settings/security` tap Duress row (Pro active) | Navigates to `/settings/security/duress`; rose-tint card shows "If you're forced to unlock, this pattern opens one vault and erases the rest."; compact 5×5 grid centered; "Choose the vault to open" / "Draw its pattern to confirm" labels; "What gets erased" section lists "All other vault indexes", "All other files", "Local sync state"; "Hold to enable duress" ink pill disabled |
| T12-02 | Draw valid pattern matching existing vault | Functional | P0 | 1. On duress setup screen, draw pattern of an existing vault 2. Release | Iris pulse; screen shows "Encrypting into <Vault label>" (generic, no vault name leaked); "Hold to enable duress" pill becomes active with 2s iris sweep progress |
| T12-03 | Hold ink pill for full 2s to enable | Functional | P0 | 1. After pattern match, press and hold "Hold to enable duress" pill 2. Maintain hold for 2s until sweep completes | Mint check appears: "Duress enabled. Decoy vault set."; Back button returns to `/settings/security`; Duress row now shows enabled state with weight indistinguishable from other rows |
| T12-04 | Duress row weight indistinguishable after enable | Visual | P0 | 1. Return to `/settings/security` after enabling duress 2. Observe Duress row | Row has same visual weight as Change pattern / Recovery phrase / Hidden vaults rows; no special highlight or badge beyond "[PRO]" |
| T12-05 | Open decoy guidance from setup screen | Functional | P0 | 1. On duress setup screen, tap Guidance link | Navigates to `/settings/security/duress/guidance`; title "Making it look lived-in"; body uses "this vault" not "decoy"; tips: add ordinary photos, keep realistic amount, avoid empty vault, don't name collections "fake", use occasionally; butter honesty card non-dismissible: "If someone knows your normal vault exists, a perfect decoy may still not convince them." |
| T12-06 | Return from guidance to setup preserves state | Functional | P1 | 1. Open guidance from setup 2. Tap Back | Returns to `/settings/security/duress` with previous state (pattern drawn / matched / hold progress) intact |
| T12-07 | Deep link opens setup directly | Functional | P1 | 1. Launch `vaultaire://settings/security/duress` with Pro active | Opens duress setup screen directly; same UI as T12-01 |
| T12-08 | Tablet layout: centered card 480dp max with left rail | Visual | P1 | 1. Open setup on device ≥600dp width | Card centered max 480dp; left rail shows Security group highlighted; grid stays 280dp; rose card scrolls if needed |
| T12-09 | Foldable: pattern grid never spans hinge | Visual | P1 | 1. Open setup on foldable in tabletop mode | Grid placed on bottom half; instructions on top; never across fold |
| T12-10 | 200% font scale: rose card scrolls, grid unchanged | Accessibility | P0 | 1. Set system font ≥200% 2. Open setup | Rose card scrolls; grid stays 280dp; text wraps; touch targets ≥48dp |
| T12-11 | Auto-lock during setup discards draft | Security | P0 | 1. Draw pattern on setup screen 2. Wait for auto-lock (30s default) 3. Unlock | Returns to `/settings/security/duress` in initial state (S0); no pattern retained; duress not enabled |
| T12-12 | Backgrounding discards draft | Security | P0 | 1. Draw pattern on setup screen 2. Press Home 3. Reopen app | Returns to `/settings/security/duress` in initial state; draft discarded |
| T12-13 | Process death during hold discards enable | Security | P0 | 1. Match pattern, start 2s hold 2. Kill process before completion 3. Relaunch | Duress not enabled; returns to Security settings or Lock screen per session validity |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T12-14 | Draw pattern matching no vault | Error | P0 | 1. Draw arbitrary pattern not matching any vault 2. Release | Neutral ink toast: "Pattern not recognized" + Retry; grid clears; stays on setup screen; no vault count or list revealed |
| T12-15 | Retry after no-match returns to draw state | Error | P1 | 1. Trigger T12-14 2. Tap Retry / draw again | Grid ready for new pattern (S3 Idle); rose card still visible |
| T12-16 | Release hold before 2s cancels enable | Error | P0 | 1. Match pattern, start hold 2. Release at 1.5s | Returns to S3 Idle; "Hold to enable duress" pill resets; no enable; no partial state persisted |
| T12-17 | Drag cancel during draw clears dots | Error | P1 | 1. Start drawing pattern 2. Drag off grid / cancel gesture | Dots cleared; returns to S3 Idle; no haptic on cancel |
| T12-18 | Open setup without Pro entitlement | Error | P0 | 1. Ensure Pro inactive (E1/E3) 2. Deep link to `/settings/security/duress` | Pro gate `S10` shows: "Duress mode opens a decoy vault and erases others" with "Try Pro" / "Not now"; no access to pattern grid |
| T12-19 | Pro lapses mid-screen shows [PRO] badge immediately | Error | P1 | 1. On setup screen with Pro active 2. Simulate entitlement loss (E2→E3) | Duress row in Security settings shows `[PRO]` immediately; next tap triggers gate |
| T12-20 | No vault exists on device (edge case) | Error | P1 | 1. Fresh install, no vaults created 2. Open setup (requires at least one vault per preconditions) | N/A — preconditions require existing vault; if somehow reached, pattern match fails per T12-14 |
| T12-21 | Guidance screen: honesty card non-dismissible | Visual | P1 | 1. Open guidance 2. Attempt to swipe/dismiss butter card | Card remains; butter background, `body.m` copy visible at all scroll positions |
| T12-22 | Guidance screen: no "decoy" word in title/body | Content | P0 | 1. Open guidance 2. Inspect title and body text | Title: "Making it look lived-in"; body uses "this vault"; no occurrence of "decoy" |
| T12-23 | Guidance screen: no checklist or score | Content | P0 | 1. Open guidance 2. Inspect for interactive checklist or scoring UI | None present; only static tips and honesty card |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T12-24 | Setup screen may use rose + explicit language | Security | P0 | 1. Open setup screen 2. Verify rose-tint card and copy | Rose tint present; copy explicitly states "opens one vault and erases the rest" |
| T12-25 | Unlock screen never uses rose or explicit language | Security | P0 | 1. Enable duress 2. Lock app 3. Enter duress pattern at unlock | Unlock screen shows standard pattern grid (no rose, no "erases" copy); silent wipe runs before decoy opens |
| T12-26 | Never list vaults or show vault count anywhere | Security | P0 | 1. Open setup screen 2. Inspect all UI elements | No vault names, no vault list, no vault count, no picker; only compact 5×5 grid |
| T12-27 | Never reveal which vault is decoy outside this screen | Security | P0 | 1. Enable duress 2. Navigate entire app (Settings, Vault, Timeline, etc.) 3. Search for decoy identifier | No screen shows which vault is decoy; no debug chip; no analytics event contains vault ID; Security settings row weight indistinguishable |
| T12-28 | No analytics event with vault ID on enable | Security | P0 | 1. Enable duress 2. Capture network/logs | No analytics event emitted containing vault ID, pattern hash, or decoy locator |
| T12-29 | No debug chip with vault ID in any build | Security | P0 | 1. Enable duress 2. Inspect UI in debug/beta/release builds | No debug chip, banner, or overlay reveals decoy vault index or ID |
| T12-30 | Disable requires pattern + biometric | Security | P0 | 1. Enable duress 2. Return to Security settings 3. Tap Duress row (now shows disable flow) | Disable flow requires: draw current pattern + biometric auth; copy states "Erasure history cannot be undone" |
| T12-31 | Disable warns erasure history cannot be undone | Security | P0 | 1. Start disable flow 2. Inspect confirmation copy | Explicit warning: "Erasure history cannot be undone" shown before final confirm |
| T12-32 | Pattern grid compact 5×5, no labels | Security | P0 | 1. Open setup 2. Verify grid | 5×5 compact grid; no dot labels, no vault names, no pattern name hints |
| T12-33 | Haptic per dot during draw | Security | P1 | 1. Draw pattern on grid | Haptic feedback per dot connected; no haptic on cancel |
| T12-34 | FLAG_SECURE on confirmation step only | Security | P0 | 1. Open setup 2. Verify FLAG_SECURE via ADB / screenshot attempt | Screenshot blocked during hold-to-enable (confirmation step); allowed on initial draw state per matrix `✅ (confirm)` |
| T12-35 | Clipboard disabled on setup screen | Security | P0 | 1. Open setup 2. Attempt copy/paste in any field | Clipboard auto-clear off; no paste affordance; copy disabled per matrix |
| T12-36 | Recents shows brand cover | Security | P0 | 1. Open setup 2. Open recents/overview | App switcher shows solid brand cover, not screen content |
| T12-37 | Wipe is silent: no progress UI on next duress unlock | Security | P0 | 1. Enable duress 2. Lock app 3. Enter duress pattern | Decoy vault opens directly; no wipe progress, no "wiped" notification, no confirmation; per `duress-mode\planning.md` AC-03 |
| T12-38 | Sync engine paused/isolated on duress trigger | Security | P1 | 1. Enable duress with sync active 2. Trigger duress unlock 3. Inspect sync state | Sync paused; device appears disconnected on other devices; no sync of wiped data per `duress-mode\planning.md` AC-04 |
| T12-39 | AttemptThrottler persists through restarts | Security | P1 | 1. Trigger throttle on unlock 2. Kill app / reboot 3. Verify throttle state | Throttle state persists; 5 KDF submissions / 2 min rate limit maintained per `01 §R1` |
| T12-40 | Duress pattern checked before vault index lookup | Security | P0 | 1. Enable duress 2. Enter duress pattern at unlock 3. Verify execution order | Duress pattern match occurs before any vault index lookup; per `duress-mode\planning.md` AC-02 |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T12-41 | TalkBack: rose card announced on entry | Accessibility | P0 | 1. Enable TalkBack 2. Open setup | "Duress setup. If you're forced to unlock, this pattern opens one vault and erases the rest." announced |
| T12-42 | TalkBack: grid announced as "Pattern grid, 5 by 5" | Accessibility | P0 | 1. Enable TalkBack 2. Focus grid | "Pattern grid, 5 by 5, double tap to draw" or equivalent |
| T12-43 | TalkBack: hold pill announced with state | Accessibility | P0 | 1. Enable TalkBack 2. After pattern match, focus pill | "Hold to enable duress, button, press and hold for 2 seconds" |
| T12-44 | TalkBack: success announced "Duress enabled" | Accessibility | P0 | 1. Enable TalkBack 2. Complete hold | "Duress enabled. Decoy vault set." announced |
| T12-45 | TalkBack: error "Pattern not recognized" announced | Accessibility | P1 | 1. Enable TalkBack 2. Draw non-matching pattern | "Pattern not recognized" announced; focus returns to grid |
| T12-46 | Switch Access: all targets reachable | Accessibility | P1 | 1. Enable Switch Access 2. Navigate setup screen | All controls (grid, pill, guidance link, back) reachable and activatable |
| T12-47 | Color contrast: rose card text meets 4.5:1 | Accessibility | P0 | 1. Inspect rose card text vs background | Contrast ≥4.5:1 for `body.m` on rose tint |
| T12-48 | Color contrast: mint check text meets 4.5:1 | Accessibility | P0 | 1. Inspect success state text | Contrast ≥4.5:1 for mint check text |
| T12-49 | Touch targets ≥48dp on all controls | Accessibility | P0 | 1. Measure ink pill, guidance link, back button | All ≥48×48dp |
| T12-50 | Landscape phone: grid centered, not stretched | Adaptive | P1 | 1. Rotate to landscape on phone | Grid centered in single pane; rose card above; never split across panes |
| T12-51 | Tablet two-pane: rail stays on Security | Adaptive | P0 | 1. Open setup on tablet ≥600dp | Left rail highlights Security group; detail pane shows setup card |
| T12-52 | Foldable tabletop: grid bottom half, instructions top | Adaptive | P1 | 1. Open setup on foldable in tabletop | Grid on bottom half; rose card / instructions on top half |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T12-53 | Pro purchase mid-screen unlocks setup | Race | P1 | 1. Open setup with Pro inactive (gate showing) 2. Purchase Pro 3. Return to app | Gate dismissed; setup screen (S0) shown automatically; no manual navigation needed |
| T12-54 | Pro expiry mid-screen shows [PRO] badge | Race | P1 | 1. On setup screen with Pro active 2. Expire Pro entitlement 3. Observe Security settings row | Row shows `[PRO]` immediately; next tap triggers gate |
| T12-55 | Configuration change (rotation) preserves draw state | Lifecycle | P1 | 1. Draw partial pattern 2. Rotate device | Partial pattern retained; grid state preserved |
| T12-56 | Configuration change during hold preserves progress | Lifecycle | P1 | 1. Start 2s hold 2. Rotate at 1s | Hold progress preserved; completes at 2s total; enable succeeds |
| T12-57 | Process death during setup returns to Security or Lock | Lifecycle | P0 | 1. On setup screen 2. Kill process 3. Relaunch | Returns to `/settings/security` if session valid; else `/lock`; scroll position restored per `00-state-model.md G4` |
| T12-58 | Process death during guidance returns to guidance | Lifecycle | P1 | 1. On guidance screen 2. Kill process 3. Relaunch | Returns to guidance if session valid |
| T12-59 | Background > threshold locks app, discards draft | Lifecycle | P0 | 1. Draw pattern on setup 2. Background app > auto-lock threshold 3. Foreground | Brand cover shown; unlock required; draft discarded; returns to setup initial state |

## Security invariants to assert
- [ ] Never lists vaults anywhere on setup, guidance, or Security settings screen
- [ ] Never reveals which vault is the decoy outside `/settings/security/duress` (no debug chip, no analytics event with vault ID, no toast/log with vault identifier)
- [ ] 2s hold confirm with iris sweep mandatory for enable; release before 2s cancels
- [ ] Setup screen may use rose tint and explicit "erases" language; unlock screen must not
- [ ] No analytics event emitted containing vault ID, pattern hash, decoy locator, or wipe status
- [ ] Duress row weight indistinguishable from other security rows when enabled
- [ ] Disable flow requires pattern + biometric; copy states "Erasure history cannot be undone"
- [ ] FLAG_SECURE applied only to confirmation step (hold-to-enable) per matrix
- [ ] Clipboard disabled on setup screen
- [ ] Recents shows brand cover
- [ ] Wipe is silent: no progress UI, no notification, no "wiped" message on duress unlock
- [ ] Decoy guidance title never uses "decoy"; body uses "this vault"
- [ ] Honesty card on guidance non-dismissible, butter background
- [ ] Pattern grid compact 5×5, no labels, no vault names, no count
- [ ] No checklist, scoring, or discoverable artifacts on guidance screen

## Gaps & contradictions
- **Flows doc §4.3** states "Duress already enabled: screen shows current state (rose card becomes 'Duress is enabled. Hold to disable.')" but **screens.md §12** does not describe this enabled state UI — only the setup flow. Test coverage needed for disable flow (T12-30, T12-31).
- **Flows doc §4.3** says disabling "requires pattern + biometric, warns erasure history cannot be undone" but no screen spec exists for the disable confirmation screen — only the enable flow is in screens.md.
- **Duress-mode planning.md** AC-03 says "Silent, multi-pass wipe of internal storage `.dat` files" but **flows doc §4.3** says "wipe runs before decoy opens" with "no progress UI" — consistent, but multi-pass detail only in planning doc.
- **Plausible-deniability planning.md** AC-3 requires pattern-derived locator lookup (no vault ID iteration) — flows doc §4.3 "Derive key, match against one vault's index" aligns; verify implementation uses constant-time check per duress-mode planning.md step 14.
- **Hidden-vaults planning.md** AC-3: "Empty State for Unknown Patterns" creates vault automatically — but duress setup requires matching an *existing* vault. Contradiction if user draws unused pattern on setup: flows doc says "No match → S6 neutral 'Pattern not recognized'" (not auto-create). This is correct for duress (must designate existing vault as decoy).
- **Security matrix** shows Duress setup: `FLAG_SECURE (confirm)` and `Solid panel` — but flows doc states `Secure: yes (confirmation step)`. Clarify: FLAG_SECURE only during hold-to-enable (S14), not full screen.
- **Flows doc §4.3** state `S15 Committing` shows "Encrypting into <Vault label> (generic)" — but **plausible-deniability planning.md** says "No vault names shown in app bar (or shown as generic 'Vault')". Confirm "generic" means never the actual user-defined label.
- **Flows doc §4.3** entry points include deep link `vaultaire://settings/security/duress` with precondition "Pro active" — but no handling for deep link when Pro inactive (gate should show). T12-18 covers this.
- **Gaps list in flows doc** item 4: "Duress partial-wipe has no durable ledger... crash loop could strand residue silently" — no test can verify wipe completion; only behavior tests (T12-37, T12-38).
- **Decoy guidance** reachable only from Duress setup per screens.md §29 — flows doc §4.4 confirms single entry point. No standalone access.