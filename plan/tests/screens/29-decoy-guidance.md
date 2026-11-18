# Decoy Guidance — Test Cases
## Scope
§29 "Decoy believability guidance (Pro)" in screens.md; flow §4.4 `/settings/security/duress/guidance` in 04-security-settings.md; invariants from plausible-deniability/planning.md (AC-2, AC-4, US-6, forensic limits) and duress-mode/planning.md (silent trigger, no vault list).

## Preconditions
- Pro entitlement active (E2)
- Duress setup screen `/settings/security/duress` reached via Security settings → Duress row (Pro-gated)
- User taps "Guidance" link from Duress setup
- Screen is **never** reachable from home, vault, or any non-duress entry point
- FLAG_SECURE=on, clipboard=off, recents=brand cover, solid panel=on
- Session valid (not auto-locked)

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T29-01 | Open guidance from duress setup | functional | P0 | 1. Enable Pro<br>2. Open Settings → Security → Duress (E2)<br>3. Tap "Guidance" link | Navigates to `/settings/security/duress/guidance`; title reads "Making it look lived-in." |
| T29-02 | Title uses correct copy | visual | P0 | 1. Open guidance screen | Toolbar/title shows exactly "Making it look lived-in." — no "decoy" anywhere |
| T29-03 | Body uses "this vault" not "decoy" | visual | P0 | 1. Open guidance screen<br>2. Read all body text | Zero occurrences of "decoy" in body; references use "this vault" or "your vault" |
| T29-04 | Tips list renders completely | functional | P0 | 1. Open guidance screen<br>2. Scroll to tips | All five tips present: "add ordinary photos", "keep a realistic amount", "avoid an empty vault", "don't name collections 'fake'", "use it occasionally" |
| T29-05 | Honesty card visible and non-dismissible | functional | P0 | 1. Open guidance screen<br>2. Attempt to swipe/dismiss card | Card remains; butter background; copy exactly: "If someone knows your normal vault exists, a perfect decoy may still not convince them." |
| T29-06 | Honesty card uses body.m typography | visual | P1 | 1. Open guidance screen<br>2. Inspect honesty card text style | Text rendered in `body.m` token; butter surface token |
| T29-07 | Back navigation returns to duress setup | navigation | P0 | 1. Open guidance<br>2. Press system back / toolbar back | Returns to `/settings/security/duress`; no data loss |
| T29-08 | Close affordance (if present) returns to duress setup | navigation | P1 | 1. Open guidance<br>2. Tap close icon (if any) | Returns to `/settings/security/duress` |
| T29-09 | Scroll works for long content | functional | P1 | 1. Open guidance<br>2. Scroll to bottom | All content accessible; no clipping; honesty card stays visible at bottom |
| T29-10 | Announcement on load | accessibility | P1 | 1. Open guidance with TalkBack | Announces "Decoy guidance" (per flow S0 Initial) |
| T29-11 | No checklist or score UI present | security | P0 | 1. Open guidance<br>2. Inspect entire scrollable area | Zero checkboxes, progress bars, score counters, completion indicators |
| T29-12 | Pro badge not shown on guidance screen itself | visual | P2 | 1. Open guidance | No `[PRO]` badge on title or body (Pro gate is at Duress entry) |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T29-13 | Auto-lock during guidance → brand cover | lifecycle | P0 | 1. Open guidance<br>2. Wait for auto-lock threshold (30s default)<br>3. Unlock with pattern | Screen covered by brand cover; unlock returns to guidance (session valid) |
| T29-14 | Background then foreground → guidance restored | lifecycle | P0 | 1. Open guidance<br>2. Home → reopen app<br>3. Unlock if needed | Guidance screen restored; scroll position retained |
| T29-15 | Process death (G4) → guidance restored | lifecycle | P0 | 1. Open guidance<br>2. Kill process (adb shell am kill)<br>3. Relaunch → unlock | Guidance screen restored (per flow edge case) |
| T29-16 | Rotate device → layout adapts | adaptive | P1 | 1. Open guidance in portrait<br>2. Rotate to landscape | V1/V4 rules apply: phone stays single column; tablet ≥600dp centered 600dp max with rail |
| T29-17 | Foldable hinge → content not split | adaptive | P1 | 1. Open guidance on foldable<br>2. Fold to tabletop | Content centered in one pane; not across hinge |
| T29-18 | 200% font scale → text wraps, card grows | accessibility | P0 | 1. Set system font ≥200%<br>2. Open guidance | Text wraps, honesty card grows and stays visible; no truncation; touch targets ≥48dp |
| T29-19 | 200% font + landscape → no horizontal scroll | accessibility | P1 | 1. Set font ≥200%<br>2. Rotate to landscape | Content fits width; no horizontal scroll |
| T29-20 | Screen reader navigates all tips | accessibility | P1 | 1. Enable TalkBack<br>2. Swipe through guidance | All tips and honesty card reachable; honesty card announced with full copy |
| T29-21 | TalkBack on honesty card → reads exact copy | accessibility | P1 | 1. Focus honesty card with TalkBack | Announces exactly: "If someone knows your normal vault exists, a perfect decoy may still not convince them." |
| T29-22 | No error states possible (content-only) | negative | P2 | 1. Open guidance<br>2. Verify no error UI | No empty state, no error banner, no retry button — static content only |
| T29-23 | Deep link to guidance without Pro → blocked | security | P0 | 1. Disable Pro (E1/E3)<br>2. Attempt deep link `vaultaire://settings/security/duress/guidance` | Redirects to Pro gate or Settings hub; guidance not shown |
| T29-24 | Deep link to guidance with Pro but no duress setup → allowed | functional | P1 | 1. Enable Pro<br>2. Deep link directly to guidance | Guidance opens (entry precondition is Pro, not duress-enabled) |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T29-25 | Never reachable from home screen | security | P0 | 1. From vault home, attempt to navigate to guidance | No path exists: no nav drawer item, no settings row, no FAB action |
| T29-26 | Never reachable from vault timeline/collections | security | P0 | 1. From timeline or collection, open settings → verify no guidance entry | Settings hub has no row linking to guidance |
| T29-27 | No "decoy" word in title or body when observed | security | P0 | 1. Open guidance<br>2. Search rendered text for "decoy" (case-insensitive) | Zero matches in title + body; only "this vault" / "your vault" |
| T29-28 | Honesty card copy exact per screens.md §29 | security | P0 | 1. Open guidance<br>2. Compare honesty card text to spec | Byte-for-byte match: "If someone knows your normal vault exists, a perfect decoy may still not convince them." |
| T29-29 | Honesty card never dismissible | security | P0 | 1. Open guidance<br>2. Long-press, swipe, tap, back — attempt all dismiss patterns | Card persists across all interactions; no "Got it" / "Dismiss" / close icon |
| T29-30 | No checklist discoverable by observer | security | P0 | 1. Open guidance<br>2. Screenshot full scroll | No interactive checklist, no completion marks, no scoring UI |
| T29-31 | No "score" or "rating" of believability | security | P0 | 1. Open guidance<br>2. Inspect for any metric display | Zero numeric scores, stars, progress rings, or qualitative ratings |
| T29-32 | FLAG_SECURE prevents screenshot/recording | security | P0 | 1. Open guidance<br>2. Attempt screenshot (vol- + power) / screenrecord | Screenshot blocked (black/blank); screenrecord shows black; per security matrix |
| T29-33 | Clipboard disabled on guidance screen | security | P0 | 1. Open guidance<br>2. Long-press any text → Copy | Copy action disabled/no clipboard write; per security matrix clipboard=off |
| T29-34 | Recents shows brand cover, not content | security | P0 | 1. Open guidance<br>2. Open recents/overview | App snapshot is solid brand cover, not guidance content; per security matrix recents=brand |
| T29-35 | Solid panel backdrop present | security | P1 | 1. Open guidance<br>2. Inspect view hierarchy | `AuroraSecureSurface` or equivalent solid panel behind content; per security matrix |
| T29-36 | Guidance does not reveal which vault is decoy | security | P0 | 1. Open guidance<br>2. Search for vault labels, IDs, indexes | Zero vault identifiers; generic "this vault" only |
| T29-37 | Guidance does not reveal vault count | security | P0 | 1. Open guidance<br>2. Search for count language | No mention of "1 vault", "multiple vaults", "other vaults" |
| T29-38 | No forensic detail in guidance | security | P0 | 1. Open guidance<br>2. Search for crypto/forensic terms | No "PBKDF2", "locator", "index", "cover file", "wipe", "erase" |
| T29-39 | Honesty card acknowledges forensic limit | security | P0 | 1. Open guidance<br>2. Verify honesty card aligns with plausible-deniability §19-25 | Card states decoy may not convince if real vault existence known — matches "vault-count estimation" limit |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T29-40 | V1 phone portrait <600dp → single column, floating nav | adaptive | P0 | 1. Run on phone <600dp<br>2. Open guidance | Single column; floating bottom nav; max width 320dp for content |
| T29-41 | V4 tablet 600–840dp → centered 600dp max, rail left | adaptive | P0 | 1. Run on tablet 600–840dp<br>2. Open guidance | Two-pane: left rail (Security highlighted), right detail centered ≤600dp |
| T29-42 | V4 tablet >840dp → centered 600dp max, rail left | adaptive | P0 | 1. Run on desktop/large tablet >840dp<br>2. Open guidance | Three-pane not used for guidance; stays centered 600dp max with rail |
| T29-43 | Landscape phone → single column, no overlap | adaptive | P1 | 1. Rotate phone to landscape<br>2. Open guidance | Single column; content centered; no cutout overlap |
| T29-44 | Touch targets ≥48dp at all scales | accessibility | P0 | 1. Set font 200%<br>2. Open guidance<br>3. Measure back/close targets | All interactive targets ≥48dp |
| T29-45 | Color contrast ≥4.5:1 for all text | accessibility | P1 | 1. Open guidance in light/dark theme<br>2. Audit contrast | Title, body, honesty card all meet WCAG AA |
| T29-46 | Honesty card butter background meets contrast | accessibility | P1 | 1. Open guidance in light/dark<br>2. Check card text on butter surface | `body.m` on butter surface ≥4.5:1 |
| T29-47 | Scroll announcement for screen readers | accessibility | P2 | 1. TalkBack on<br>2. Scroll guidance | Scroll position announced; no duplicate announcements |
| T29-48 | No motion animation that can't be reduced | accessibility | P2 | 1. Enable "Remove animations"<br>2. Open guidance | No parallax, fade, or slide animations on entry |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T29-49 | Pro status lapses mid-screen (E2→E3) → guidance stays | race | P1 | 1. Open guidance (Pro active)<br>2. Revoke Pro entitlement remotely<br>3. Observe guidance | Guidance remains visible (content already loaded); no immediate eject |
| T29-50 | Pro status lapses → back to duress shows gate | race | P1 | 1. Open guidance (Pro active)<br>2. Revoke Pro<br>3. Back to duress setup | Duress setup shows `S10` Pro gate on next tap |
| T29-51 | Auto-lock during scroll → brand cover on unlock | race | P0 | 1. Open guidance<br>2. Scroll continuously<br>3. Auto-lock triggers<br>4. Unlock | Brand cover shown; unlock returns to guidance at same scroll position |
| T29-52 | Configuration change (locale) → content re-localized | race | P1 | 1. Open guidance<br>2. Change system language | Guidance re-composes with localized strings; honesty card copy localized per spec |
| T29-53 | Theme change (light/dark) → immediate recolor | race | P1 | 1. Open guidance<br>2. Toggle system theme | Colors update without flicker; butter surface adapts |
| T29-54 | Process death during scroll → position restored | lifecycle | P1 | 1. Open guidance<br>2. Scroll to bottom<br>3. Kill process<br>4. Relaunch → unlock | Guidance opens at bottom scroll position |
| T29-55 | Split-screen / multi-window → layout adapts | adaptive | P1 | 1. Open guidance in split-screen<br>2. Resize divider | Content reflows per width bucket; honesty card stays visible |
| T29-56 | Rapid back presses → no double-pop | navigation | P2 | 1. Open guidance<br>2. Double-tap back rapidly | Single navigation back to duress setup; no crash |
| T29-57 | Guidance link tapped twice rapidly → single navigation | navigation | P2 | 1. On duress setup, double-tap Guidance link | Single guidance instance pushed; no stack duplication |

## Security invariants to assert

- [ ] **Never reachable from home screen** — no nav path from `/vault`, `/vault/timeline`, `/vault/collection/*`, widgets, shortcuts, or notifications
- [ ] **No "decoy" in title or body** — title exactly "Making it look lived-in."; body uses only "this vault" / "your vault"
- [ ] **No discoverable checklist or score** — zero interactive checklist items, completion indicators, numeric scores, star ratings, progress bars
- [ ] **Honesty card copy exact** — "If someone knows your normal vault exists, a perfect decoy may still not convince them." (byte-for-byte)
- [ ] **Honesty card non-dismissible** — no swipe, tap, long-press, back, or close affordance removes it
- [ ] **No dead-end navigation** — back/close always returns to `/settings/security/duress`
- [ ] **FLAG_SECURE enabled** — screenshot, screenrecord, casting blocked
- [ ] **Clipboard disabled** — no copy/paste from guidance text
- [ ] **Recents shows brand cover** — not guidance content
- [ ] **Solid panel backdrop** — `AuroraSecureSurface` or equivalent behind content
- [ ] **Pro-gated at entry** — guidance only reachable when Pro entitlement E2 active
- [ ] **No vault identifiers** — no labels, IDs, indexes, counts, or "which vault is decoy" hints
- [ ] **No forensic terminology** — no PBKDF2, locator, index, cover file, wipe, erase, salt, key derivation
- [ ] **Honesty card acknowledges forensic limit** — aligns with plausible-deniability §19-25 (vault-count estimation)

## Gaps & contradictions

- **Gap**: screens.md §29 says "Content-only screen" but flow §4.4 defines `S0 Initial` → `S3 Content` states; no `S6 Error` state defined — what if content fails to load? (unlikely for static content)
- **Contradiction**: screens.md §29 says "never from the home screen" but flow §4.4 entry points only list `/settings/security/duress` → Guidance link; deep link behavior not specified for non-Pro (flow says Pro: yes, but no gate logic described for direct deep link)
- **Gap**: flow §4.4 states "Announcement: 'Decoy guidance'" for S0 — but screens.md §29 forbids "decoy" in title/body if observed; TalkBack announcement may be overheard; should announcement use "Making it look lived-in" instead?
- **Gap**: No specification for what happens if user enables duress, then opens guidance, then disables duress — guidance still accessible? (likely yes, per "Pro gate at entry" not "duress-enabled gate")
- **Gap**: screens.md §33 security matrix shows Decoy guidance: FLAG_SECURE ✅, Solid panel ✅, Clipboard off, Recents brand — but flow §4.4 badges say "Secure: no"; contradiction between matrix and flow badge
- **Gap**: Honesty card copy in screens.md §29 uses "normal vault" but plausible-deniability planning.md never uses "normal" — uses "real vault" / "sensitive vault"; terminology inconsistency
- **Gap**: No spec for horizontal scroll behavior on very narrow screens with 200% font — honesty card must stay visible but may need horizontal scroll if width < card min-width
- **Gap**: Flow §4.4 edge cases mention "Process death (G4): Returns to guidance if session valid" but no spec for "session invalid" (returns to lock? settings? duress setup?)