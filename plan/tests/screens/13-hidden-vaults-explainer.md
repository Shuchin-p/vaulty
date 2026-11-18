# Hidden Vaults Explainer — Test Cases
## Scope — screens.md §13 + flows/04-security-settings.md §4.5 + hidden-vaults/planning.md + plausible-deniability/planning.md
## Preconditions
- Content-only screen; reachable from Security settings via "Hidden vaults" row
- Honest-limits card (butter, non-dismissible) always visible
- No vault list, no vault count, no "manage" button, no rename action
- Session valid; app unlocked; auto-lock threshold not exceeded

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T13-01 | Open from Security settings | Functional | P0 | 1. Navigate to `/settings/security`<br>2. Tap "Hidden vaults" row | Screen pushes; title "Hidden vaults" announced; content renders |
| T13-02 | Scroll reads full explainer | Functional | P0 | 1. Open screen<br>2. Scroll to bottom | All body copy visible; no truncation; honesty card remains in view |
| T13-03 | Back navigation returns to Security | Functional | P0 | 1. Open screen<br>2. Press system back / toolbar back | Returns to `/settings/security`; no side effects |
| T13-04 | Close via gesture returns to Security | Functional | P1 | 1. Open screen<br>2. Swipe-from-edge back gesture | Returns to `/settings/security` |
| T13-05 | Content explains pattern = vault mapping | Content | P0 | 1. Open screen<br>2. Verify body text | Copy includes: "different patterns resolve different vaults" or equivalent |
| T13-06 | Content explains no catalog exists | Content | P0 | 1. Open screen<br>2. Verify body text | Copy states app does not show a vault list/catalog/picker |
| T13-07 | Honest-limits card present and non-dismissible | Security | P0 | 1. Open screen<br>2. Locate butter card<br>3. Attempt swipe/dismiss | Card visible; butter background; no dismiss affordance; copy exactly: "A forensic examiner with file access may still estimate how many vaults exist. Hidden vaults hide the catalog, not the storage." |
| T13-08 | No vault list rendered | Security | P0 | 1. Open screen<br>2. Inspect UI tree | Zero RecyclerView/FlatList/Column of vault items |
| T13-09 | No vault count displayed | Security | P0 | 1. Open screen<br>2. Search text for numbers + "vault" | No occurrence of vault count (e.g., "3 vaults", "1 vault") |
| T13-10 | No "manage" button or link | Security | P0 | 1. Open screen<br>2. Scan for clickable "Manage", "Edit", "Configure" | None present |
| T13-11 | No rename action or vault name field | Security | P0 | 1. Open screen<br>2. Scan for text input, "Rename", pencil icon | None present |
| T13-12 | Collection names mentioned as organization | Content | P1 | 1. Open screen<br>2. Verify body text | Copy references collection names for organization (per planning.md §4) |
| T13-13 | 200% font scale — text wraps, no truncation | Adaptive | P1 | 1. Set system font ≥ 200%<br>2. Open screen<br>3. Scroll | All text wraps; honesty card grows; no ellipsis; touch targets ≥ 48dp |
| T13-14 | Phone portrait — single column, floating nav | Adaptive | P0 | 1. Width < 600dp<br>2. Open screen | Single column layout; floating nav bar; content centered ≤ 600dp max |
| T13-15 | Tablet — centered 600dp max, rail on left | Adaptive | P1 | 1. Width ≥ 600dp<br>2. Open screen | Two-pane: left rail highlights Security; detail pane centered 600dp max |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T13-16 | Process death (G4) — returns to explainer | Lifecycle | P1 | 1. Open screen<br>2. Kill process<br>3. Relaunch app<br>4. Unlock | Reopens at `/settings/security/hidden-vaults` if session valid; scroll position restored |
| T13-17 | Auto-lock (S9) — brand cover shown | Lifecycle | P0 | 1. Open screen<br>2. Wait past auto-lock threshold / background app | Brand cover appears; "Vault locked" announced; unlock returns to explainer |
| T13-18 | Background > threshold — locked on return | Lifecycle | P0 | 1. Open screen<br>2. Home → wait > threshold<br>3. Return to app | Brand cover; unlock returns to explainer |
| T13-19 | Deep link opens directly | Functional | P2 | 1. Cold start via `vaultaire://settings/security/hidden-vaults` | Screen composes; content loads; honesty card visible |
| T13-20 | No network — content still renders (local-only) | Functional | P1 | 1. Airplane mode<br>2. Open screen | No spinner/banner; content identical to online |
| T13-21 | Rapid open/close — no duplicate pushes | Race | P2 | 1. Tap "Hidden vaults" row rapidly 3× | Only one instance pushed; back returns to Security once |
| T13-22 | Screen rotate — state preserved | Lifecycle | P1 | 1. Open screen<br>2. Rotate device | Content, scroll position, honesty card preserved |
| T13-23 | Theme change — colors update | Adaptive | P2 | 1. Open screen<br>2. Toggle system dark/light | Butter card background updates; text contrast maintained |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T13-24 | Honest-limits copy matches screens.md exactly | Security | P0 | 1. Open screen<br>2. Read butter card text | Exact match: "A forensic examiner with file access may still estimate how many vaults exist. Hidden vaults hide the catalog, not the storage." |
| T13-25 | No forensic estimates or numbers exposed | Security | P0 | 1. Open screen<br>2. Search for "estimate", "count", "number", "64", "cover" | No specific numbers, slot counts, or forensic methodology in user-facing text |
| T13-26 | No vault IDs, labels, or patterns exposed | Security | P0 | 1. Open screen<br>2. Inspect all text and content descriptions | Zero occurrences of vault IDs, labels, pattern coordinates, locator values |
| T13-27 | No "decoy", "duress", "plausible deniability" terms in user copy | Security | P1 | 1. Open screen<br>2. Full text search | Terms absent (per plausible-deniability planning: never uses "decoy" in title/body if user might be observed) |
| T13-28 | Explainer clarifies hidden ≠ invisible forensically | Security | P0 | 1. Open screen<br>2. Verify body copy | Copy distinguishes "casual inspection" protection from "filesystem access" limits |
| T13-29 | No analytics/event with vault identity fired | Security | P2 | 1. Open screen<br>2. Capture analytics events | No event containing vault count, IDs, or labels |
| T13-30 | Empty state not shown (not an empty-state screen) | Security | P1 | 1. Open screen with 0 vaults on device | Same content; honesty card still visible; no "no vaults" message |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T13-31 | TalkBack — title announced | Accessibility | P0 | 1. Enable TalkBack<br>2. Open screen | "Hidden vaults" announced |
| T13-32 | TalkBack — body content traversable | Accessibility | P1 | 1. Enable TalkBack<br>2. Swipe through content | All paragraphs reachable; honesty card announced with butter context |
| T13-33 | TalkBack — honesty card not skipped | Accessibility | P0 | 1. Enable TalkBack<br>2. Linear navigation | Honesty card included in reading order; not marked as decorative |
| T13-34 | Large text (A4) — no horizontal scroll | Adaptive | P1 | 1. Font scale 200%<br>2. Open screen | Content fits width; no horizontal scroll; chips/rows wrap vertically |
| T13-35 | Switch access / keyboard — focus order logical | Accessibility | P2 | 1. Connect switch/keyboard<br>2. Tab through screen | Focus moves top-to-bottom; back button reachable; no traps |
| T13-36 | Color contrast — butter card meets WCAG AA | Accessibility | P1 | 1. Inspect butter card text/background | Contrast ≥ 4.5:1 for body.m text on butter surface |
| T13-37 | RTL — layout mirrors correctly | Adaptive | P2 | 1. Set RTL locale<br>2. Open screen | Content mirrors; back icon on right; honesty card aligned |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T13-38 | Config change (locale) — content reloads | Lifecycle | P1 | 1. Open screen<br>2. Change system language | Strings update in place; scroll position kept; honesty card persists |
| T13-39 | Multi-window — drag to split screen | Adaptive | P2 | 1. Open screen<br>2. Enter split-screen mode | Layout adapts to available width; honesty card visible |
| T13-40 | Picture-in-picture not applicable | Adaptive | P2 | 1. Attempt PiP from screen | No PiP affordance; screen not PiP-eligible |
| T13-41 | Low memory — process killed, state restored | Lifecycle | P2 | 1. Open screen<br>2. Simulate LMK kill<br>3. Relaunch | Returns to explainer if session valid; no crash |
| T13-42 | Quick settings tile toggle — no effect | Race | P2 | 1. Open screen<br>2. Toggle Wi-Fi/BT from shade | Screen unchanged; no reload |
| T13-43 | System update / app update — stale instance handled | Lifecycle | P2 | 1. Open screen<br>2. Update app via Play<br>3. Return | If instance recreated, shows current version content; no stale copy |
| T13-44 | Multiple users / work profile — isolated | Security | P2 | 1. Open in work profile<br>2. Verify content | Content identical; no cross-profile data leakage |

## Security invariants to assert

- [ ] **No vault list** — zero vault items rendered in any list/grid/column
- [ ] **No vault count** — no numeric count of vaults appears in UI text
- [ ] **No "manage" button** — no action navigates to vault management
- [ ] **No rename action** — no text input, rename affordance, or vault name field
- [ ] **Honest-limits copy exact** — butter card text matches screens.md §13 verbatim: "A forensic examiner with file access may still estimate how many vaults exist. Hidden vaults hide the catalog, not the storage."
- [ ] **Non-dismissible honesty card** — no swipe, tap, or button removes the card
- [ ] **No dead-end back navigation** — back always returns to `/settings/security`
- [ ] **No vault IDs/labels/patterns** — zero occurrences in content, content descriptions, or analytics
- [ ] **No "decoy"/"duress" terminology** — user-facing copy avoids these terms
- [ ] **Forensic limits stated honestly** — distinguishes casual inspection from filesystem access

## Gaps & contradictions

- **Gap:** screens.md §13 states "No rename action. A user-editable vault name would recreate the plaintext catalog the feature removes; collection names already provide organization." — but no screen spec exists for collection management; unclear where users organize collections if not in hidden vaults context.
- **Contradiction:** flows/04-security-settings.md §4.5 Data Contract says "Must never appear: vault count, vault labels, vault IDs, forensic estimates" — yet plausible-deniability/planning.md §5 table explicitly lists "Examiner with filesystem access can see all encrypted index files" and "Examiner can still estimate 'active' vs 'decoy' based on storage file activity" as user-facing documentation targets. The explainer must convey this without showing estimates.
- **Gap:** hidden-vaults/planning.md §4.3 Empty State shows "Create New Vault" option for unused patterns, but screens.md §13 says "No rename action" and flows §4.5 says "No 'manage' button" — unclear if "Create New Vault" from empty unlock state contradicts the no-management principle.
- **Gap:** plausible-deniability/planning.md AC-5 requires "Padded Storage Pool — 64 slots padded with random cover files" — but screens.md §13 honesty card says "Hidden vaults hide the catalog, not the storage" without mentioning cover files. The explainer may need to mention cover files exist without revealing the 64-slot number.
- **Gap:** No specification for how the explainer handles the case where user has only 1 vault (no hidden vaults yet) — should it encourage multiple patterns or remain neutral?
- **Contradiction:** hidden-vaults/planning.md §3 AC-5 says "app clearly states that a forensic examiner with filesystem access can estimate the number of vaults that hold files" — but screens.md §13 honesty card says "estimate how many vaults exist" (not "that hold files"). Which phrasing is correct?