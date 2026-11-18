# Security Settings — Test Cases

## Scope
screens.md §11 "Security Settings" + flows/04-security-settings.md §4.2 + flows/01-access-and-recovery.md §R1,R2,R3 + pattern-encryption/planning.md + secret-phrase/planning.md + duress-mode/planning.md + hidden-vaults/planning.md

## Preconditions
- Change pattern flow: interstitial shown → current pattern verified → redraw twice → new 9-word phrase forced
- Recovery phrase reveal gating: biometric/pattern re-auth required before `/recovery/display`
- Auto-lock options: immediately / 30s / 1m / 5m (default 30s)
- Attempt throttle: 5 attempts/2 min → 30s / 2m / 10m / 30m cap; persists across restart; "Please wait a moment." + mono.m countdown
- Clipboard auto-clear: 30s / 60s / off (value sheet)
- Screen capture: read-only, locked on for secure screens (FLAG_SECURE)
- Biometric availability: toggle only if enrolled; pattern always remains required; off = unlock path disabled, enrolled biometrics retained
- Duress row: Pro-gated (E2); neutral weight when enabled; shows `[PRO]` badge when E1/E3
- Hidden vaults row: explainer only; no vault list/count/manage; butter honesty card non-dismissible

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T11-01 | Open Security Settings from Settings hub | Navigation | P0 | 1. Unlock vault<br>2. Open nav drawer → Settings<br>3. Tap "Security" row | Navigates to `/settings/security`; title "Security settings" announced; all rows rendered |
| T11-02 | Change pattern — full flow with interstitial | Functional | P0 | 1. Tap "Change pattern"<br>2. Interstitial shows: "Changing your pattern creates a new recovery phrase. Your files stay encrypted."<br>3. Tap "Continue"<br>4. Draw current pattern<br>5. Draw new pattern (≥6 dots, ≥1 turn)<br>6. Redraw new pattern to confirm<br>7. New 9-word phrase displayed<br>8. Confirm phrase saved | Interstitial copy exact; current pattern verified; new pattern committed; **new phrase generated**; old phrase invalidated; returns to Security Settings |
| T11-03 | Change pattern — cancel at interstitial | Functional | P0 | 1. Tap "Change pattern"<br>2. Tap "Cancel" on interstitial | Returns to Security Settings; **no change** to pattern or phrase |
| T11-04 | Change pattern — cancel during redraw | Functional | P0 | 1. Tap "Change pattern" → "Continue"<br>2. Draw current pattern<br>3. Draw new pattern<br>4. Tap "Start over" or Back during confirm | Returns to Security Settings; **old pattern and phrase remain valid** |
| T11-05 | Recovery phrase reveal — biometric auth success | Functional | P0 | 1. Tap "Recovery phrase"<br>2. Biometric prompt appears<br>3. Authenticate with enrolled biometric | Navigates to `/recovery/display`; phrase shown with FLAG_SECURE; "Recovery phrase shown. Screen capture is blocked." announced |
| T11-06 | Recovery phrase reveal — pattern auth success | Functional | P0 | 1. Tap "Recovery phrase"<br>2. Biometric unavailable → pattern entry<br>3. Draw current pattern | Navigates to `/recovery/display`; phrase shown; same security as biometric path |
| T11-07 | Recovery phrase reveal — auth failure stays on screen | Functional | P0 | 1. Tap "Recovery phrase"<br>2. Fail biometric/pattern<br>3. Retry | Stays on Security Settings; "Auth fail → stay, retry" per flow; no navigation |
| T11-08 | Duress mode row — Pro user opens setup | Navigation | P0 | 1. Pro entitlement active (E2)<br>2. Tap "Duress mode" row | Pushes `/settings/security/duress`; no Pro gate |
| T11-09 | Duress mode row — non-Pro shows gate | Navigation | P0 | 1. Entitlement E1 or E3<br>2. Tap "Duress mode" row | Shows inline `AuroraProGate`: "Duress mode opens a decoy vault and erases others" with "Try Pro" / "Not now" |
| T11-10 | Duress mode — enable via hold 2s | Functional | P0 | 1. Open Duress setup (Pro)<br>2. Draw pattern of vault to designate as decoy<br>3. Hold "Hold to enable duress" pill 2s (iris sweep)<br>4. Release | Mint check: "Duress enabled. Decoy vault set." announced; returns to Security Settings; Duress row weight **indistinguishable** from other rows |
| T11-11 | Duress mode — cancel before hold complete | Functional | P0 | 1. Open Duress setup<br>2. Draw matching pattern<br>3. Release hold before 2s | Returns to Duress setup idle; **duress not enabled** |
| T11-12 | Hidden vaults row opens explainer | Navigation | P0 | 1. Tap "Hidden vaults" row | Pushes `/settings/security/hidden-vaults`; content: "different patterns = different vaults; no catalog" + butter honesty card: "A forensic examiner with file access may still estimate how many vaults exist. Hidden vaults hide the catalog, not the storage." |
| T11-13 | Auto-lock picker — change to 1m | Functional | P0 | 1. Tap "Auto-lock" row<br>2. Select "1m"<br>3. Save | Value persisted; `S16` success; next auto-lock triggers at 1 minute |
| T11-14 | Auto-lock picker — "immediately" option | Functional | P0 | 1. Tap "Auto-lock" → select "immediately" → save | App locks instantly on background / screen off |
| T11-15 | Clipboard auto-clear — set to 30s | Functional | P0 | 1. Tap "Clipboard auto-clear" → select "30s" → save | Value persisted; clipboard clears 30s after copy on phrase screens |
| T11-16 | Clipboard auto-clear — set to off | Functional | P0 | 1. Tap "Clipboard auto-clear" → select "off" → save | Clipboard **never** auto-clears on phrase screens |
| T11-17 | Biometric unlock — toggle on when enrolled | Functional | P0 | 1. Biometric enrolled + available<br>2. Toggle "Biometric unlock" on | Toggle on; `S16` saved; biometric unlock offered on `/lock`; pattern still required |
| T11-18 | Biometric unlock — toggle off disables unlock path only | Functional | P0 | 1. Biometric unlock on<br>2. Toggle off | Biometric row absent on `/lock`; enrolled biometrics **retained** in system; pattern required |
| T11-19 | Attempt throttle row — read-only shows tier | Informational | P1 | 1. Observe "Attempt throttle" row | Shows current tier / deadline (e.g., "Tier 0", "30s remaining"); no interaction |
| T11-20 | Screen capture row — read-only shows locked | Informational | P1 | 1. Observe "Screen capture" row | Shows "Locked on for secure screens"; no toggle; read-only |
| T11-21 | Security info row opens screen | Navigation | P0 | 1. Tap "Security info" row | Pushes `/settings/security-info`; threat model content rendered |
| T11-22 | Back gesture returns to Settings hub | Navigation | P0 | 1. From Security Settings, swipe back / press Back | Returns to `/settings`; no data loss |
| T11-23 | Deep link opens Security Settings directly | Navigation | P1 | 1. Launch `vaultaire://settings/security` | Opens `/settings/security` directly; same state as nav entry |
| T11-24 | Pro status change mid-screen updates Duress row | State | P1 | 1. Open Security Settings (E2)<br>2. Revoke Pro (E2→E3) via backend<br>3. Observe Duress row | Row updates in place: shows `[PRO]` badge immediately; **no reload** |
| T11-25 | Auto-lock default is 30s on first install | Functional | P1 | 1. Fresh install, no prefs<br>2. Open Security Settings | Auto-lock row shows "30s" as selected |
| T11-26 | Change pattern — invalid current pattern rejected | Functional | P0 | 1. Tap "Change pattern" → "Continue"<br>2. Draw wrong pattern<br>3. Observe | Neutral handling: no shake, no error copy, no counter; returns to pattern entry (per "no wrong-pattern state") |
| T11-27 | Change pattern — new pattern <6 dots rejected | Functional | P0 | 1. Pass current pattern<br>2. Draw 5 dots → Continue | Inline hint: "Connect at least 6 dots." (creation-only error styling allowed) |
| T11-28 | Change pattern — new pattern no turn rejected | Functional | P0 | 1. Pass current pattern<br>2. Draw 6 dots straight line → Continue | Inline hint: "Add at least one turn." |
| T11-29 | Change pattern — mismatch on confirm rejected | Functional | P0 | 1. Pass current pattern<br>2. Draw new pattern<br>3. Redraw different pattern | 420ms shake + retry (creation mismatch allowed); no penalty, no counter |
| T11-30 | Duress already enabled — shows disable state | Functional | P1 | 1. Duress enabled<br>2. Open Duress setup | Rose card: "Duress is enabled. Hold to disable."; disable requires pattern + biometric; warns "erasure history cannot be undone" |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T11-31 | Process death during Change pattern interstitial | Resilience | P1 | 1. Tap "Change pattern" → interstitial shown<br>2. Kill app<br>3. Relaunch | Returns to `/settings/security`; interstitial **dropped**; no partial state |
| T11-32 | Process death during pattern redraw (change) | Resilience | P1 | 1. In Change pattern flow, after current pattern verified<br>2. Kill app<br>3. Relaunch | Returns to `/lock`; **old pattern and phrase still valid**; reset state discarded |
| T11-33 | Process death during Duress hold | Resilience | P1 | 1. Duress setup, matched pattern, holding pill<br>2. Kill app<br>3. Relaunch | Returns to Security Settings; **duress not enabled**; draft discarded |
| T11-34 | Biometric hardware failure → toggle off | Error | P1 | 1. Toggle Biometric unlock on<br>2. BiometricManager reports hardware unavailable | Toggle remains off; no crash; inline toast if applicable |
| T11-35 | Auto-lock "immediately" + background race | Race | P1 | 1. Set Auto-lock to "immediately"<br>2. Press Home<br>3. Immediately reopen app | Shows `/lock` with `reason=screen-off`; no content flash |
| T11-36 | Throttle tier persists across force-stop | Persistence | P1 | 1. Trigger tier 2 (2m wait)<br>2. Force-stop app<br>3. Relaunch | Throttle wait **still active**; countdown from persisted deadline |
| T11-37 | Clock rollback during throttle wait re-arms wait | Race | P2 | 1. In tier 1 wait (30s)<br>2. Set device clock back 1 min<br>3. Observe | Wait **re-armed** to full current-tier duration (wall rollback protection) |
| T11-38 | Clock forward jump across deadline ends wait | Race | P2 | 1. In tier 1 wait (30s)<br>2. Set device clock forward past deadline<br>3. Observe | Wait **ends early**; accepted cost per R1 |
| T11-39 | Recovery phrase reveal — background >5s zeroizes | Security | P0 | 1. Open `/recovery/display` via Security Settings<br>2. Background app 6s<br>3. Foreground | Phrase zeroized; requires **full re-auth** (pattern/biometric) to reopen |
| T11-40 | Duress row neutral weight when enabled (no red) | Visual | P1 | 1. Enable Duress<br>2. Return to Security Settings | Duress row **weight indistinguishable** from other rows; no red, no badge |
| T11-41 | Hidden vaults honesty card non-dismissible | Visual | P1 | 1. Open Hidden vaults explainer<br>2. Attempt to swipe away butter card | Card **cannot be dismissed**; butter background, `body.m` copy persists |
| T11-42 | Screen capture blocked on Security Settings (FLAG_SECURE) | Security | P0 | 1. On Security Settings, attempt screenshot / screen record | Capture blocked; "Screen capture is blocked on this screen" announced if attempted (X5 state) |
| T11-43 | Screen capture blocked on Duress setup | Security | P0 | 1. On `/settings/security/duress`, attempt screenshot | Blocked (Secure: yes per flow) |
| T11-44 | Screen capture blocked on Recovery phrase display | Security | P0 | 1. On `/recovery/display`, attempt screenshot | Blocked (FLAG_SECURE whole screen) |
| T11-45 | Change pattern — write failure on commit keeps old creds | Error | P1 | 1. Simulate storage write failure at final commit<br>2. Observe | Inline error: "Your files are safe. The reset didn't finish."; **old pattern and phrase remain valid** |
| T11-46 | Auto-lock picker — cancel leaves value unchanged | Functional | P1 | 1. Tap Auto-lock → select "5m" → tap outside / Back | Value **unchanged**; remains previous selection |
| T11-47 | Clipboard auto-clear — cancel leaves value unchanged | Functional | P1 | 1. Tap Clipboard auto-clear → select "60s" → cancel | Value **unchanged** |
| T11-48 | Duress pattern no match → neutral "Pattern not recognized" | Functional | P0 | 1. Duress setup, draw pattern not matching any vault<br>2. Release | Neutral ink: "Pattern not recognized" + Retry; **no vault count leak** |
| T11-49 | Pro gate "Not now" returns to Security Settings | Navigation | P1 | 1. Non-Pro taps Duress row → Pro gate shown<br>2. Tap "Not now" | Returns to Security Settings; no navigation |
| T11-50 | Duress disable — requires pattern + biometric | Functional | P1 | 1. Duress enabled<br>2. Open Duress setup → Hold to disable<br>3. Prompt for pattern + biometric | Both required; warns "erasure history cannot be undone" |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T11-51 | No vault count/label/ID anywhere on Security Settings | Security | P0 | 1. Inspect all rows and states | **Must never appear**: vault count, vault label, vault ID, file names, recovery phrase plaintext, pattern coordinates, duress decoy vault ID, wipe ledger, throttle attempt timestamps |
| T11-52 | No vault count/label on Duress setup | Security | P0 | 1. Open Duress setup<br>2. Inspect grid and text | Compact 5×5 grid, **no vault names**, "Choose the vault to open" copy generic; "Encrypting into <Vault label>" uses generic placeholder |
| T11-53 | No vault count/label on Hidden vaults explainer | Security | P0 | 1. Open Hidden vaults<br>2. Inspect content | **No list, no count, no "manage" button, no rename** (per screens.md §13) |
| T11-54 | Change pattern forces new phrase; old invalidated | Security | P0 | 1. Complete Change pattern flow<br>2. Attempt to use old phrase on `/recovery/enter` | Old phrase **rejected**: "That phrase didn't work."; new phrase required |
| T11-55 | Biometric toggle off does not remove enrollment | Security | P1 | 1. Toggle Biometric unlock off<br>2. Check system biometric settings | Enrolled biometrics **still present** in system settings |
| T11-56 | Duress enabled row gives no coercion tell | Deniability | P0 | 1. Duress enabled<br>2. Observe Security Settings row | Row **weight indistinguishable**; no icon, no color, no badge differentiating it |
| T11-57 | Attempt throttle copy neutral: "Please wait a moment." | Security | P0 | 1. Trigger throttle tier 1<br>2. Observe lock screen | Neutral ink cover: "Please wait a moment." + `mono.m` countdown; **never** "Too many attempts", never a count |
| T11-58 | Recovery entry throttle separate from lock throttle | Security | P1 | 1. Trigger recovery throttle (3 wrong submissions)<br>2. Check lock screen throttle | Separate counter; identical ladder; neutral copy same |
| T11-59 | Screen capture locked on for all secure screens | Security | P0 | 1. Check `/settings/security`, `/settings/security/duress`, `/recovery/display`, `/onboarding/phrase` | FLAG_SECURE set **before first frame** on all; recents shows solid cover |
| T11-60 | Hidden vaults honesty card states forensic limits exactly | Security | P0 | 1. Open Hidden vaults explainer<br>2. Read butter card | Exact copy: "A forensic examiner with file access may still estimate how many vaults exist. Hidden vaults hide the catalog, not the storage." |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T11-61 | 200% font scale — rows wrap, chips stack, no truncation | Accessibility | P1 | 1. Set system font scale ≥ 200%<br>2. Open Security Settings | Row labels wrap, value chips stack, profile card grows, **no truncation**; touch targets stay 48dp |
| T11-62 | 200% font — picker sheets wrap, chips stack | Accessibility | P1 | 1. Font scale ≥ 200%<br>2. Open Auto-lock / Clipboard picker | Sheets wrap, chips stack, **no truncation** |
| T11-63 | TalkBack — Security Settings announces correctly | Accessibility | P1 | 1. Enable TalkBack<br>2. Navigate Security Settings | "Security settings" announced; each row label + value announced; toggle states announced |
| T11-64 | TalkBack — Recovery phrase screen warns before reading | Accessibility | P0 | 1. Enable TalkBack<br>2. Open `/recovery/display` | Butter banner: "Screen reader active. Your phrase will be spoken aloud. Use headphones."; focus lands on title, never word 1 |
| T11-65 | Reduced motion — shake replaced by rose border pulse | Accessibility | P1 | 1. Enable reduced motion<br>2. Trigger pattern mismatch on Change pattern | 420ms shake **replaced** by 200ms rose border pulse; announcement unchanged |
| T11-66 | Phone portrait (<600dp) — single column, floating nav | Adaptive | P1 | 1. Width < 600dp<br>2. Open Security Settings | Single column, floating nav, all rows accessible |
| T11-67 | Tablet (≥600dp) — two-pane: left rail, right detail | Adaptive | P1 | 1. Width ≥ 600dp<br>2. Open Security Settings | Two-pane: left rail groups (Security/Cloud/Appearance), right detail shows Security rows; rail highlights Security |
| T11-68 | Duress setup — 200% font: card scrolls, grid stays 280dp | Adaptive | P1 | 1. Font scale ≥ 200%<br>2. Open Duress setup | Rose card scrolls, grid **stays 280dp**, text wraps |
| T11-69 | Hidden vaults — 200% font: text wraps, card grows | Adaptive | P1 | 1. Font scale ≥ 200%<br>2. Open Hidden vaults explainer | Text wraps, card grows, honesty card stays visible |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T11-70 | Auto-lock during Security Settings → brand cover | Lifecycle | P0 | 1. Open Security Settings<br>2. Wait past auto-lock threshold<br>3. Observe | Brand cover shown; "Vault locked" announced; unlock returns to Security Settings |
| T11-71 | Background > threshold → lock → unlock restores screen | Lifecycle | P0 | 1. Open Security Settings<br>2. Home → wait > auto-lock<br>3. Reopen → unlock | Returns to Security Settings; scroll position restored |
| T11-72 | Process death (G4) on Security Settings → reopen at screen | Resilience | P1 | 1. Open Security Settings<br>2. Kill process<br>3. Relaunch | Reopens at `/settings/security` if session valid; else `/lock`; scroll position restored |
| T11-73 | Pro purchase return mid-screen → Duress row updates | Race | P1 | 1. Non-Pro on Security Settings<br>2. Purchase Pro via Pro gate<br>3. Return | Duress row **updates in place**: `[PRO]` badge removed; tap opens setup directly |
| T11-74 | Duress wipe resuming on cold start blocks UI | Race | P1 | 1. Trigger duress wipe<br>2. Kill app mid-wipe<br>3. Relaunch | Wipe **resumes before decoy interactive**; UI blocked until complete; no partial state visible |
| T11-75 | Config change (rotation) during Change pattern preserves buffer | Lifecycle | P1 | 1. In Change pattern, after current pattern verified<br>2. Rotate device<br>3. Observe | Buffer **survives**; no data loss; interstitial/state preserved |

## Security invariants to assert

- [ ] Pattern change requires current pattern + generates new phrase (old invalidated)
- [ ] Screen capture locked on (FLAG_SECURE) for: Security Settings, Duress setup, Recovery phrase display, Onboarding phrase
- [ ] Duress row neutral weight when enabled (no red, no badge, no coercion tell)
- [ ] Hidden vaults row is explainer only (no list, no count, no manage, no rename)
- [ ] Attempt throttle uses neutral copy "Please wait a moment." + countdown; never "Too many attempts"
- [ ] Recovery phrase reveal requires fresh biometric/pattern re-auth every time
- [ ] Clipboard auto-clear respects user setting (30s/60s/off)
- [ ] Biometric toggle off retains system enrollments; only disables unlock path
- [ ] Auto-lock settings persist and apply correctly (immediately/30s/1m/5m)
- [ ] Process death during any flow discards draft state; no partial commits
- [ ] Throttle state persists across restart/force-stop/reboot; clock rollback re-arms wait
- [ ] No vault metadata (count, labels, IDs) appears anywhere in Security Settings subtree

## Gaps & contradictions

- **Throttle counting basis** (flows/04-security-settings.md Gaps §1): "With no wrong-pattern state, '5 failures' is unobservable; throttler counts attempts (5 KDF submissions / 2 min), which also throttles 6 legitimate quick unlocks. Alternative (time-damping only) changes brute-force economics." — needs product sign-off.
- **Post-restore pattern creation** (Gaps §2): "A device-bound salt means a restored vault cannot verify the old pattern; 01 designs a 2-step pattern create (no phrase step). Confirm the crypto spec binds the salt to the device." — Security Settings Change pattern assumes current pattern verifiable; may conflict with restored vaults.
- **Biometric target vault** (Gaps §3): "01 pins to 'last unlocked vault'; multi-vault users get no choice, and 'last used' is a small coercion/forensic leak." — Biometric unlock toggle in Security Settings has no vault selector.
- **Duress partial-wipe ledger** (Gaps §4): "No durable ledger design guarantees resume — a crash loop could strand residue silently." — Security Settings shows no wipe status; Security Info only acknowledges residue.
- **Encrypted trash setting** (Gaps §5): "The opt-in '7-day encrypted trash' changes delete semantics everywhere but has no row in screens.md/settings." — Privacy screen (04.7) has Clipboard auto-clear but no trash setting.
- **Duress row copy** (screens.md §11 vs flows): screens.md says "Duress row: neutral. No red." but flows show rose-tint card in setup. Contradiction: setup uses rose/explicit language ("The unlock screen may not" — flows §4.3). This is intentional per design but worth flagging.
- **Hidden vaults "different patterns resolve different vaults"** (screens.md §13) vs "no catalog" — the explainer must explain the mechanism without showing it; current copy "different patterns = different vaults; no catalog" may be too abstract for users.