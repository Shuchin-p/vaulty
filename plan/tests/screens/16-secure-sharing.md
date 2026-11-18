# Secure Sharing — Test Cases

## Scope
screens.md §16 (16.1 Create, 16.2 Phrase display, 16.3 Open, 16.4 Shared vault) + flows 5.4–5.8 (05-cloud-and-sharing.md) + secure-sharing/planning.md AC + secret-phrase/planning.md phrase security + screens.md §24 system states

## Preconditions
- Pro entitlement (E2) required for **create**; Free tiers (E1/E3) blocked with `AuroraProGate` copy "Creating shares is a Pro feature"
- Share scope: **Entire vault** (non-empty) OR **Selected items (≥1)** — entire vault disabled if vault empty
- Policy defaults: Expires **Never**, Max opens **Unlimited**, Export **off** (one-line explanation shown)
- 12-word phrase generated once, held in memory only, never persisted
- Clipboard auto-clear **60 s** after Copy; `EXTRA_IS_SENSITIVE` set
- Paste on entry screen requires **1.5 s hold**; no clipboard suggestions/autofill/IME learning
- FLAG_SECURE **on** for `/vault/share/phrase` and `/shared/{id}`; **off** for create, open entry, my shares
- Wrong/expired/revoked/used phrases all converge to generic copy — no distinction
- Read-only shared vault: no FAB, no edit/delete/re-share, Export **absent** when policy forbids
- Revocation mid-session → content blurs immediately + solid full-screen notice; no cached thumbnails
- No recipient identity, email, device, or "send to" field anywhere

---

## Test Cases

### 16.1 Create — `/vault/share/create`

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T16-01 | Pro gate blocks create on Free tier | Happy | P0 | 1. Free user opens `/vault` → selects items → taps Share | `AuroraProGate` shows "Creating shares is a Pro feature" + `Try Pro` / `Not now` |
| T16-02 | Pro gate blocks create from `/shares` empty state | Happy | P0 | 1. Free user opens `/shares` → taps `Share a vault` | Same Pro gate; existing shares (none) stay usable — gate only on create |
| T16-03 | Entire vault radio enabled when vault non-empty | Happy | P1 | 1. Pro user opens create with non-empty vault | "Entire vault" radio selectable; primary enabled |
| T16-04 | Entire vault radio disabled when vault empty | Edge | P1 | 1. Pro user opens create with empty vault | "Entire vault" disabled + helper "This vault is empty"; primary disabled |
| T16-05 | Selected items radio reflects selection count | Happy | P1 | 1. Pro user opens create with 3 items selected | "Selected items (3)" radio checked; primary enabled |
| T16-06 | Zero selected disables primary with helper | Edge | P0 | 1. Pro user switches to Selected items with 0 selected | Primary disabled + helper "Choose at least one item."; `Add items` returns to origin |
| T16-07 | Expires sheet offers Never/24h/7d/30d/Custom | Happy | P1 | 1. Tap Expires row → sheet opens | Sheet shows 5 options; selection updates row value |
| T16-08 | Max opens sheet offers 1/3/5/10/Unlimited | Happy | P1 | 1. Tap Max opens row → sheet opens | Sheet shows 5 options; selection updates row value |
| T16-09 | Export switch off by default with explanation | Happy | P0 | 1. Open create screen | Export switch off; one-line explanation visible ("Exported copies leave the protection model") |
| T16-10 | Export on warns protection model loss | Edge | P1 | 1. Toggle Export on | Warning line appears; policy bit bound as AAD (changing later requires new share) |
| T16-11 | Create share succeeds online → phrase screen | Happy | P0 | 1. Set policy → tap `Create share` | Inline progress "Encrypting n of n" → transitions to `/vault/share/phrase` with phrase in memory |
| T16-12 | Create share offline blocked with copy | Edge | P0 | 1. Airplane mode → tap `Create share` | Blocking inline "Sharing needs a connection." + `Retry` |
| T16-13 | Create share no Drive access → named error | Edge | P0 | 1. Revoke Drive consent → tap `Create share` | Danger banner with named cause (e.g., "Backup needs Drive access.") + `Retry` / `Why?` |
| T16-14 | Create share storage full blocked pre-encrypt | Edge | P1 | 1. Fill storage ≤50 MB → tap `Create share` | "This device is out of space." + `Free up space`; no encrypt attempted |
| T16-15 | Back with dirty draft prompts confirm | Edge | P1 | 1. Change policy → press Back | Confirm dialog "Discard changes?"; `Discard` returns to origin, `Keep editing` stays |
| T16-16 | Home/background drops draft, no phrase created | Lifecycle | P1 | 1. Change policy → Home → reopen app | Returns to `/lock` → origin; no share record, no phrase |
| T16-17 | G4 during Committing leaves no orphan share | Race | P1 | 1. Tap Create → kill process during encrypt | Relaunch → `/lock` → origin; no share in `/shares`, no phrase on disk |
| T16-18 | No recipient field anywhere in create flow | Security | P0 | 1. Inspect all create screens | No email/account/contact picker/input exists |
| T16-19 | Estimated size shows real sum of selected items | Happy | P1 | 1. Select items totaling 340 MB | "About 340 MB" shown; never a guess presented as precise |

### 16.2 Phrase Display — `/vault/share/phrase`

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T16-20 | FLAG_SECURE active on phrase screen | Security | P0 | 1. Open phrase screen → attempt screenshot/recording | Screenshot blocked; screen recorder shows black; MediaProjection blocked |
| T16-21 | 12-word phrase displayed as 2×6 word tiles | Happy | P0 | 1. Arrive from successful create | 12 words in 2×6 grid, `mono.m` 22sp, index `mono.s` |
| T16-22 | Copy copies phrase with 60s auto-clear | Happy | P0 | 1. Tap `Copy` → wait 60 s → check clipboard | Snackbar "Copied. Clears in 60 s."; phrase cleared from clipboard after 60 s |
| T16-23 | Copy denied when clipboard access blocked | Edge | P1 | 1. Deny clipboard permission → tap `Copy` | Copy disabled + note; no crash |
| T16-24 | Share sheet excludes phrase by default | Security | P0 | 1. Tap `Share` → inspect system sheet content | Neutral note only; 12 words **not** in shared text |
| T16-25 | Show QR displays FLAG_SECURE QR panel | Happy | P1 | 1. Tap `Show QR` | QR panel inside secure surface; "Anyone who scans this can open the share." |
| T16-26 | QR auto-dims to 10% after 60 s | Edge | P1 | 1. Show QR → wait 60 s | QR dims to 10%; tap restores full brightness |
| T16-27 | Checkbox "I've sent it" enables Done | Happy | P0 | 1. Tick checkbox | `Done` button enabled |
| T16-28 | Done zeroizes phrase, activates share | Happy | P0 | 1. Tick checkbox → tap `Done` | Phrase buffer cleared; share becomes active; navigates to `/shares` |
| T16-29 | Back/gesture/close prompts confirm before leaving | Edge | P0 | 1. Press Back without checking "I've sent it" | Solid confirm dialog; `Discard` zeroizes phrase, share exists without recoverable phrase; `Keep` stays |
| T16-30 | Regenerate confirms with warning, invalidates old | Security | P0 | 1. Tap `Regenerate` → confirm dialog shows warning | "The previous phrase will stop working immediately. Recipients who already claimed it will lose access." |
| T16-31 | Regenerate produces new phrase, old dead | Security | P0 | 1. Confirm regenerate | Inline spinner "Regenerating" → new 12-word phrase; old phrase fails on `/shared/open` |
| T16-32 | Regenerate after recipient claimed warns re-send | Security | P1 | 1. Recipient claimed → owner regenerates | Warning states previous phrase invalid; new phrase must be re-sent |
| T16-33 | Background/Home clears phrase buffer | Lifecycle | P0 | 1. On phrase screen → Home → reopen | Returns to `/lock` → `/shares`; phrase gone; share active |
| T16-34 | G4 process death makes phrase unrecoverable | Race | P0 | 1. On phrase screen → kill process | Relaunch → `/lock` → `/shares`; share exists, **no way to re-show phrase**; only Regenerate |
| T16-35 | TalkBack shows banner "Screen reader active. Your phrase will be spoken aloud. Use headphones." | Accessibility | P1 | 1. Enable TalkBack → open phrase screen | Butter banner visible; words read with index 1–12 |
| T16-36 | Reveal again does not exist | Security | P0 | 1. Search UI for "Reveal again" or similar | No such control; phrase truly shown once |
| T16-37 | A4 200% font: tiles grow, may scroll | Adaptive | P1 | 1. Set font 200% → open phrase | 2×6 layout preserved with scroll if needed; all words readable |
| T16-38 | Tablet V4: larger tiles, same 2×6 | Adaptive | P1 | 1. Run on tablet → open phrase | Larger tiles; 2×6 grid maintained |

### 16.3 Open — `/shared/open`

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T16-39 | Entry screen FLAG_SECURE per matrix | Security | P0 | 1. Open `/shared/open` → attempt screenshot | FLAG_SECURE on entry screen (screens.md §16.3 / flow matrix) |
| T16-40 | 12-word entry grid 2×6, auto-advance | Happy | P0 | 1. Type words | Slots fill left-to-right, top-to-bottom; focus advances |
| T16-41 | Paste requires 1.5 s hold, fills all slots | Happy | P0 | 1. Copy valid phrase → hold `Paste words` 1.5 s | All 12 slots filled; `Open` enabled |
| T16-42 | Paste early release does not fill | Edge | P1 | 1. Hold `Paste` < 1.5 s → release | No fill; action inert + note |
| T16-43 | Empty clipboard: Paste inert + note | Edge | P1 | 1. Clear clipboard → hold `Paste` | Action inert + note "Nothing to paste" |
| T16-44 | Scan QR opens camera, requests permission | Happy | P1 | 1. Tap `Scan QR` | Camera permission rationale if needed; scanner opens |
| T16-45 | Camera denied P2/P3 → rationale + Open settings | Edge | P1 | 1. Deny camera twice → tap Scan | Rationale sheet; only `Open settings` action |
| T16-46 | No camera hardware: Scan control absent | Edge | P1 | 1. Run on device without camera | `Scan QR` control **absent** (not disabled) |
| T16-47 | Open with valid unused phrase → shared vault | Happy | P0 | 1. Enter valid phrase → tap `Open` | Full-width iris line "Opening. This can take a few seconds." → `/shared/{id}` |
| T16-48 | Wrong phrase: generic "That phrase didn't work." | Security | P0 | 1. Enter invalid phrase → tap `Open` | "That phrase didn't work." — **no hint** about typo vs expiry vs revoked |
| T16-49 | Expired phrase: "This share is no longer available." | Security | P0 | 1. Enter expired share phrase → tap `Open` | "This share is no longer available." — same tone, no accusation |
| T16-50 | Revoked phrase: same generic copy | Security | P0 | 1. Owner revokes → recipient enters phrase | "This share is no longer available." — identical to expired |
| T16-51 | Max opens consumed: same generic copy | Security | P0 | 1. Exhaust opens → enter phrase | "This share is no longer available." — no "limit exceeded" distinction |
| T16-52 | No Drive access: named cause + Connect | Edge | P0 | 1. No Google account/Drive → enter phrase | "Sign in to Google Drive to open this share." + `Connect` |
| T16-53 | Offline: blocking "Opening needs a connection." | Edge | P0 | 1. Airplane mode → tap `Open` | Blocking inline "Opening needs a connection." + `Retry` |
| T16-54 | Rate-throttle rapid wrong attempts neutral | Security | P1 | 1. Submit 5 wrong phrases rapidly | Neutral copy; never implies valid-but-revoked share exists |
| T16-55 | G4 during Opening: no partial mount | Race | P1 | 1. Tap Open → kill during iris line | Relaunch → entry with cleared buffers |
| T16-56 | Recipient without Pro opens free | Happy | P0 | 1. Free user enters valid phrase | Opens successfully; **no paywall** on this path |
| T16-57 | Clear all empties slots | Happy | P1 | 1. Fill some slots → tap `Clear all` | All slots empty; `Open` disabled |
| T16-58 | Autocorrect/suggestions off on entry fields | Security | P1 | 1. Type in word slots | No suggestions, no autocorrect, no clipboard strip |
| T16-59 | A2 TalkBack: each slot announces index | Accessibility | P1 | 1. Enable TalkBack → focus slot | "Word {i} of 12, empty/filled" |

### 16.4 Shared Vault — `/shared/{id}`

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T16-60 | FLAG_SECURE active on shared vault | Security | P0 | 1. Open shared vault → attempt screenshot | FLAG_SECURE on; screenshot/recording blocked |
| T16-61 | Header shows read-only badge, shared collection name | Happy | P0 | 1. Open valid share | Header: collection name (encrypted content chosen by owner) + `Read-only` badge |
| T16-62 | No owner label, no vault label, no recipient identity | Security | P0 | 1. Inspect header and all UI | No "Owner:", no "Vault:", no recipient email/device |
| T16-63 | Policy line shows expiry/opens/export status | Happy | P1 | 1. Open share with policy | Policy line visible (e.g., "Expires in 7 days · 2 of 3 opens") |
| T16-64 | No FAB, no edit, no delete, no re-share | Security | P0 | 1. Inspect shared vault UI | Floating action bar absent; no edit/delete/re-share controls |
| T16-65 | Export absent when policy forbids all export | Security | P0 | 1. Open share with Export off → long-press item | Export menu item **absent** (not disabled) |
| T16-66 | Export present when policy allows | Happy | P1 | 1. Open share with Export on → long-press item | Export menu item present |
| T16-67 | Per-file non-exportable: Export absent for that file | Edge | P1 | 1. Mixed policy → open non-exportable file | Export absent for that file only |
| T16-68 | Open item → full-screen read-only viewer | Happy | P0 | 1. Tap any item | Full-screen viewer; no edit/save/share |
| T16-69 | Filter chips: Photos/Videos/Docs | Happy | P1 | 1. Tap filter chips | Grid filters by type; no matches → S4 within |
| T16-70 | Info shows metadata: date, size | Happy | P1 | 1. Open item → tap `Info` | Metadata sheet: date, size |
| T16-71 | Refresh pulls updates when online | Happy | P1 | 1. Owner publishes update → pull refresh | New items appear; old removed |
| T16-72 | Offline: cached items stay interactive | Edge | P1 | 1. Go offline → open shared vault | Sky banner "You're offline."; cached items openable |
| T16-73 | Revoked mid-session: content blurs + notice | Security | P0 | 1. Owner revokes while recipient viewing | Content **blurs immediately** + solid full-screen "This share is no longer available." |
| T16-74 | Revoked mid-session: no cached thumbnails remain | Security | P0 | 1. After revoke blur → background → foreground | No thumbnails visible; notice persists |
| T16-75 | Manifest undecryptable (tampered): fatal notice | Security | P0 | 1. Corrupt share manifest → open | Full-bleed "This share can't be opened." + safe exit `Back` |
| T16-76 | Per-item decrypt failure: inline + Why? | Edge | P1 | 1. Corrupt single item → tap | Inline "We couldn't open this item. It may be damaged." + `Retry` on tile |
| T16-77 | Empty shared vault: "This shared vault is empty." | Happy | P1 | 1. Open share with 0 items | Empty state copy + `Refresh` |
| T16-78 | Back/gesture clears media cache | Lifecycle | P1 | 1. View items → press Back | Returns to `/shared/open`; thumbnails dropped |
| T16-79 | Background → FLAG_SECURE cover | Lifecycle | P1 | 1. Home → app switcher | Solid cover; return requires unlock |
| T16-80 | A4 200%: policy line wraps, grid reflows, badge not clipped | Adaptive | P1 | 1. Font 200% → open shared vault | All elements readable; no truncation |
| T16-81 | V1/V4 adaptive: 2-up/3-up grid + rail | Adaptive | P1 | 1. Phone vs tablet layout | Phone 2-up; tablet 3-up + nav rail |

### My Shares — `/shares` (owner side, from flow 5.4)

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T16-82 | Active share card: label, mint dot, "2 of 3 opens · 6d" | Happy | P0 | 1. Create share with max opens 3, expiry 7d → open twice | Card shows mint dot, "Active · 2 of 3 opens · 6d" |
| T16-83 | Expired card: sand dot, "Expired · Nov 12" | Happy | P1 | 1. Wait past expiry | Card shows sand dot, "Expired · Nov 12" |
| T16-84 | Revoked card: ink.400, "Revoked · Nov 10" | Happy | P1 | 1. Revoke share | Card shows ink.400, "Revoked · Nov 10" |
| T16-85 | Revoking card: butter + 2dp progress line | Edge | P1 | 1. Revoke → offline >24h | Card shows butter + progress line + `Stuck? Retry` |
| T16-86 | Revoke confirm copy exact | Security | P0 | 1. Tap `Revoke` on active share | Dialog: "Revoke this share? The recipient loses access the next time they're online. Files already exported stay exported." Primary `Revoke access` (danger text), secondary `Keep share` |
| T16-87 | Revoke queues durably, retries automatically | Race | P0 | 1. Revoke offline → online | Queued; applied on reconnect; card → Revoked |
| T16-88 | Never delete card on revoke | Security | P0 | 1. Revoke share → check `/shares` | Card remains with Revoked status; record persists |
| T16-89 | Publish update only on active shares | Happy | P1 | 1. Tap `Publish update` on active | Queues incremental encrypted update; `Pending` if offline |
| T16-90 | Publish update absent on expired/revoked | Edge | P1 | 1. Check expired/revoked cards | Control **absent**, not disabled |
| T16-91 | Deleted collection: card shows "Source deleted" | Edge | P1 | 1. Delete shared collection locally | Card survives as `Source deleted`; revoke still works |
| T16-92 | No recipient identity on any card | Security | P0 | 1. Inspect all cards | No email, device, "To:", or contact info |
| T16-93 | 50 shares: grouped by status, scrollable | Happy | P1 | 1. Create 50 shares | Grouped Active/Expired/Revoked; stable scroll position |
| T16-94 | Selection mode: batch revoke | Happy | P1 | 1. Long-press card → multi-select → `Revoke all` | Batch revoke queued |
| T16-95 | Empty state: "You haven't shared anything." + Share a vault | Happy | P1 | 1. No shares → open `/shares` | Copy exact + `Share a vault` (Pro gate) |

### Cross-cutting / System States (from screens.md §24)

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T16-96 | Share expired system state copy exact | System | P0 | 1. Trigger share expiry notification | "This share is no longer available." solid notice, no retry loop |
| T16-97 | Rate limited: "Please wait a moment." | System | P1 | 1. Trigger rate limit on open | Neutral, `mono.m` countdown |
| T16-98 | Decryption failure per-item: "We couldn't open this item. It may be damaged." | System | P1 | 1. Corrupt item in shared vault | Danger banner, no technical detail |
| T16-99 | Duress: no special state, identical to success | Security | P0 | 1. Trigger duress → open share | No indication of duress; behaves as normal |

---

## Security Invariants to Assert (Checklist)

- [ ] **FLAG_SECURE** enforced on `/vault/share/phrase` and `/shared/{id}` (solid, not conditional)
- [ ] **60 s clipboard auto-clear** after Copy on phrase screen; `EXTRA_IS_SENSITIVE` set
- [ ] **Phrase shown once** — no "Reveal again", no recovery after background/G4/leave
- [ ] **Regenerate warns** "The previous phrase will stop working immediately..." before invalidating
- [ ] **Wrong/expired/revoked/used phrases** all converge to **generic copy** — no distinction:
  - Wrong: "That phrase didn't work."
  - Expired/Revoked/Used: "This share is no longer available."
- [ ] **Read-only shared vault blurs immediately** on revocation mid-session; **no cached thumbnails remain**
- [ ] **No owner identity** anywhere (no email, device, vault label, "shared by")
- [ ] **Export absent (not disabled)** when policy forbids; present only when allowed
- [ ] **Paste is deliberate** (1.5 s hold); no clipboard suggestions/autofill/IME learning
- [ ] **Pro gate only on create** — opening/viewing always free (E1–E4)
- [ ] **Revocation eventual** — queued durably, retries with backoff, `Stuck? Retry` after 24 h
- [ ] **Never delete revoked share card** — record of revocation persists
- [ ] **Manifest tamper → fatal** "This share can't be opened." + safe exit
- [ ] **Duress indistinguishable** from normal operation

---

## Gaps & Contradictions

- **Publish update semantics undefined** (flows gap #3): "Update" not defined for phrase-based shares — what changed, what recipient pulls, when. Needs definition before button ships.
- **No re-entry surface for claimed share** (flows gap #7): Once recipient leaves `/shared/{id}`, no way back except re-entering 12-word phrase on `/shared/open`. Either accept (document in copy) or add "Shared with me" to `/shares`. Do not invent row on `/vault`.
- **Secret-phrase planning.md** specifies 9-word recovery phrase; secure-sharing uses **12-word** sharing phrase. Different word counts, different derivations (800k vs 250k PBKDF2). Ensure no confusion in code paths.
- **Screens.md §16.2** says "Phrase is never shown again; Regenerate invalidates previous" but **§24 Share expired** copy is "This share is no longer available." — consistent with flow generic copy for expired/revoked/used.
- **Flow 5.6** says FLAG_SECURE "solid" on phrase; **flow matrix §18** says FLAG_SECURE on for `/vault/share/phrase` and `/shared/{id}`; off for create, open entry, my shares. **No contradiction** — confirmed.
- **Planning.md AC-02.4** says "Error: 'This shared vault has expired'" but **screens.md §16.3** and **§24** converge expired/revoked/used to "This share is no longer available." — **planning.md AC contradicts screens.md**. Test cases follow screens.md (generic copy).
- **Planning.md AC-02.2** "Phrase already claimed → Error: 'This share has already been claimed'" — contradicts generic "This share is no longer available." — **planning.md AC contradicts screens.md**. Test cases follow screens.md.
- **Planning.md §4.3** shows header "Shared Vault • Personal Docs" with owner's vault name — **contradicts screens.md §16.4** "No owner or vault label exists. Header shows only shared collection's name." Test cases follow screens.md.
- **Planning.md §4.4** My Shares shows recipient email "john@..." — **contradicts screens.md §28.1** "No recipient identity, email, or device." Test cases follow screens.md.