# Appearance — Test Cases
## Scope
Tests cover `screens.md §18` (Appearance screen rows and preview), `flows/06-appearance-and-monetization.md §1.1` (entry/exit/actions/states/env/transitions), `accessibility.md §3,6,7,9` (reduce motion/transparency/text size/RTL/focus/targets), `planning.md` (Solid framed as speed/contrast), and `plausible-deniability/planning.md` (vault title invariant).

## Preconditions
- Theme: Light / Night / Auto
- Surface: Glass / Solid / Auto
- Reduce motion: Off / On
- Reduce transparency: Off / On
- Hide previews in recents: Off / On
- Text size: 85% / 100% / 150% / 200%
- App language: device default / per-app locale (API ≥ 33) / system hand-off (API < 33)
- Vault label: **not present** (doc contradiction — see Gaps)
- Profile name: unset ("Not set") / set (local-only, never uploaded)

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T18-01 | Open Appearance from Settings | Happy | P0 | 1. Launch app, unlock<br>2. Navigate to `/settings`<br>3. Tap `Appearance` row | Route `/settings/appearance` pushes; title "Appearance"; preview card shows current theme×surface; rows in order: Theme, Surface, Reduce motion, Reduce transparency, Hide previews, Text size, App language, Profile name |
| T18-02 | Change Theme to Night | Happy | P0 | 1. Open `/settings/appearance`<br>2. Tap `Theme` row → sheet opens<br>3. Pick `Night` | 240 ms crossfade to Night; preview card re-renders Night×current surface; encrypted prefs written; sheet dismisses; row value shows "Night" |
| T18-03 | Change Theme to Auto | Happy | P1 | 1. Theme = Light<br>2. Tap `Theme` → pick `Auto` | Crossfade follows system night mode; preview updates; row value "Auto" |
| T18-04 | Change Surface to Solid | Happy | P0 | 1. Surface = Glass<br>2. Tap `Surface` → sheet opens<br>3. Pick `Solid` | Immediate apply; preview card solid (no blur); row value "Solid"; pref persisted |
| T18-05 | Change Surface to Auto | Happy | P1 | 1. Surface = Glass<br>2. Tap `Surface` → pick `Auto` | Immediate apply; preview follows battery saver / system; row value "Solid (set by battery saver)" if active else "Auto" |
| T18-06 | Toggle Reduce motion | Happy | P0 | 1. Reduce motion = Off<br>2. Toggle row switch → On | Immediate; all springs → 120 ms fades; ambient bloom off; preview card animation budget reduced; pref written |
| T18-07 | Toggle Reduce transparency | Happy | P1 | 1. Reduce transparency = Off<br>2. Toggle → On | Immediate; every glass fill promotes to solid; preview shows effective Solid; pref written |
| T18-08 | Toggle Hide previews in recents | Happy | P0 | 1. Hide previews = Off<br>2. Toggle → On | Immediate; recents snapshot solid brand cover; notification previews suppressed; pref written |
| T18-09 | Toggle Haptics | Happy | P1 | 1. Haptics = On<br>2. Toggle → Off | Immediate; app-wide haptics off except system; pref written |
| T18-10 | Open Text size → system settings | Happy | P0 | 1. Tap `Text size` row<br>2. System Display settings opens | App backgrounds; returns to `/settings/appearance` with same route; no app restart |
| T18-11 | Open App language (API ≥ 33) | Happy | P0 | 1. API 33+ device<br>2. Tap `App language` → sheet opens<br>3. Pick another language | Activity recreated; layout re-laid; session survives; row value updates to selected locale |
| T18-12 | Open App language (API < 33) | Happy | P1 | 1. API 31 device<br>2. Tap `App language` | Hand-off to system Language settings; note shown "Language is set for the whole device." |
| T18-13 | Set Profile name | Happy | P0 | 1. Profile name = "Not set"<br>2. Tap row → edit sheet opens<br>3. Enter "Alex"<br>4. Tap `Save` | Sheet closes; row shows "Alex"; 4 s snackbar "Saved"; greeting on `/vault` gains the name; encrypted prefs written |
| T18-14 | Clear Profile name | Happy | P1 | 1. Profile name = "Alex"<br>2. Tap row → edit sheet<br>3. Clear field → `Save` | Sheet closes; row shows "Not set"; snackbar "Saved"; greeting reverts to time-aware default without name |
| T18-15 | Tap App icon row | Happy | P0 | 1. Tap `App icon` row | Pushes `/settings/appearance/icons`; Appearance remains in back stack |
| T18-16 | Preview card reflects battery-saver override | Happy | P1 | 1. Surface = Glass<br>2. Enable battery saver | Row reads "Solid (set by battery saver)"; preview renders Solid; butter pill "Surface set to Solid by battery saver" appears once |
| T18-17 | Preview card honest on API < 31 | Happy | P1 | 1. API 30 device<br>2. Open `/settings/appearance` | Surface row disabled with inline "Blur needs Android 12 or newer."; preview renders Solid; row value "Solid" |
| T18-18 | Tablet two-pane layout (V4) | Happy | P2 | 1. 700 dp width<br>2. Open `/settings/appearance` | Rows left pane; preview pinned right; nav rail visible; all controls functional |
| T18-19 | 200% font scale (A4) | Happy | P2 | 1. System font 200%<br>2. Open `/settings/appearance` | Subtitles wrap to 2 lines (no truncation); switches keep 48 dp targets; preview shrinks but keeps every row |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T18-20 | Theme write fails | Edge | P0 | 1. Simulate encrypted-prefs write failure on theme pick | Inline banner "Couldn't save that change." + `Retry`; theme reverts to persisted value; other rows survive |
| T18-21 | Surface write fails | Edge | P1 | 1. Simulate write failure on surface pick | Banner + `Retry`; reverts to last good surface; preview reverts |
| T18-22 | Reduce motion write fails | Edge | P1 | 1. Toggle Reduce motion → write fails | Banner + `Retry`; toggle reverts; other rows unaffected |
| T18-23 | Text size hand-off fails (no activity) | Edge | P1 | 1. Tap `Text size` on device with no Display settings activity | Snackbar "Open system settings to change text size."; stays on screen |
| T18-24 | App language recreation drops unsaved name | Edge | P0 | 1. Open Profile name sheet → enter dirty text<br>2. Tap `App language` → pick locale → activity recreates | Confirmation shown; unsaved name dropped; sheet closed; old name persists |
| T18-25 | Profile name save write fails | Edge | P0 | 1. Enter valid name → `Save` → write fails | Button spinner stops; inline banner "Couldn't save that change."; field retains text; nothing persisted |
| T18-26 | Profile name save with blank-after-trim | Edge | P1 | 1. Edit sheet → enter only spaces → `Save` | `Save` remains disabled; no write attempted |
| T18-27 | Surface sheet open on API < 31 | Edge | P1 | 1. API 30<br>2. Tap `Surface` row | Sheet opens but Glass disabled with reason; preview shows Solid; Auto selects Solid |
| T18-28 | Battery saver toggles during Surface sheet open | Edge | P1 | 1. Open Surface sheet<br>2. Battery saver toggles On | Row and preview evaluated at sheet open; no disagreement; butter pill appears once per session |
| T18-29 | Rotation during 240 ms crossfade | Edge | P2 | 1. Change Theme → crossfade starts<br>2. Rotate device mid-fade | Animation cancels; target mode wins; no mid-transition hue committed |
| T18-30 | Theme change while any sheet open | Edge | P2 | 1. Open Profile name sheet<br>2. Change Theme via system broadcast / other trigger | Sheet re-themes without reopening; enter animation finishes first; no double crossfade |
| T18-31 | Low RAM disables Glass (S5) | Edge | P1 | 1. Device reports low RAM<br>2. Open `/settings/appearance` | Surface row shows value + inline reason "Blur reduced to save memory"; preview renders Solid; all toggles work |
| T18-32 | Back from App icons with unsaved appearance state | Edge | P1 | 1. Modify any toggle<br>2. Tap App icon → back from icons screen | All appearance prefs already flushed; no loss; returns to `/settings/appearance` with updated values |
| T18-33 | App language sheet dismissed without pick (API ≥ 33) | Edge | P2 | 1. Open App language sheet<br>2. Back / scrim tap | Sheet dismisses; locale unchanged; returns to `/settings/appearance` |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T18-34 | Vault label row absent (invariant) | Security | P0 | 1. Open `/settings/appearance`<br>2. Scan all rows | No "Vault label" row exists; title on `/vault` is always "Vault"; no user-editable vault name anywhere in Settings |
| T18-35 | Profile name never in recents/task title | Security | P0 | 1. Set Profile name = "SecretAlex"<br>2. Home → Recents | Recents shows solid brand cover (or blur per setting); task title never contains "SecretAlex"; app label shows current disguise icon name if alias active else "Vaultaire" |
| T18-36 | Profile name never in notifications | Security | P0 | 1. Set Profile name<br>2. Trigger backup/sync notification | Notification body never includes profile name; uses generic copy "Vaultaire backup needs attention." (see Gap #1) |
| T18-37 | Profile name never uploaded / synced | Security | P0 | 1. Set Profile name<br>2. Inspect encrypted prefs / network | Profile name stored only in local encrypted prefs; absent from backup payload, sync metadata, cloud keys |
| T18-38 | Solid mode framed as speed/contrast | Security | P1 | 1. Open Surface sheet<br>2. Inspect copy | Row / preview labels Solid as "Faster, higher contrast"; nowhere labelled "Reduced", "Basic", or "Accessibility mode" |
| T18-39 | App icon disguise never reflected in-app | Security | P0 | 1. Switch alias to "Calculator"<br>2. Open `/settings/appearance` | In-app UI shows "Vaultaire" branding; no disguise icon/label in header, About, or any surface; row still reads "App icon" |
| T18-40 | Duress/configuration survives Pro lapse | Security | P0 | 1. Pro expires (E3)<br>2. Open `/settings/appearance` | All rows remain interactive; no gate on this screen; previously changed app icon stays active per 07 §3.1 |
| T18-41 | Hide previews forces solid recents cover | Security | P0 | 1. Toggle Hide previews = On<br>2. Home → Recents | Recents snapshot is solid brand cover (never blurred content) regardless of Surface/Theme |
| T18-42 | Pattern grid identical across decoy/primary (indirect) | Security | P2 | 1. Unlock with decoy pattern → open Appearance<br>2. Unlock with primary pattern → open Appearance | Screen structure, rows, and copy are identical; no vault identity leaked via settings |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T18-43 | Reduce motion: theme crossfade 240→120 ms fade | Access | P0 | 1. Reduce motion = On<br>2. Change Theme | Crossfade is 120 ms fade only (no translate); preview updates after fade |
| T18-44 | Reduce motion: sheet slide 440→120 ms | Access | P1 | 1. Reduce motion = On<br>2. Open Profile name sheet | Sheet enters with 120 ms fade; no slide translate |
| T18-45 | Reduce motion: pattern success glow 380→120 ms | Access | P1 | 1. Reduce motion = On<br>2. Unlock vault (not on this screen but state persists) | Glow animation replaced by 120 ms color change (validated end-to-end) |
| T18-46 | Reduce motion: nav pill spring→120 ms crossfade | Access | P2 | 1. Reduce motion = On<br>2. Navigate between tabs | Nav pill morphs via 120 ms crossfade; no spring overshoot |
| T18-47 | Reduce transparency promotes all glass to solid | Access | P0 | 1. Reduce transparency = On<br>2. Open `/settings/appearance` (Glass theme) | Every glass surface renders solid; preview card solid; no blur calls in hierarchy |
| T18-48 | 200% font: no text clipping on preview card | Access | P0 | 1. Font 200%<br>2. Inspect preview card | All text wraps; no ellipsization; switches 48 dp min; preview shrinks proportionally |
| T18-49 | 150% font: nav labels hide, icons remain with content-desc | Access | P1 | 1. Font 150%<br>2. Inspect nav rail/bar | Labels hidden; icons retain content descriptions |
| T18-50 | RTL: rows and preview mirror; icons directional-mirror; numbers LTR | Access | P1 | 1. Set app language to Arabic/Hebrew<br>2. Open `/settings/appearance` | Row order mirrors; preview card mirrors; toggle switches mirror; numeric values (e.g., version) remain LTR |
| T18-51 | TalkBack: row focus order top-to-bottom | Access | P0 | 1. TalkBack on<br>2. Swipe through rows | Order: Theme → Surface → Reduce motion → Reduce transparency → Hide previews → Text size → App language → Profile name → App icon |
| T18-52 | TalkBack: preview card announced as decorative | Access | P2 | 1. TalkBack on<br>2. Focus preview card | Card announced as "Preview of current appearance settings" (or similar); not actionable |
| T18-53 | Focus target ≥ 48 dp all interactive rows | Access | P0 | 1. Layout Inspector → measure row tappable area | Each row + switch ≥ 48×48 dp; 8 dp spacing between adjacent targets |
| T18-54 | Keyboard: Tab order matches visual; Esc closes sheets; Enter activates | Access | P1 | 1. Connect keyboard<br>2. Tab through screen | Focus order matches visual; `Esc` dismisses edit/language/theme/surface sheets; `Enter` on row opens sheet |
| T18-55 | Pointer hover states on desktop/tablet | Access | P2 | 1. Mouse hover rows / switches | Visual hover state shown; no hover-only information |
| T18-56 | Grayscale / deuteranopia filters: all status has icon+word | Access | P1 | 1. Enable grayscale filter<br>2. Inspect toggles / badges | Every state (on/off, Pro, battery-saver) has both color and text/icon indicator |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T18-57 | Auto-lock (S9) during profile name edit | Race | P0 | 1. Open Profile name sheet → enter text<br>2. Wait for auto-lock threshold / screen off | Sheet dismissed with session; nothing written; old name stands on unlock |
| T18-58 | G4 process death during prefs write | Race | P0 | 1. Toggle any setting → write in flight<br>2. Kill process (G4)<br>3. Relaunch → unlock | Last committed value wins; no torn state; prefs atomic (temp + rename) |
| T18-59 | Battery saver toggle races with Surface sheet | Race | P1 | 1. Battery saver off, Surface = Glass<br>2. Open Surface sheet<br>3. Battery saver toggles On (system)<br>4. Sheet still open | Row and preview evaluated at sheet open; preview shows Glass; row unchanged until sheet closes; on reopen shows Solid + reason |
| T18-60 | Language switch with name sheet open | Race | P1 | 1. Open Profile name sheet (dirty)<br>2. Change App language → activity recreate | Confirmation shown; unsaved text dropped; name sheet closed; old name persists |
| T18-61 | Restore takeover (G5) completes on this screen | Race | P1 | 1. `/settings/appearance` open<br>2. Restore finishes in background | Settings are device-local; restored vault changes nothing on this screen; UI unaffected |
| T18-62 | Offline (N2): nothing changes on this screen | Race | P2 | 1. Airplane mode<br>2. Toggle any setting | All values local; every toggle applies immediately; no network dependency |
| T18-63 | Pro purchase completes while on this screen | Race | P2 | 1. Entitlement E1<br>2. Open `/settings/appearance`<br>3. Purchase Pro from another entry point | Entitlement re-queried on resume (S2); Pro badge on App icon row updates; no gate on this screen |
| T18-64 | Theme change + immediate rotation | Race | P2 | 1. Change Theme → crossfade starts<br>2. Rotate before fade ends | Configuration change handled; target mode applied; no visual artifact |

## Security invariants to assert

- [ ] Profile name never uploaded (encrypted prefs only; excluded from backup/sync/cloud)
- [ ] Vault label row does not exist; `/vault` title always "Vault" (no user-editable vault name)
- [ ] 240 ms crossfade never hard flash (no instant snap; always animated or 120 ms fade under reduce motion)
- [ ] Solid mode copy = "Faster, higher contrast" — never "Reduced", "Basic", "Accessibility mode"
- [ ] App icon disguise never appears in any in-app surface (header, About, Settings, preview)
- [ ] Recents/task title never contains profile name or vault identity; shows alias label if active
- [ ] Notification bodies never include profile name or vault label (uses generic copy)
- [ ] Duress configuration and active disguise icon survive Pro lapse (E3)
- [ ] Pattern grid and Appearance screen structurally identical across decoy/primary vaults

## Gaps & contradictions

- **Contradiction: Vault label row** — `screens.md §18` lists "Vault label" as a row; `flows/06-appearance-and-monetization.md §1.1` explicitly states "Aurora ships **no such row**" and "the invariant is that the title is always `Vault`". Implementation must drop the row to preserve plausible deniability.
- **Gap: Notification copy leaks identity** — `flows/06-appearance-and-monetization.md ##Gaps #1` and `accessibility.md §5.3` both flag that notification text "Vaultaire backup needs attention" betrays the app when disguised as "Calculator"/etc. Need neutral notification strings while alias active.
- **Gap: Reduce transparency system source** — `flows/06-appearance-and-monetization.md ##Gaps #6`: Android has no public system setting named "Reduce transparency" before API 35; toggle is app-only. Confirm or drop in favor of Surface = Solid.
- **Contradiction: App icon label in recents** — `app-icons/planning.md` legacy line "the label will read Vaultaire" superseded by canonical copy "The app will now appear as Calculator." — recents title must follow alias label (utility name), never "Vaultaire".
- **Gap: RTL pattern storage invariant** — `flows/06-appearance-and-monetization.md ##Gaps #2` and `accessibility.md §9`: pattern grid mirrors in RTL; no written guarantee that RTL locale switch cannot change what a stored pattern resolves to. Needs pixel/behaviour test.
- **Gap: Family sharing detection** — `flows/06-appearance-and-monetization.md ##Gaps #5`: Play family purchases surface through `queryPurchasesAsync`; UI "shared" line and organizer-cancel behavior during active session are unspecified.
- **Gap: Play Store policy on utility labels** — `flows/06-appearance-and-monetization.md ##Gaps #3`: Calculator/Printer/Network labels with look-alike art may read as impersonation; legal review required before ship.