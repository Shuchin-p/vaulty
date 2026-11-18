# Global & System States

> Everything that can be true of the *device and session* rather than of one screen.
> Codes are defined in [`_TEMPLATE.md §2`](./_TEMPLATE.md); precedence and lifecycle in
> [`00-state-model.md`](./00-state-model.md).

Each section states: how it is detected, what changes, what must not change, and the copy.

---

## 1. Network — `N`

Vaultaire has **no content server**. Only three things ever touch the network: Google Drive
(backup, sync, sharing), Play Billing, and the optional opt-in crash report. Everything else
must be fully functional offline.

| Code | Detection | UI | Behavior change |
|---|---|---|---|
| `N1` Online | `NET_CAPABILITY_VALIDATED` | none | Transport operations may run |
| `N2` Offline | no validated network | Sky banner on transport screens only | Queue writes; never block browsing, viewing, editing, or unlocking |
| `N3` Metered | `NET_CAPABILITY_NOT_METERED` absent | Banner on backup/sync: "Using mobile data" | Backup waits unless "Allow mobile data" is on |
| `N4` Captive portal | `NET_CAPABILITY_CAPTIVE_PORTAL` | Banner: "Sign in to this network to sync." + `Open sign-in` | Treated as offline for transport; never opens the portal automatically |
| `N5` Airplane | radio off | Same as `N2` | Same as `N2` |

### 1.1 Where offline is allowed to say something

| Screen | Offline treatment |
|---|---|
| `/lock`, `/vault`, timeline, collection, viewer, search | **No banner.** Local data is the product. A queued row on the timeline may carry a small clock badge — that describes pending transport of that event, not whether the vault works. |
| `/vault/camera`, review, import | Nothing. Capture and import are local. |
| `/settings/cloud/backup` | Banner + `Last backed up 3 days ago` |
| `/settings/cloud/sync` | Banner + queued count |
| `/vault/share/create`, `/shared/open` | Blocking inline state: "Sharing needs a connection." |
| `/shares` | Banner; revoke is queued and marked `Pending` |
| `/pro` | Price area shows "Connect to see plans." No fake prices. |

### 1.2 Queued operations

| Operation | Queue | On reconnect |
|---|---|---|
| Backup | WorkManager, `NetworkType.UNMETERED` by default | Resumes; never duplicates |
| Sync | WorkManager + encrypted outbox | Merges; conflict rules apply |
| Revoke share | Durable queue | Applies; card moves from `Revoking…` to `Revoked` |
| Publish share update | Durable queue | Applies |
| Delete cloud backup | Durable queue | Applies |

Queue entries are **opaque** — no file names, no vault labels, no counts in WorkManager input
data.

### 1.3 Copy

- Offline transport: "You're offline. This will retry when you're connected."
- Captive portal: "This network needs a sign-in before it can sync."
- Metered: "Backup is paused on mobile data." `Resume anyway` / `Wait for Wi-Fi`
- Long offline (7+ days): "Last backup was 8 days ago." No alarm color; butter, not rose.

---

## 2. Storage — `D`

| Code | Threshold | Effect |
|---|---|---|
| `D1` Ok | > 500 MB free | Normal |
| `D2` Low | ≤ 500 MB free | Banner on camera/import/backup: "Storage is almost full." Writes proceed |
| `D3` Full | ≤ 50 MB free | Writes blocked at the **action**, reads unaffected |

### 2.1 Blocking matrix

| Action | D2 | D3 |
|---|---|---|
| Open / view / search / unlock | allowed | allowed |
| Take photo | allowed, banner | blocked before shutter: "Free up space to take photos." |
| Record video | warn at start, may stop early | blocked |
| Import | warn with estimated fit | blocked; `Free up space` |
| Backup staging | warn | backup pauses, no data loss |
| Rename / label / settings | allowed | allowed |
| Share create | allowed | allowed (share is metadata until upload) |

### 2.2 Rules

- Never delete user data to make room. Ever.
- Never silently downscale a capture to fit. Tell the user and let them choose.
- A write that fails for space mid-operation must leave **no partial item** and must say what
  happened: "Ran out of space at 8 of 12. The first 8 were saved."
- The storage figure is computed on demand, never polled while a screen is open.

---

## 3. Entitlement — `E`

| Code | Meaning |
|---|---|
| `E1` Free | Core features, backup default-on |
| `E2` Pro | All features |
| `E3` Pro expired | Gated features read-only |
| `E4` Purchase pending | Play Billing in flight or deferred payment |

### 3.1 Gate matrix

| Feature | E1 | E2 | E3 | E4 |
|---|---|---|---|---|
| Pattern, vaults, camera, import, recovery | full | full | full | full |
| Encrypted backup | full (free) | full | full | full |
| Duress mode — **configure** | gate | full | gate | gate |
| Duress mode — **already configured** | **stays active** | active | **stays active** | active |
| Device sync — new sync | gate | full | gate | gate |
| Device sync — existing | — | full | local data intact, syncing pauses, nothing deleted | full |
| Sharing — create | gate | full | gate | gate |
| Sharing — open, view, revoke | full | full | full | full |
| App icon — change | gate | full | gate | gate |
| App icon — **already changed** | **stays** | stays | **stays** | stays |

**Safety over revenue.** Duress and an active disguise icon are coercion-safety features. If
Pro lapses they keep working; only *changes* are gated. Flagged in `05` and `06`; do not
"correct" this without a product decision.

### 3.2 Gate presentation

| Situation | Presentation |
|---|---|
| User taps a gated row | `AuroraProGate` sheet: value sentence, `Try Pro`, `Not now` |
| Gated destination reached directly | Full-screen `S10` with the same copy |
| Gated action inside an otherwise free screen | Inline lock row; the rest of the screen works |
| E3 | Same gate, copy: "Your Pro access has ended. Your data is still here." |
| E4 | Neutral inline "Finishing purchase…" with a 60 s timeout → `Check again` |

Never: countdown, fake discount, blocked back, blurred user content, or a gate over the lock
screen.

---

## 4. Permissions — `P`

| Code | Meaning |
|---|---|
| `P1` Granted | Normal |
| `P2` Denied once | Rationale may be shown once more, then request |
| `P3` Permanently denied | No system dialog; only `Open settings` |
| `P4` Partial / limited | e.g. selected photos only; feature degraded but usable |
| `P5` Restricted by policy | Device admin / parental control; explain and stop |

### 4.1 Permission table

| Permission | Used by | `P2` | `P3` | Notes |
|---|---|---|---|---|
| `CAMERA` | `/vault/camera` | Rationale → request | `Open settings` | Never requested at launch |
| `RECORD_AUDIO` | video | Rationale → request | `Record without sound` stays available | Audio is optional by design |
| Photo picker | import | n/a | n/a | System picker; no storage permission on modern Android |
| `POST_NOTIFICATIONS` | backup, sync, shares | Asked after the first backup is enabled | Row in Settings explains | Never at launch |
| `USE_BIOMETRIC` | unlock shortcut | Optional prompt | Pattern remains | Never the only way in |

### 4.2 Request timing

1. Never during onboarding, splash, or the first unlock.
2. Only when the user performs the action that needs it.
3. One request per session per permission. A second denial is treated as `P3` in the UI even if
   the OS still allows a dialog — do not burn the user's patience.
4. Rationale is a sheet with one sentence and one action, not a full screen.
5. `Open settings` uses the app's own settings deep link, with a fallback if the OEM blocks it.

### 4.3 Copy

| Case | Copy |
|---|---|
| Camera rationale | "Vaultaire needs the camera to take photos into your vault. Nothing is saved to your gallery." |
| Mic rationale | "Sound is optional. You can record video without it." |
| Mic denied in camera | "Video will record without sound." (persistent butter pill) |
| Camera `P3` | "Camera access is off. You can still import photos." `Open settings` |
| Notifications | "Get a quiet notice when a backup finishes or a share expires." |

---

## 5. Viewport — `V`

| Code | Width | Layout |
|---|---|---|
| `V1` Phone portrait | < 600 dp | Single column, floating nav, FAB |
| `V2` Phone landscape | < 600 dp, height < width | Nav hides; camera controls move to the right edge |
| `V3` Foldable | varies | Hinge-aware; never split the pattern grid |
| `V4` Tablet | 600–839 dp | Nav rail, 2-pane list/detail, 720 dp content cap |
| `V5` Desktop / freeform | ≥ 840 dp | Rail, 3-pane, 1040 dp cap, keyboard + pointer |
| `V6` Multi-window | any | Minimum 320 × 480 dp; degrade gracefully below |

### 5.1 Rules

- **Pattern grid** is always centered in one pane; 280 / 320 / 360 dp by width class.
- **Camera** in a window < 360 dp wide shows a single-column control strip; the shutter stays
  ≥ 76 dp and bottom-right.
- **Sheets** become side panels at ≥ 840 dp; dialogs stay dialogs.
- **Lists** keep a stable scroll anchor on resize; do not jump to top on rotation.
- **Multi-window**: losing window focus does **not** lock. Only background beyond the
  auto-lock threshold does.
- **Freeform resize below minimum**: letterbox with a neutral message rather than reflow.

---

## 6. Accessibility — `A`

| Code | What changes |
|---|---|
| `A1` Default | Baseline |
| `A2` TalkBack | Grid becomes one node + explore-by-touch dots; every state change announced; phrase warning before reading |
| `A3` Switch access | Linear focus order; no drag-only gesture is the only path |
| `A4` 200% font | Nav labels hide, hero stats 2-up, buttons grow to 60 dp, nothing truncates a primary action |
| `A5` Reduced motion | Ambient off, springs → 120 ms fades, shake → 200 ms border pulse; duress still instant |
| `A6` RTL | Grid column order mirrors, chevrons mirror, numbers stay LTR |

**Drag-only is banned** as a sole input: the pattern grid must accept discrete taps
(TalkBack double-tap per dot) and the camera must expose every control as a button.

---

## 7. Device pressure — `X`

| Code | Detection | Effect |
|---|---|---|
| `X1` Normal | — | Glass mode allowed |
| `X2` Battery saver | `PowerManager.isPowerSaveMode` | Auto-switch to **Solid**, ambient off, backup deferred; **say so once**, do not nag |
| `X3` Doze | `PowerManager.isDeviceIdleMode` | Defer WorkManager jobs; no UI change |
| `X4` Thermal throttled | `PowerManager.getThermalStatus` | Camera drops to lower resolution/fps; recording may stop with a reason |
| `X5` Screen capture | `MediaProjection` active, or capture attempt on a secure screen | Secure screens render normally for the user but are excluded from capture; neutral notice only if the user tried to capture |

### 7.1 Rules

- Solid mode is a **legitimate mode**, not an error: never label it "reduced" or "basic".
- When the system forces Solid, Appearance shows "Solid (set by battery saver)" and the user's
  stored preference is preserved for when it clears.
- Thermal copy: "The device is warm. Recording stopped to protect it. Saved up to 0:42."
- `X5` never accuses the user. Copy: "Screen capture is blocked on this screen." One line.

---

## 8. Lifecycle surfaces

### 8.1 Recents / app switcher

| Screen class | Snapshot |
|---|---|
| Secure screens (lock grid, camera, review, viewer, phrase, duress confirm, shared vault) | Solid brand cover, captured **before** content draws |
| Home, timeline, settings, shares | Blurred snapshot by default; solid if "Hide previews" is on |
| During `G6` duress | Brand cover; identical to a normal lock |

Implementation: set `FLAG_SECURE` before the first frame on secure destinations, and set a solid
`window` background in `onPause` for non-secure ones.

### 8.2 Biometric prompt

- Optional shortcut only; the pattern is always available on the same screen.
- Fail → one retry → fall back to pattern, no lockout for biometrics alone.
- Enrollment change invalidates the wrapped key → pattern required, then re-offer enrollment.
- Never show biometric over a sheet; dismiss the sheet first.

### 8.3 Predictive back

- Previews the *pop*, never the content of a secure destination.
- From `/lock`, back backgrounds the app.
- From an in-flight destructive confirm, back cancels the confirm.
- From `S15 Committing`, back is either ignored (with a reason) or cancels — decide per action,
  never sometimes.

### 8.4 Notification permission

Asked only after the first backup is enabled or the first share is created. Denial does not
degrade anything except delivery of those notices; the in-app surfaces still show state.

---

## 9. Global state × screen matrix

Rows are representative destinations. Cells describe what the user sees; "—" means no change.

| Screen | `S9` Locked | `N2` Offline | `D3` Full | `E3` Lapsed | `P3` Denied | `A4` 200% | `V6` Multi-window | `X5` Capture |
|---|---|---|---|---|---|---|---|---|
| `/lock` | — (is lock) | — | — | — | — | Grid shrinks, never scrolls | Centered single pane | Blocked, no notice |
| `/vault` | Cover | — | Banner on writes only | — | — | 2-up tiles, labels hidden | 2-pane at ≥ 600 | Blurred snapshot |
| `/vault/timeline` | Cover | Queued badges | — | — | — | Chips scroll, 5 days | Rail collapses | Blurred |
| `/vault/file/{id}` | Cover | — | — | — | — | Metadata wraps | Letterboxed media | Blocked |
| `/vault/camera` | Cover | — | Shutter blocked at `D3` | — | Rationale | Controls enlarge | Single column | Blocked |
| `/vault/import` | Cover | — | Blocked + estimate | — | — | Rows grow | Full width | Blurred |
| `/settings/cloud/backup` | Cover | Banner + last backup | Pauses staging | — | Notif row | Rows wrap | Side panel | Blurred |
| `/settings/cloud/sync` | Cover | Banner + queue | — | Local only notice | — | Rows wrap | Side panel | Blurred |
| `/vault/share/create` | Cover | Blocking inline | — | Gate | — | Stepper wraps | Side panel | Blocked |
| `/shared/{id}` | Cover | Cached files only | — | — | — | Metadata wraps | 2-pane | Blocked |
| `/pro` | Dismissed | Prices unavailable | — | — | — | Plans stack | Centered 480 dp | Blurred |
| `/recovery/display` | Cover | — | — | — | — | Words wrap per tile | Single centered column | Blocked |

---

## 10. Global copy table

| Situation | Copy | Tone |
|---|---|---|
| Offline (transport) | "You're offline. This will retry when you're connected." | Neutral |
| Offline (share) | "Sharing needs a connection. Your vault is unaffected." | Neutral |
| Low storage | "Storage is almost full." | Informative |
| Full storage | "This device is out of space. Free up space to keep going." | Informative |
| Pro lapsed | "Your Pro access has ended. Your data is still here." | Reassuring |
| Permission denied | "Camera access is off. You can still import photos." | Alternative-first |
| Rate limited | "Please wait a moment." | Neutral, never accusatory |
| Thermal stop | "The device is warm. Recording stopped to protect it." | Factual |
| Capture blocked | "Screen capture is blocked on this screen." | Factual |
| Fatal | "This screen couldn't open. Your vault is safe." + `Back to vault` | Reassuring |
| Corrupt item | "We couldn't open this item. It may be damaged." | Non-technical |

**Banned words:** oops, uh-oh, error code (unless in a collapsed detail), "military-grade",
"unbreakable", "100% secure", "failed" without a next action.

---

## 11. Detection & test hooks

| State | Observed via |
|---|---|
| `N1–N5` | `ConnectivityManager` + `NetworkCapabilities`, `registerDefaultNetworkCallback` |
| `D1–D3` | `StorageManager.getAllocatableBytes` + free-space query on demand |
| `E1–E4` | `BillingClient.queryPurchasesAsync` at session start, then cached; reconciled on resume |
| `P1–P5` | `checkSelfPermission` + `shouldShowRequestPermissionRationale`; `P3` inferred after two denials |
| `V1–V6` | `WindowMetricsCalculator` + `WindowInfoTracker` |
| `A1–A6` | `AccessibilityManager`, `Settings.Global.ANIMATOR_DURATION_SCALE`, `layoutDirection` |
| `X2–X4` | `PowerManager`, `getThermalStatus` (API 29+) |
| `X5` | `MediaProjectionManager` callbacks; `FLAG_SECURE` attempt detection is best-effort |

All of these are injectable in debug via the state injector
([`00-state-model.md §10`](./00-state-model.md)). Production has no toggles, no remote flags,
and no telemetry.
