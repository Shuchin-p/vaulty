# App Icons — Test Cases

## Scope
screens.md §17 "App Icons (Pro)" + flows/06-appearance-and-monetization.md §1.2 (entry/exit/actions/states/flows/edge cases) + app-icons/planning.md (AC-01..06, icon set, Pro gate) + plausible-deniability/planning.md (no in-app disguise leakage, launcher-only alias)

## Preconditions
- Pro entitlement `E2` active for change actions; `E1`/`E3`/`E4` for gate/preview flows
- Launcher alias resolution: `PackageManager.getComponentEnabledSetting()` reads the currently enabled alias
- Confirmation dialog copy is exactly: "The app will now appear as Calculator."
- Splash branding under disguise: `/` shows canvas only, no wordmark when alias ≠ default (flows/01 §1.1 S1 disguise)

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T17-01 | Pro user opens screen, sees 8 tiles with Current highlighted | Functional | P0 | 1. Have Pro `E2`<br>2. Navigate to `/settings/appearance/icons` | Grid renders 8 tiles (Vaultaire + 7 utility), Vaultaire tile has iris ring + check, label "Vaultaire" under each, `Change icon` button visible, announcement "App icon" |
| T17-02 | Pro user taps Calculator tile → confirm → Change → alias written | Functional | P0 | 1. Pro `E2`<br>2. Tap Calculator tile<br>3. Confirm dialog appears<br>4. Tap `Change` | Selection ring moves to Calculator; dialog shows new icon + "The app will now appear as Calculator."; `Change`/`Cancel`; on `Change` spinner shows "Changing icon"; on success tile selected, note "The launcher can take a few seconds" + `Check again`, announcement "The app will now appear as Calculator." |
| T17-03 | Pro user reverts to default Vaultaire icon | Functional | P0 | 1. Pro `E2`, active alias = Calculator<br>2. Tap Vaultaire tile<br>3. Confirm → `Change` | Main alias enabled, seven utility aliases disabled; prefs cleared; Vaultaire tile gets ring + check; announcement "The app will now appear as Vaultaire." |
| T17-04 | Free user previews all 8 tiles, no change possible | Functional | P1 | 1. Free `E1`<br>2. Open `/settings/appearance/icons`<br>3. Tap any utility tile | Grid fully visible and browsable; `AuroraProGate` sheet opens: "App icons are part of Pro", `Try Pro`, `Not now`; no alias write; on `Not now` returns to grid |
| T17-05 | Pro user purchases via gate sheet on icon tap | Functional | P0 | 1. Free `E1`<br>2. Tap utility tile → gate sheet → `Try Pro`<br>3. Complete purchase on `/pro`<br>4. Return to icons screen | Entitlement flips to `E2`; gate sheet closes; origin screen re-armed; tile tap now triggers confirm dialog (S14) |
| T17-06 | Expired Pro user sees lapsed copy, active disguise stays | Functional | P1 | 1. Pro `E3` (expired), active alias = Calculator<br>2. Open icons screen | Grid fully visible; tap utility tile → gate sheet: "Your Pro access has ended. Your data is still here."; Calculator tile remains selected; no auto-revert |
| T17-07 | Pending purchase `E4` shows inline "Finishing purchase…" + `Check again` | Functional | P1 | 1. `E4` state<br>2. Open icons screen | Inline neutral note "Finishing purchase…" + `Check again` after 60 s; tiles not operable; `Check again` re-queries purchases |
| T17-08 | Launcher lag: stay in S16 with note + `Check again`, no false failure | Functional | P1 | 1. Pro `E2`, switch to Calculator<br>2. Wait 10 s, launcher still shows old icon | Screen shows note "The launcher can take a few seconds" + `Check again`; `Check again` reads enabled component; no spinner past 8 s; write not re-run |
| T17-09 | `Check again` confirms alias after launcher catches up | Functional | P1 | 1. In S16 after switch<br>2. Tap `Check again` when launcher updated | Enabled component matches selection; note clears; grid interactive |
| T17-10 | Play update reconciliation re-applies stored alias on cold start | Functional | P0 | 1. Pro `E2`, alias = Calculator<br>2. Simulate app update (reinstall)<br>3. Cold launch | `Application.onCreate()` re-enables Calculator alias; splash shows no wordmark (disguise active); icons screen shows Calculator selected |
| T17-11 | Play update reconciliation works under `E3` (expired) — disguise stays | Security | P0 | 1. `E3`, alias = Calculator<br>2. App update + cold launch | Alias re-applied (already-changed disguise persists per 07 §3.1); only *changing* is gated |
| T17-12 | Tablet two-pane: Current pinned right, grid left, 720 dp cap | Layout | P1 | 1. Tablet 600–839 dp<br>2. Open icons screen | Two panes: Current block right, grid 6–8 columns left; max width 720 dp; nav rail present |
| T17-13 | 200% font: 3 columns, labels wrap 2 lines, `Change icon` 60 dp | Accessibility | P1 | 1. Font scale 200%<br>2. Open icons screen | Grid 3 columns; labels wrap to 2 lines, no truncation; `Change icon` button 60 dp height; dialog copy wraps, primary stays visible |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T17-14 | Switch fails: `SecurityException` → S6 banner + `Try again`, reverts to last good alias | Error | P0 | 1. Pro `E2`<br>2. Mock `setComponentEnabledSetting` throwing `SecurityException`<br>3. Tap utility → confirm → `Change` | S15 spinner → S6 banner "The icon didn't change." + `Try again`; active alias unchanged; `Try again` re-runs identical write |
| T17-15 | Second failure adds "If it keeps happening, restart the app." | Error | P1 | 1. Trigger T17-14 failure<br>2. Tap `Try again` → fails again | Banner updates with additional line "If it keeps happening, restart the app." |
| T17-16 | Missing drawable/alias on this build → tile absent, not disabled | Error | P1 | 1. Build without Calculator alias/drawable<br>2. Open icons screen | Calculator tile completely absent from grid; remaining 7 tiles render; no gap |
| T17-17 | Rapid switching: tiles disabled during S15, second tap ignored | Race | P1 | 1. Pro `E2`<br>2. Tap utility → confirm → `Change`<br>3. Immediately tap another tile while spinner shows | Second tap ignored, not queued; only first switch proceeds |
| T17-18 | Backgrounded mid-switch: write atomic, on return screen re-reads truth | Lifecycle | P1 | 1. Pro `E2`, tap utility → confirm → `Change`<br>2. Home before S15 completes<br>3. Return to app | Screen re-reads enabled component; shows actual state (S3 if write done, S6 if failed); process survived (`DONT_KILL_APP`) |
| T17-19 | Auto-lock during screen: brand cover, no content beneath, alias state committed | Lifecycle | P1 | 1. Pro `E2` on icons screen<br>2. Wait for auto-lock threshold<br>3. Unlock | `/lock` shows solid cover; on unlock returns to icons screen with selection intact; alias state already committed |
| T17-20 | Process death during confirm dialog (S14): nothing written, selection discarded | Lifecycle | P1 | 1. Pro `E2`, tap utility → confirm dialog open<br>2. Kill process<br>3. Relaunch | Icons screen shows previous active alias selected; no partial write |
| T17-21 | Offline `N2`: nothing changes, no network needed | Environment | P2 | 1. Airplane mode<br>2. Open icons screen | Grid renders identically; all local operations work |
| T17-22 | Battery saver `X2`: no effect on this screen | Environment | P2 | 1. Battery saver on<br>2. Open icons screen | No visual/behavioral change; Surface row in Appearance shows override, but icons screen unaffected |
| T17-23 | Reduced motion `A5`: tile press = 120 ms scale, no ring draw-on | Accessibility | P1 | 1. Reduced motion on<br>2. Tap tile, observe selection animation | Scale animation 120 ms; iris ring appears instantly without draw-on |
| T17-24 | RTL `A6`: grid order mirrors, icon art never mirrors | Accessibility | P1 | 1. RTL locale<br>2. Open icons screen | Grid columns mirror (right-to-left); icon art unchanged; labels LTR in `mono` |
| T17-25 | Free user taps `Try Pro` on gate → opens `/pro` sheet over origin | Navigation | P1 | 1. Free `E1`<br>2. Tap utility tile → gate sheet → `Try Pro` | `/pro` opens as sheet over icons screen; origin held in memory; on purchase returns to icons with `E2` |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T17-26 | In-app UI never changes when alias active: header shows Vaultaire, not disguise | Security | P0 | 1. Pro `E2`, alias = Calculator<br>2. Navigate to `/vault`, `/settings`, `/settings/appearance`, About | App header/logo always "Vaultaire"; no Calculator icon/label anywhere in-app; recents shows disguise (expected) |
| T17-27 | Recents shows alias icon/label, never Vaultaire when disguised | Security | P0 | 1. Pro `E2`, alias = Calculator<br>2. Open recents | Recents task shows Calculator icon + "Calculator" label; no Vaultaire branding |
| T17-28 | Notification channels inherit alias label, body copy never says "Vaultaire" | Security | P1 | 1. Pro `E2`, alias = Calculator<br>2. Trigger backup/sync/share notification | Notification channel name = "Calculator"; body text neutral, no "Vaultaire" |
| T17-29 | Uninstall prompt shows alias label "Calculator", not "Vaultaire" | Security | P1 | 1. Pro `E2`, alias = Calculator<br>2. Long-press icon → Uninstall | System uninstall dialog reads "Calculator" |
| T17-30 | Long-press shortcuts suppressed or use alias label — no "Vaultaire" shortcut under Calculator icon | Security | P1 | 1. Pro `E2`, alias = Calculator<br>2. Long-press launcher icon | Shortcut menu either suppressed or shows "Calculator" actions; no "Open Vaultaire" |
| T17-31 | Confirmation dialog copy names new label exactly: "The app will now appear as Calculator." | Security | P0 | 1. Pro `E2`, tap Calculator → confirm dialog | Dialog body text exactly: "The app will now appear as Calculator." (never "the app icon", never "Vaultaire") |
| T17-32 | Splash under disguise: canvas only, no wordmark, no glyph | Security | P0 | 1. Pro `E2`, alias = Calculator<br>2. Cold launch | Splash shows canvas + orbit lines only; no "Vaultaire" wordmark; routes to `/lock` |
| T17-33 | Duress wipe does not revert active disguise alias | Security | P0 | 1. Pro `E2`, alias = Calculator<br>2. Enter duress pattern at `/lock` → decoy vault<br>3. Return to icons screen | Calculator remains selected; alias unchanged (duress wipes vaults, not launcher config) |
| T17-34 | No analytics: no icon-change event, no conversion metric, no telemetry | Security | P1 | 1. Pro `E2`, switch icons multiple times<br>2. Inspect encrypted prefs, logs | No icon-change events, no purchase tokens in logs, no analytics payloads |
| T17-35 | Encrypted prefs store only alias id, no vault data, no device IDs | Security | P1 | 1. Pro `E2`, alias = Calculator<br>2. Inspect encrypted prefs | Only alias identifier persisted; no vault count, labels, collection names, file names, pattern data |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T17-36 | TalkBack: tile announces "Calculator, not selected" / "Calculator, selected" | Accessibility | P1 | 1. TalkBack on<br>2. Swipe through grid | Each tile announces label + selection state; `Change icon` announces "Change icon, button"; dialog announces title + "The app will now appear as Calculator." |
| T17-37 | TalkBack price announcement not applicable (no prices here) — gate sheet announces "App icons are part of Pro" | Accessibility | P2 | 1. Free `E1`, TalkBack on<br>2. Tap utility tile | Gate sheet announces "App icons are part of Pro", `Try Pro` button, `Not now` button |
| T17-38 | Switch control: all tiles reachable, `Change icon` 48 dp min target | Accessibility | P1 | 1. Switch control on<br>2. Navigate grid | Grid traversable; all interactive elements ≥ 48 dp; dialog buttons ≥ 48 dp |
| T17-39 | High contrast: iris ring visible, check badge visible, no color-only info | Accessibility | P1 | 1. High contrast mode<br>2. Open icons screen | Selection ring 2 dp visible; check badge visible; disabled/gated states have text labels not color-only |
| T17-40 | Landscape `V2`: 4×2 grid, 12 dp gap maintained | Layout | P2 | 1. Rotate to landscape<br>2. Open icons screen | Grid 4 columns × 2 rows; 12 dp gap; Current block adapts |
| T17-41 | Foldable `V3`: grid never across hinge; tabletop puts grid in bottom half | Layout | P2 | 1. Foldable half-folded<br>2. Open icons screen | Grid confined to one display half; tabletop mode: grid in bottom half |
| T17-42 | Multi-window `V6` < 360 dp: single column, dialog full-width | Layout | P2 | 1. Multi-window narrow<br>2. Open icons screen → confirm dialog | Grid stacks to 1 column; dialog uses full width; `Change icon` 60 dp |
| T17-43 | Font scale `A4` 200%: labels wrap, button 60 dp, dialog copy wraps | Accessibility | P1 | 1. Font 200%<br>2. Open icons screen → confirm dialog | Already covered in T17-13; dialog primary action visible without scrolling |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T17-44 | Open task after alias change: running task keeps old alias in recents until recreated | Race | P1 | 1. Pro `E2`, alias = Calculator<br>2. App running in background<br>3. Switch to Units icon<br>4. Open recents | Recents shows Calculator (old alias) until task recreated; vault session unaffected; kill + reopen shows Units |
| T17-45 | G4 during S15 write: atomic temp+rename, last committed wins on relaunch | Lifecycle | P0 | 1. Pro `E2`, tap utility → `Change`<br>2. Kill process during `setComponentEnabledSetting`<br>3. Relaunch | On relaunch, screen reads enabled component; shows either old or new alias, never torn state |
| T17-46 | G4 during gate sheet: origin held in memory, relaunch resolves to `/lock` | Lifecycle | P1 | 1. Free `E1`, gate sheet open<br>2. Kill process<br>3. Relaunch | Cold start → `/` splash → `/lock`; no gate sheet restored |
| T17-47 | Language switch with dialog open: activity recreation closes dialog, unsaved text dropped after confirm | Race | P1 | 1. Pro `E2`, confirm dialog open<br>2. Change app language (API ≥ 33)<br>3. Activity recreates | Dialog dismissed; selection not confirmed; on return to icons, previous alias still selected |
| T17-48 | Rotation during S15 spinner: animation cancels, target state wins | Race | P1 | 1. Pro `E2`, `Change` tapped, spinner showing<br>2. Rotate device | Spinner cancels; on rotation complete, screen shows final state (S16 or S6) |
| T17-49 | Throttle not applicable here (no KDF) — verify no attempt counter on icon taps | Security | P2 | 1. Pro `E2`, rapidly tap tiles<br>2. Observe throttle store | No throttle tier/deadline written; throttle only on `/lock` and `/recovery/enter` |
| T17-50 | `E4` purchase resolves while on icons screen: inline note → `Check again` → `E2` → tiles operable | Race | P1 | 1. `E4` on icons screen<br>2. Purchase completes (Play callback)<br>3. Tap `Check again` | Note clears; tiles become interactive; entitlement `E2` |
| T17-51 | Background `G3` during S16: prefs already flushed, recents snapshot per "Hide previews" | Lifecycle | P1 | 1. Pro `E2`, just switched icon (S16)<br>2. Home<br>3. Open recents | Recents shows disguise icon; "Hide previews" respected (solid cover) |
| T17-52 | Predictive back from S14 confirm dialog: selection discarded, back to grid | Navigation | P1 | 1. Pro `E2`, confirm dialog open<br>2. Predictive back gesture | Dialog dismissed; grid shows previous active alias selected |
| T17-53 | Back from S16 (success): returns to grid with new selection, note + `Check again` | Navigation | P1 | 1. Pro `E2`, switch complete (S16)<br>2. Back | Returns to `/settings/appearance`; grid shows new selection |
| T17-54 | Deep link to `/settings/appearance/icons` with `E1`: renders full-screen S10 gate | Navigation | P1 | 1. Free `E1`<br>2. Deep link to icons screen | Full-screen gate (S10), same copy "App icons are part of Pro", `Try Pro`/`Not now` |
| T17-55 | Origin memory-only: stale origin id resolves silently to `/vault` | Navigation | P2 | 1. Pro `E2`, purchase via gate from icons<br>2. Kill process during `/pro`<br>3. Relaunch | `/lock` → unlock → `/vault` (origin unresolvable); no crash |

## Security invariants to assert

- [ ] In-app UI never changes under disguise (header, About, recents inside app all show "Vaultaire")
- [ ] No disguise icon in app header, About screen, or any in-app surface
- [ ] Confirmation dialog copy mentions the new label exactly: "The app will now appear as Calculator."
- [ ] Splash shows no Vaultaire branding under disguise (canvas only, no wordmark)
- [ ] Launcher alias label drives Recents task title, notification channels, uninstall prompt
- [ ] Long-press shortcuts suppressed or use alias label — no "Vaultaire" shortcut under disguise
- [ ] Already-changed disguise persists when Pro lapses (`E3`) and across Play updates
- [ ] No analytics events on icon preview, change, or launcher refresh
- [ ] Encrypted prefs store only alias id; no vault data, device IDs, purchase tokens
- [ ] Duress wipe does not revert launcher alias
- [ ] Notification bodies never contain "Vaultaire" when alias active

## Gaps & contradictions

- **Gap**: `screens.md §25` notification copy "Vaultaire backup needs attention" contradicts complete disguise — needs neutral string set for notification bodies while alias active (noted in flows/06 Gaps #1)
- **Gap**: Play Store policy review required for utility labels (Calculator/Printer/Network) — may read as impersonation; renamed label set may be needed (flows/06 Gaps #3)
- **Gap**: `app-icons/planning.md` AC-03 confirmation dialog message says "Vaultaire name stays unchanged" but flows/06 §1.2 and screens.md §17 mandate alias label = utility name ("Calculator") — planning.md is stale/superseded
- **Gap**: `app-icons/planning.md` mentions "Toast: Icon updated ✓" but flows/06 uses inline note "The launcher can take a few seconds" + `Check again` — toast not in spec
- **Gap**: `app-icons/planning.md` UX spec says 2-column grid portrait / 4-column landscape; flows/06 says 4×2 grid phone portrait, 6–8 columns tablet — planning.md is stale
- **Gap**: `app-icons/planning.md` mentions optional toggle "Enable alternative icons" — not in screens.md or flows/06
- **Contradiction**: `app-icons/planning.md` "Label Under Icon Rationale" decision says set to utility name, but iOS keeps "Vaultaire" — flows/06 note confirms Android uses utility name for Recents/notification channels; iOS difference documented
- **Gap**: `plausible-deniability/planning.md` forensic limits note examiner can estimate vault count from encrypted files — launcher disguise does not hide this; only hides from casual observation
- **Gap**: No test for "app update reconciliation on Amazon Appstore / non-Play devices" — planning.md edge case #4 says Pro still allows switching without billing dependency; no flow for entitlement verification without Play