# Notifications — Test Cases
## Scope — screens.md §25 Notifications + flow/03-camera-and-notifications.md Overlay: Notifications + encrypted-cloud-backup/device-sync planning invariants
## Preconditions
- Local only; no push infrastructure ever used for vault content
- Four channels created idempotently at first app start: `backup` (LOW, no sound/vibration), `sync` (LOW, no sound/vibration), `share_expiry` (DEFAULT, default sound/vibration), `security` (HIGH, **silent**, no vibration)
- `POST_NOTIFICATIONS` requested **only** after first backup enabled or first share created — never at launch/splash/onboarding/first unlock
- Share expiry text carries **no count** ("A share expires soon." not "3 shares expire")
- Tap destination after unlock: settings or shares screen, **never** a file; pending destination stored as opaque `{kind, opaqueId}`
- "Hide previews" on → lockscreen shows title only "Vaultaire", body redacted; recents becomes solid cover
- Dismissed notice does not cancel underlying durable queue (revoke/backup continues)
- No progress notifications for import/encryption (would reveal activity)
- Security channel silent by design — loud tone would draw attention to device

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T25-01 | Backup channel created with correct importance | Functional | P0 | 1. Fresh install<br>2. Enable backup in Settings<br>3. Inspect channel `backup` | Channel exists with importance `LOW`, no sound, no vibration, name "Backup" |
| T25-02 | Sync channel created with correct importance | Functional | P0 | 1. Enable Pro sync<br>2. Inspect channel `sync` | Channel exists with importance `LOW`, no sound, no vibration, name "Sync" |
| T25-03 | Share expiry channel created with correct importance | Functional | P0 | 1. Create first share<br>2. Inspect channel `share_expiry` | Channel exists with importance `DEFAULT`, default sound, default vibration, name "Share expiry" |
| T25-04 | Security channel created silent high | Functional | P0 | 1. Trigger integrity check failure<br>2. Inspect channel `security` | Channel exists with importance `HIGH`, **silent** (no sound), no vibration, name "Security" |
| T25-05 | Backup finished notice appears | Functional | P0 | 1. Enable backup<br>2. Wait for WorkManager backup to complete<br>3. Observe shade | Notice shows "Backup finished." via `backup` channel |
| T25-06 | Backup needs attention notice appears on failure | Functional | P0 | 1. Enable backup<br>2. Simulate backup failure (network off)<br>3. Observe shade | Notice shows "Vaultaire backup needs attention." via `backup` channel |
| T25-07 | Sync needs review notice appears on conflict | Functional | P0 | 1. Enable Pro sync on two devices<br>2. Create conflict<br>3. Observe shade on device B | Notice shows "Vaultaire sync needs your review." via `sync` channel |
| T25-08 | Share expiry notice appears near expiry | Functional | P0 | 1. Create share with 1-day expiry<br>2. Wait until expiry window<br>3. Observe shade | Notice shows "A share expires soon." via `share_expiry` channel — **no count** |
| T25-09 | Share revoked notice appears | Functional | P0 | 1. Create share<br>2. Revoke it from another device<br>3. Observe shade | Notice shows "A share expires soon." via `share_expiry` channel |
| T25-10 | Share exhausted notice appears | Functional | P0 | 1. Create share with open limit<br>2. Exhaust opens<br>3. Observe shade | Notice shows "A share expires soon." via `share_expiry` channel |
| T25-11 | Security notice appears on integrity failure | Functional | P0 | 1. Trigger integrity check failure<br>2. Observe shade | Notice shows "Vaultaire security notice." via `security` channel — **silent** |
| T25-12 | Security notice appears on backup key change | Functional | P0 | 1. Rotate backup key<br>2. Observe shade | Notice shows "Vaultaire security notice." via `security` channel |
| T25-13 | Security notice appears on enrollment invalidation | Functional | P0 | 1. Invalidate biometric enrollment<br>2. Observe shade | Notice shows "Vaultaire security notice." via `security` channel |
| T25-14 | Tap notice while unlocked opens correct screen | Functional | P0 | 1. App unlocked<br>2. Tap "Vaultaire backup needs attention." | Opens `/settings/cloud/backup` directly |
| T25-15 | Tap sync notice while unlocked opens sync settings | Functional | P0 | 1. App unlocked<br>2. Tap "Vaultaire sync needs your review." | Opens `/settings/cloud/sync` directly |
| T25-16 | Tap share expiry notice while unlocked opens shares | Functional | P0 | 1. App unlocked<br>2. Tap "A share expires soon." | Opens `/shares` directly |
| T25-17 | Tap security notice while unlocked opens security info | Functional | P0 | 1. App unlocked<br>2. Tap "Vaultaire security notice." | Opens `/settings/security-info` directly |
| T25-18 | Tap notice while locked routes through lock then destination | Functional | P0 | 1. App locked<br>2. Tap "Vaultaire backup needs attention."<br>3. Unlock with pattern | Opens `/lock` → after unlock → `/settings/cloud/backup` |
| T25-19 | Dismiss notice does not cancel underlying job | Functional | P1 | 1. Backup running notice visible<br>2. Swipe dismiss<br>3. Verify backup completes in-app | Backup continues; in-app banner persists; no re-notify storm |
| T25-20 | Channel disabled hides notices; in-app banner persists | Functional | P1 | 1. Disable `backup` channel in system settings<br>2. Trigger backup failure<br>3. Check shade and in-app | No notice in shade; in-app "Backup didn't finish. Your files are safe on this device." butter banner visible |
| T25-21 | POST_NOTIFICATIONS requested only after first backup/share | Functional | P0 | 1. Fresh install, no backup, no shares<br>2. Launch app, unlock<br>3. Verify no permission prompt | No `POST_NOTIFICATIONS` request at launch/splash/onboarding/first unlock |
| T25-22 | POST_NOTIFICATIONS requested after enabling backup | Functional | P0 | 1. Fresh install<br>2. Enable backup in Settings<br>3. Verify prompt appears | Prompt: "Get a quiet notice when a backup finishes or a share expires." |
| T25-23 | POST_NOTIFICATIONS requested after creating first share | Functional | P0 | 1. Fresh install, backup disabled<br>2. Create first share<br>3. Verify prompt appears | Prompt: "Get a quiet notice when a backup finishes or a share expires." |
| T25-24 | Hide previews on redacts lockscreen body | Functional | P0 | 1. Settings → Privacy → Hide previews ON<br>2. Trigger backup notice<br>3. Lock device, view lockscreen | Title "Vaultaire", body redacted; no "Vaultaire backup needs attention." visible |
| T25-25 | Hide previews on makes recents solid cover | Functional | P1 | 1. Hide previews ON<br>2. Trigger notice<br>3. Open recents | Recents shows solid brand cover, no notice content |
| T25-26 | Unresolvable opaqueId lands on /vault silently | Functional | P1 | 1. Tap notice with stale opaqueId<br>2. App unlocked | Opens `/vault` silently (no error, no crash) |
| T25-27 | Doze deferral delays notice until Doze exits | Timing | P1 | 1. Device in Doze<br>2. Trigger backup completion<br>3. Exit Doze<br>4. Observe notice | Notice appears after Doze exits, not during |
| T25-28 | Share expiring during Doze reported after exit | Timing | P1 | 1. Share expires while in Doze<br>2. Exit Doze<br>3. Observe notice | "A share expires soon." appears after Doze exits |
| T25-29 | No progress notification for import | Security | P0 | 1. Import multiple photos<br>2. Observe shade during import | No import progress notification ever appears |
| T25-30 | No progress notification for encryption | Security | P0 | 1. Trigger Keep/encrypt in review<br>2. Observe shade | No encryption progress notification ever appears |
| T25-31 | Security channel truly silent — no sound ever | Security | P0 | 1. Trigger security notice<br>2. Observe audio | No sound played; channel importance HIGH but silent by design |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T25-32 | Notice text never contains file name | Security | P0 | 1. Trigger any notice<br>2. Inspect full notice text | No file name appears in title or body |
| T25-33 | Notice text never contains vault label | Security | P0 | 1. Trigger any notice<br>2. Inspect full notice text | No vault label (e.g. "Personal", "Work") appears |
| T25-34 | Notice text never contains count revealing vault size | Security | P0 | 1. Trigger sync conflict with 3 items<br>2. Inspect notice text | Shows "Vaultaire sync needs your review." not "3 items need your review." |
| T25-35 | Share expiry notice never includes count | Security | P0 | 1. Have 5 shares expiring<br>2. Trigger expiry notice<br>3. Inspect text | Shows "A share expires soon." not "5 shares expire" |
| T25-36 | Channel creation idempotent on reinstall | Functional | P1 | 1. Uninstall/reinstall<br>2. Enable backup<br>3. Verify channels | Channels recreated with same IDs/importance; no duplicates |
| T25-37 | Backup skipped notice appears | Functional | P1 | 1. Enable backup<br>2. Condition causes skip (e.g. no changes)<br>3. Observe shade | Notice shows "Vaultaire backup needs attention." or "Backup finished." per implementation |
| T25-38 | Sync notice on device disconnected | Functional | P1 | 1. Enable sync<br>2. Disconnect device > threshold<br>3. Observe shade | Notice shows "Vaultaire sync needs your review." |
| T25-39 | Sync notice on key mismatch | Functional | P1 | 1. Enable sync<br>2. Simulate key mismatch<br>3. Observe shade | Notice shows "Vaultaire sync needs your review." |
| T25-40 | Multiple notices coexist without stacking incorrectly | Functional | P1 | 1. Trigger backup failure + share expiry + security<br>2. Observe shade | Three distinct notices visible, each with correct channel |
| T25-41 | Dismiss all clears only notices, not queues | Functional | P1 | 1. Multiple notices visible<br>2. Tap "Clear all"<br>3. Verify in-app state | Notices cleared; durable queues (revoke/backup) continue unaffected |
| T25-42 | OEM channel settings fallback works | Functional | P2 | 1. Open channel settings via in-app row<br>2. OEM omits deep link<br>3. Verify manual path shown | In-app row explains: "Settings → Apps → Vaultaire → Notifications" |
| T25-43 | Notice tap with pending destination survives process death | Lifecycle | P1 | 1. Locked, tap notice → pendingDestination set<br>2. Kill app process<br>3. Relaunch, unlock | Lands on correct destination after unlock |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T25-44 | Shade safe to read over shoulder — no vault existence hint | Security | P0 | 1. Trigger all notice types<br>2. Read lockscreen/shade | No file names, vault labels, counts, share phrases, or vault existence hints |
| T25-45 | Security notice silent — no attention-drawing tone | Security | P0 | 1. Trigger security notice in quiet room<br>2. Observe | No audible sound; device does not draw attention |
| T25-46 | No push infrastructure code paths exist | Security | P0 | 1. Audit code for FCM/GCM/push<br>2. Verify only WorkManager local triggers | Zero push infrastructure; all notices from local WorkManager |
| T25-47 | Tap never opens file directly — only settings/shares | Security | P0 | 1. Tap every notice type unlocked<br>2. Verify destination | Always `/settings/cloud/backup`, `/settings/cloud/sync`, `/shares`, or `/settings/security-info` |
| T25-48 | Tap while locked never bypasses lock | Security | P0 | 1. Locked, tap notice<br>2. Observe | Opens `/lock` first; destination only after successful unlock |
| T25-49 | Hide previews ON redacts all notice content on lockscreen | Security | P0 | 1. Hide previews ON<br>2. Trigger all notice types<br>3. View lockscreen | All show title "Vaultaire" only; bodies fully redacted |
| T25-50 | Recents solid cover when Hide previews ON | Security | P0 | 1. Hide previews ON<br>2. Trigger notice<br>3. Open recents | Solid brand cover; no notice text visible in recents |
| T25-51 | Notice text uses exact copy from spec | Security | P0 | 1. Verify each notice text matches spec exactly | "Vaultaire backup needs attention.", "Backup finished.", "Vaultaire sync needs your review.", "A share expires soon.", "Vaultaire security notice." |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T25-52 | Notice content descriptions present for TalkBack | Accessibility | P1 | 1. Enable TalkBack<br>2. Focus each notice | Each notice announces channel name + content (e.g. "Backup, Vaultaire backup needs attention") |
| T25-53 | Channel names localized correctly | Accessibility | P2 | 1. Switch locale<br>2. Inspect channel names in system settings | "Backup", "Sync", "Share expiry", "Security" localized per strings.xml |
| T25-54 | Notice text scales with font size | Accessibility | P1 | 1. Set font size to largest<br>2. View notice in shade | Text wraps, no truncation; all copy readable |
| T25-55 | High contrast mode preserves notice visibility | Accessibility | P1 | 1. Enable high contrast<br>2. View notice | Notice text meets contrast ratios; no invisible elements |
| T25-56 | Adaptive layout — notice renders on fold/flip | Adaptive | P2 | 1. Trigger notice on foldable<br>2. Fold/unfold while shade open | Notice remains readable; no layout break |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T25-57 | Notice during auto-lock → lockscreen shows notice | Lifecycle | P1 | 1. Trigger notice<br>2. Wait for auto-lock<br>3. View lockscreen | Notice visible on lockscreen (respecting Hide previews) |
| T25-58 | Process death during notice delivery → notice appears on relaunch | Lifecycle | P1 | 1. Trigger notice<br>2. Kill process before shade render<br>3. Relaunch | Notice appears in shade after relaunch |
| T25-59 | Channel permission revoked → notices stop, in-app persists | Lifecycle | P1 | 1. Revoke notification permission<br>2. Trigger backup failure<br>3. Check shade and in-app | No notice in shade; in-app butter banner "Backup didn't finish. Your files are safe on this device." |
| T25-60 | Multiple rapid triggers → no notification storm | Race | P1 | 1. Trigger 10 backup failures rapidly<br>2. Count notices | Single notice per channel (system collapse); no spam |

## Security invariants to assert — checklist
- [ ] Notice text **never** includes file names, vault labels, collection names, or counts revealing vault existence/size
- [ ] Tap opens settings/share screen **after unlock**, **never** the file directly
- [ ] Previews hidden when "Hide previews" ON — lockscreen shows "Vaultaire" title only, body redacted; recents solid cover
- [ ] No push infrastructure code paths exist for vault content (FCM, GCM, etc.)
- [ ] Security channel is **silent** (HIGH importance, no sound) by design
- [ ] Share expiry carries **no count** ("A share expires soon." not "N shares expire")
- [ ] Dismissed notice does not cancel durable queue (revoke/backup continues)
- [ ] No progress notifications for import/encryption (would reveal activity)
- [ ] `POST_NOTIFICATIONS` requested only after first backup enabled or first share created
- [ ] Unresolvable `opaqueId` lands on `/vault` silently

## Gaps & contradictions
- **Contradiction**: screens.md §25 says "Notification text never includes file names, vault labels, or counts that reveal vault existence. Use 'Vaultaire backup needs attention.'" but screens.md §24 "Sync conflict" copy says "3 items need your review." — the flow doc §3.5 correctly uses "Vaultaire sync needs your review." (no count). Spec must align: all notices use content-free copy.
- **Gap**: Flow doc says `POST_NOTIFICATIONS` requested after first backup enabled **or** first share created. If user does neither, `security` channel can never deliver (permission never requested). Either accept silent absence for security notices on that path, or add a trigger to request permission for security channel.
- **Gap**: No specification for notice grouping/stacking behavior when multiple notices from same channel fire rapidly.
- **Gap**: No specification for notice TTL/expiration — how long a notice persists if not dismissed.
- **Gap**: Widget/Quick Settings tile behavior under active disguise alias undefined (per flow doc Gap #6) — a "Vaultaire" widget label would leak the disguise.