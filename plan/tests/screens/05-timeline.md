# Timeline — Test Cases
## Scope — screens.md §5 "Timeline" + flows/02-vault-core.md §2.2 + secure-sharing planning.md (recipient phrase invariants) + encrypted-cloud-backup planning.md (queued offline badges)
## Preconditions
- Vault unlocked; timeline at `/vault/timeline`
- "Hide previews" setting available in Security settings (read live; flip re-renders without reload)
- Offline queue (N2) can have pending backup/sync/share events
- Recipient phrase/email exists for share events but must never render
- Empty day: single "No activity" row, no illustration
- Month sheet: 3 months live, then lazy-loads older months

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T05-01 | Open timeline from Home tab | functional | P0 | 1. Unlock vault<br>2. Tap Timeline nav destination | Strip + rail render; today selected; "Timeline" announced; S0→S3 ≤300 ms |
| T05-02 | Open timeline via notification deep link | functional | P0 | 1. Background app<br>2. Trigger backup-complete notification<br>3. Tap notification | Opens `/vault/timeline` with `pendingDestination={kind:timeline}`; never names content in notification |
| T05-03 | Select a different day via calendar strip | functional | P0 | 1. Tap any day cell in strip | Day selected; that day's rail loads; S2→S3 |
| T05-04 | Swipe week left/right in strip | functional | P0 | 1. Horizontal swipe on calendar strip | Week changes; selected day updates; rail reloads for new day |
| T05-05 | Tap month header opens month sheet | functional | P0 | 1. Tap month header ("June, 2026") | `AuroraSheet` opens over timeline; month grid visible |
| T05-06 | Scroll month sheet within 3 months | functional | P0 | 1. Open month sheet<br>2. Scroll up to 3 months back | All 3 months render live; no lazy-load trigger |
| T05-07 | Scroll month sheet beyond 3 months triggers lazy load | functional | P0 | 1. Open month sheet<br>2. Scroll past 3-month boundary | Older months lazy-load; S1→S3 for each page; unreadable → S6 inline |
| T05-08 | Tap media import event opens Viewer | functional | P0 | 1. Ensure media import event exists (e.g. "Imported 12 photos")<br>2. Tap event row | Navigates to `/vault/file/{id}`; Viewer opens on that item |
| T05-09 | Tap collection event opens Collection | functional | P0 | 1. Ensure collection event exists (e.g. "Shared album")<br>2. Tap event row | Navigates to `/vault/collection/{id}` |
| T05-10 | Tap share event opens Shares screen | functional | P0 | 1. Ensure share event exists<br>2. Tap event row | Navigates to `/shares` |
| T05-11 | Tap sync event opens Sync settings | functional | P0 | 1. Ensure sync event exists<br>2. Tap event row | Navigates to `/settings/cloud/sync` |
| T05-12 | Pull-to-refresh re-derives day buckets | functional | P0 | 1. Pull down on timeline rail<br>2. Release | S2→S3; local re-read; banners + thumbnails update |
| T05-13 | FAB (+) opens Camera | functional | P0 | 1. Tap 44dp glass circle FAB | Opens `/vault/camera`; permission check if needed (P2/P3→S11 rationale) |
| T05-14 | Event overflow (⋯) menu on media import | functional | P0 | 1. Long-press or tap (⋯) on media import event<br>2. Verify menu items | Row menu: Open, Add to collection, Delete |
| T05-15 | Day with events renders hour-grouped tinted cards | visual | P0 | 1. Select a day with activity | Rail shows hour headers (10 AM, 11 AM, 1 PM) with rose/mint/sky tint cards |
| T05-16 | Empty day shows single "No activity" row | visual | P0 | 1. Select a day with zero events | Single row "No activity"; no illustration; announced "No activity" |
| T05-17 | New vault (no history) shows empty month | visual | P0 | 1. Fresh vault, open timeline | Single "No activity" row for the month; camera FAB available |
| T05-18 | Offline queued events show 16dp clock badge | visual | P0 | 1. Disable network (N2)<br>2. Trigger backup/sync/share<br>3. Open timeline | Queued cards display `AuroraClockBadge` (16dp pill, icon + `label.m`, never color-only) |
| T05-19 | Reconnect clears clock badges in place | functional | P0 | 1. Have queued events with clock badges<br>2. Re-enable network | Badges clear in place; cards not duplicated; no row re-layout |
| T05-20 | Adaptive: V1 single rail + nav bar | visual | P0 | 1. Run on phone width (<600dp) | Calendar strip + rail stacked; floating nav bar |
| T05-21 | Adaptive: V4 2-pane timeline + day detail | visual | P0 | 1. Run on tablet width (600–840dp) | Calendar strip left, timeline rail right; month grid + timeline side by side |
| T05-22 | Adaptive: V4 three-pane at >840dp | visual | P0 | 1. Run on desktop width (>840dp) | Month grid + timeline + detail; max 1040dp content width |
| T05-23 | Font scale 200%: five day chips visible, rail grows | accessibility | P0 | 1. Set system font scale to 200%<br>2. Open timeline | Five day chips visible; rail grows; no truncation |
| T05-24 | Timezone change re-buckets events to new local day | functional | P0 | 1. Change device timezone<br>2. Observe timeline | Events re-bucketed; selected day preserved if exists, else nearest day with activity; no content change |
| T05-25 | Manual clock change re-buckets identically | functional | P0 | 1. Change device time manually<br>2. Observe timeline | Identical re-bucket; future-dated events clamp to "later today" |
| T05-26 | DST spring-forward merges skipped hour | functional | P0 | 1. Simulate DST spring-forward<br>2. Observe timeline | Skipped hour merged into neighbour; no empty phantom hour |
| T05-27 | DST fall-back merges repeated hour with +1h marker | functional | P0 | 1. Simulate DST fall-back<br>2. Observe timeline | Repeated hour merged with small `+1h` marker; chronological ordering preserved |
| T05-28 | Process death restores selectedDate + scroll anchor | functional | P0 | 1. Scroll to specific event<br>2. Kill process (G4)<br>3. Relaunch | Reopens locked; unlock → timeline at restored `selectedDate`; scroll anchored to stable event id; if id gone, lands on today |
| T05-29 | Warm resume (G2) resumes at same state | functional | P0 | 1. Background app briefly<br>2. Return to timeline | Resumes at same state; no reload |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T05-30 | Day bucket read error shows inline danger row + Retry | error | P0 | 1. Corrupt index for a day<br>2. Select that day | S6: inline danger row "This day couldn't load" + `Retry`; Retry re-reads that day only |
| T05-31 | Partial load shows neutral placeholders for unresolved events | error | P0 | 1. Some events still resolving<br>2. Observe rail | S5: rows with neutral placeholder; rest usable; tap others works; announced "Some activity is still loading" |
| T05-32 | Month sheet older month unreadable → S6 inline | error | P0 | 1. Open month sheet<br>2. Scroll to unreadable month | S6 inline in sheet; `Retry` re-loads that month |
| T05-33 | Activity referencing deleted item: row removed, never dead | security | P0 | 1. Have event for item<br>2. Delete item from vault<br>3. Return to timeline | Row removed entirely; never rendered dead; share/backup event outliving target stays but tap target gone (non-interactive, muted) |
| T05-34 | Deleted item was open in Viewer behind pushed event | functional | P0 | 1. Open Viewer on item<br>2. Item deleted remotely<br>3. Timeline receives push event | Viewer's own flow handles it (S6 danger banner "We couldn't open this item. It may be damaged." + `Retry`) |
| T05-35 | Race: sync merge arrives during older month lazy-load | race | P1 | 1. Scroll month sheet to trigger lazy-load<br>2. Simulate sync merge arriving | Merge repaints loaded buckets; loading generation counter drops stale page; no duplicate rows |
| T05-36 | Pull-to-refresh on unreadable index → S6 | error | P0 | 1. Corrupt local index<br>2. Pull-to-refresh | S6 inline danger banner + `Retry` (re-reads index slice only) |
| T05-37 | FAB tapped without camera permission → rationale | error | P1 | 1. Deny camera permission<br>2. Tap FAB | P2/P3 → S11 rationale sheet; state preserved |
| T05-38 | Auto-lock covers timeline with brand cover | security | P0 | 1. Wait for auto-lock threshold<br>2. Observe | S9: brand cover, no content beneath; announced "Vault locked"; exit → `/lock` |
| T05-39 | Recents cover uses blur/solid toggle (not brand) | security | P0 | 1. Open timeline<br>2. Open app switcher | Snapshot shows blur/solid cover per setting; not brand cover |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T05-40 | Hide previews ON: media events show counts only | security | P0 | 1. Enable "Hide previews"<br>2. Observe media import event | Shows "Imported 12 photos" (count only); no file names; flip re-renders without reload |
| T05-41 | Hide previews OFF: file names may appear | security | P0 | 1. Disable "Hide previews"<br>2. Observe media import event | File names may appear in event card |
| T05-42 | Share event never shows recipient phrase | security | P0 | 1. Create share with recipient phrase<br>2. Observe share event on timeline | Recipient phrase never rendered; event shows generic "Shared album" + expiry only |
| T05-43 | Share event never shows recipient email | security | P0 | 1. Create share with recipient email<br>2. Observe share event on timeline | Recipient email never rendered |
| T05-44 | No file names in logs/diagnostics from timeline | security | P0 | 1. Enable verbose logging<br>2. Scroll timeline<br>3. Inspect logs | No file names, vault label, pattern mapping, plaintext paths, secret/share phrases |
| T05-45 | No recipient identity in any timeline output | security | P0 | 1. Share with known recipient<br>2. Check notifications, recents, logs | Recipient identity never appears |
| T05-46 | No device IDs or record IDs exposed | security | P0 | 1. Inspect timeline data contract<br>2. Verify reads/writes | Reads: encrypted activity records (type, timestamp, opaque target id, count); Writes: selectedDate, scroll anchor (session) only |
| T05-47 | Share phrase spent on claim never leaks to timeline | security | P0 | 1. Recipient claims share phrase<br>2. Observe owner timeline | Owner timeline shows share event but never reveals phrase spent status or claim details |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T05-48 | TalkBack announces day selection | accessibility | P0 | 1. Enable TalkBack<br>2. Tap day cell | Announces selected date; rail content announced |
| T05-49 | TalkBack announces "No activity" on empty day | accessibility | P0 | 1. Enable TalkBack<br>2. Select empty day | Announces "No activity" |
| T05-50 | TalkBack announces clock badge as "Waiting to sync" | accessibility | P0 | 1. Enable TalkBack<br>2. Focus queued event with clock badge | Announces "Waiting to sync" (not color-only) |
| T05-51 | Month sheet keyboard navigation | accessibility | P0 | 1. Open month sheet with keyboard<br>2. Tab/arrow through months | Focus moves logically; Enter selects month |
| T05-52 | Switch access: all controls reachable | accessibility | P0 | 1. Enable Switch Access<br>2. Navigate timeline | All interactive elements (day cells, events, FAB, month header) reachable |
| T05-53 | High contrast mode: tint cards distinguishable | accessibility | P0 | 1. Enable high contrast<br>2. Observe hour cards | Rose/mint/sky tints distinguishable; no color-only info |
| T05-54 | Foldable hinge-aware: strip not across fold | adaptive | P1 | 1. Run on foldable<br>2. Fold at hinge | Calendar strip and rail never placed across fold; tabletop mode puts grid on bottom half |
| T05-55 | Landscape V1: floating nav, 2-up grid | adaptive | P1 | 1. Rotate phone to landscape | 2-up grid; floating nav; FAB accessible |
| T05-56 | Mouse/trackpad: wheel scroll on rail | adaptive | P1 | 1. Use mouse on desktop build<br>2. Scroll rail | Vertical wheel scrolls rail; horizontal wheel on strip changes week |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T05-57 | Background → foreground preserves selectedDate + scroll | lifecycle | P0 | 1. Scroll to specific event<br>2. Home button (background)<br>3. Return to app | Resumes at same selectedDate + scroll anchor; no reload |
| T05-58 | Process death during month sheet lazy-load | lifecycle | P0 | 1. Open month sheet<br>2. Scroll to trigger lazy-load<br>3. Kill process<br>4. Relaunch | Reopens locked; unlock → timeline at today (or restored date); month sheet closed |
| T05-59 | Notification tap while timeline open updates in place | race | P1 | 1. Timeline open<br>2. Backup completes notification fires<br>3. Tap notification | Timeline updates in place; no navigation reset |
| T05-60 | Sync merge during day selection | race | P1 | 1. Select day<br>2. Sync merge arrives for that day | Day rail repaints with merged events; no flicker to skeleton |

## Security invariants to assert — checklist
- [ ] No file names rendered on timeline when "Hide previews" is ON (counts only: "Imported 12 photos")
- [ ] Recipient phrase never appears in any share event card
- [ ] Recipient email never appears in any share event card
- [ ] No dead rows: activity referencing deleted item → row removed entirely
- [ ] Share/backup event outliving target → stays but tap target non-interactive, muted
- [ ] No vault label, device IDs, record IDs, pattern mapping, plaintext paths in logs/diagnostics
- [ ] No secret phrases, share phrases, recovery phrases in any timeline output
- [ ] Clock badge uses icon + `label.m` (never color-only)
- [ ] Recents cover uses blur/solid toggle (not brand cover)
- [ ] FLAG_SECURE not applied (Timeline is not secure screen)
- [ ] Clipboard policy normal (not blocked)

## Gaps & contradictions
- **Paging unit undefined**: "3 months max before lazy loading" (§5) needs paging unit (month? week?) and behavior for vault >10 years (flows.md Gap #6)
- **Month sheet lazy-load failure UX**: S6 inline in sheet specified but no detail on retry affordance placement
- **Share event tap target when target gone**: flows says "non-interactive, muted" but no visual spec for muted state
- **Clock badge on share events**: secure-sharing doesn't specify if share events get queued offline badges; encrypted-cloud-backup implies backup/sync only
- **Event overflow (⋯) menu on non-import events**: Only media import events documented to have row menu; collection/share/sync events' overflow behavior unspecified
- **Empty month vs empty day copy**: Both use "No activity" but flows.md distinguishes S4 Empty (day) and S4 Empty (month) — same copy, different exits
- **Timezone change selected day preservation**: "preserved if it still exists, else nearest day with activity" — what if no days have activity? (falls to today?)
- **Month sheet scroll-back limit**: 3 months live then lazy-load — but flows.md says "3 months cached; older lazy-loads" — contradiction: live vs cached