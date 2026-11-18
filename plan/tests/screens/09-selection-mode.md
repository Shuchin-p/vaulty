# Selection Mode — Test Cases
## Scope
screens.md §9 "Selection Mode" + flows/02-vault-core.md §2.7 "Selection mode" + secure-sharing/planning.md Pro gating (AC-01.6, E2EE_SHARED_VAULTS)

## Preconditions
- App unlocked, on `/vault` (or `/vault/collection/{id}`) with ≥1 item
- Long-press any tile/row → enters selection with `EFFECT_CLICK`, first item selected, "1 selected" announced
- Multi-select via tap, long-press, or select-all (filtered set only)
- Share action at entitlement E1/E3 opens `AuroraProGate` (value sentence + `Try Pro` / `Not now`), never a disabled button
- Delete opens count-aware dialog: exact copy "Delete N items? This can't be undone." — scrim-dismiss disabled; `Esc`/`Back`/`Keep` cancel
- Back exits: pops confirm first, then selection, then nav; at root backgrounds app

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T09-01 | Enter selection via long-press on grid tile | functional | P0 | 1. On `/vault` with items visible<br>2. Long-press any tile | Selection mode enters with `EFFECT_CLICK`; tile shows iris check + 2 dp border; header shows "1 selected"; floating ink action bar appears 12 dp above nav with `surface.ink` background, 36 dp radius; "1 selected" announced |
| T09-02 | Enter selection via long-press on collection row | functional | P0 | 1. Open a collection (not read-only)<br>2. Long-press any row | Selection mode enters; row shows selection indicator; header "1 selected"; action bar visible; announced |
| T09-03 | Enter selection via selection circle tap | functional | P1 | 1. Long-press to enter selection<br>2. Tap 28 dp selection circle on another tile | That tile toggles selected; header count updates; announced "N selected" |
| T09-04 | Enter selection via keyboard Shift+Space (desktop) | functional | P1 | 1. On `/vault` with keyboard focus on a tile<br>2. Press Shift+Space | Selection mode enters with that tile selected; "1 selected" announced |
| T09-05 | Toggle selection by tapping tiles | functional | P0 | 1. In selection mode<br>2. Tap 3 different tiles sequentially | Each tap toggles selection; header count updates immediately; iris check + border appears/disappears per tile; announced count each change |
| T09-06 | Select all in current filter | functional | P0 | 1. Apply a filter (e.g., Videos)<br>2. Enter selection<br>3. Tap "select all" | Only filtered items selected; header shows filtered count (not entire vault); at 100k items count computed from cursor; announced |
| T09-07 | Long-press another tile while in selection adds/removes it | functional | P1 | 1. In selection with 2 items selected<br>2. Long-press a third tile | Third tile toggles; count updates to 3; announced "3 selected" |
| T09-08 | Deselect last item auto-exits selection | functional | P0 | 1. In selection with 1 item selected<br>2. Tap that tile to deselect | Selection exits automatically to normal `/vault` (S3); no empty selection bar; "Selection cleared" announced |
| T09-09 | Close button exits selection | functional | P0 | 1. In selection with N>0 items<br>2. Tap close (×) in header | Selection exits to normal `/vault`; count resets; nav bar returns; no snackbar |
| T09-10 | Back button exits selection | functional | P0 | 1. In selection with N>0 items<br>2. Press system back | Selection exits to normal `/vault`; count resets; nav bar returns |
| T09-11 | Delete owned items — confirm and commit | functional | P0 | 1. Select 3 owned items<br>2. Tap Delete<br>3. In dialog tap Delete | Dialog shows exact "Delete 3 items? This can't be undone."; scrim-dismiss disabled; commit → "Deleting" state; then snackbar "3 items moved to trash" with `Undo` 7 s; selection exits to S3 |
| T09-12 | Delete — tap Keep cancels and returns to selection | functional | P0 | 1. Select 2 items<br>2. Tap Delete<br>3. Tap Keep | Dialog dismisses; selection mode remains with 2 items still selected; "2 selected" announced |
| T09-13 | Delete — back key cancels dialog | functional | P0 | 1. Select 1 item<br>2. Tap Delete<br>3. Press system back | Dialog dismisses; selection mode remains with 1 item selected |
| T09-14 | Add to collection — opens picker, completes | functional | P0 | 1. Select 2 items<br>2. Tap Add to<br>3. Pick existing collection<br>4. Confirm | Items added to collection; inline confirmation "Saved" (S16); selection exits to S3 |
| T09-15 | Share at E2 (Pro) opens create-share pre-filled | functional | P0 | 1. Pro user, select 3 items<br>2. Tap Share | `/vault/share/create` opens with selected items pre-filled; selection preserved on return |
| T09-16 | Share at E1/E3 opens AuroraProGate, selection preserved on dismiss | functional | P0 | 1. Free user (E1) or trial expired (E3), select 2 items<br>2. Tap Share | `AuroraProGate` sheet opens with value sentence + `Try Pro` / `Not now`; not a disabled button; tap `Not now` → returns to `/vault` with 2 items still selected |
| T09-17 | Undo snackbar restores deleted items | functional | P0 | 1. Delete 3 items, confirm<br>2. Within 7 s tap Undo on snackbar | Items restored to vault; selection exits to S3; snackbar dismisses |
| T09-18 | Undo snackbar times out after 7 s | functional | P0 | 1. Delete 2 items, confirm<br>2. Wait 8 s without tapping Undo | Snackbar dismisses automatically; items remain deleted; selection exited to S3 |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T09-19 | Delete with read-only item in selection hides Delete and explains | functional | P0 | 1. Select 2 owned + 1 read-only (shared) item<br>2. Observe action bar | Delete action absent; helper text explains why (e.g., "Read-only items can't be deleted"); Share and Add to still visible per policy |
| T09-20 | Delete with all read-only items hides Delete | functional | P1 | 1. Select only read-only items<br>2. Observe action bar | Delete absent; copy explains why; Share/Add to may still apply |
| T09-21 | Select all at 100k filtered items shows count, no crash | functional | P1 | 1. Vault with 100k items, filter to large set<br>2. Enter selection, tap select all | Header shows filtered count (computed from cursor, not materialized); UI responsive; no ANR |
| T09-22 | Count updates live when items deleted remotely during selection | functional | P1 | 1. Select 5 items<br>2. Another device deletes 2 of them via sync<br>3. Observe header | Header count decrements to 3 immediately; Delete dialog re-reads live count so copy matches |
| T09-23 | Delete confirm copy matches live count exactly | functional | P0 | 1. Select 4 items<br>2. Tap Delete | Dialog shows exactly "Delete 4 items? This can't be undone." (no pluralization bugs) |
| T09-24 | Scrim tap on delete dialog does nothing | functional | P0 | 1. Open delete dialog<br>2. Tap outside dialog (scrim) | Dialog stays open; no dismissal (scrim-dismiss disabled for destructive dialog) |
| T09-25 | Escape key on delete dialog cancels | functional | P1 | 1. Open delete dialog (desktop)<br>2. Press Esc | Dialog cancels; returns to selection with items still selected |
| T09-26 | Auto-lock while in selection drops selection | functional | P0 | 1. Enter selection, select 3 items<br>2. Wait for auto-lock timeout (or trigger) | App locks; on unlock → `/lock` then `/vault` at restored scroll anchor with no selection; "Vault locked" announced |
| T09-27 | Process death (G4) does not restore selection | functional | P0 | 1. Enter selection, select 4 items<br>2. Kill process (swipe from recents)<br>3. Relaunch app | App opens locked; unlock → `/vault` at restored scroll anchor; no selection; header shows normal state |
| T09-28 | Back at root of `/vault` backgrounds app (not exit selection) | functional | P1 | 1. On `/vault` not in selection<br>2. Press back | App backgrounds (home); no crash; selection not involved |
| T09-29 | Back during delete confirm cancels confirm first | functional | P0 | 1. In selection, tap Delete<br>2. Press back | Delete dialog cancels; selection mode remains with items selected |
| T09-30 | Back during selection exits selection (no confirm open) | functional | P0 | 1. In selection, 3 items selected<br>2. Press back | Selection exits to normal `/vault`; count resets |
| T09-31 | Empty vault enters selection — long-press on empty state no-op | functional | P1 | 1. On empty vault ("Nothing here yet.")<br>2. Long-press empty area | No selection mode entered; no action bar; no crash |
| T09-32 | Selection circle 28 dp hit target accessibility | functional | P1 | 1. In selection<br>2. Tap selection circle on tile | Toggles selection; 44 dp minimum hit target respected (circle + padding) |
| T09-33 | Floating action bar 12 dp above nav, 36 dp radius, surface.ink | visual | P1 | 1. Enter selection<br>2. Inspect action bar | Bar uses `surface.ink` background; 36 dp corner radius; positioned 12 dp above navigation bar; 3 actions: Share, Add to, Delete |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T09-34 | No batch-delete without confirmation dialog | security | P0 | 1. Select 5 items<br>2. Attempt any direct delete without dialog | Impossible — Delete always opens count-aware dialog first; no silent batch delete |
| T09-35 | Delete dialog copy never exposes crypto terms, file paths, stack traces | security | P0 | 1. Open delete dialog for any count<br>2. Inspect copy | Copy: "Delete N items? This can't be undone." — plain, short, no blame, no "oops", no exclamation marks, no crypto terms |
| T09-36 | Share gate shows AuroraProGate, never disabled button | security | P0 | 1. Free user, select items, tap Share<br>2. Inspect UI | `AuroraProGate` sheet opens with value sentence + `Try Pro` / `Not now`; Share button never rendered as disabled |
| T09-37 | Selection state never persisted beyond process death | security | P0 | 1. Select items<br>2. Kill process<br>3. Relaunch, unlock | No selection restored; selection is memory-only per data contract |
| T09-38 | Selected file names/count never in logs, notifications, recents | security | P0 | 1. Select items<br>2. Check logcat, notification shade, recents screen | No selected file names, counts, or IDs appear in any system-visible surface |
| T09-39 | Read-only items in selection hide Delete, explain why | security | P0 | 1. Mix owned + read-only in selection<br>2. Verify Delete absent, explanation present | Delete absent; helper text explains (e.g., "Some items are read-only and can't be deleted") — no blame, plain tone |
| T09-40 | AuroraProGate preserves selection on dismiss | security | P1 | 1. Free user, select 3 items, tap Share<br>2. Tap `Not now` on paywall | Returns to `/vault` with 3 items still selected; "3 selected" announced |
| T09-41 | Auto-lock drops selection, no data leak | security | P0 | 1. Select items<br>2. Auto-lock triggers<br>3. Unlock | Selection gone; no selected item metadata exposed on lock screen or in logs |
| T09-42 | Back behaviour: pops confirm → selection → nav | security | P0 | 1. In selection, open delete dialog<br>2. Press back (cancel dialog)<br>3. Press back (exit selection)<br>4. Press back (nav) | Layered back handling: dialog → selection → app background; no skipping layers |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T09-43 | TalkBack announces "N selected" on every count change | accessibility | P0 | 1. Enable TalkBack<br>2. Enter selection, toggle items | Each selection change announces "2 selected", "3 selected", etc.; "Selection cleared" on exit |
| T09-44 | TalkBack announces delete dialog copy exactly | accessibility | P0 | 1. Enable TalkBack<br>2. Open delete dialog for 3 items | Announces "Delete 3 items? This can't be undone."; buttons "Delete" and "Keep" announced |
| T09-45 | TalkBack announces Undo snackbar with action | accessibility | P0 | 1. Enable TalkBack<br>2. Delete items, confirm | Snackbar announced: "3 items moved to trash, Undo button"; Undo action focusable and activatable |
| T09-46 | Selection circle meets 44 dp minimum touch target | accessibility | P0 | 1. Enter selection<br>2. Measure selection circle hit area | 28 dp circle + padding ≥ 44 dp; passes accessibility touch target check |
| T09-47 | Floating action bar actions have accessible labels | accessibility | P0 | 1. Enable TalkBack<br>2. Focus Share, Add to, Delete | Each announced with clear label: "Share", "Add to collection", "Delete" |
| T09-48 | Keyboard navigation: Tab moves between tiles, Space toggles | accessibility | P1 | 1. Desktop/keyboard mode<br>2. Tab to tile, press Space | Focus moves logically; Space toggles selection; Shift+Space enters selection |
| T09-49 | High contrast mode: iris check + border visible | accessibility | P1 | 1. Enable high contrast<br>2. Enter selection | Selection indicators (iris check, 2 dp border) visible with high contrast colors; action bar `surface.ink` adapts |
| T09-50 | Large font / display size: header count, action bar scale | accessibility | P1 | 1. Set display size to largest<br>2. Enter selection | Header "N selected" wraps/scales; action bar remains 12 dp above nav; 36 dp radius maintained; no overflow |
| T09-51 | Landscape / foldable: action bar above nav, grid reflows | adaptive | P1 | 1. Rotate to landscape or unfold<br>2. Enter selection | Grid reflows; action bar stays 12 dp above nav (or gesture area); select-all respects visible filtered set |
| T09-52 | Desktop windowed: multi-window selection independent per window | adaptive | P1 | 1. Open app in two windows<br>2. Enter selection in window A<br>3. Window B unaffected | Selection state per-window; window B shows normal grid; no cross-window leakage |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T09-53 | Remote delete of selected item during selection updates count | race | P0 | 1. Select 4 items<br>2. Another device deletes 1 via sync<br>3. Observe header | Header decrements to 3 immediately; Delete dialog re-reads live count |
| T09-54 | Collection deleted remotely while in selection shows snackbar, pops | race | P0 | 1. In collection, enter selection<br>2. Another device deletes collection<br>3. Observe | Butter banner "This collection was removed on another device." + `Back to vault`; selection dropped; pops to `/vault` |
| T09-55 | Item deleted remotely while selected — Delete dialog count adjusts | race | P1 | 1. Select 3 items<br>2. Remote delete 1<br>3. Open Delete dialog | Dialog shows "Delete 2 items? This can't be undone." (live count) |
| T09-56 | Filter change during selection updates select-all scope | race | P1 | 1. Select 2 items in "All"<br>2. Change filter to "Videos" (1 of 2 matches)<br>3. Tap select all | Only the 1 video in current filter selects; header shows 1; previously selected photo deselected |
| T09-57 | Process death during delete commit (S15) — no partial state restored | lifecycle | P0 | 1. Confirm delete 3 items<br>2. Kill process during "Deleting" (S15)<br>3. Relaunch, unlock | No selection restored; items either deleted or not (atomic); no "Deleting" UI restored |
| T09-58 | Process death during Undo snackbar (S17) — no snackbar restored | lifecycle | P0 | 1. Delete items, snackbar showing<br>2. Kill process<br>3. Relaunch, unlock | No snackbar restored; items remain deleted; no selection |
| T09-59 | Auto-lock during delete confirm cancels confirm, drops selection | lifecycle | P0 | 1. Open delete dialog<br>2. Auto-lock triggers before confirm<br>3. Unlock | Dialog gone; selection dropped; at `/vault` with no selection |
| T09-60 | Configuration change (rotation) preserves selection state | lifecycle | P0 | 1. Enter selection, select 3 items<br>2. Rotate device<br>3. Observe | Selection preserved: 3 items still selected; header "3 selected"; action bar intact; no reset |

## Security invariants to assert
- [ ] Never batch-delete without confirmation dialog (exact copy "Delete N items? This can't be undone.")
- [ ] Pro gate always shows `AuroraProGate` sheet (value sentence + `Try Pro` / `Not now`), never a disabled button
- [ ] Back exits layered: confirm → selection → nav; at root backgrounds app
- [ ] Scrim-dismiss disabled for delete confirmation dialog
- [ ] Selection state never persisted beyond process death (G4)
- [ ] Selected file names/counts never in logs, notifications, or recents
- [ ] Read-only items in selection hide Delete and explain why (plain tone)
- [ ] Auto-lock drops selection immediately
- [ ] Delete dialog copy re-reads live count at open time
- [ ] Undo snackbar 7 s default; encrypted trash opt-in not yet specified (gap)

## Gaps & contradictions
- Encrypted trash (7-day) setting changes delete semantics but has no row in `screens.md`/`settings` — default is immediate + 7 s Undo; test T09-17/T09-18 assume default
- "Add to" can target non-existent collection — inline create not specified; flow implies picker only
- Collection delete semantics: flow says "photos stay in your vault" but `screens.md §6` Delete row doesn't confirm — potential data-loss bug if contents deleted
- Import duplicate policy: per-file `Skip`/`Add anyway` here vs bulk in `screens.md §27.3` — scope unclear
- Paging/decoding tokens (page size ~60, thumbnail cache budget) not in `foundations.md §8.1` — behavior at 100k items assumes tokens exist
- Timeline scroll-back limit "3 months max" (`screens.md §5`) needs paging unit (month/week) and behavior for >10 year vaults
- `AuroraProGate` value sentence copy not specified in screens.md — test assumes feature-visible sheet per flows
- Read-only explanation copy for Delete not specified — test assumes plain "Some items are read-only and can't be deleted"