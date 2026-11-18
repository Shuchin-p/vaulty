# Encrypted Backup — Test Cases

## Scope
screens.md §14 "Encrypted Backup" + flows/05-cloud-and-sharing.md §5.1 + encrypted-cloud-backup/planning.md + secret-phrase/planning.md

## Preconditions
- App installed, user has an existing vault (pattern set, recovery phrase saved)
- Google Play Services present; Google Drive API available (or S12 unsupported state)
- Network states controllable: Wi-Fi, metered, offline
- Device storage states controllable: normal, low (≤500 MB), full (≤50 MB)
- Battery saver / Doze modes triggerable
- User may or may not have a Google account connected to Drive
- Backup may be in any state: Off (never enabled), Content on (healthy), Content off (disabled), Stale (>48h), First run, Partial, Error, Offline, Metered, Confirming, Committing
- Ring color: mint (healthy), butter (stale >48h), danger (failed) — always with text
- Progress: ring sweep + `mono.m` percentage + "Encrypting N of M" — never fake percentage
- Failure banner: cause + `Retry` + `Why?` — never generic "Something went wrong"
- Disabling backup requires confirmation and explains existing cloud records remain until deletion
- Google account row: only if connected; shows account alias, never token
- `Wi-Fi only` switch on by default; `Include videos` switch on by default
- `Backup key` row: explainer + biometric/pattern reveal (gate per 04-security-settings)
- `Delete cloud data` available when ≥1 record exists
- `Last 5 backups` sheet: time, size, status; read-only; decrypts in memory
- `Restore on a new device` → phrase gate → `/restore/progress`
- Notification channel `Backup` (low) taps open this screen after unlock

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T14-01 | Screen loads with backup off (never enabled) | functional | P0 | 1. Open `/settings/cloud/backup` on fresh install with vault | Neutral ring, "Back up this vault", `Enable` switch off, no Google account row, `Backup now` disabled, `Wi-Fi only`/`Include videos` disabled |
| T14-02 | Screen loads with healthy backup (mint ring) | functional | P0 | 1. Enable backup, complete first run 2. Open screen | Mint ring 72dp, "Backed up 2 hours ago · 1.8 GB", "Google Drive", all controls enabled |
| T14-03 | Screen loads with stale backup (butter ring, >48h) | functional | P0 | 1. Set last-backup timestamp >48h ago 2. Open screen | Butter ring, "Last backup 3 days ago", `Backup now` prominent |
| T14-04 | Enable backup → consent → first run completes | functional | P0 | 1. Toggle `Enable backup` on 2. Grant Google Drive consent 3. Wait for S4→S15→S16 | Ring sweep + "Encrypting N of M" real counts → mint ring, fresh timestamp, "Backup complete" announcement |
| T14-05 | Backup now (manual) runs incremental | functional | P0 | 1. From healthy state, tap `Backup now` | Ring sweep + "Encrypting N of M" real counts → timestamp advances, "Backup complete" |
| T14-06 | Wi-Fi only toggle constrains to UNMETERED | functional | P0 | 1. Enable backup 2. Turn `Wi-Fi only` off 3. Verify WorkManager constraint changes | WorkManager job constraint = `UNMETERED` when on; `NONE` when off |
| T14-07 | Include videos toggle affects next run | functional | P0 | 1. Turn `Include videos` off 2. Trigger `Backup now` | Next backup excludes video chunks; size smaller |
| T14-08 | Google account row shows alias, opens explainer | functional | P0 | 1. Connect Google account 2. Tap account row | Sheet shows alias (e.g., "alice@gmail.com"), `Disconnect` button; no token shown |
| T14-09 | Disconnect account removes local token, keeps cloud records | functional | P0 | 1. Tap account row → `Disconnect` 2. Confirm | Local token removed; cloud records remain; screen returns to S3 Content off; re-consent needed to re-enable |
| T14-10 | Backup key row opens explainer + biometric reveal | functional | P0 | 1. Tap `Backup key` 2. Authenticate (pattern/biometric) | Explainer sheet shown; key revealed after auth; mismatch on restore → S6 `Why?` |
| T14-11 | Restore on a new device → phrase gate → /restore/progress | functional | P0 | 1. Tap `Restore on a new device` 2. Enter valid recovery phrase | Navigates to `/restore/progress` with iris ring, "42 of 128" |
| T14-12 | Last 5 backups sheet shows time, size, status | functional | P0 | 1. Complete ≥2 backups 2. Tap `Last 5 backups` | Sheet lists up to 5 rows: timestamp, size (e.g., "1.8 GB"), status (mint/butter/danger dot); read-only |
| T14-13 | Last 5 backups sheet empty state | functional | P1 | 1. No backups exist 2. Tap `Last 5 backups` | Sheet shows "No backups yet" row (S4 inside sheet) |
| T14-14 | Delete cloud data confirms, queues durable delete | functional | P0 | 1. Tap `Delete cloud data` 2. Confirm dialog 3. Wait | Dialog: "Delete cloud data? Your vault stays on this device. This cannot be undone." → S15 committing → S3 Content off |
| T14-15 | Delete cloud data offline → queued pending | functional | P1 | 1. Go offline 2. Confirm delete | Row marked `Pending` with 2dp butter progress line; retries on reconnect; never silently drops |
| T14-16 | Disable backup switch → confirm dialog explains records remain | functional | P0 | 1. Turn `Enable backup` off 2. Confirm | Dialog: "Turn off backup? Your encrypted copies stay in Google Drive until you delete them." → switch off, records remain |
| T14-17 | Cancel disable keeps backup enabled | functional | P1 | 1. Turn switch off 2. Tap `Keep on` in dialog | Switch remains on; no state change |
| T14-18 | Retry on failure banner re-runs failed stage only | functional | P0 | 1. Induce auth failure (S6) 2. Tap `Retry` | Re-runs failed stage; if succeeds → mint ring; if repeats → S6 stays |
| T14-19 | Why? on failure banner opens plain-language cause sheet | functional | P0 | 1. Induce quota failure (S6) 2. Tap `Why?` | Sheet: "Google Drive is full. Free up space or turn off backup." — no technical detail |
| T14-20 | Metered banner shows Resume anyway / Wait for Wi-Fi | functional | P0 | 1. Enable backup on metered network 2. Trigger run | N3 banner: "Using mobile data" + `Resume anyway` + `Wait for Wi-Fi` |
| T14-21 | Resume anyway does one-shot override | functional | P1 | 1. On metered banner, tap `Resume anyway` | Current run proceeds on mobile data; next run respects Wi-Fi only |
| T14-22 | Offline banner queues retry, shows last backup time | functional | P0 | 1. Go offline 2. Open screen | Sky banner: "You're offline. This will retry when you're connected." + "Last backed up 3 days ago" |
| T14-23 | Battery saver pauses backup, says so once | functional | P1 | 1. Enable battery saver 2. Open screen | Solid surface; "Backup is paused while battery saver is on." shown once |
| T14-24 | Doze mode no visual change | functional | P2 | 1. Enter Doze (idle) 2. Open screen | No visual change; WorkManager defers |
| T14-25 | Low storage (≤500 MB) shows butter banner, continues | functional | P1 | 1. Fill storage to ≤500 MB free 2. Open screen | Butter banner: "Storage is almost full." backup continues |
| T14-26 | Full storage (≤50 MB) pauses backup, shows Free up space | functional | P0 | 1. Fill storage to ≤50 MB free 2. Open screen | Backup pauses; "This device is out of space. Free up space or connect a drive." + `Free up space` |
| T14-27 | Progress ring shows real "Encrypting N of M" never fake % | security | P0 | 1. Observe any backup run (S5/S15) | Ring sweep + `mono.m` percentage + "Encrypting 42 of 128" — counts from real scan |
| T14-28 | Announcement text matches state (TalkBack) | accessibility | P1 | 1. Enable TalkBack 2. Navigate each state | S3: "Backup complete, 2 hours ago, 1.8 gigabytes" S6: "Backup didn't finish. Your files are safe on this device." S8: "You're offline. This will retry when you're connected." |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T14-29 | No Play Services / Drive API → unsupported state | functional | P1 | 1. Run on device without Play Services 2. Open screen | Disabled toggle + explanation row: "Backup isn't available on this device." (S12) |
| T14-30 | Drive auth revoked in Google settings → S6 named cause | functional | P0 | 1. Enable backup 2. Revoke Drive access in Google settings 3. Trigger backup | S6: "Backup needs Drive access." + `Try again` (re-consent) |
| T14-31 | Quota exceeded → S6 named cause, never auto-delete | functional | P0 | 1. Fill Drive quota 2. Trigger backup | S6: "Google Drive is full. Free up space or turn off backup." + `Retry` + `Why?` |
| T14-32 | Two backups race (manual during scheduled) → second ignored | race | P1 | 1. Start scheduled run 2. Immediately tap `Backup now` | Second ignored; idempotency key prevents duplicate upload |
| T14-33 | G4 during staging → resumable, no duplicate backup | race | P0 | 1. Start backup 2. Kill process mid-staging 3. Relaunch | UI shows resumable state; already-uploaded chunks recognized; no duplicate backup |
| T14-34 | Clock skew future timestamp → "time unavailable" | edge | P1 | 1. Set last-backup timestamp in future 2. Open screen | Shows "Last backup time is unavailable" not "-3 hours"; scheduling uses elapsed time |
| T14-35 | Backup key mismatch on restore → S6 names key, never record ID | functional | P0 | 1. Corrupt backup key locally 2. Attempt restore | Restore fails: "Backup key doesn't match" + `Retry`; never deletes/overwrites cloud records |
| T14-36 | Storage fills mid-staging → pauses at real count | edge | P1 | 1. Start backup 2. Fill storage mid-run | Pauses at "Stopped at 40 of 128"; no partial item listed |
| T14-37 | Cancel during first run → already-uploaded chunks stay | functional | P1 | 1. Start first backup 2. Tap `Cancel` during S5 | Uploaded chunks remain; resumes on next run; no partial list shown |
| T14-38 | Cancel during incremental → uploaded chunks stay | functional | P1 | 1. Start incremental backup 2. Tap `Cancel` | Already-uploaded chunks stay; resumes next run |
| T14-39 | Failure banner copy exact per §24 | functional | P0 | 1. Induce any failure state | Banner: "Backup didn't finish. Your files are safe on this device." + `Retry` + `Why?` |
| T14-40 | Sync conflict banner not shown here (sync is separate screen) | negative | P2 | 1. Have sync conflict 2. Open backup screen | No sync conflict banner on this screen |
| T14-41 | Unknown pattern → no error, neutral empty vault (per §24) | negative | P2 | 1. Enter wrong pattern at lock 3. Open backup screen | Not applicable — backup screen requires unlock first |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T14-42 | No file names, vault labels, record IDs, Drive file IDs ever shown | security | P0 | 1. Inspect all states (S3, S5, S6, S15, sheet, dialogs) | Zero occurrences of content identifiers in UI, logs, or accessibility |
| T14-43 | No full account email shown (alias only) | security | P0 | 1. Connect account 2. Inspect account row and all dialogs | Only alias (e.g., "alice@gmail.com"); never OAuth token or full email in logs |
| T14-44 | No thumbnails, per-file sizes in any state | security | P0 | 1. Inspect progress, history sheet, restore | Only aggregate size ("1.8 GB") and real counts ("42 of 128") |
| T14-45 | Backup never blocks vault unlock/view/edit/import/capture | security | P0 | 1. Induce backup failure/stale 2. Use vault normally | Vault fully functional; backup failures non-blocking |
| T14-46 | FLAG_SECURE off for backup screen (per flow §5.1 rule 4) | security | P1 | 1. Check Window flags on `/settings/cloud/backup` | FLAG_SECURE = false; Hide previews in recents still applies |
| T14-47 | Notification text never includes file names/vault labels/counts | security | P0 | 1. Trigger backup notification (failed/stale) | Text: "Vaultaire backup needs attention." — no content metadata |
| T14-48 | Notification tap opens this screen after unlock, not file | security | P0 | 1. Tap backup notification 2. Unlock | Opens `/settings/cloud/backup` |
| T14-49 | Disable confirmation explains records remain until deletion | security | P0 | 1. Turn off backup 2. Read dialog | "Turn off backup? Your encrypted copies stay in Google Drive until you delete them." |
| T14-50 | Delete cloud data confirmation says vault stays on device | security | P0 | 1. Tap `Delete cloud data` 2. Read dialog | "Delete cloud data? Your vault stays on this device. This cannot be undone." |
| T14-51 | Backup key reveal requires biometric/pattern re-auth | security | P0 | 1. Tap `Backup key` without auth | Prompts for pattern/biometric before revealing key |
| T14-52 | Restore on new device requires recovery phrase (not pattern) | security | P0 | 1. Tap `Restore on a new device` | Phrase entry gate; pattern alone insufficient |
| T14-53 | Pattern-only restore works only on originating device (per gap 6) | security | P1 | 1. Attempt pattern restore on new device | Not offered; UI distinguishes phrase vs pattern restore |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T14-54 | Font scale 200% → rows grow, no label truncation | accessibility | P1 | 1. Set font scale 200% 2. Open screen | All rows grow; ring stays 72dp; no truncation; all controls reachable |
| T14-55 | TalkBack announces ring state + timestamp + size | accessibility | P0 | 1. Enable TalkBack 2. Focus ring | "Backup complete, 2 hours ago, 1.8 gigabytes" (mint) / "Backup is 3 days old" (butter) / "Backup didn't finish. Your files are safe on this device." (danger) |
| T14-56 | TalkBack on progress announces real counts | accessibility | P1 | 1. Enable TalkBack 2. Start backup | "Encrypting 42 of 128" announced live |
| T14-57 | Switches have accessible names + state | accessibility | P0 | 1. Enable TalkBack 2. Focus `Wi-Fi only` / `Include videos` / `Enable backup` | "Wi-Fi only, on, switch" / "Include videos, off, switch" / "Encrypted backup, off, switch" |
| T14-58 | Buttons have accessible names (Backup now, Retry, Why?) | accessibility | P0 | 1. Enable TalkBack 2. Focus each button | "Backup now, button" / "Retry, button" / "Why, button" |
| T14-59 | Dialogs trap focus, announce title + message | accessibility | P0 | 1. Open disable/delete confirm dialogs | Focus trapped; title + message announced; buttons reachable |
| T14-60 | Sheet (account, backup key, last 5) traps focus | accessibility | P1 | 1. Open each sheet with TalkBack | Focus trapped; close button announced; swipe dismiss works |
| T14-61 | Tablet (600–840dp) → two-pane list/detail with nav rail | adaptive | P1 | 1. Run on 720dp width 2. Open screen | Two-pane layout; nav rail; ring card in list pane |
| T14-62 | Desktop (>840dp) → three-pane, max 1040dp, nav rail | adaptive | P1 | 1. Run on 1000dp width 2. Open screen | Three-pane; content centered max 1040dp; nav rail |
| T14-63 | Foldable hinge-aware → pattern grid never across fold | adaptive | P2 | 1. Run on foldable 2. Open screen (not pattern grid) | N/A — no pattern grid on this screen; layout respects hinge |
| T14-64 | Landscape phone → single column, controls reachable | adaptive | P1 | 1. Rotate to landscape 2. Open screen | Single column; all controls reachable; no overlap |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T14-65 | Background → WorkManager continues, progress in-app only | lifecycle | P0 | 1. Start backup 2. Press Home 3. Reopen app | Progress still visible in-app; no notification progress |
| T14-66 | Screen off → WorkManager continues | lifecycle | P0 | 1. Start backup 2. Turn screen off 3. Turn on | Progress continues; UI updates on resume |
| T14-67 | G4 process death → relaunch → /lock → resumable state | lifecycle | P0 | 1. Start backup 2. Force-stop app 3. Relaunch | Lands on `/lock`; after unlock shows resumable state (S8/S5) |
| T14-68 | Rotate during backup → progress preserved | lifecycle | P1 | 1. Start backup 2. Rotate device | Ring sweep + counts continue; no reset |
| T14-69 | Split-screen / multi-window → layout adapts | lifecycle | P2 | 1. Enter split-screen 2. Open screen | Layout adapts to available width; no truncation |
| T14-70 | Cancel during S15 Committing → safe, uploaded chunks stay | race | P0 | 1. Tap `Cancel` during commit | Uploaded chunks remain; no partial item; resumes next run |
| T14-71 | Disable → confirm → G4 → relaunch → backup off | race | P1 | 1. Confirm disable 2. Kill process before commit 3. Relaunch | Backup remains enabled (commit not durably recorded) |
| T14-72 | Delete cloud data → confirm → G4 → relaunch → pending | race | P1 | 1. Confirm delete 2. Kill process 3. Relaunch | Row shows `Pending` with progress line; retries automatically |
| T14-73 | Metered → Resume anyway → G4 → relaunch → one-shot done | race | P1 | 1. Tap `Resume anyway` 2. Kill process 3. Relaunch | Current run completed or queued; next run respects Wi-Fi only |
| T14-74 | Offline → queued → reconnect → auto-retry | race | P0 | 1. Go offline during backup 2. Reconnect | Auto-retries; no user action needed; banner clears |
| T14-75 | Stale → Backup now → succeeds → mint ring | race | P0 | 1. Make backup stale (>48h) 2. Tap `Backup now` 3. Wait | Butter → ring sweep → mint ring, fresh timestamp |

## Security invariants to assert
- [ ] Never fake percentage — progress always "Encrypting N of M" from real scan
- [ ] Real counts only — "42 of 128" derived from actual chunk enumeration
- [ ] Failure banner always has cause + `Retry` + `Why?` — never generic "Something went wrong"
- [ ] Account alias shown, never token — OAuth token never in UI, logs, or accessibility
- [ ] Disable confirmation explains: "Your encrypted copies stay in Google Drive until you delete them."
- [ ] Delete confirmation explains: "Your vault stays on this device. This cannot be undone."
- [ ] No file names, vault labels, record IDs, Drive file IDs, per-file sizes in any state
- [ ] No full account email — alias only (e.g., "alice@gmail.com")
- [ ] No thumbnails in backup UI or history sheet
- [ ] Backup never blocks vault unlock, view, edit, import, capture
- [ ] FLAG_SECURE off for this screen; Hide previews in recents still applies
- [ ] Notification text: "Vaultaire backup needs attention." — no content metadata
- [ ] Backup key reveal gated by pattern/biometric re-auth
- [ ] Restore on new device requires recovery phrase, not pattern
- [ ] Clock skew future timestamp → "Last backup time is unavailable" not negative duration
- [ ] Storage full pauses at real count ("Stopped at 40 of 128"), no partial items
- [ ] Delete cloud data offline → queued `Pending`, never silently dropped
- [ ] Two backups race → second ignored via idempotency key, no duplicate upload
- [ ] G4 during staging → resumable, already-uploaded chunks recognized, no duplicate backup

## Gaps & contradictions
- **Allow mobile data persistence**: `flows/05-cloud-and-sharing.md §5.1` gap 1 — `Allow mobile data` has no home row; N3 banner shows `Resume anyway / Wait for Wi-Fi` but `screens.md §14` lists no persisted toggle. Must decide: per-run (banner only) or stored setting.
- **Backup-key reveal gate**: `flows/05-cloud-and-sharing.md §5.1` gap 4 — §14 shows explainer + reveal row but does not require biometric/pattern re-auth; recovery and duress do. Align with `04-security-settings` re-auth requirement.
- **Delete vs Disable overlap**: `flows/05-cloud-and-sharing.md §5.1` gap 5 — §14 lists both `Delete cloud data` and "disabling backup… existing cloud records remain until deletion." Clarify whether delete is available while backup is on and what it does to the last-5 list.
- **Pattern-only restore**: `flows/05-cloud-and-sharing.md §5.1` gap 6 — `encrypted-cloud-backup` claims pattern restore works only on originating device; UI has no copy distinguishing that from phrase restore on `/restore/progress`. Decide whether restore entry states the difference.
- **Unsupported state copy**: `screens.md §14` S12 says "Backup isn't available on this device." but `flows/05-cloud-and-sharing.md` S12 same. Consistent.
- **Last 5 backups sheet empty**: `flows/05-cloud-and-sharing.md` S4 inside sheet "No backups yet" — not in `screens.md §14`. Add to screens.md.
- **Metered banner actions**: `screens.md §14` lists no `Resume anyway` row; `flows/05-cloud-and-sharing.md` N3 has it. screens.md needs update.
- **Battery saver copy**: `flows/05-cloud-and-sharing.md` X2: "Backup is paused while battery saver is on." — not in screens.md §14. Add.
- **Doze no visual change**: `flows/05-cloud-and-sharing.md` X3 — not in screens.md §14. Document.
- **Sync conflict banner not on this screen**: `screens.md §14` has no sync conflict; correct per domain separation.