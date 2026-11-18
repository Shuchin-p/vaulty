# Search — Test Cases

## Scope
screens.md §7 Search + flows/02-vault-core.md §2.4 + secret-phrase planning.md (invariants: no secret/share phrase indexing) + plausible-deniability planning.md (invariants: no vault catalog, equal validity of empty vaults)

## Preconditions
- Local encrypted content index only; no network calls, no cloud suggestions
- Recent searches opt-in only, stored encrypted; cleared on lock
- 120 ms debounce on keystrokes; skeletons only if first query > 300 ms
- Filters: Photos / Videos / Docs / Dates (chips)
- Max query length enforced (token TBD); over-long query treated as no match → S4
- Spaces-only query trimmed to empty; shows recents/filters, issues no query
- Query discarded on Back/Cancel unless recents opted in
- Search never indexes secret phrases, share phrases, pattern data, or key material
- Process death drops in-memory query; recents (if opted in) survive encrypted

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T07-01 | Open search from vault home | functional | P0 | 1. Unlock vault<br>2. Tap search icon in top bar | Screen `/vault/search` opens; field auto-focused; hint "Search this vault"; filters visible; recents area present (empty if opt-in off) |
| T07-02 | Type query and get results | functional | P0 | 1. Open search<br>2. Type "garden"<br>3. Wait > 120 ms debounce | After debounce, results update; "Results · N" shown; paged tile grid renders matches |
| T07-03 | Filter by Photos chip | functional | P0 | 1. Open search<br>2. Type "vacation"<br>3. Tap `Photos` chip | Results narrow to photos only; chip highlighted; count updates |
| T07-04 | Filter by Videos chip | functional | P0 | 1. Open search<br>2. Type "birthday"<br>3. Tap `Videos` chip | Results narrow to videos only |
| T07-05 | Filter by Docs chip | functional | P0 | 1. Open search<br>2. Type "invoice"<br>3. Tap `Docs` chip | Results narrow to documents only |
| T07-06 | Filter by Dates chip | functional | P0 | 1. Open search<br>2. Type "2023"<br>3. Tap `Dates` chip | Results narrow to date range matches |
| T07-07 | Tap media result opens viewer | functional | P0 | 1. Search for existing item<br>2. Tap a photo result | Navigates to `/vault/file/{id}`; FLAG_SECURE set; media displays |
| T07-08 | Tap collection result opens collection | functional | P0 | 1. Search for collection name<br>2. Tap collection result | Navigates to `/vault/collection/{id}`; collection header renders |
| T07-09 | Clear field returns to idle with recents/filters | functional | P0 | 1. Type query with results<br>2. Tap clear (X) on field | Query cleared; recents (if opted in) + filters shown; "Results · N" gone |
| T07-10 | Clear filters from empty state | functional | P0 | 1. Search with filters yielding zero results<br>2. Tap `Clear filters` | Filters dropped; query retained; results update to unfiltered matches |
| T07-11 | Recent search tap re-runs query | functional | P1 | 1. Enable "Remember searches"<br>2. Search "cat"<br>3. Back to search<br>4. Tap "cat" recent chip | Query "cat" re-run; results appear; chip removable |
| T07-12 | Remove recent search | functional | P1 | 1. Have recent searches<br>2. Tap remove on one chip | That recent deleted; others remain; no query executed |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T07-13 | No matches shows exact copy | functional | P0 | 1. Search "xyznonexistent"<br>2. Wait for results | Empty state: "No matches in this vault." + `Clear filters` button; never "0 results found" |
| T07-14 | Spaces-only query treated as empty | functional | P0 | 1. Type "   " (spaces only)<br>2. Wait | Query trimmed to empty; recents/filters shown; no query issued; no skeleton |
| T07-15 | Very long query clamped to max length | boundary | P1 | 1. Type query exceeding max length (token TBD)<br>2. Observe field behavior | Field scrolls; query clamped; if no match → "No matches in this vault." |
| T07-16 | First search before index built shows Preparing | functional | P0 | 1. Restore vault, open search immediately<br>2. Observe before index ready | "Preparing search…" with real progress, cancellable; Back returns when ready |
| T07-17 | Index rebuilding shows incremental results | functional | P1 | 1. Trigger large sync merge<br>2. Open search during rebuild | Banner "Updating search index"; results appear incrementally; field stays usable |
| T07-18 | Decrypt failure on result shows Retry | error | P1 | 1. Corrupt an indexed item<br>2. Search for it | Danger row "Some results couldn't be read" + `Retry`; Retry re-runs query only |
| T07-19 | Debounce cancels in-flight query by generation | race | P1 | 1. Type "a"<br>2. Wait 50 ms<br>3. Type "ab"<br>4. Wait 50 ms<br>5. Type "abc" | Only final "abc" results render; no flash of "a" or "ab" results |
| T07-20 | Skeletons only after 300 ms on first keystroke | timing | P1 | 1. Open search<br>2. Type first character<br>3. Observe < 300 ms | No skeleton before 300 ms; static geometry only; after 300 ms → AuroraSkeleton |
| T07-21 | Subsequent keystrokes no skeleton | timing | P1 | 1. Type "cat" (results loaded)<br>2. Type "c"<br>3. Observe | No skeleton on subsequent keystrokes; results update in place |
| T07-22 | Back from search discards query | navigation | P0 | 1. Type query with results<br>2. Press Back | Returns to `/vault`; query discarded (not persisted unless recents opted in) |
| T07-23 | Cancel from search discards query | navigation | P0 | 1. Type query<br>2. Tap Cancel / Back arrow | Returns to `/vault`; query discarded |
| T07-24 | Auto-lock clears query from memory | lifecycle | P0 | 1. Type query<br>2. Wait for auto-lock threshold<br>3. Unlock and open search | Query cleared; field empty; recents/filters shown |
| T07-25 | Result deleted remotely removed from list | race | P1 | 1. Search showing item X<br>2. Delete X from another device<br>3. Sync merge arrives | Item X removed from list on next merge; no dead row |
| T07-26 | Open result deleted under you → viewer handles | race | P1 | 1. Search result Y<br>2. Tap Y → viewer opens<br>3. Y deleted remotely while viewing | Viewer shows deleted-under-you flow: media clears, snackbar "This item was removed." with Undo if reversible, then pop |
| T07-27 | Filter applied mid-page discards in-flight page | race | P1 | 1. Scroll search results (paged)<br>2. Tap different filter chip mid-load | In-flight page discarded via generation counter; new filter results render cleanly |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T07-28 | No network call on search | security | P0 | 1. Capture network traffic<br>2. Perform searches | Zero network requests; local index only |
| T07-29 | Query not persisted in plaintext | security | P0 | 1. Search "secret"<br>2. Check logs, SharedPreferences, WorkManager input | Query absent from all plaintext storage; only in memory |
| T07-30 | Recent searches stored encrypted when opted in | security | P0 | 1. Enable "Remember searches"<br>2. Search "private"<br>3. Inspect vault storage | Recent query stored encrypted; not readable without vault key |
| T07-31 | Recent searches cleared on lock | security | P0 | 1. Opt-in recents, search "test"<br>2. Auto-lock<br>3. Unlock and open search | Recent "test" not shown (cleared on lock) |
| T07-32 | Search never indexes recovery phrase | security | P0 | 1. Create vault with recovery phrase<br>2. Search for a word from the phrase | No results for phrase words; phrase never in index |
| T07-33 | Search never indexes share phrase | security | P0 | 1. Create share, get share phrase<br>2. Search for share phrase word | No results for share phrase words |
| T07-34 | Search never indexes pattern data | security | P0 | 1. Search for pattern-related terms<br>2. Verify no pattern data in index | Pattern data (hashes, envelopes, locators) never in search index |
| T07-35 | No vault label or count in search UI | security | P0 | 1. Open search<br>2. Inspect all visible text | Title is generic; no vault id, count, label, pattern mapping exposed |
| T07-36 | Decoy vault search indistinguishable | deniability | P0 | 1. Unlock decoy pattern (empty or sparse vault)<br>2. Open search | Identical UI to real vault; "No matches in this vault." copy same; no "decoy" indicator |
| T07-37 | Unknown pattern search identical to empty vault | deniability | P0 | 1. Draw unknown pattern → empty vault<br>2. Open search | Same as T07-36; per screens.md §24 "Unknown pattern: No error. Same glow, same transition, neutral empty vault" |
| T07-38 | Search query never in recents snapshot | security | P0 | 1. Type query in search<br>2. Press Home → check recents | Recents shows solid brand cover; no query text visible |
| T07-39 | Search query never in notifications | security | P0 | 1. Type query<br>2. Check notification channels | No notification contains search query |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T07-40 | Screen reader announces search field | a11y | P0 | 1. Open search with TalkBack<br>2. Focus field | Announces "Search this vault, edit box" |
| T07-41 | Screen reader announces results count | a11y | P0 | 1. Search with results<br>2. Focus results area | Announces "N results" |
| T07-42 | Screen reader announces no matches | a11y | P0 | 1. Search no matches<br>2. Focus empty state | Announces "No matches in this vault, button Clear filters" |
| T07-43 | Filter chips announced with state | a11y | P1 | 1. Open search with TalkBack<br>2. Navigate filter chips | Each chip announces "Photos, not selected, button" or "Photos, selected, button" |
| T07-44 | 200% font scale: field grows, chips scroll, results 2-up | adaptive | P1 | 1. Set font scale 200%<br>2. Open search | Field grows; chips horizontally scrollable; results grid stays 2-up |
| T07-45 | V1 narrow: single column results | adaptive | P1 | 1. Run on V1 width class (phone portrait)<br>2. Search with results | Results single column; filters scrollable |
| T07-46 | V4/V5: results pane beside rail, query retained | adaptive | P1 | 1. Run on V4/V5 (tablet/desktop)<br>2. Search with results | Two-pane: rail with filters/recents, results pane beside; query retained |
| T07-47 | Keyboard: Tab navigates field → filters → results | a11y | P1 | 1. Connect keyboard<br>2. Tab through search screen | Order: search field → filter chips → result tiles; Enter on result opens viewer |
| T07-48 | Arrow keys navigate result grid | a11y | P1 | 1. Focus result grid<br>2. Use arrow keys | Up/down/left/right moves focus between tiles; Enter opens |
| T07-49 | Escape closes search from field | a11y | P1 | 1. Focus search field<br>2. Press Escape | Returns to `/vault`; query discarded |
| T07-50 | Reduce motion: no skeleton animation | adaptive | P1 | 1. Enable Reduce motion<br>2. Type first query > 300 ms | Skeleton appears without pulse/shimmer animation |
| T07-51 | High contrast: filters and chips meet contrast | a11y | P1 | 1. Enable high contrast<br>2. Inspect filter chips | Selected/unselected states meet 4.5:1 contrast |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T07-52 | Process death drops query; recents survive encrypted | lifecycle | P0 | 1. Type query, enable recents<br>2. Kill process<br>3. Relaunch, unlock, open search | Query gone; recent chips present (encrypted); tapping re-runs query |
| T07-53 | Background then resume retains filter state | lifecycle | P1 | 1. Search, apply Photos filter<br>2. Home → background 10 s<br>3. Resume | Filter still active; results filtered; query retained |
| T07-54 | Auto-lock during search clears query | lifecycle | P0 | 1. Type query<br>2. Screen off → auto-lock<br>3. Unlock and open search | Query cleared; field empty |
| T07-55 | Configuration change preserves query and filters | lifecycle | P1 | 1. Search with query + filters<br>2. Rotate device | Query, filters, scroll position, results preserved |
| T07-56 | Multi-window resize retains state | lifecycle | P1 | 1. Search in split-screen<br>2. Resize pane | Query, filters, results reflow; no data loss |
| T07-57 | Rapid filter toggle during load handles gracefully | race | P1 | 1. Type query<br>2. Rapidly tap Photos → Videos → Docs → All<br>3. Wait | Final filter (All) results render; no crash, no stale filter UI |

## Security invariants to assert

- [ ] Zero network calls during any search operation
- [ ] Query never written to plaintext logs, SharedPreferences, WorkManager, backup metadata
- [ ] Query never in recents snapshot, notification, or crash report
- [ ] Recent searches opt-in only; stored encrypted in vault; cleared on lock
- [ ] Search index excludes: recovery phrases, share phrases, pattern data (envelopes, hashes, locators), key material
- [ ] No vault identifier, count, label, or pattern mapping in search UI or data
- [ ] Decoy/empty vault search UI identical to real vault (plausible deniability)
- [ ] FLAG_SECURE not required on search screen (Secure: no per screens.md §7), but query must not leak via recents

## Gaps & contradictions

- Max query length token not defined in foundations.md §8.1 (flows/02-vault-core.md §2.4 Gaps #2)
- Paging token (~60 items) and thumbnail cache budget not defined (same gap)
- "Remember searches" opt-in copy "Remember searches" inferred; exact string not in screens.md
- Recent search chip remove affordance not specified in screens.md §7 (only "individually removable" in flows)
- Search screen Secure: no but recents cover solid per "Hide previews"; conflict if previews off → screens.md §7 says Secure: no (capturable; recents follows "Hide previews") — acceptable
- No specification for "Still indexing…" line visual beyond S5 Partial state
- No specification for "Updating search index" banner beyond S6 Index rebuilding state
- Collection result tap target not detailed in screens.md §7 wireframe (only media tiles shown)
- Date filter chip behavior (range picker? preset ranges?) not specified