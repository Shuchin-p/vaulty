# My Shares and Revocation — Test Cases

## Scope
screens.md §28 (28.1 My Shares /shares, 28.2 Revoke confirmation, 28.3 QR handoff optional) + flows 5.4–5.8 cloud-and-sharing.md + secure-sharing planning.md invariants (no recipient identity, phrase-based, revoke eventual, policy AAD) + plausible-deniability planning.md (no vault list, pattern routing only).

## Preconditions
- Pro entitlement (E2) for create/publish/revoke; expired/lapsed Pro (E3) keeps list read-only
- Share statuses exist: Active (mint), Expired (sand), Revoked (ink.400), Revoking (butter + 2 dp line), Stuck >24h (butter + `Stuck? Retry`), Source deleted
- Network states: Online, Offline (N2/N4), Metered (N3), Drive auth revoked, Drive quota exceeded
- Empty list: zero shares ever created
- Pending revoke queued offline; durable across G4 process death
- FLAG_SECURE off for /shares; on only for /vault/share/phrase and /shared/{id}

## Test cases

### 28.1 My Shares — `/shares`

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T28-01 | List loads with Active share | Happy path | P0 | 1. Unlock app (Pro E2)<br>2. Navigate to `/shares` (nav or Settings → Shares) | Header "(back) My Shares [PRO]"; Active card shows label "Kyoto, 2024", status line "Active · 2 of 3 opens · 6d", mint dot, actions `[ Publish update ]  [ Revoke ]` |
| T28-02 | List loads with Expired share | Happy path | P0 | 1. Precondition: share expired Nov 12<br>2. Open `/shares` | Card shows label "Documents", status line "Expired · Nov 12", sand dot, no actions |
| T28-03 | List loads with Revoked share | Happy path | P0 | 1. Precondition: share revoked Nov 10<br>2. Open `/shares` | Card shows label, status line "Revoked · Nov 10", ink.400 dot, no actions |
| T28-04 | List shows Revoking in-flight | Happy path | P0 | 1. Tap Revoke on Active → confirm Revoke access<br>2. Observe card before network returns | Card shows butter background, status line "`Revoking…`" with 2 dp butter progress line, no actions |
| T28-05 | List shows Stuck after 24 h offline | Edge & error | P1 | 1. Revoke while offline<br>2. Wait 24 h (or mock clock)<br>3. Open `/shares` online | Card shows `Stuck? Retry` link; tapping retries revoke; progress line reappears |
| T28-06 | Empty state zero shares | Happy path | P0 | 1. Fresh install Pro E2, no shares created<br>2. Open `/shares` | Screen shows "You haven't shared anything." + `Share a vault` button (enabled, Pro gate passed) |
| T28-07 | Empty state Pro gate on Share a vault | Security & deniability | P1 | 1. Free tier (E1) user opens `/shares`<br>2. Tap `Share a vault` | `AuroraProGate` appears: value sentence, `Try Pro`, `Not now`; list remains accessible (read-only) |
| T28-08 | No recipient identity anywhere | Security & deniability | P0 | 1. Inspect all cards, sheets, dialogs on `/shares` | No email, phone, device name, contact avatar, or "To:" field appears; only encrypted label, dates, policy |
| T28-09 | Card tap opens detail sheet | Happy path | P1 | 1. Tap any card (Active/Expired/Revoked)<br>2. Observe sheet | Sheet shows: policy (expiry, max opens, export toggle), created date, opens used, current status; no recipient info |
| T28-10 | Active share shows Publish update | Happy path | P0 | 1. Open `/shares` with Active share<br>2. Verify action row | `[ Publish update ]` present; tapping queues incremental encrypted update (S15→S3) |
| T28-11 | Expired share hides Publish update | Security & deniability | P1 | 1. Open `/shares` with Expired share | `Publish update` **absent** (not disabled); only status line shown |
| T28-12 | Revoked share hides Publish update | Security & deniability | P1 | 1. Open `/shares` with Revoked share | `Publish update` **absent**; `Revoke` **absent** |
| T28-13 | Revoke button is ghost danger style | Security & deniability | P0 | 1. Inspect Revoke on Active card | Button uses ghost danger styling (danger text, no filled red background); not a filled red button |
| T28-14 | Sort by expiry / created / status | Happy path | P2 | 1. Open `/shares` with ≥2 shares of mixed status<br>2. Use sort control | Groups by status (Active → Expired → Revoked → Revoking → Stuck → Source deleted); stable by share id |
| T28-15 | 50 shares scroll and group | Happy path | P2 | 1. Precondition: 50 shares across statuses<br>2. Open `/shares` | Scrollable; status grouping preserved; scroll position restored by stable share id after process death |
| T28-16 | Source deleted collection survives revoke | Edge & error | P1 | 1. Delete shared collection locally (owner)<br>2. Open `/shares` | Card shows `Source deleted` label; status dot per revoke state; `Revoke` still works and reaches Drive |
| T28-17 | Network offline shows sky banner | Races & lifecycle | P1 | 1. Disable network<br>2. Open `/shares` | Sky banner "You're offline. Revoke will finish when you're connected."; revoke/publish queue locally |
| T28-18 | Network offline revoke queues | Races & lifecycle | P0 | 1. Offline → revoke Active share<br>2. Observe card<br>3. Go online | Card shows `Revoking…` with progress line; auto-retries when connectivity returns; transitions to Revoked |
| T28-19 | G4 during revoke restores Revoking | Races & lifecycle | P1 | 1. Confirm revoke → card shows Revoking…<br>2. Kill process (G4)<br>3. Relaunch → unlock → `/shares` | Card returns to `Revoking…` with progress line and retries; no orphan state |
| T28-20 | Drive auth revoked revoke fails with cause | Edge & error | P1 | 1. Revoke → Drive 401/403<br>2. Observe inline error | Danger inline: named cause (e.g., "Backup needs Drive access.") + `Retry` + `Why?`; Revoking persists |
| T28-21 | Drive quota exceeded revoke fails with cause | Edge & error | P1 | 1. Revoke → Drive quota error<br>2. Observe inline error | Danger inline: "Google Drive is full. Free up space or turn off backup." + `Retry` + `Why?` |
| T28-22 | Partial decrypt shows neutral Unavailable | Edge & error | P1 | 1. Corrupt one share record locally<br>2. Open `/shares` | Failed card renders neutral `Unavailable` with `Delete card` action; other cards normal |
| T28-23 | List store unreadable shows inline error | Edge & error | P1 | 1. Corrupt local shares index<br>2. Open `/shares` | Inline error "Your shares couldn't be loaded." + `Retry` button |
| T28-24 | Selection mode batch revoke | Happy path | P2 | 1. Long-press Active card → multi-select<br>2. Select 3 Active shares<br>3. Tap batch Revoke → confirm each | Each selected share enters Revoking → Revoked; selection drops after G4 |
| T28-25 | Accessibility TalkBack announces status | Accessibility & adaptive | P1 | 1. Enable TalkBack<br>2. Navigate `/shares` cards | Each card announces: "{label}, active/expired/revoked, 2 of 3 opens, expires in 6 days"; actions announced |
| T28-26 | Font scale 200% cards reflow no truncation | Accessibility & adaptive | P1 | 1. Set font scale 200% (A4)<br>2. Open `/shares` | Cards grow; status line wraps; mint/sand/ink/butter dots not clipped; actions remain tappable |
| T28-27 | Tablet V4 two-column with nav rail | Accessibility & adaptive | P1 | 1. Run on tablet/foldable (V4)<br>2. Open `/shares` | Two-column grid + nav rail; cards equal width; empty state centered |
| T28-28 | Reduce motion disables progress line animation | Accessibility & adaptive | P2 | 1. Enable "Reduce motion"<br>2. Trigger Revoking state | 2 dp progress line appears static (no sweep animation); auto-dim/transition still works |
| T28-29 | Share a vault from empty navigates create | Happy path | P0 | 1. Empty list → tap `Share a vault`<br>2. Observe navigation | Navigates to `/vault/share/create` (flow 5.5); back returns to `/shares` with new Active card |
| T28-30 | New share card appears after create flow | Happy path | P0 | 1. Complete `/vault/share/create` → `/vault/share/phrase` → Done<br>2. Back to `/shares` | New Active card at top: label per selection, mint dot, opens "0 of N", expiry per policy, actions present |

### 28.2 Revoke confirmation

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T28-31 | Dialog copy exact per screens.md | Happy path | P0 | 1. Tap Revoke on Active card | Solid dialog (no scrim dismiss): "Revoke this share? The recipient loses access the next time they're online. Files already exported stay exported." Primary `Revoke access` (danger text), secondary `Keep share` |
| T28-32 | Keep share dismisses no state change | Happy path | P0 | 1. Open Revoke dialog → tap `Keep share` | Dialog dismisses; card remains Active; no network call; no local state change |
| T28-33 | Revoke access sets Revoking state | Happy path | P0 | 1. Open Revoke dialog → tap `Revoke access` | Dialog dismisses; card immediately shows `Revoking…` with 2 dp butter progress line; revoke queued to Drive |
| T28-34 | Revoke primary is danger text not filled red | Security & deniability | P0 | 1. Inspect `Revoke access` button in dialog | Button uses danger text color; background is **not** filled red (ghost style); matches "ghost danger action" spec |
| T28-35 | Card never deleted after revoke | Security & deniability | P0 | 1. Revoke → wait for Revoked state<br>2. Observe list | Card persists with status "Revoked · {date}", ink.400 dot; no `Delete card` action on Revoked (only on Expired/Revoked per flow) — *correction*: flow says Expired/Revoked show `Delete card`; Revoked card remains as record |
| T28-36 | Revoke offline queues durable | Races & lifecycle | P0 | 1. Offline → Revoke access<br>2. Kill app → relaunch online | Card shows `Revoking…`; revoke retries automatically; transitions to Revoked when Drive acks |
| T28-37 | Revoke retry automatic then Stuck 24h | Races & lifecycle | P1 | 1. Revoke → network fails persistently<br>2. Wait 24 h (mock)<br>3. Observe | Automatic retries with backoff; after 24 h card shows `Stuck? Retry`; tapping restarts Revoking |
| T28-38 | Revoke does not block vault operations | Security & deniability | P1 | 1. Revoke in progress<br>2. Navigate to `/vault` → import, view, edit | All vault operations work; cloud failures never block local vault (domain invariant #3) |
| T28-39 | Dialog announces correctly for TalkBack | Accessibility & adaptive | P1 | 1. TalkBack on → open Revoke dialog | Dialog title "Revoke this share?"; body read fully; `Revoke access` announced as "button, danger"; `Keep share` as "button" |
| T28-40 | Font scale 200% dialog wraps no clip | Accessibility & adaptive | P1 | 1. Font scale 200% → open Revoke dialog | Copy wraps; buttons full-width if needed; no text clipping; scrim covers full screen |

### 28.3 QR handoff (optional)

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T28-41 | QR shown only inside AuroraSecureSurface | Security & deniability | P0 | 1. On `/vault/share/phrase` (FLAG_SECURE) → tap `Show QR` | QR panel renders inside `AuroraSecureSurface`; warning "Anyone who scans this can open the share." visible |
| T28-42 | QR encodes 12-word phrase exactly | Happy path | P0 | 1. Show QR → scan with second device | Scanned phrase matches the 12 words displayed on `/vault/share/phrase` (2×6 tiles) |
| T28-43 | QR auto-dims after 60 s | Happy path | P1 | 1. Show QR → wait 60 s (mock clock) | QR dims to 10% opacity; tap anywhere on panel restores full brightness |
| T28-44 | QR no screenshot no save-to-photos | Security & deniability | P0 | 1. Show QR → attempt screenshot / long-press save | `FLAG_SECURE` on host activity blocks screenshot; no save-to-photos affordance in panel |
| T28-45 | QR panel hides on Hide QR or back | Happy path | P1 | 1. Show QR → tap `Hide QR` or press back | Panel collapses; returns to phrase tiles; phrase still visible until Done/background |
| T28-46 | QR warning copy exact | Happy path | P0 | 1. Inspect QR panel text | "Anyone who scans this can open the share." (exact copy from screens.md §28.3) |
| T28-47 | TalkBack announces QR warning | Accessibility & adaptive | P1 | 1. TalkBack → show QR panel | Announces warning before QR image; QR image described as "QR code containing sharing phrase" |
| T28-48 | Font scale 200% QR panel readable | Accessibility & adaptive | P1 | 1. Font scale 200% → show QR | Warning text wraps; QR code remains scannable (minimum 180 dp); tap target for redisplay ≥ 48 dp |
| T28-49 | Background clears QR and phrase | Races & lifecycle | P0 | 1. Show QR → Home / screen off → unlock | Returns to `/lock`; phrase buffer cleared; QR gone; share active on `/shares` |
| T28-50 | G4 during QR clears phrase | Races & lifecycle | P0 | 1. Show QR → kill process → relaunch | Phrase unrecoverable; user lands on `/lock` → `/shares`; share exists; only Regenerate available |

### Cross-cutting / Shared flows

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T28-51 | Share expiry notification taps to `/shares` | Happy path | P1 | 1. Share expiring/expired → notification tap → unlock | Opens `/shares` scrolled to relevant card; card status reflects expiry |
| T28-52 | Timeline "Shared album" event taps to share detail | Happy path | P2 | 1. Timeline event → tap → unlock | Opens detail sheet for that share (policy, opens, dates) |
| T28-53 | Owner opens own share via `/shared/{id}` not allowed | Security & deniability | P1 | 1. Owner taps own Active card content → navigate | Owner **cannot** open `/shared/{id}`; recipient-only route; tapping card opens detail sheet only |
| T28-54 | Revoked mid-session on recipient side blurs | Security & deniability | P0 | 1. Recipient viewing `/shared/{id}`<br>2. Owner revokes → recipient syncs | Content blurs immediately; solid full-screen notice "This share is no longer available."; no cached thumbnails remain |
| T28-55 | Publish update on expired/revoked control absent | Security & deniability | P1 | 1. Open `/shares` with Expired share<br>2. Inspect actions | `Publish update` absent (not disabled); same for Revoked |
| T28-56 | Regenerate on `/vault/share/phrase` invalidates old | Races & lifecycle | P1 | 1. Phrase screen → Regenerate → confirm<br>2. Old phrase used on `/shared/open` | Old phrase fails with generic "That phrase didn't work."; new phrase required |
| T28-57 | No recipient identity in any screen or log | Security & deniability | P0 | 1. Inspect `/shares`, revoke dialog, QR, notifications, sync log | Zero occurrence of email, phone, device ID, contact name, or "to recipient" language |
| T28-58 | Pro gate on create only; list always accessible | Security & deniability | P0 | 1. Free tier (E1) → open `/shares` | List fully visible with all statuses; only `Share a vault`/`New share` show `AuroraProGate` |
| T28-59 | Pattern change does not affect shares | Security & deniability | P1 | 1. Owner changes pattern<br>2. Open `/shares` | All shares unchanged; share keys derived independently of pattern (separate hierarchy) |
| T28-60 | Duress wipe leaves shares in cloud intact | Security & deniability | P1 | 1. Owner draws Duress pattern → local purge<br>2. Reinstall → restore → `/shares` | Share records persist in cloud; revoke/publish still work; local vault indices purged only |

## Security invariants to assert

| # | Invariant | Test coverage |
|---|-----------|---------------|
| 1 | **No recipient identity/email/device** anywhere (cards, dialogs, sheets, QR, notifications, logs) | T28-08, T28-41, T28-57 |
| 2 | **Revoke is ghost danger not filled red** (text color only, no red fill) | T28-13, T28-34 |
| 3 | **Card never deleted after revoke** — record of revocation persists | T28-35 |
| 4 | **QR only inside AuroraSecureSurface** (solid + FLAG_SECURE host) | T28-41 |
| 5 | **QR auto-dim 60 s** (10% opacity, tap to redisplay) | T28-43 |
| 6 | **No screenshot on QR** (FLAG_SECURE blocks capture; no save affordance) | T28-44 |
| 7 | **Share phrase never logged, stored plaintext, or in clipboard >60 s** | T28-49, T28-50 (covered in phrase screen tests) |
| 8 | **Revocation eventual** — recipient loses access next online check; already-exported files stay exported (dialog copy) | T28-31, T28-54 |
| 9 | **Policy tamper-evident** (AAD bound to encrypted package) — covered in open-share tests | — |
| 10 | **Cloud never blocks local vault** — revoke/publish failures non-blocking | T28-38 |

## Gaps & contradictions

- **Gap A**: `Publish update` semantics undefined (cloud-and-sharing.md Gap #3). What constitutes an "update"? When does recipient pull? No definition of incremental vs full re-encrypt. Test T28-10 assumes queue incremental; may need revision when spec settled.
- **Gap B**: `AuroraProGate` copy for "Creating shares is a Pro feature" vs screens.md "[PRO]" badge only. Flow 5.4 S10 says gate shows value sentence + Try Pro/Not now; screens.md §28.1 shows only `[PRO]` in header. Need resolution before gate copy final.
- **Gap C**: `Source deleted` card shows `Delete card` action per flow 5.4 S3 Source deleted row, but screens.md §28.1 shows no delete action on any card. Flow says "Delete card" available on Expired/Revoked/Source deleted; screens.md silent. Test T28-16 follows flow.
- **Gap D**: Revoke dialog copy in screens.md: "The recipient loses access the next time they're online." Flow 5.4 edge case repeats same copy. Secure-sharing planning.md Flow C says "Recipient will lose access on next sync." Wording mismatch ("online" vs "sync") — use screens.md copy as source of truth.
- **Gap E**: QR handoff marked "optional" in screens.md §28.3 but `AuroraQrHandoffPanel` listed as new component in cloud-and-sharing.md New components #6. If optional, component may be conditionally compiled; tests T28-41–T28-50 should be gated by feature flag.
- **Gap F**: `Stuck? Retry` after 24 h: flow says "show `Stuck? Retry`"; screens.md says "after 24h show `Stuck? Retry`". No spec for retry backoff algorithm or max retries. Test T28-05, T28-37 assume exponential backoff per cloud-and-sharing.md §5.4 edge cases.
- **Gap G**: Plausible deniability planning.md confirms pattern routing only; no vault list anywhere. `/shares` list is cloud-derived, not pattern-addressed — correct separation. No contradiction.
- **Gap H**: `AuroraSecureSurface` used for QR (screens.md §28.3) and revoked mid-session notice (§16.4, §5.8). Component must enforce FLAG_SECURE + solid background; no transparency. Not fully spec'd in screens.md.
- **Gap I**: Notification "Share expiry" channel (cloud-and-sharing.md §5 table) taps to `/shares` with `{kind=share}`. Copy for expired/revoked/already-used must converge to "This share is no longer available." (flows F5, screens.md §16.3, §24 row "Share expired"). Test T28-51 assumes this convergence.