# Widgets and Quick Settings — Test Cases
## Scope
screens.md §26 Widgets and Quick Settings (optional, later); flows/03-camera-and-notifications.md Overlay: Widgets & Quick Settings tile; flows/06-appearance-and-monetization.md §1.2 App icons (disguise alias behavior); plausible-deniability/planning.md (pattern-addressed routing, interface compartmentalization); ease-of-use/planning.md (auto-lock on background/home/screen-off).

## Preconditions
- No photo/media widget is permitted — any surface showing vault content, thumbnail, or count is a defect.
- Optional 1×1 home widget: neutral glyph only; label follows active app alias; tap launches → `/` → `/lock`.
- Optional Quick Settings tile: neutral "Vaultaire" glyph; tap launches → `/lock` only.
- No QSS tile or widget bypasses pattern lock; neither opens `/vault`, camera, or file directly.
- Disguise alias active → widget label and glyph must follow alias (e.g., "Calculator" glyph/label, not "Vaultaire").
- Widget/tile never binds to vault counts, storage, lock state, or last-capture thumbnail.
- Long-press on tile → system app info only; never reveals vault state.

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T26-01 | Widget/QSS tile render neutral glyph with default alias | Functional | P0 | 1. Add 1×1 widget and QSS tile with default alias. | Both show neutral glyph; widget label reads "Vaultaire". |
| T26-02 | Widget/QSS tap cold-launches to /lock | Functional | P0 | 1. Device locked, process dead. 2. Tap widget or QSS tile. | App launches; splash canvas only (no Vaultaire branding per §1); routes to `/lock`; "Welcome back / Draw your pattern". |
| T26-03 | Widget/QSS tap warm-launches to /lock | Functional | P0 | 1. App in background, locked (S9). 2. Tap widget or QSS tile. | App foregrounds directly to `/lock`; "Welcome back / Draw your pattern". |
| T26-04 | Widget/QSS label/glyph follow active disguise alias | Functional | P0 | 1. Enable Pro; switch to "Calculator" alias. 2. Observe widget label and QSS glyph. | Widget label reads "Calculator"; both glyphs follow Calculator alias art. |
| T26-05 | Disguised widget/QSS tap launches to /lock only | Functional | P0 | 1. Active alias "Printer". 2. Tap widget and QSS tile. | Both launch to `/lock`; pattern grid shown; no alias branding on lock screen. |
| T26-06 | Long-press widget/QSS opens system app info | Functional | P1 | 1. Long-press widget on home; long-press QSS in shade. | System app info opens; no vault state visible. |
| T26-07 | Widget/QSS absent when platform unsupported | Functional | P1 | 1. Use launcher without widget support; ROM without QSS. 2. Check pickers. | Widget/tile not listed (S12). |
| T26-08 | Widget/QSS show neutral glyph only — no vault count | Visual | P0 | 1. Add items to vault. 2. Observe widget and QSS tile. | Both unchanged; no item count, no "X items" badge. |
| T26-09 | Widget/QSS show no last-photo thumbnail | Visual | P0 | 1. Capture photo via camera; Keep. 2. Observe both. | Both show neutral glyph only; no thumbnail, no preview. |
| T26-10 | Widget idle (S3) and locked (S9) states identical | Visual | P0 | 1. App not running → observe widget. 2. App running but locked → observe. | Identical: neutral glyph + current alias label; widget cannot show lock state. |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T26-11 | Widget/QSS tap after process death (G4) cold-starts to /lock | Edge | P1 | 1. Kill app process. 2. Tap widget or QSS tile. | Cold start → splash canvas → `/lock`; "Welcome back / Draw your pattern". |
| T26-12 | Widget/QSS tap during Doze (X3) defers to lock screen | Edge | P2 | 1. Device in Doze, app idle. 2. Tap widget or tile. | Launch deferred; when Doze exits, app starts → `/lock`. |
| T26-13 | Widget/QSS tap with invalid pendingDestination ignored | Edge | P2 | 1. Set malicious pendingDestination via test hook. 2. Tap widget or tile. | Still launches only to `/lock`; pendingDestination ignored. |
| T26-14 | Widget/QSS label/glyph update when alias changed while visible | Edge | P1 | 1. Widget on home, tile in shade. 2. Switch alias to "Sound". 3. Return to home / reopen shade. | Both update to "Sound" within launcher/system refresh interval. |
| T26-15 | Widget/QSS tap with alias missing (corrupt component) shows neutral | Edge | P2 | 1. Corrupt alias component via test hook. 2. Tap widget or tile. | Launches to `/lock`; widget shows neutral glyph only (alias missing → neutral glyph only). |
| T26-16 | Rapid widget/QSS taps do not queue or crash | Edge | P2 | 1. Tap widget 5× rapidly; tap tile 5× rapidly. | Single launch to `/lock` each; no ANR, no duplicate tasks. |
| T26-17 | Widget on work profile shows work alias | Edge | P2 | 1. Install in work profile with "Calculator" alias. 2. Add widget to work home. | Widget shows "Calculator" label/glyph. |
| T26-18 | Widget persists across app update (Play update reconciliation) | Edge | P1 | 1. Add widget. 2. Update app via Play (alias reset to manifest default). 3. Observe widget. | Widget shows default "Vaultaire" label/glyph until `Application.onCreate()` reapplies stored alias. |
| T26-19 | Widget absent after uninstall/reinstall (prefs gone) | Edge | P1 | 1. Add widget. 2. Uninstall. 3. Reinstall. 4. Check picker. | Widget absent (S12) until re-added; new widget shows default alias. |
| T26-20 | Widget/QSS do not bind to storage-full or backup/sync status | Edge | P1 | 1. Fill storage; trigger backup failure. 2. Observe widget and tile. | Both unchanged; no "Storage full" or backup status indicators. |
| T26-21 | QSS tile disabled by OS shows no vault hint | Edge | P2 | 1. Force OS to disable tile (admin policy). 2. Observe shade. | Tile disabled/absent; no vault-related text. |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T26-22 | Widget/QSS never reveal vault existence via presence/absence | Security | P0 | 1. No vault. 2. Add widget & tile. 3. Create vault. 4. Observe both. | Both present and identical in both cases; no vault-existence signal. |
| T26-23 | Widget/QSS tap with unknown pattern → neutral empty vault | Security | P0 | 1. Tap widget or tile → `/lock`. 2. Draw pattern matching no vault. | Same 380ms glow; navigates to empty vault with "Nothing here yet." — no error, no shake, no red. |
| T26-24 | Widget/QSS tap with duress pattern → decoy vault identically | Security | P0 | 1. Configure duress. 2. Tap widget or tile → `/lock`. 3. Draw duress pattern. | Pixel-identical to Resolved: opens decoy vault; no confirmation, no delay, no badge. |
| T26-25 | Widget label / QSS glyph under disguise never read "Vaultaire" | Security | P0 | 1. Active alias "Calculator". 2. Observe widget label and QSS glyph. | Widget label reads "Calculator"; QSS shows Calculator glyph; never "Vaultaire". |
| T26-26 | Widget/QSS long-press never reveals vault path/name in app info | Security | P0 | 1. Long-press widget; long-press tile. 2. Inspect system app info. | No vault path, no vault name, no file count. |
| T26-27 | Widget/QSS do not leak lock state (S3 and S9 identical) | Security | P0 | 1. App unlocked. 2. Lock app. 3. Compare widget and tile appearance. | Identical visual; neither shows lock state. |
| T26-28 | Widget/QSS tap never bypasses pattern to camera or vault | Security | P0 | 1. Tap widget; tap tile. 2. Verify navigation. | Both land on `/lock` only; never `/vault/camera` or `/vault` directly. |
| T26-29 | Launcher shortcuts suppressed or alias-labeled under disguise | Security | P1 | 1. Active alias "Network". 2. Long-press app icon. | Shortcut menu uses "Network" label or suppressed; no "Vaultaire" shortcuts. |
| T26-30 | Recents shows alias icon/label, never vault content | Security | P1 | 1. Unlock vault. 2. Open recents. | Recents shows alias icon/label; content hidden by FLAG_SECURE (Hide previews). |
| T26-31 | Notification channel names follow alias under disguise | Security | P1 | 1. Active alias "Info". 2. Check notification channels. | Channels named "Info backup", "Info sync", etc.; never "Vaultaire". |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T26-32 | Widget/QSS glyph meet 48dp touch target | A11y | P1 | 1. Add widget and tile. 2. Verify tappable areas. | Launcher/QSS provide ≥48dp; glyphs centered. |
| T26-33 | Widget label scales with system font (200%) | A11y | P1 | 1. Set font 200%. 2. Add widget. | Label wraps to 2 lines; glyph unchanged; no truncation. |
| T26-34 | Widget/QSS work in RTL layout | A11y | P1 | 1. Set RTL locale. 2. Add widget and tile. | Glyphs centered; widget label RTL-aligned; no glyph mirroring. |
| T26-35 | Widget/QSS respect reduced motion (A5) | A11y | P2 | 1. Enable reduced motion. 2. Tap widget and tile. | Launch animations reduced; no iris ring draw-on. |
| T26-36 | Widget/QSS high-contrast glyph visible in dark/light theme | A11y | P1 | 1. Switch theme. 2. Observe both. | Neutral glyphs visible in both; no hardcoded colors. |
| T26-37 | Widget on tablet (600–839dp) uses same 1×1 spec | Adaptive | P2 | 1. Add widget on tablet. | 1×1 cell; neutral glyph + label; no multi-cell variant. |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T26-38 | Widget/QSS tap during auto-lock (screen off) → /lock | Race | P1 | 1. Screen off triggers auto-lock. 2. Tap widget or tile immediately. | Launches to `/lock`; no race to unlocked vault. |
| T26-39 | Widget/QSS tap while encryption/import in progress → /lock | Race | P1 | 1. Keep encrypting (S15) or import running. 2. Tap widget or tile. | Launches to `/lock`; background operation continues. |
| T26-40 | Widget/QSS added while app locked → show alias label/glyph | Lifecycle | P1 | 1. App locked. 2. Add widget via picker; add tile via QSS editor. | Both show current alias label/glyph. |
| T26-41 | Alias change during widget/tile tap in-flight → launch uses tap-time alias | Race | P2 | 1. Tap widget or tile. 2. Immediately switch alias in settings. | Launch uses alias at tap time; surfaces update post-launch. |
| T26-42 | Process death during widget/tile tap handled by cold start | Race | P1 | 1. Tap widget or tile. 2. Kill process before launch completes. | Cold start on next foreground → `/lock`. |
| T26-43 | Widget/QSS survive configuration change (rotation) | Lifecycle | P2 | 1. Add widget and tile. 2. Rotate device. | Both remain; labels/glyphs intact. |

## Security invariants to assert
- [ ] Widget never shows vault content, thumbnail, or item count.
- [ ] QSS tile never shows vault content, thumbnail, or item count.
- [ ] Widget tap never bypasses `/lock` — never opens `/vault`, `/vault/camera`, or a file directly.
- [ ] QSS tile tap never bypasses `/lock` — never opens `/vault`, `/vault/camera`, or a file directly.
- [ ] Widget label/glyph follows active disguise alias; never reads "Vaultaire" when alias active.
- [ ] QSS tile glyph follows active disguise alias; never shows "Vaultaire" glyph when alias active.
- [ ] Long-press on widget/tile never reveals vault state in system app info.
- [ ] Widget/tile never binds to lock state (S3 and S9 visually identical).
- [ ] Widget/tile never binds to vault counts, storage, backup/sync status, or last-capture thumbnail.
- [ ] Unknown pattern via widget/tile → neutral empty vault ("Nothing here yet.") — no error, no shake, no red.
- [ ] Duress pattern via widget/tile → pixel-identical to success; opens decoy vault.
- [ ] Lock-screen exposure: recents shows alias icon/label only; content hidden by FLAG_SECURE.
- [ ] Notification channel names follow alias under disguise; body copy never says "Vaultaire".
- [ ] Launcher shortcuts suppressed or alias-labeled under disguise; no "Vaultaire" shortcut.

## Gaps & contradictions
- **Gap 1 (confirmed in flows/03 §Gaps #6):** `screens.md §17` covers launcher icon disguise but not widget/tile label behavior. Spec states "widget label/glyph follow the alias" but §17 says "in-app UI never changes" — widget is a system surface, not in-app, so alias-following is correct but needs explicit confirmation in §26.
- **Gap 2:** `screens.md §26` states "Optional 1×1 'Vaultaire' launch tile" but flows/03 says label follows alias. Contradiction: §26 implies static "Vaultaire" label; flows/03 mandates alias-following. Flows/03 is more specific and security-critical — alias-following wins.
- **Gap 3:** QSS tile glyph described as "neutral 'Vaultaire' glyph" in flows/03 table but must follow alias per disguise consistency rule. Spec needs to clarify: default glyph is Vaultaire; under disguise, glyph follows alias art.
- **Gap 4:** No spec for widget/tile behavior when multiple vaults exist (different patterns). Since widget/tile only launch to `/lock`, pattern selection happens at lock screen — no gap, but worth noting.
- **Gap 5:** `screens.md §25` notification copy uses "Vaultaire backup needs attention" — contradicts disguise completeness (flows/06 §Gaps #1). Notification bodies must use neutral strings when alias active; widget/tile not affected but related.
- **Gap 6:** No spec for widget/tile glyph design tokens (size, color, shape) — references "neutral glyph" and "72dp squircle" for app icon tiles but not for 1×1 widget or QSS tile. Needs foundations.md linkage.
- **Gap 7:** `screens.md §26` says "never a last-photo thumbnail" but doesn't explicitly forbid "last capture" placeholder or timestamp. Flows/03 explicitly forbids "last capture thumbnail" — consistent but §26 could be stronger.
- **Gap 8:** No spec for widget/tile behavior on foldables/dual-screen — adaptive layout not addressed. §26 marked "optional, later" so deferred.
- **Gap 9:** No spec for widget/tile when app is in duress mode (decoy vault active). Since widget/tile only launch to `/lock`, and duress is a pattern, no special handling needed — but not documented.
- **Gap 10:** `flows/03` data contract says "Reads: active app alias only" but `Application.onCreate()` reconciliation on Play update (flows/06) re-applies stored alias. Race: widget may show stale alias until launcher refreshes. Acceptable but undocumented.