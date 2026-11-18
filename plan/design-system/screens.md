# Aurora Screen Blueprints

> Every user-facing screen implied by `plan/`, mapped to the Aurora system.
> Layouts are portrait-phone first; adaptive behavior is in §12.

**Routes** are suggestions, not a navigation contract. **Secure** means the screen must run under `AuroraSecureSurface` (FLAG_SECURE + solid panel).

---

## 0. Screen map

| Group | Screens |
|---|---|
| **Entry** | **1** Entry → **2** Onboarding (pattern → confirm → phrase → done) · **3** Pattern Lock (unlock · duress) |
| **Vault** | **4** Home · **5** Timeline · **6** Collection · **7** Search · **8** Viewer · **9** Selection · Camera + Review (`components.md §6.2–6.3`; behavior in `flows/03`) · Import (`§27.3`) |
| **Settings** | **10** Hub → **11** Security · **12** Duress · **13** Hidden vaults · **14** Backup · **15** Sync · **16** Sharing · **17** App icons · **18** Appearance · **19** About / Privacy |
| **Recovery** | **20** Phrase display · **21** Phrase entry · **22** Reset pattern |
| **Sharing** | **16.1** Create · **16.2** Phrase display · **16.3** Open · **16.4** Shared vault · **28** My Shares |
| **Pro / system** | **23** Paywall · **24** Empty / error / system states · **25** Notifications · **26** Widgets and QSS |
| **Restore / import** | **27** Restore entry · **27.1** Restore progress · **27.3** Gallery import |
| **Security depth** | **29** Decoy guidance · **30** Privacy and Security info · **31** Sync activity log · **32** Adaptive layouts · **33** Screen-level security matrix |

Camera and review are component-specified (`components.md §6.2–6.3`) rather than numbered here, so the
existing cross-references stay stable. Their full state behavior lives in [`flows/03`](./flows/03-camera-and-notifications.md).

---

## 1. Entry / Splash

**Route:** `/` · **Secure:** no · **Pro:** no

**Purpose:** 300ms maximum. Resolve the active launcher alias, check for a vault index, route. No logo animation longer than a breath.

```
┌──────────────────────────────┐
│                              │
│           ( orbit )          │   canvas + bloom only
│                              │
│           Vaultaire          │   headline.l, ink.900
│        (no tagline)          │
│                              │
└──────────────────────────────┘
```

- If a disguise icon is active, show **no Vaultaire branding** — just the canvas.
- Never show "Unlocking…" or a progress bar; KDF is fast after unlock, and a progress bar implies variable security.
- **State:** cold, warm (skip splash), restore-from-backup (straight to phrase entry).

---

## 2. Onboarding

**Route:** `/onboarding/pattern` · **Secure:** phrase step only · **Pro:** no

Three steps, no account, no email, no wizard chrome.

| Step | Screen | Copy | Action |
|---|---|---|---|
| 1 | Pattern create | "Draw a pattern you'll remember." | 5×5 grid; continue enabled at 6 dots |
| 2 | Pattern confirm | "Draw it again." | mismatch = shake + retry, no penalty |
| 3 | Recovery phrase | "This is your only way back in." | 9-word solid panel, save confirmation |
| 4 | Home | "Your vault is ready." | Primary: `Add photos`; ghost: `Take a photo` |

**Rules**
- No "Skip" on the phrase step. Leaving opens a confirm dialog with an explicit "I understand I can lose access" checkbox.
- No marketing carousel. No permissions requested until the user taps the camera or import.
- Progress is 3 dots (`ink.200`, active ink pill 20dp), not a percentage.
- Background uses `gradient.dawn` for warmth; the phrase step switches to `Plain` + solid card.
- Duress / hidden vaults are **not** introduced during onboarding.

---

## 3. Pattern Lock

**Route:** `/lock` · **Secure:** grid area · **Pro:** no

The most important screen. It must feel like the phone's own lock screen.

```
┌──────────────────────────────┐
│  [avatar]            [help]  │   top bar, no title
│                              │
│        Welcome back          │   display.l, centered
│      Draw your pattern       │   body.m, ink.600
│                              │
│        ┌────────────┐        │
│        │  · · · · · │        │
│        │  · · · · · │        │   5×5 grid, 280dp
│        │  · · · · · │        │
│        │  · · · · · │        │
│        │  · · · · · │        │
│        └────────────┘        │
│                              │
│        Forgot pattern?       │   ghost, centered
└──────────────────────────────┘
```

**States**

| State | Behavior |
|---|---|
| Idle | No error text. No vault name. No count. |
| Drawing | Dots enlarge, iris line, light haptic per dot |
| Validating | Grid holds, subtle iris pulse; no spinner before 400ms |
| Resolved | 380ms iris glow, then content fades in; nav enters after 120ms |
| Unknown pattern | **Same 380ms glow and same navigation into a neutral empty vault.** No shake, no red, no error, no counter |
| Rate-limited | "Please wait a moment." + `mono.m` countdown; neutral ink, never danger; grid disabled |
| Duress | **Pixel-identical to Resolved.** No confirmation, no delay, no badge, no unique haptic |
| Creation validation | Only place error styling is allowed: < 6 dots or no turn → inline hint under the grid |

### 3.1 Pattern outcome policy (resolves a plan contradiction)

The plan contains two incompatible behaviors:

- `pattern-encryption` specifies a visible "Incorrect pattern" error + counter on GCM tag failure.
- `hidden-vaults` and `plausible-deniability` require that an unknown pattern reveal **nothing** — otherwise an attacker learns that a pattern does *not* open another vault, and the empty-state/deterrent model collapses.

**Aurora ships the deniability-safe behavior as canonical:**

1. A pattern that matches no vault resolves to the **same neutral empty vault** as a genuinely new vault. Same glow, same transition, same home, same empty state.
2. **No shake, no red, no "incorrect pattern", no failed-attempt counter on unlock.** Error styling (`danger`, shake) is reserved for *pattern creation* (too few dots / no turn) and for destructive confirmations.
3. Rate limiting may exist to stop rapid brute force, but its copy is neutral ("Please wait a moment.") and must not imply that a specific pattern was wrong.
4. A typo therefore opens an empty vault rather than scolding the user. This is an accepted usability cost; the user can lock and redraw. It is the price of the deniability claim.
5. The legacy error variant exists in `PatternFeedback` for research builds only and **must not ship** in any build that claims plausible deniability.

> If product decides to drop plausible deniability, switch `PatternFeedback` to `LegacyError` in one place. Do not mix the two behaviors per screen.

**Copy rules**
- Unknown: no copy at all. The empty vault says only "Nothing here yet."
- Rate-limited: "Please wait a moment." — never "Too many attempts" if that implies a known failure count.
- Forgot: neutral; opens phrase entry. Do not say "recover your vault" if the pattern may address no vault.
- The help icon opens a sheet on pattern safety (shoulder surfing, screen protectors), not account recovery.

**Security**
- FLAG_SECURE over the grid; recents shows a solid cover.
- No pattern preview, no "show pattern" toggle, no pattern hints.
- Attempt count is stored encrypted and survives restart.
- Screen dims to 10% after 30s of inactivity; never shows content.

---

## 4. Vault Home

**Route:** `/vault` → resolved session only · **Secure:** no · **Pro:** no

The reference screen. Greeting, hero, quick filters, recent media, calm stats.

```
┌──────────────────────────────┐
│ (avatar)              (search)(more) │
│ Welcome back                 │   overline
│ Vault                        │   display.l (generic title)
│                              │
│ (filter) [All] [Photos] [Docs] [Videos] │
│                              │
│ ┌──────────────────────────┐ │
│ │ Kyoto, 2024              │ │   hero card 34xl
│ │ The quiet side of Japan. │ │
│ │                          │ │   art: gradient / blurred cover
│ │ 24       10       1.8 GB │ │   stat row
│ │ Photos   Videos   Used   │ │
│ │                     (→)  │ │   44dp ink circle
│ └──────────────────────────┘ │
│                              │
│ Recent                See all│
│ ┌────────┐ ┌────────┐        │
│ │ media  │ │ media  │        │   2-up media tiles
│ └────────┘ └────────┘        │
│ ┌──────────────────────────┐ │
│ │ Backed up · 2 hours ago  │ │   mint banner, dismissible
│ └──────────────────────────┘ │
│      [ floating nav + fab ]  │
└──────────────────────────────┘
```

**Components:** top bar, filter chips, hero card, stat blocks, media grid, status banner, nav bar, FAB.
**States**
- Empty: neutral `AuroraEmptyState` — "Nothing here yet." + `Add photos` / `Take a photo`. **Never** "This vault doesn't exist."
- New vault: same empty state, no "vault created" celebration.
- Offline: nothing changes. The vault is local; only cloud-dependent banners age in place.
- Backup off: no banner on home; backup lives in settings.
- Locked: transitions to lock, no flash of content.

**Rules**
- The **vault title is always generic: "Vault".** Never render a vault ID, pattern, label, count, or hash. Collection/album names ("Kyoto, 2024") are encrypted content and are allowed; they must not encode vault identity.
- Do not add a vault switcher, a vault count, or a "which vault is this?" affordance.
- The greeting is time-aware but never contains the user's name unless set in Appearance → Profile (stored locally, never uploaded).
- Only one hero. If the vault has < 4 items, the hero becomes the empty-state card.
- No "create vault" button anywhere. A new pattern is the only way to create a vault, and that happens at the lock screen.

---

## 5. Timeline

**Route:** `/vault/timeline` · **Secure:** no · **Pro:** no

The calendar/timeline screen from the reference. Groups activity and media by day and hour.

```
┌──────────────────────────────┐
│ June, 2026            ( + )  │   display.m + 44dp glass circle
│ (Mon)(Tue)(Wed)(Thu)(Fri)(Sat)(Sun) │
│  11   12   13   14  [15]  16  17 │   selected = ink circle
│                              │
│ 10 AM  ┌──────────────────┐  │
│        │ Imported 12 photos │ │   rose tint card
│        │ From Camera        │ │
│        │            (⋯)    │ │
│        └──────────────────┘  │
│ 11 AM  ┌──────────────────┐  │
│        │ Backed up         │ │   mint tint card
│        │ 2 hours ago       │ │
│        └──────────────────┘  │
│ 1 PM   ┌──────────────────┐  │
│        │ Shared album      │ │   sky tint card
│        │ Expires in 7 days │ │
│        └──────────────────┘  │
│      [ floating nav ]        │
└──────────────────────────────┘
```

**Components:** month header, calendar strip, timeline rail, event cards, FAB.
**States:** empty day (single "No activity" row, no illustration), loading skeleton, offline (queued events show a small clock badge).
**Rules**
- Activity never reveals file names on the timeline if "Hide previews" is on; show counts only.
- Sharing events never show the recipient phrase or email.
- Tapping an event opens the relevant file/collection/share; no dead rows.
- Month sheet: 3 months max scroll-back before lazy loading.

---

## 6. Collection (Vault Detail)

**Route:** `/vault/collection/{id}` · **Secure:** no · **Pro:** no

Reference screen 2. A named set inside a vault: trip, project, album.

```
┌──────────────────────────────┐
│ (back)              (share)(more) │
│ Collection                   │   overline
│ Kyoto, 2024                  │   display.m
│                              │
│ (All) (Photos) (Videos) (Docs) │
│                              │
│ ┌──────────────────────────┐ │
│ │  [ cover art / 3-up ]    │ │
│ │  Summer in Kyoto         │ │   hero 220dp
│ │  24 items · 1.2 GB       │ │
│ └──────────────────────────┘ │
│                              │
│ Files                   12:16│
│ ┌──────────────────────────┐ │
│ │ (icon) Fushimi Inari     │ │   64dp rows
│ │        Photo · 4.2 MB    │ │
│ ├──────────────────────────┤ │
│ │ (icon) Gion at dusk      │ │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

**States:** empty collection, read-only shared collection (no edit/delete, share badge), Pro-locked share button.
**Rules**
- Hero art is blurred user media with a 24% ink scrim if a title overlays it.
- Counts use `mono.m`; do not animate them.
- The more menu: Rename, Add to, Export (if allowed), Delete. Delete is last, danger.
- If the collection came from a share, the header shows a 28dp `Read-only` status pill; no other visual difference.

---

## 7. Search

**Route:** `/vault/search` · **Secure:** no · **Pro:** no

```
┌──────────────────────────────┐
│ (back) [ Search this vault  ]│   focused field
│                              │
│ Filters                      │
│ (Photos) (Videos) (Docs) (Dates) │
│                              │
│ Results · 18                 │
│ ┌────────┐ ┌────────┐        │
│ │        │ │        │        │
│ └────────┘ └────────┘        │
└──────────────────────────────┘
```

- Local index only. No network call, no cloud suggestion, no "Search the web".
- Query is not persisted in plaintext; recent searches are opt-in and stored encrypted.
- Empty: "No matches in this vault." + `Clear filters`.
- While typing, results update after 120ms debounce; skeletons only if > 300ms.
- Search never indexes secret phrases, share phrases, or pattern data.

---

## 8. File Viewer

**Route:** `/vault/file/{id}` · **Secure:** yes (media) · **Pro:** no

```
┌──────────────────────────────┐
│ (back)   IMG_0421        (more) │
│                              │
│                              │
│          [ media ]           │   black or blurred ambient bg
│                              │
│                              │
│ ──────────────────────────── │
│ 4.2 MB · 4032×3024 · Nov 14  │   mono.s
│ (info)   (share)   (export)   (delete) │
└──────────────────────────────┘
```

- Double-tap zoom 1–5×; swipe down to dismiss with a 0.85 scale backdrop; video uses a 44dp play overlay and a 4dp scrubber.
- Bottom actions are 44dp glass circles; **Delete is separated** by a 12dp gap and uses danger.
- FLAG_SECURE while viewing; recents shows a solid cover.
- Trackpad/mouse: wheel zoom, arrow keys navigate between files, `Esc` closes.
- No filename exposure to the system share sheet beyond an explicit Export.

---

## 9. Selection Mode

**Route:** state on `/vault` · **Secure:** no · **Pro:** no

```
┌──────────────────────────────┐
│ (close)   3 selected    (all)│
│ ┌────┐ ┌────┐ ┌────┐         │   iris check + border
│ └────┘ └────┘ └────┘         │
│                              │
│ [ Share ] [ Add to ] [ Delete ] │  floating ink bar
└──────────────────────────────┘
```

- Long-press enters selection with `EFFECT_CLICK`; back exits.
- The floating action bar uses `surface.ink`, 36dp radius, 12dp above nav.
- Delete opens a count-aware dialog: "Delete 3 items? This can't be undone." Never batch-delete without confirmation.
- Share is Pro-gated; tapping shows `AuroraProGate`, not a disabled button.

---

## 10. Settings Hub

**Route:** `/settings` · **Secure:** no

```
┌──────────────────────────────┐
│ (back) Settings              │   headline.l
│ ┌──────────────────────────┐ │
│ │ (avatar) Vault           │ │   profile card
│ │          Pro · 2.1 GB    │ │
│ └──────────────────────────┘ │
│ Security                  →  │   group header
│ ┌──────────────────────────┐ │
│ │ Pattern & recovery       │ │
│ │ Duress mode        [PRO] │ │
│ │ Hidden vaults            │ │
│ └──────────────────────────┘ │
│ Cloud                        │
│ ┌──────────────────────────┐ │
│ │ Encrypted backup  [On]   │ │
│ │ Device sync      [PRO]   │ │
│ │ Storage          2.1 GB  │ │
│ └──────────────────────────┘ │
│ Appearance                   │
│ │ App icon · Theme · Motion│ │
│ About · Privacy · Licenses   │
│             v1.0 (build)     │
└──────────────────────────────┘
```

- Group cards use `AuroraListGroup`; no nested cards.
- Pro rows keep their chevron and open a value sheet.
- The version row is long-pressable for diagnostics (never exposes keys).
- No account row, no sign-out, no email. This is a feature; do not add one.

---

## 11. Security Settings

**Route:** `/settings/security`

Rows: Change pattern · Recovery phrase · Duress mode (Pro) · Hidden vaults · Auto-lock (immediately / 30s / 1m / 5m) · Attempt throttle · Screen capture (locked on for secure screens) · Clipboard auto-clear (30s / 60s / off) · Biometric unlock (if enabled; pattern always remains).

- Changing the pattern requires the current pattern, then redraw twice. It generates a **new recovery phrase**; show a full-screen interstitial before starting.
- Recovery phrase row: `Reveal` requires biometric or pattern re-entry; opens the secure phrase screen.
- Duress row: neutral. No red. Setup opens §12.
- Hidden vaults row: explainer only; no list of hidden vaults.

---

## 12. Duress Setup (Pro)

**Route:** `/settings/security/duress` · **Secure:** yes (confirmation)

```
┌──────────────────────────────┐
│ (back) Duress mode     [PRO] │
│                              │
│ ┌──────────────────────────┐ │
│ │ If you're forced to      │ │   rose tint, body.m
│ │ unlock, this pattern     │ │
│ │ opens one vault and      │ │
│ │ erases the rest.         │ │
│ └──────────────────────────┘ │
│ Choose the vault to open     │
│ Draw its pattern to confirm  │
│ [ 5×5 grid, compact ]        │   no list, no count
│                              │
│ What gets erased             │
│ • All other vault indexes    │
│ • All other files            │
│ • Local sync state           │
│                              │
│ [ Hold to enable duress ]    │   ink pill, 2s hold
└──────────────────────────────┘
```

- The setup screen may use rose and explicit language. **The unlock screen may not.**
- **Never list vaults.** The decoy is designated by redrawing its pattern (or selecting "the vault I just opened"), not by choosing from a catalog. A picker would leak the vault count that the whole feature is designed to hide.
- Hold-to-confirm with a 2s iris sweep; no red panic button.
- After enabling, the setting row is indistinguishable in weight from other security rows.
- Turning it off requires pattern + biometric; copy states that erasure history cannot be undone.
- Never show which vault is the decoy anywhere outside this screen. No debug chip, no analytics event with vault ID.

---

## 13. Hidden Vaults Explainer

**Route:** `/settings/security/hidden-vaults` · **Secure:** no

- Pure content screen: what "hidden" means, what it does **not** mean (forensic limits), and how different patterns resolve different vaults.
- One honest-limits card in `butter`, non-dismissible: "A forensic examiner with file access may still estimate how many vaults exist. Hidden vaults hide the catalog, not the storage."
- No list of vaults, no count, no "manage" button.
- No rename action. A user-editable vault name would recreate the plaintext catalog the feature removes; collection names already provide organization.

---

## 14. Encrypted Backup

**Route:** `/settings/cloud/backup`

```
┌──────────────────────────────┐
│ (back) Encrypted backup      │
│ ┌──────────────────────────┐ │
│ │   ●  Backed up           │ │   mint ring 72dp
│ │      2 hours ago · 1.8 GB│ │
│ │      Google Drive        │ │
│ └──────────────────────────┘ │
│ Backup now            (→)    │
│ Wi-Fi only            [on]   │
│ Include videos        [on]   │
│ Backup key            (→)    │   explainer + reveal
│ Restore on a new device (→)  │
│ Last 5 backups        (→)    │
└──────────────────────────────┘
```

- Ring color: mint healthy, butter stale > 48h, danger failed. Always with text.
- Progress: ring sweep + `mono.m` percentage + "Encrypting 42 of 128" — never a fake percentage.
- Failure banner: cause + `Retry` + `Why?`; never a generic "Something went wrong."
- Disabling backup requires confirmation and explains that existing cloud records remain until deletion.
- Google account row: only if connected; shows the account alias, never the token.

---

## 15. Device Sync (Pro)

**Route:** `/settings/cloud/sync`

```
┌──────────────────────────────┐
│ (back) Device sync     [PRO] │
│ ┌──────────────────────────┐ │
│ │ 3 devices · Synced 2m ago│ │   sky tint
│ └──────────────────────────┘ │
│ This device                  │
│ ┌──────────────────────────┐ │
│ │ (icon) Pixel 9 · Active  │ │   iris ring
│ └──────────────────────────┘ │
│ Other devices                │
│ │ (icon) Tablet · 2h ago   │ │
│ │ (icon) Old phone · 4d    │ │
│ [ Disconnect this device ]   │   secondary, not danger
└──────────────────────────────┘
```

- "Disconnect this device" is local and reversible; copy says so. It is not a red destructive action.
- Conflict state: butter banner "Some changes merged. 3 items need review." → review list.
- Offline: sky banner, queued count, no spinner.
- Duress isolation is invisible; after a duress wipe the device simply appears disconnected on other devices.
- Never show device IDs, IPs, or full account emails.

---

## 16. Secure Sharing

### 16.1 Create — `/vault/share/create`

```
(back) Share this vault              [PRO]
Choose what to share
( ) Entire vault
(•) Selected items (3)
Access
Expires        ( 7 days ▾ )
Max opens      ( 3 ▾ )
Export         [ off ]
[ Create share ]
```

- Policy controls are `AuroraListRow` with trailing value + chevron; open a sheet, not a dropdown.
- "Export" off is the default and shows a one-line explanation.
- Primary button disabled until a selection exists; helper text explains why.
- No recipient email/account field. Sharing is phrase-based by design.

### 16.2 Phrase display — secure

- 12 word tiles, 2×6, solid, FLAG_SECURE.
- "Send this phrase to the recipient through a different channel." + `Copy` (60s auto-clear) and `Share` (system sheet with phrase **not** included by default).
- Checkbox "I've sent it" → Done. Leaving prompts confirmation.
- Phrase is never shown again; "Regenerate" invalidates the previous one with a warning.

### 16.3 Open — `/shared/open`

```
(back) Open a shared vault
[ 12-word phrase entry, 2×6 ]
[ Open ]
```
- Paste requires a deliberate hold; no clipboard suggestions.
- Wrong phrase: generic "That phrase didn't work." No hint about expiry vs. revocation vs. typo.
- Expired/revoked: "This share is no longer available." Same tone, no accusation.

### 16.4 Shared vault — `/shared/{id}`

- Read-only badge in the header; no FAB, no edit, no delete, no re-share.
- No owner or vault label exists. The header shows only the shared collection's name (encrypted content chosen by the owner) and the `Read-only` badge.
- Export disabled per policy; if a file is non-exportable, the menu item is absent, not disabled.
- Revoked mid-session: content blurs immediately and a solid full-screen notice appears; no cached thumbnails remain.

---

## 17. App Icons (Pro)

**Route:** `/settings/appearance/icons`

```
(back) App icon        [PRO]
Current
 ┌────────┐
 │ vault  │  Vaultaire
 └────────┘
Choose an icon
 (vault) (calc) (printer) (units)
 (info)  (level) (sound)  (network)
[ Change icon ]
```

- Each icon is a 72dp squircle preview with the label below (`label.m`).
- Selected = 2dp iris ring + check badge.
- Confirmation dialog: "The app will now appear as Calculator." + `Change` / `Cancel`.
- The in-app UI does **not** change appearance when disguised; only the launcher label/icon.
- Never show a disguise icon inside the app header, about screen, or recents.

---

## 18. Appearance

**Route:** `/settings/appearance`

Rows: Theme (Light / Night / Auto) · Surface (Glass / Solid / Auto) · Reduce motion · Reduce transparency · Hide previews in recents · Text size (system) · App language · Vault label · Profile name (optional, local only).
- Changing theme animates a 240ms crossfade, never a hard flash.
- A live preview card at the top shows the selected surface mode.
- Solid mode is presented as "Faster, higher contrast" not "accessibility mode".

---

## 19. About / Privacy

- Version, build, open-source licenses, threat model summary, honest limitations.
- Links to privacy policy and security architecture open in-app with a 44dp close button.
- No analytics toggle (there is no analytics). State that clearly.
- Diagnostics are opt-in, local, and never include keys, phrases, or file names.

---

## 20. Recovery Phrase Display

**Route:** `/recovery/display` · **Secure:** yes · **Pro:** no

```
┌──────────────────────────────┐
│ (close) Your recovery phrase │
│                              │
│ 1 garden   2 silver  3 orbit │
│ 4 quiet    5 ember   6 north │
│ 7 paper    8 river   9 stone │
│                              │
│ ┌──────────────────────────┐ │
│ │ This is the only way back│ │   butter notice
│ │ in. We can't reset it.   │ │
│ └──────────────────────────┘ │
│ [ ] I wrote it down          │
│ [ I've saved it ]            │
└──────────────────────────────┘
```

- Solid panel, no glass, no blur, no elevation; `Plain` background.
- 3×3 word tiles (9) or 2×6 (12), `mono.m` 22sp, index `mono.s`.
- No copy button. No screenshot. No share. Long-press selection disabled.
- Backgrounding: app switcher shows a solid cover; return requires the pattern again if > 5s.
- Done triggers a 2-step confirm ("Did you write down all 9 words?" → "Yes, I have them").
- The phrase is never retrievable in plaintext from storage; this screen either comes from the creation flow or a biometric-gated reveal that re-derives after pattern entry.

---

## 21. Recovery Phrase Entry

**Route:** `/recovery/enter` · **Secure:** yes

- Segmented control: `9 words` / `12 words` / `Custom`.
- Word slots auto-advance; no suggestions, no autocorrect, no clipboard by default.
- `Paste` is a ghost action requiring a 1.5s hold, with a confirmation.
- On submit: full-width iris progress line (no percentage), cancel disabled, 800k KDF takes what it takes.
- Wrong: generic message, clear only the last field, keep the rest, throttle after 3 attempts.
- After success: choose `Reset pattern` or `Open vault`, then force new phrase if the pattern is reset.

---

## 22. Pattern Reset

- Interstitial first: "Resetting your pattern creates a new recovery phrase. Your files stay encrypted and stay yours." 
- Require current pattern **or** recovery phrase, not both.
- New pattern + confirm + new phrase + save confirmation. Four screens, no shortcuts.
- Cancel at any point returns to Security settings; nothing changes until the final save.

---

## 23. Pro / Paywall

**Route:** `/pro`

- Presented as a sheet, not a full-screen interstitial, when triggered by a feature tap.
- Hero: iris eyebrow, `display.m` benefit, three mint check rows, price `mono.l`.
- Plans as two glass cards; annual is preselected and shows monthly equivalent, no fake strike-through.
- Primary `Continue` (ink), ghost `Restore`, `body.s` legal.
- Close is always available (44dp glass circle, top-right). No countdown, no "limited time", no blocked back.
- After purchase: return to the exact feature the user tapped, with state preserved.

---

## 24. Empty / Error / System States

| State | Copy | Visual |
|---|---|---|
| Empty vault | "Nothing here yet." | abstract object, `Add photos`, `Take a photo` |
| Empty collection | "This collection is empty." | one action `Add items` |
| No search results | "No matches in this vault." | `Clear filters` |
| Unknown pattern | **No error.** Same glow, same transition, neutral empty vault. | no shake, no red |
| Rate limited | "Please wait a moment." | neutral, `mono.m` countdown |
| Invalid new pattern | "Connect at least 6 dots." / "Add at least one turn." | inline hint under grid, creation only |
| Recovery wrong | "That phrase didn't work." | last field clears |
| Share expired | "This share is no longer available." | solid notice, no retry loop |
| Backup failed | "Backup didn't finish. Your files are safe on this device." | butter banner + Retry + Why |
| Sync conflict | "3 items need your review." | butter banner → review list |
| Storage full | "This device is out of space. Free up space or connect a drive." | butter banner |
| Permission denied (camera) | "Vaultaire needs camera access to take photos. You can still import." | education sheet, `Open settings` / `Not now` |
| Mic denied | "Video will record without sound." | butter pill in viewfinder |
| Unsupported file | "This file can't be previewed here." | icon + `Open with…` (if policy allows) |
| Decryption failure | "We couldn't open this item. It may be damaged." | danger banner, no technical detail |
| Duress | **No special state.** | identical to success |

**Error tone:** plain, short, no blame, no "oops", no exclamation marks. Never expose stack traces, file paths, or crypto terms.

---

## 25. Notifications

- Local only. No push infrastructure for vault content.
- Channels: `Backup` (low), `Sync` (low), `Share expiry` (default), `Security` (high, silent).
- Notification text never includes file names, vault labels, or counts that reveal vault existence. Use "Vaultaire backup needs attention."
- Tapping opens the relevant settings screen after unlock, never the file directly.
- Notification previews are hidden when "Hide previews" is on.

---

## 26. Widgets and Quick Settings (optional, later)

- No photo widget. A widget that shows vault content defeats the product.
- Optional 1×1 "Vaultaire" launch tile with a neutral glyph; never a last-photo thumbnail.
- No Quick Settings tile that bypasses the pattern.

---

## 27. Restore, import, and no-account recovery

### 27.1 New device / restore entry — `/welcome`

The only place a "welcome" surface is allowed, because a device with no local vault genuinely has nothing to open.

```
┌──────────────────────────────┐
│                              │
│        ( orbit art )         │
│                              │
│     Your vault, here.        │   display.m
│  Restore it with the phrase  │   body.m
│  you saved.                  │
│                              │
│ [ Restore with phrase ]      │   ink pill
│   Start fresh                │   ghost
└──────────────────────────────┘
```

- `Start fresh` goes straight to pattern creation. No account, no email, no "sign in".
- Restore requires the recovery phrase; Google Drive is discovered only after the phrase derives the backup key.
- The screen never says "No vault found" — it offers two known paths without confirming or denying local vault existence.
- On a device that already has a vault, this screen is unreachable except via Settings → Restore, which requires pattern re-auth.

### 27.2 Restore progress — `/restore/progress`

- Iris ring, `mono.m` "42 of 128", no file names, no vault labels, no thumbnails.
- Cancel requires confirmation: "Stop restoring? Downloaded data stays encrypted and can resume later."
- Success: "Vault restored." + `Open vault`. No confetti, no "welcome back" personalization.
- Failure: reason in plain language (`Not enough space`, `Connection lost`, `Backup key doesn't match`) + `Retry`. Never expose record IDs or sizes beyond a total.

### 27.3 Gallery import — system picker → `/vault/import`

```
┌──────────────────────────────┐
│ (close) Import 12 items      │
│ ┌──────────────────────────┐ │
│ │  [ 3-up thumbnails ]     │ │
│ └──────────────────────────┘ │
│ Encrypting into Vault        │
│ ▓▓▓▓▓▓▓░░░░  7 of 12         │
│                              │
│ Delete originals from Photos │
│ [ off ]  Why?                │
│ [ Import 12 ]                │
└──────────────────────────────┘
```

- Import is encrypt-in-place from the picker stream; never write a plaintext temp copy.
- "Delete originals" is **off by default**. Enabling it explains that deletion happens in the system gallery after a successful import and cannot be undone by Vaultaire.
- Duplicate detection: "3 items are already in this vault." Skip or add anyway.
- Cancel mid-import: already-imported items remain, count is shown; no partial files.
- No import progress in notifications (could reveal activity); in-app only.

---

## 28. My Shares and revocation

### 28.1 My Shares — `/shares`

```
(back) My Shares               [PRO]
┌──────────────────────────────┐
│ Kyoto, 2024                  │
│ Active · 2 of 3 opens · 6d   │
│ [ Publish update ]  [ Revoke ]│
└──────────────────────────────┘
┌──────────────────────────────┐
│ Documents                    │
│ Expired · Nov 12             │
└──────────────────────────────┘
```

- Each card shows the shared collection/vault label (content, encrypted), created date, opens used, expiry, and status.
- **No recipient identity, email, or device.** Sharing is phrase-based; there is nobody to name.
- `Publish update` only for active shares; `Revoke` is a ghost danger action, not a filled red button.
- Status: Active (mint dot), Expired (sand), Revoked (ink.400), Pending revoke (butter, with a small progress line).
- Empty state: "You haven't shared anything." + `Share a vault` (Pro gate).

### 28.2 Revoke confirmation

- Dialog: "Revoke this share? The recipient loses access the next time they're online. Files already exported stay exported."
- Primary is `Revoke access` in danger text; secondary `Keep share`.
- After confirm: card shows `Revoking…` with a 2dp butter progress line; retry is automatic; after 24h show `Stuck? Retry`.
- Never delete the card on revoke; the user needs a record that it happened.

### 28.3 QR handoff (optional)

- QR encodes the 12-word phrase. It is shown only inside `AuroraSecureSurface`, with the warning "Anyone who scans this can open the share."
- Auto-dims after 60s and requires a tap to redisplay; no screenshot, no save-to-photos.
- The receiving side can scan or type; typing remains the default.

---

## 29. Decoy believability guidance (Pro)

- Content-only screen, reachable only from Duress setup, never from the home screen.
- Title: "Making it look lived-in." No use of the word "decoy" in the screen title or body if the user might be observed; body copy uses "this vault".
- Tips: add ordinary photos, keep a realistic amount, avoid an empty vault, don't name collections "fake", use it occasionally.
- No checklist that could be discovered and used against the user; no "score".
- A butter honesty card: "If someone knows your normal vault exists, a perfect decoy may still not convince them."

---

## 30. Privacy and Security Info

### 30.1 Privacy — `/settings/privacy`

- Rows: Crash reports (off by default), Diagnostics (off), Local activity log (on, encrypted), Clipboard auto-clear (30/60/off), Hide previews in recents (on).
- Copy at top: "Vaultaire has no server that can read your vault. There is no account to delete."
- No analytics SDK, no advertising ID, no third-party trackers. State it in one sentence, not a badge wall.
- Export my data: produces an encrypted archive only; never a plaintext ZIP.

### 30.2 Security Info / Help — `/settings/security-info`

- Threat model in plain language: what the app protects against, what it does not.
- Honest limits: forensic vault-count estimation; flash residue after a duress wipe; offline pattern guessing against copied indexes; loss of pattern + phrase = permanent loss.
- What FLAG_SECURE blocks (screenshots, screen recording, casting) and what it does not (another camera pointed at the screen).
- No "military-grade" claims, no lock illustrations, no fear copy.
- One action: `Read the full threat model` (in-app document).

---

## 31. Sync activity log (Pro)

- Local, encrypted, opt-in. Rows: time, action (`Merged 12 changes`, `Device disconnected`), device alias if the user set one.
- Never shows file names, vault labels, IP addresses, or record IDs.
- Clear log requires confirmation; clearing is local only.
- Empty: "No sync activity yet."

---

## 32. Adaptive layouts

| Width | Home | Timeline | Settings | Camera |
|---|---|---|---|---|
| < 600dp | 1 column, floating nav | 1 column, strip | 1 column list | full-bleed |
| 600–840dp | 2 columns (hero spans 2), nav rail | calendar + timeline side by side | two-pane list/detail | preview + side controls |
| > 840dp | 3 columns, max 1040dp, nav rail | month grid + timeline | three-pane | preview + right control panel |

- Foldables: hinge-aware; never place the pattern grid across a fold. Tabletop mode puts the grid on the bottom half and instructions on top.
- Landscape camera: controls move to the right edge, shutter bottom-right, no overlap with the cutout.
- The pattern grid is always centered in a single pane, never split.

---

## 33. Screen-level security matrix

| Screen | FLAG_SECURE | Solid panel | Clipboard | Recents cover |
|---|---|---|---|---|
| Entry | — | — | — | brand |
| Onboarding pattern | ✅ | ✅ | off | brand |
| Pattern lock | ✅ (grid) | — | off | brand |
| Vault home | — | — | normal | blur/solid toggle |
| Timeline | — | — | normal | blur/solid toggle |
| Viewer | ✅ | media | off | brand |
| Camera / review | ✅ | controls | off | brand |
| Recovery display / entry | ✅ | ✅ | off | brand |
| Share phrase display / entry | ✅ | ✅ | off (hold-to-paste) | brand |
| Duress setup | ✅ (confirm) | ✅ | off | brand |
| Backup / sync | — | — | normal | blur/solid toggle |
| Settings | — | — | normal | blur/solid toggle |
| Pro | — | — | normal | blur/solid toggle |
| New device / restore entry | — | — | normal | brand |
| Restore progress | — | — | normal | blur/solid toggle |
| Import | — | — | normal | blur/solid toggle |
| My Shares / revoke | — | — | normal | blur/solid toggle |
| Shared vault (read-only) | ✅ | media | off | brand |
| QR handoff | ✅ | ✅ | off | brand |
| Decoy guidance | ✅ | ✅ | off | brand |
| Privacy / Security info | — | — | normal | blur/solid toggle |
| Sync activity log | — | — | normal | blur/solid toggle |

**Reading the matrix:** "brand" recents means the app-switcher snapshot is replaced with a solid brand cover, not a blurred thumbnail. `✅ (grid)` and `✅ (confirm)` mean FLAG_SECURE is applied for the sensitive portion of the screen rather than the whole destination.
