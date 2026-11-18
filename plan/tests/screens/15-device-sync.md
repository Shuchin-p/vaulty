# Device Sync — Test Cases
## Scope
screens.md §15 "Device Sync (Pro)" + flows/05-cloud-and-sharing.md §5.2 + device-sync/planning.md + plausible-deniability/planning.md + duress-mode/planning.md
## Preconditions
- Pro entitlement active (E2)
- 3 devices enrolled: This device (Pixel 9), Tablet (2h ago), Old phone (4d ago)
- Conflict banner visible: "Some changes merged. 3 items need review."
- Offline queue: 4 changes waiting
- Disconnect is local/reversible and NOT danger-styled
- Post-duress wipe: peer shows `Disconnected` with no coercion hints
## Test cases
### Happy path
| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T15-01 | Open sync screen with Pro entitlement | functional | P0 | 1. Unlock vault<br>2. Navigate to Settings → Device sync [PRO] | Sky card "3 devices · Synced 2m ago"; This device row "Pixel 9 · Active" with iris ring; Other devices rows "Tablet · 2h ago", "Old phone · 4d"; "Disconnect this device" secondary button |
| T15-02 | Enable sync for first time | functional | P0 | 1. On E2 device with backup on<br>2. Tap Enable sync switch<br>3. Confirm Google account | "Preparing first sync…" + real count → "Sync is on" announcement; count becomes 2+ |
| T15-03 | Add a second device via recovery phrase | functional | P0 | 1. Tap "Add a device"<br>2. Install on other device, enter recovery phrase | New `AuroraDeviceRow` appears; "2 devices" announced; no partial row if other device never completes |
| T15-04 | Rename device alias | functional | P1 | 1. Tap alias "Pixel 9"<br>2. Edit to "Phone"<br>3. Confirm | Inline edit; alias stored inside encrypted vault; "Phone · Active" shown; merge semantics apply on rename race |
| T15-05 | Disconnect this device | functional | P0 | 1. Tap "Disconnect this device"<br>2. Confirm in sheet (not danger styling)<br>3. Verify | Sheet confirms local + reversible; row removed; copy says local + reversible; peers continue syncing; cloud records unchanged; no peer notification |
| T15-06 | View activity log | functional | P1 | 1. Tap "Activity log" row<br>2. Verify list | Navigates to `/settings/cloud/sync/log`; shows encrypted local events: time, action, alias; no content identifiers |
| T15-07 | Offline changes queue and drain | functional | P0 | 1. Go offline<br>2. Add photo on this device<br>3. Go online | Sky banner "You're offline. 4 changes waiting."; no spinner; on reconnect encrypted outbox drains via WorkManager; merge runs order-independent |
| T15-08 | Conflict review flow | functional | P0 | 1. Tap conflict banner "Some changes merged. 3 items need review."<br>2. Review each item: Keep added / Keep deleted<br>3. Complete | Butter banner "Some changes merged. 3 items need review." → review list; per-item actions resolve; banner clears; vault converges |
| T15-09 | Ignore conflict → auto add-wins merge | functional | P1 | 1. Conflict banner present<br>2. Do nothing, wait for next sync cycle | Auto add-wins merge runs; banner persists until seen; identical chunks collapse; order-independent |

### Edge & error
| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T15-10 | Pro gate on E1/E3 device | functional | P0 | 1. Open sync screen on Free tier<br>2. Tap Enable sync | `AuroraProGate`: "Sync is a Pro feature" + `Try Pro` / `Not now`; existing shares stay usable |
| T15-11 | Backup disabled for this vault | error | P0 | 1. Enable sync with backup off | Danger banner "Enable encrypted Google Drive backup first" + `Retry` + `Why?`; deep link to backup settings |
| T15-12 | Different Google account on peer | error | P0 | 1. Peer uses different Google account<br>2. Attempt sync | Danger banner "Sync requires the same Google account on all devices" + `Retry` + `Why?` |
| T15-13 | Outdated app version on peer | error | P1 | 1. Peer runs outdated version<br>2. Sync runs | Danger banner "Update Vaultaire on all devices to continue sync" + Play Store link |
| T15-14 | Drive quota exceeded | error | P1 | 1. Drive storage full<br>2. Sync runs | Danger banner "Google Drive storage full — sync paused" + `Retry` + `Why?`; never auto-delete |
| T15-15 | Network unavailable | error | P0 | 1. Disable network<br>2. Open sync screen | Sky banner "You're offline. 4 changes waiting." + `Retry` (queued); "Waiting for connection…" auto-retry when online |
| T15-16 | No other devices enrolled | edge | P1 | 1. Only this device has sync on | Sky card "This device · Synced"; empty "Other devices" row; "Add a device" available; "This device only" announced; never an error |
| T15-17 | Peer offline 90 days | edge | P1 | 1. Peer last seen 90 days ago | Row shows `Offline · 90 d` (short form); no nudge to delete; on reconnect merges; adds win over unseen deletes |
| T15-18 | Clock skew — future timestamp | edge | P1 | 1. Peer reports future last-sync time | Shows "Last sync time unavailable"; never negative duration; scheduling uses elapsed time |
| T15-19 | Pro lapsed (E3) while enrolled | error | P0 | 1. Subscription expires<br>2. Open sync screen | Butter banner "Sync is paused." + `Resume` / `View data`; "Your Pro access has ended. Your data is still here." announced; local vault intact, cloud records preserved |
| T15-20 | Resubscribe after lapse | functional | P1 | 1. Resubscribe to Pro<br>2. Open sync screen | "Sync is on" announced; resumes sync; no data loss |
| T15-21 | Delete cloud data while offline | edge | P1 | 1. Go offline<br>2. Tap Delete cloud data<br>3. Go online | Row marked `Pending` with progress line; queued; never silently drops; executes on reconnect |
| T15-22 | Two syncs race (manual + scheduled) | race | P1 | 1. Trigger "Backup now" during scheduled run | Second ignored; no duplicate upload; idempotency key per run |

### Security & deniability
| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T15-23 | Never show device IDs | security | P0 | 1. Inspect UI on sync screen<br>2. Check activity log<br>3. Check network traffic | No device IDs, IPs, or full account emails anywhere; only count ("3 devices") and aliases (user-set, encrypted) |
| T15-24 | Disconnect is local/reversible, not danger-styled | security | P0 | 1. Tap "Disconnect this device"<br>2. Inspect confirm sheet | Sheet (not dialog); secondary styling; copy confirms local + reversible; no red/danger styling; no revocation language |
| T15-25 | Duress wipe on peer → invisible isolation | security | P0 | 1. Trigger duress on peer device<br>2. Observe this device | Peer becomes `Disconnected`; nothing about coercion, content, or count revealed; peer never auto-rejoins; sync continues on remaining devices |
| T15-26 | Duress wipe on this device | security | P0 | 1. Trigger duress pattern on this device | Silent multi-pass wipe of non-decoy files; sync engine paused/isolated; decoy vault opens; no confirmation, no warning, no progress |
| T15-27 | Conflict copy exact | security | P0 | 1. Trigger merge conflict<br>2. Verify banner text | Butter banner: "Some changes merged. 3 items need review." — exact copy from screens.md §15; announcement "Three items need review" |
| T15-28 | No recipient identity in sync | security | P0 | 1. Inspect all sync UI | No email, phone, account, or device field anywhere; sharing is phrase-based (separate flow) |
| T15-29 | Sync metadata never crosses network in plaintext | security | P0 | 1. Inspect network traffic | Sync metadata inside encrypted vault payload; ciphertext only; record names = random UUIDs; sizes padded to fixed buckets |
| T15-30 | Device enrollment key never synced | security | P0 | 1. Check cloud records | Device enrollment key stays device-only; generated on first setup; never in cloud |

### Accessibility & adaptive
| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T15-31 | Font scale 200% | accessibility | P1 | 1. Set font scale 200%<br>2. Open sync screen | Rows grow; no label truncation; all actions available; sky card and device rows reflow |
| T15-32 | TalkBack navigation | accessibility | P1 | 1. Enable TalkBack<br>2. Navigate sync screen | "3 devices, synced 2 minutes ago" announced; each device row announces alias + status; conflict banner announces "Three items need review" |
| T15-33 | Phone layout (V1) | adaptive | P1 | 1. Run on phone | 1 column; all rows scrollable; nav bottom bar |
| T15-34 | Tablet layout (V4) | adaptive | P1 | 1. Run on tablet | 2 columns + nav rail; sky card spans; device rows in grid |
| T15-35 | Foldable inner screen | adaptive | P1 | 1. Open on foldable inner display | Adapts to expanded width; 2+ columns; device rows use available space |
| T15-36 | Reduce motion | accessibility | P1 | 1. Enable reduce motion<br>2. Trigger conflict banner | No animation on banner appear; state changes instant; all transitions respect setting |
| T15-37 | Hide previews in recents | security | P1 | 1. Enable Hide previews<br>2. Background app | Recents shows `AuroraSecureSurface` brand cover; no sync status or device count leaked |

### Races & lifecycle
| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T15-38 | G4 during first sync | race | P0 | 1. Start first sync<br>2. Kill app (LMK)<br>3. Relaunch | Resumes via WorkManager; already-merged chunks not re-applied; UI shows resumable state; "Preparing first sync…" resolves; no duplicate backup |
| T15-39 | G4 during conflict review | race | P1 | 1. Open review list<br>2. Kill app<br>3. Relaunch | Review state preserved; conflict banner persists until seen; auto add-wins merge if ignored |
| T15-40 | Rename race — concurrent alias change | race | P1 | 1. Device A renames to "Phone"<br>2. Device B renames same alias to "Tablet"<br>3. Both sync | Last-writer-set wins; alias merges like any record (content); no conflict banner for alias alone |
| T15-41 | Merge semantics — add wins over unseen delete | race | P0 | 1. Device A offline: adds Photo A<br>2. Device B offline: deletes Photo X (had seen X), adds Photo B<br>3. Both online | Both converge: Photo A added, Photo X deleted (B had seen X), Photo B added; identical chunks collapse; order-independent |
| T15-42 | Identical file added twice → deduplicated | race | P0 | 1. Device A offline: adds Photo P<br>2. Device B offline: adds same Photo P (byte-identical)<br>3. Both sync | Single entry for Photo P; duplicate encrypted chunks evicted by cleanup |
| T15-43 | Metered connection — no auto sync | race | P1 | 1. On metered network<br>2. Trigger sync | No auto upload; queue count only; "Backup is paused on mobile data." banner with `Resume anyway` / `Wait for Wi-Fi` |
| T15-44 | Offline queue persists process death | race | P0 | 1. Make changes offline<br>2. Force stop app<br>3. Relaunch offline | Queued changes persist; sky banner shows correct count; drains on next connectivity |
| T15-45 | Locked session → brand cover | lifecycle | P0 | 1. Lock vault (timeout/back)<br>2. Observe sync screen | `AuroraSecureSurface` brand cover; no sync status or device count visible; unlock required |
| T15-46 | Foreign wipe observed (G6) | lifecycle | P1 | 1. Peer wiped via non-duress means<br>2. Observe this device | Peer simply shows `Disconnected`; nothing revealing; no auto-rejoin |
| T15-47 | Sync paused → background → resume | lifecycle | P1 | 1. Background app during sync<br>2. Foreground after 30s | WorkManager continues; progress visible in-app only; on foreground shows updated state |
| T15-48 | Cancel first sync during prepare | edge | P1 | 1. Enable sync<br>2. Tap Cancel during "Preparing first sync…" | Stays off; nothing uploaded; "Sync is off" announced; no partial state |
## Security invariants to assert
- [ ] Never shows device IDs, IPs, or full account emails anywhere (UI, logs, network)
- [ ] "Disconnect this device" is secondary styling, NOT danger/red; confirm sheet says local + reversible
- [ ] Duress isolation invisible: peer shows `Disconnected` with no coercion/content/count hints; never auto-rejoins
- [ ] Conflict banner copy exact: "Some changes merged. 3 items need review." (butter banner)
- [ ] Sync metadata never leaves device in plaintext; inside encrypted vault payload only
- [ ] Device enrollment key, pattern, VDK, MVK, recovery phrase never transmitted
- [ ] Device count only in UI ("3 devices"); no names/IDs in cloud
- [ ] Activity log entries never contain: file names, vault labels, collection names, IPs, record IDs, Drive file IDs, device IDs, account emails, per-item sizes
## Gaps & contradictions
- **Review list route missing**: screens.md §15 says "→ review list" but flows/05-cloud-and-sharing.md gap #2 notes no route defined for `/settings/cloud/sync/review` — needs sheet or new route
- **Allow mobile data persistence**: flows gap #1 — banner has `Resume anyway / Wait for Wi-Fi` but screens.md §14 lists no persistence toggle; decide per-run vs stored setting
- **Duress wipe sync isolation detail**: duress-mode/planning.md says "SyncEngine paused/isolated" but device-sync/planning.md AC-07 says "cut from sync" — clarify whether paused = stops upload/download locally or also revokes cloud tokens
- **Pro gate vs backup prerequisite**: flows §5.2 shows enable sync requires "backup on + account" but device-sync/planning.md US-05 says "use existing encrypted Google Drive backup as transport" — ensure backup enable deep-link works from sync error state
- **Activity log opt-in default**: flows §5.3 shows logging off by default but device-sync/planning.md §209 mentions "Activity log (optional, Pro)" — confirm default is off and opt-in persists