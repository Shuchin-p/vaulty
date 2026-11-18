# Collection — Test Cases
## Scope
screens.md §6 Collection (Vault Detail) + flows/02-vault-core.md §2.3 Collection + secure-sharing/planning.md (read-only shared vault) + hidden-vaults/planning.md (deniability invariants)

## Preconditions
- Empty collection: S4 Empty — "This collection is empty." + `Add items`
- Read-only shared collection: header shows 28 dp `Read-only` status pill; Rename/Add to/Delete/Share absent (not disabled); Export absent per policy
- Pro-locked share: at E1/E3, Share opens `AuroraProGate` (value sentence + `Try Pro` / `Not now`); never a disabled button
- Delete confirmation: danger dialog "Delete this collection? The photos stay in your vault."; no scrim dismiss

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T06-01 | Open collection from Home hero | functional | P0 | 1. Unlock vault<br>2. Tap hero card (collection) on `/vault` | Opens `/vault/collection/{id}`; header shows collection name; hero 220 dp; filter chips (All/Photos/Videos/Docs); rows 64 dp with icon, name, type, size |
| T06-02 | Open collection from `See all` | functional | P0 | 1. Unlock vault<br>2. Tap `See all` on a collection hero | Opens same collection; scroll position at top |
| T06-03 | Filter by type chips | functional | P0 | 1. Open collection with mixed media<br>2. Tap `Photos` chip<br>3. Tap `Videos` chip<br>4. Tap `All` | Each tap re-filters rows locally; count updates in `mono.m`; no network call; `Clear filters` appears when filtered to zero |
| T06-04 | Tap row opens viewer | functional | P0 | 1. Open collection<br>2. Tap first row | Opens `/vault/file/{id}` with FLAG_SECURE; back returns to collection at same scroll |
| T06-05 | Long-press enters selection | functional | P0 | 1. Open collection<br>2. Long-press any row | Enters selection (S13); iris check + 2 dp border; header shows `N selected`; floating ink bar with `Share` `Add to` `Delete` |
| T06-06 | Rename collection | functional | P0 | 1. Open owned collection<br>2. Tap `(⋯)` → `Rename`<br>3. Enter new name, save | Sheet with `AuroraTextField`; exact-name validation; header updates; name stored encrypted; announcement "Saved" |
| T06-07 | Add items to collection | functional | P0 | 1. Open owned collection<br>2. Tap `(⋯)` → `Add to`<br>3. Pick 3 items, confirm | Picker shows vault items; count bumps; inline confirmation "Saved" (S16) |
| T06-08 | Delete collection (owner) | functional | P0 | 1. Open owned collection<br>2. Tap `(⋯)` → `Delete`<br>3. Confirm dialog<br>4. Verify files remain in vault | Dialog: "Delete this collection? The photos stay in your vault."; collection removed; files kept; snackbar "Items moved to trash" with `Undo` 7 s |
| T06-09 | Pull-to-refresh re-reads local index | functional | P0 | 1. Open collection<br>2. Pull to refresh | Re-reads local index; recomputes count; no network; banners re-derived |
| T06-10 | Hero cover renders blurred media with 24% ink scrim | visual | P1 | 1. Open collection with ≥1 item<br>2. Inspect hero | Cover art = blurred user media; 24% ink scrim under title; 220 dp height |
| T06-11 | Count uses `mono.m`, no animation | visual | P1 | 1. Add/remove items<br>2. Observe count | Count updates instantly; `mono.m` style; no counter animation |
| T06-12 | More menu order: Rename, Add to, Export, Delete (last, danger) | functional | P1 | 1. Tap `(⋯)` on owned collection | Menu items in that order; Delete last with danger styling |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T06-13 | Empty collection shows correct copy + action | functional | P0 | 1. Open collection with 0 items | "This collection is empty." + `Add items` (single action); announcement "This collection is empty" |
| T06-14 | Filter to zero shows `Clear filters` | functional | P0 | 1. Open collection with items<br>2. Filter to type with 0 matches | `S4 Empty` with "This collection is empty." + `Clear filters`; tapping `Clear filters` restores all |
| T06-15 | Partial rows show placeholder glyphs, list usable | functional | P1 | 1. Open collection during sync/index rebuild<br>2. Some rows unresolved | Placeholder glyphs; other rows openable; announcement "Some items are still loading" |
| T06-16 | Count/index slice mismatch shows "Updating…" | functional | P1 | 1. Sync merge arrives mid-view | Header shows subtle `mono.m` "Updating…" instead of wrong number; never negative or "0 of 24" |
| T06-17 | Collection deleted on another device → banner + pop | functional | P0 | 1. Open collection<br>2. Delete it from another device | Butter banner "This collection was removed on another device." + `Back to vault`; pop to `/vault`; content stays until pop |
| T06-18 | Row menu on corrupted item → viewer handles S6 | functional | P1 | 1. Open collection with corrupt item<br>2. Tap row | Opens viewer; viewer shows S6 "We couldn't open this item. It may be damaged." + `Retry` |
| T06-19 | Rename with empty name → inline invalid | functional | P1 | 1. Rename collection<br>2. Clear name, attempt save | Sheet stays open; inline hint shown; no save |
| T06-20 | Rename exceeds limit → truncated | functional | P1 | 1. Rename with > limit chars | Name truncated to limit; save succeeds with truncated name |
| T06-21 | Add to: item deleted mid-move → skipped, counted | functional | P1 | 1. Start Add to, pick items<br>2. Delete one picked item from another device | Move completes; skipped item reported in S16 count |
| T06-22 | Export absent when policy denies (not disabled) | security | P0 | 1. Open collection with export policy off<br>2. Open `(⋯)` menu | Export item **absent**; no disabled button |
| T06-23 | Deep link to deleted collection → `/vault` silently | functional | P0 | 1. Delete collection<br>2. Tap notification deep link to it | Lands on `/vault` silently; no error toast |
| T06-24 | Offline (N2) — no banner, browse/rename work | functional | P1 | 1. Disable network<br>2. Open collection, rename | No offline banner; rename succeeds locally; export/share gated only |
| T06-25 | Auto-lock (S9) covers content, recovers to lock | functional | P0 | 1. Open collection<br>2. Wait past auto-lock threshold | Brand cover drawn; key zeroized; unlock returns to collection by opaqueId |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T06-26 | Read-only pill shown for shared collection | security | P0 | 1. Open `/shared/{id}` (shared collection)<br>2. Inspect header | 28 dp `Read-only` status pill visible; no other visual difference from owned |
| T06-27 | Read-only: Rename absent | security | P0 | 1. Open shared collection<br>2. Open `(⋯)` menu | Rename **absent** (not disabled) |
| T06-28 | Read-only: Add to absent | security | P0 | 1. Open shared collection<br>2. Open `(⋯)` menu | Add to **absent** |
| T06-29 | Read-only: Delete absent | security | P0 | 1. Open shared collection<br>2. Open `(⋯)` menu | Delete **absent** |
| T06-30 | Read-only: Share absent | security | P0 | 1. Open shared collection<br>2. Inspect top bar | Share button **absent** |
| T06-31 | Read-only: Export absent per policy | security | P0 | 1. Open shared collection with export denied<br>2. Open row `(⋯)` | Export **absent** |
| T06-32 | Read-only: rows open viewer under no-export policy | security | P0 | 1. Open shared collection<br>2. Tap a row | Viewer opens; FLAG_SECURE; export/share absent in viewer per policy |
| T06-33 | Collection name is encrypted content (never plaintext in logs/recents) | security | P0 | 1. Open collection<br>2. Check recents, logs, WorkManager input | No collection name in recents snapshot, logs, notifications, WorkManager, backup metadata |
| T06-34 | No vault identity leak (no vault id/count/label) | security | P0 | 1. Open collection<br>2. Inspect all UI and data contracts | Title is "Collection" (generic); no vault id, count, label, pattern mapping in UI or data |
| T06-35 | Delete confirm copy exact: "Delete this collection? The photos stay in your vault." | security | P0 | 1. Tap Delete on owned collection | Dialog shows exact copy; no scrim dismiss; `Esc`/back/`Keep` cancel |
| T06-36 | Share button Pro-gated: E1/E3 opens `AuroraProGate` | security | P0 | 1. Free tier user opens collection<br>2. Tap Share | `AuroraProGate` sheet (value sentence + `Try Pro` / `Not now`); never a disabled button; selection preserved on dismiss |
| T06-37 | Shared collection header shows only shared name + Read-only badge | security | P0 | 1. Open `/shared/{id}` | No owner name, no vault label; only shared collection name (encrypted content) + `Read-only` badge |
| T06-38 | Revoked mid-session: content blurs, full-screen notice | security | P0 | 1. Open shared collection<br>2. Owner revokes share<br>3. Recipient syncs | Content blurs immediately; solid full-screen notice; no cached thumbnails remain |
| T06-39 | Forensic: collection names not in storage metadata | security | P1 | 1. Inspect encrypted index files<br>2. Verify no plaintext collection names | Collection names encrypted in index; only opaque ids at rest |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T06-40 | 200% font: header wraps, count one line, row actions 48 dp | accessibility | P1 | 1. Set font scale 200%<br>2. Open collection | Header text wraps; count stays one line; row touch targets ≥ 48 dp |
| T06-41 | V1 (compact): 2-up grid layout | adaptive | P1 | 1. Run on phone width < 600 dp<br>2. Open collection | 2-up grid; floating nav bar |
| T06-42 | V4 (expanded): 3-up grid + nav rail, 720 dp cap | adaptive | P1 | 1. Run on tablet/desktop ≥ 840 dp<br>2. Open collection | 3-up grid; nav rail replaces nav bar; hero spans columns; 720 dp content cap |
| T06-43 | A4: hero stats 2-up, chip labels hide, `Add photos` visible | adaptive | P1 | 1. Set font scale large + landscape<br>2. Open collection | Hero shows stats 2-up; filter chip labels hidden; `Add photos` remains visible |
| T06-44 | TalkBack: collection name announced on open | accessibility | P0 | 1. Enable TalkBack<br>2. Open collection | Announces "Collection" (screen name) |
| T06-45 | TalkBack: empty state announced | accessibility | P0 | 1. Enable TalkBack<br>2. Open empty collection | Announces "This collection is empty" |
| T06-46 | TalkBack: Read-only pill announced | accessibility | P0 | 1. Enable TalkBack<br>2. Open shared collection | Announces "Read-only" status |
| T06-47 | Keyboard: Tab navigates filter chips, rows, menu | accessibility | P1 | 1. Connect keyboard<br>2. Tab through collection | Focus order: back → filter chips → hero → rows → `(⋯)` menu |
| T06-48 | Mouse: hover shows row highlight, right-click → row menu | accessibility | P1 | 1. Connect mouse<br>2. Hover rows, right-click | Row highlight on hover; right-click opens row menu |
| T06-49 | Reduce motion: no count animation, crossfade only | accessibility | P1 | 1. Enable Reduce motion<br>2. Add/remove items | Count updates without animation; hero crossfade 240 ms |
| T06-50 | Reduce transparency: glass surfaces solid | accessibility | P1 | 1. Enable Reduce transparency<br>2. Open collection | Hero scrim, action bar, menus use solid surfaces |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T06-51 | Collection edited on another device mid-view → merged rows | race | P1 | 1. Open collection<br>2. Add/remove items from another device | Merged rows appear in place; no blank flash; count reconciles |
| T06-52 | Rename race: two devices rename → last-writer wins | race | P1 | 1. Rename on device A<br>2. Simultaneously rename on device B | Last-writer wins (add-wins merge); losing name not shown as error |
| T06-53 | Process death (G4) → reopens locked, then collection by opaqueId | lifecycle | P0 | 1. Open collection<br>2. Kill process<br>3. Relaunch | Reopens at `/lock`; after unlock, opens collection by opaqueId; if unresolvable → `/vault` silently; selection dropped |
| T06-54 | Background → auto-lock → resume restores collection | lifecycle | P1 | 1. Open collection<br>2. Background past threshold<br>3. Foreground | Brand cover; unlock returns to collection |
| T06-55 | Scale: 1 item → hero + one row | scale | P1 | 1. Create collection with 1 item<br>2. Open | Hero 220 dp + single 64 dp row |
| T06-56 | Scale: 12 items → single page, no paging | scale | P1 | 1. Create collection with 12 items | All 12 rows rendered; no paging token |
| T06-57 | Scale: 1,000 items → paged lazy rows, stable-id scroll anchor | scale | P1 | 1. Create collection with 1,000 items<br>2. Scroll, kill process, relaunch | Paged lazy rows; scroll restored by stable item id; thumbnails evicted only |
| T06-58 | Selection dropped on process death (G4) | lifecycle | P0 | 1. Enter selection in collection<br>2. Kill process<br>3. Relaunch, unlock | No selection restored; lands at `/vault` with restored scroll anchor |

## Security invariants to assert
- [ ] Read-only pill (28 dp) present on shared collections; edit/delete/share/rename **absent**, not disabled
- [ ] Delete confirm: "Delete this collection? The photos stay in your vault." — files kept, collection removed
- [ ] Collection names are encrypted content; never in logs, recents, notifications, WorkManager, backup metadata
- [ ] No vault identity leak: generic "Collection" title; no vault id/count/label/pattern anywhere
- [ ] Export/Share absent (not disabled) when policy denies
- [ ] Pro gate: Share opens `AuroraProGate` at E1/E3; never a disabled button
- [ ] FLAG_SECURE on viewer; recents shows brand cover
- [ ] Forensic: index files contain no plaintext collection names

## Gaps & contradictions
- **Collection delete semantics contradiction**: flows/02-vault-core.md §2.3 says "The photos stay in your vault." but screens.md §6 Delete action does not state whether contents are kept — risk of data-loss bug if implementation deletes files
- **Export policy**: screens.md §6 says "Export (if allowed)" but doesn't specify where policy is defined; secure-sharing/planning.md AC-01.5/AC-02.4 cover owner-set policy, but collection-level vs share-level policy boundary unclear
- **Shared collection entry**: screens.md §16.4 `/shared/{id}` shows shared vault with collections, but flows/02-vault-core.md §2.3 entry points only list `/vault/collection/{id}` — no explicit `/shared/collection/{id}` route documented
- **Hero cover source**: screens.md §6 says "blurred user media" but doesn't specify which item (first? newest? largest?) — implementation gap
- **Add to collection creation**: flows/02-vault-core.md Gap #5: "Add to can target a non-existent collection" — creating inline from Add to has no defined screen
- **Encrypted trash setting**: flows/02-vault-core.md Gap #1 — opt-in 7-day encrypted trash changes delete semantics but has no Settings row or storage cap defined
- **Paging/decoding tokens**: flows/02-vault-core.md Gap #2 — page size (~60), thumbnail cache budget, query max length have no foundation tokens
- **Import duplicate policy scope**: flows/02-vault-core.md Gap #4 — per-file vs bulk Skip/Add anyway not confirmed between flows and screens.md §27.3