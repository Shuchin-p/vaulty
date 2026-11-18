# Vault Home — Test Cases

## Scope
screens.md §4 (Vault Home) + 02-vault-core.md §2.1 (home flow) + 00-state-model.md (precedence, process death) + encrypted-cloud-backup\planning.md (backup banners) + security-architecture\planning.md (invariants)

## Preconditions
- Resolved session (pattern matched; G3/G4 threshold not exceeded)
- Vault exists (empty, <4 items, or ≥4 items)
- Offline (N2/N4/N5) — vault is local; only cloud banners age
- Backup off — no banner on home
- Locked transition: no content flash (G3/G4 → brand cover before S9)

---

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T04-01 | Open vault home after unlock | functional | P0 | 1. Draw valid pattern on `/lock`<br>2. Observe `/vault` | Screen shows greeting "Welcome back", title "Vault" (display.l), filter chips [All][Photos][Docs][Videos], FAB, floating nav; no vault ID/label/count |
| T04-02 | Empty vault shows neutral empty state | functional | P0 | 1. Unlock a vault with 0 items<br>2. Observe empty state | `AuroraEmptyState`: "Nothing here yet." + `Add photos` / `Take a photo`; **never** "This vault doesn't exist." |
| T04-03 | New vault (first open) shows same empty state | functional | P0 | 1. Complete onboarding<br>2. Tap "Open vault" | Identical empty state to T04-02; no "vault created" celebration |
| T04-04 | 1–3 items: hero becomes empty-state card | functional | P0 | 1. Add 1 photo<br>2. Observe home | Single hero card shows the item; below it the empty-state card with "Nothing here yet." + actions |
| T04-05 | 4+ items: hero shows collection cover + stats | functional | P0 | 1. Add 4 photos to a collection<br>2. Observe home | Hero card: cover art, title "Kyoto, 2024", stat row "24 Photos 10 Videos 1.8 GB", 44dp ink circle `(→)` |
| T04-06 | Recent media grid shows 2-up tiles | functional | P0 | 1. Have ≥2 recent items<br>2. Observe "Recent" section | Two media tiles side by side; `See all` navigates to paged grid |
| T04-07 | Filter chips re-filter locally | functional | P1 | 1. Tap `Photos` chip<br>2. Tap `Videos` chip<br>3. Tap `All` | Grid updates instantly (local query); chip state persists; `Clear filters` appears when filter yields zero |
| T04-08 | Search icon opens `/vault/search` focused | navigation | P0 | 1. Tap search icon in top bar | `/vault/search` pushes; field auto-focused; "Search this vault" announced |
| T04-09 | Overflow `more` opens `/settings` (no vault switcher) | navigation | P0 | 1. Tap overflow `more`<br>2. Tap Settings | `/settings` opens; **never** a vault switcher, vault count, or "which vault is this?" affordance |
| T04-10 | Hero `(→)` opens that collection | navigation | P0 | 1. Tap hero arrow `(→)` | `/vault/collection/{id}` opens for that collection |
| T04-11 | `See all` opens paged All grid | navigation | P0 | 1. Tap `See all` under Recent | Paged grid of all items (token ~60/page); scroll anchor by stable ID |
| T04-12 | Media tile tap opens viewer with FLAG_SECURE | navigation | P0 | 1. Tap a media tile | `/vault/file/{id}` opens; FLAG_SECURE set before first frame; recents shows brand cover |
| T04-13 | Media long-press enters selection with EFFECT_CLICK | functional | P0 | 1. Long-press a tile | `S13 Selection`: iris check + 2dp border, header "1 selected", floating ink action bar 12dp above nav |
| T04-14 | FAB tap opens camera (permission rationale if needed) | functional | P0 | 1. Tap FAB | If granted → `/vault/camera`; if P2/P3 → rationale sheet `S11`; never disabled |
| T04-15 | Pull-to-refresh re-reads local index & banners | functional | P1 | 1. Pull down past threshold<br>2. Release | Local re-scan; thumbnails re-derived; storage recomputed; backup/sync status re-queried; S2→S3 |
| T04-16 | Backup healthy banner (≤48h) shows mint, dismissible | visual | P1 | 1. Ensure backup succeeded <48h ago<br>2. Observe banner | Mint banner "Backed up · 2 hours ago" with dismiss `(×)`; tapping `Back up now` triggers manual backup |
| T04-17 | Backup stale banner (>48h) shows sand, dismissible | visual | P1 | 1. Ensure last backup >48h ago<br>2. Observe banner | Sand banner "Backed up · 3 days ago" with `Back up now`; dismissible |
| T04-18 | Offline: no "you're offline" banner; cloud banners age | functional | P0 | 1. Disable network<br>2. Observe home | No sky banner; existing backup/sync banners remain with timestamps aging in place |
| T04-19 | Backup off: no banner on home | functional | P0 | 1. Settings → Backup off<br>2. Return to home | No backup banner at all; backup UI only in settings |
| T04-20 | Greeting is time-aware, never shows name unless set | functional | P1 | 1. Unlock at 08:00 → "Good morning"<br>2. Unlock at 14:00 → "Good afternoon"<br>3. Unlock at 20:00 → "Good evening" | Greeting changes by time; user name appears only if set in Appearance → Profile (stored locally) |
| T04-21 | Nav destinations switch tabs preserving state | navigation | P1 | 1. Scroll home<br>2. Tap Timeline tab<br>3. Tap Home tab | Home scroll position restored; Timeline retains selected date |
| T04-22 | Manual lock from overflow → `/lock` with brand cover | navigation | P0 | 1. Overflow `more` → Lock | Key zeroized; recents cover drawn before content; `/lock` interactive |
| T04-23 | Screen off → immediate lock, no content flash | security | P0 | 1. Turn screen off while on `/vault`<br>2. Turn on | Brand cover shown; no vault content visible during wake |

---

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T04-24 | Unknown pattern resolves to neutral empty vault | security | P0 | 1. Draw pattern not matching any vault<br>2. Observe `/vault` | Identical to T04-02: "Nothing here yet." + actions; **no error, no shake, no red** |
| T04-25 | Filter to zero shows empty + `Clear filters` | functional | P1 | 1. Have photos only<br>2. Tap `Docs` chip | "Nothing here yet." + `Add photos` / `Take a photo` / `Clear filters`; tapping Clear restores All |
| T04-26 | Backup failed banner (priority 1) shows butter + Retry/Why | functional | P0 | 1. Simulate backup error<br>2. Observe home | Butter banner, danger icon: "Backup didn't finish. Your files are safe on this device." + `Retry` + `Why?` (not dismissible) |
| T04-27 | Storage low (D2) banner dismissible; full (D3) not | functional | P1 | 1. Fill storage to D2 threshold<br>2. Observe → D3 | D2: butter banner "This device is out of space. Free up space or connect a drive." + `Free up space` (dismissible)<br>D3: same banner, **not dismissible** |
| T04-28 | Sync paused banner (priority 3) shows butter + Open sync | functional | P1 | 1. Simulate sync lapse/wrong account<br>2. Observe home | Butter banner "Sync paused." + `Open sync`; dismissible until resolved |
| T04-29 | Banner priority: failed backup > storage > sync > stale | functional | P1 | 1. Trigger backup fail + storage low + sync paused<br>2. Observe | Only backup failed banner shows; others suppressed until higher clears |
| T04-30 | Partial thumbnails (S5): placeholders, grid usable | functional | P1 | 1. Corrupt 2 of 10 thumbnails<br>2. Observe grid | Placeholder glyphs on failed tiles; other tiles open/select normally; "Some items are still loading" announced |
| T04-31 | Index section unreadable (S6) shows inline danger + Retry | functional | P1 | 1. Corrupt index slice<br>2. Observe | Inline danger banner "Some items couldn't be read" + `Retry` (re-reads that slice only); content around survives |
| T04-32 | Decryption failure on tile open → viewer S6 | error | P1 | 1. Tap a corrupt media tile | Viewer shows danger banner "We couldn't open this item. It may be damaged." + `Retry`; neighbour items untouched |
| T04-33 | Storage full blocks import/camera with inline banner | functional | P1 | 1. Fill to D3<br>2. Tap FAB or `Add photos` | Banner "This device is out of space. Free up space or connect a drive."; camera/import gated |
| T04-34 | Deep link to deleted item lands silently on `/vault` | navigation | P1 | 1. Delete item on another device<br>2. Tap notification for that item | Opens `/vault` silently; no error, no dead row |
| T04-35 | Unsupported file in grid shows icon + `Open with…` | functional | P1 | 1. Import unsupported file type<br>2. Observe grid | Tile shows generic icon; tap → "This file can't be previewed here." + `Open with…` (if policy allows) |

---

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T04-36 | Vault title always generic "Vault" (display.l) | security | P0 | 1. Observe title in all states (empty, content, filter, selection) | Title **always** "Vault"; never vault ID, pattern, label, count, hash, or switcher |
| T04-37 | No vault count/label/switcher anywhere on screen | security | P0 | 1. Inspect top bar, hero, chips, banners, nav | Zero occurrences of vault count, label, or switcher affordance |
| T04-38 | No "vault created" celebration on first open | security | P0 | 1. Complete onboarding → Open vault | Empty state identical to T04-02; no confetti, toast, or "Your vault is ready" |
| T04-39 | Duress pattern opens decoy identically to normal unlock | security | P0 | 1. Draw duress pattern<br>2. Observe home | **Identical** to normal unlock: same greeting, same empty/content state, same timing; no special visual/haptic |
| T04-40 | Decoy vault shows plausible content, no "decoy" label | security | P0 | 1. Unlock via duress<br>2. Inspect content | Content decrypts normally; collection names are encrypted content (e.g., "Kyoto, 2024"); never "Fake" or "Decoy" |
| T04-41 | Real vaults silently wiped on duress; no error | security | P0 | 1. Have real + decoy vaults<br>2. Draw duress pattern<br>3. Re-unlock with real pattern | Real vault data gone; duress pattern now opens empty decoy; **no error, no "wiped" message** |
| T04-42 | Unknown pattern and empty vault render identically | security | P0 | 1. Compare screenshots: unknown pattern vs empty vault | Pixel-perfect match (accessibility.md §10.3 guardian) |
| T04-43 | No plaintext file names/counts in recents snapshot | security | P0 | 1. Open recents while on `/vault`<br>2. Inspect screenshot | Recents shows brand cover or blurred/solid per "Hide previews"; never media thumbnails or counts |
| T04-44 | Notification text never includes vault labels/counts | security | P1 | 1. Trigger backup notification<br>2. Inspect text | "Vaultaire backup needs attention." — never file names, vault labels, or counts |
| T04-45 | Clipboard normal on home (not blocked) | security | P1 | 1. Copy text on `/vault`<br>2. Paste elsewhere | Works normally; only blocked on secure screens (viewer, lock, onboarding) |
| T04-46 | Collection names allowed (encrypted content) | security | P1 | 1. Create collection "Kyoto, 2024"<br>2. Observe hero | Title renders; it is encrypted content, not vault identity |
| T04-47 | Backup banner shows only "Backed up · time ago" | security | P1 | 1. Observe backup banner | Copy exactly "Backed up · 2 hours ago" / "3 days ago"; never backup size, file count, or vault label |
| T04-48 | Sync conflict banner: "3 items need your review." | security | P1 | 1. Simulate sync conflict<br>2. Observe banner | Butter banner "3 items need your review." → review list; never exposes file names or share phrases |

---

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T04-49 | TalkBack announces screen as "Vault" on load | a11y | P0 | 1. Enable TalkBack<br>2. Unlock to home | "Vault" announced (not "Vault Home" or vault ID) |
| T04-50 | Empty state announced "Nothing here yet" | a11y | P0 | 1. TalkBack on empty vault | "Nothing here yet" announced; actions `Add photos`, `Take a photo` reachable |
| T04-51 | Filter chips announce selected state | a11y | P1 | 1. TalkBack on `Photos` chip | "Photos, selected" or "Photos, not selected"; double-tap toggles |
| T04-52 | Hero card stats announced with labels | a11y | P1 | 1. TalkBack on hero stat row | "24 photos, 10 videos, 1.8 gigabytes used" (mono.m values with labels) |
| T04-53 | Media tiles announce type + name | a11y | P1 | 1. TalkBack on media tile | "Photo, Kyoto temple" or "Video, Gion at dusk"; if "Hide previews" on → "Photo, 4.2 megabytes" |
| T04-54 | Banner actions reachable and announced | a11y | P1 | 1. TalkBack with backup failed banner | "Backup didn't finish. Your files are safe on this device. Retry button. Why button." |
| T04-55 | 200% font scale: header wraps, chips scroll, grid 2-up | a11y | P1 | 1. Set display size 200%<br>2. Observe home | Greeting/title wrap; filter chips horizontally scrollable; media grid stays 2-up; `Add photos` stays visible |
| T04-56 | Width <600dp: 1 column, floating nav | adaptive | P0 | 1. Run on phone portrait | Single column; hero spans full width; floating nav bar |
| T04-57 | Width 600–840dp: 2 columns, hero spans 2, nav rail | adaptive | P0 | 1. Run on tablet / foldable unfolded | Two-column grid; hero spans both; left nav rail replaces bottom bar; FAB docks in rail |
| T04-58 | Width >840dp: 3 columns, max 1040dp, nav rail | adaptive | P0 | 1. Run on desktop / large tablet | Three columns up to 1040dp center; nav rail; hero spans; FAB in rail |
| T04-59 | Foldable hinge: pattern grid never crosses fold | adaptive | P1 | 1. Fold device halfway<br>2. Observe grid | Grid stays on one pane; hinge-aware layout shifts content |
| T04-60 | Tabletop mode: grid bottom half, instructions top | adaptive | P1 | 1. Fold to 90° tabletop<br>2. Observe | Grid on bottom half; instructional text on top half |
| T04-61 | Mouse/trackpad: wheel zoom on tiles, arrows navigate | adaptive | P1 | 1. Connect mouse<br>2. Hover tile + wheel<br>3. Arrow keys | Wheel zooms tile preview; arrow keys move focus between tiles; Enter opens viewer |
| T04-62 | High contrast: all borders, chips, icons meet 4.5:1 | a11y | P1 | 1. Enable high contrast<br>2. Inspect | Filter chip borders, hero card edges, FAB, nav icons all ≥4.5:1 contrast |

---

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T04-63 | Auto-lock at threshold → brand cover, no content flash | lifecycle | P0 | 1. Background app<br>2. Wait > auto-lock threshold (default 30s)<br>3. Foreground | `/lock` shown; brand cover; key zeroized; no vault content visible |
| T04-64 | Screen off → immediate lock regardless of threshold | lifecycle | P0 | 1. On `/vault`, turn screen off<br>2. Turn on | Immediate `/lock`; no threshold delay |
| T04-65 | Process death (G4) → relaunch locked, pending destination vault | lifecycle | P0 | 1. On `/vault`, force-stop app<br>2. Relaunch | `/lock` with pending `{kind:vault}`; after unlock → `/vault` at restored scroll anchor |
| T04-66 | Process death drops selection, keeps scroll anchor | lifecycle | P0 | 1. Enter selection, select 3 items<br>2. Force-stop<br>3. Relaunch + unlock | Selection **dropped** (0 selected); scroll position restored by stable item ID |
| T04-67 | Warm resume (G2) < threshold → same state, no re-auth | lifecycle | P1 | 1. Background <30s<br>2. Foreground | `/vault` exactly as left; scroll, filter, banner states preserved |
| T04-68 | Tile deleted remotely fades out, gap closes | race | P1 | 1. Open home<br>2. Delete item from another device<br>3. Observe | Tile fades out with gap animation; no dead tile; mid-open item → viewer deleted-under-you path |
| T04-69 | Filter applied mid-page discards in-flight page | race | P1 | 1. Scroll to trigger page 2 load<br>2. Immediately tap `Videos` chip | Generation counter cancels page 2; Videos page 1 loads cleanly |
| T04-70 | Backup banner ages while offline, updates on reconnect | race | P1 | 1. Go offline<br>2. Wait 2h<br>3. Reconnect | Banner timestamp ages in place; on reconnect, if backup runs, banner updates to mint "just now" |
| T04-71 | Sync merge during lazy-load repaints loaded buckets | race | P1 | 1. Scroll timeline to trigger lazy-load<br>2. Trigger sync merge | Loading generation drops stale page; loaded buckets repaint with merged data |
| T04-72 | Duress wipe completes before decoy interactive | security | P0 | 1. Trigger duress (G6)<br>2. Measure time to decoy interactive | Wipe finishes before decoy becomes interactive; same budget as normal unlock (≤500ms pattern→vault) |
| T04-73 | No paywall/permission dialog over lock screen | security | P0 | 1. Be in Pro-lapse (E3) on `/vault`<br>2. Screen off → on | `/lock` shown cleanly; no `AuroraProGate` or rationale sheet over lock |
| T04-74 | No error styling on unlock (pattern validity leak) | security | P0 | 1. Draw wrong pattern<br>2. Observe lock screen | Same glow, same transition, neutral; **no shake, no red, no "wrong pattern"** |
| T04-75 | State injection reaches all S-codes for `/vault` | testability | P1 | 1. Open state inspector (long-press version)<br>2. Inject S0–S9, S13, S17 | Each state renders per 00-state-model §9 component map; golden screenshot captured per (screen, state) |

---

## Security invariants to assert

- [ ] Title is exactly "Vault" (display.l) — never vault ID, label, pattern, count, hash, or switcher
- [ ] No "vault created" celebration on first open
- [ ] No "This vault doesn't exist" copy anywhere
- [ ] Unknown pattern renders identically to empty vault (pixel match)
- [ ] Duress unlock indistinguishable from normal: same timing, same visuals, no special state
- [ ] Recents shows brand cover or blurred/solid per "Hide previews" — never media
- [ ] Notifications never contain file names, vault labels, or counts
- [ ] Backup banner shows only "Backed up · {time}" — never size, count, or vault label
- [ ] Sync/backup banners age in place while offline; no "offline" banner
- [ ] Clipboard normal on `/vault`; FLAG_SECURE only on viewer, lock, onboarding
- [ ] Collection names render (encrypted content) but never encode vault identity
- [ ] Process death drops selection; scroll anchor by stable ID only

---

## Gaps & contradictions

- **Gap**: 02-vault-core.md §2.1 states "Only one hero. If the vault has < 4 items, the hero becomes the empty-state card" but screens.md §4 shows hero card + empty-state card simultaneously for 1–3 items. Which is correct?
- **Gap**: screens.md §4 shows backup banner "Backed up · 2 hours ago" as mint banner; 02-vault-core.md banner table shows priority 4 "Backup stale/healthy" with sand/mint but no copy. Copy should be specified in one place.
- **Gap**: encrypted-cloud-backup\planning.md §UX shows backup indicator in "vault list" with cloud icons (gray/check/spin/exclaim) but screens.md §4 shows only a text banner on home. Which UI is implemented?
- **Gap**: 02-vault-core.md §2.1 banner table shows "Backup failed" as priority 1 with copy "Backup didn't finish. Your files are safe on this device." but screens.md §24 shows same copy. OK — consistent.
- **Gap**: screens.md §24 "Unknown pattern" row says "No error. Same glow, same transition, neutral empty vault" but 00-state-model.md §2.1 precedence puts G6 (duress) before lock. Unknown pattern is not duress — it's a normal unlock to an empty vault. Clarify: unknown pattern = S4 Empty on `/vault`, not a special state.
- **Contradiction**: screens.md §4 "Backup off: no banner on home; backup lives in settings" vs encrypted-cloud-backup\planning.md showing backup indicator in vault list. Home is not a vault list; but if multiple vaults existed, the list would need indicators. Product currently has single vault per pattern — list doesn't exist.
- **Gap**: 02-vault-core.md §2.1 "Scale" mentions 100k items → paged index cursors, scroll restored by stable item ID, but no token for page size, thumbnail cache budget, or query max length. 02-vault-core.md ## Gaps #2 confirms these are missing.
- **Gap**: 00-state-model.md §8 mandatory states for List class includes S8 (offline/stale) but 02-vault-core.md §2.1 says "Offline is not a home banner" and S8 shows "no visual change". S8 is still a state code but with no UI — clarify if S8 must render a component or is purely logical.
- **Gap**: screens.md §4 shows "Welcome back" as overline + "Vault" as display.l; 02-vault-core.md entry table shows no greeting copy. Greeting copy should be centralized.