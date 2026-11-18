# Pro / Paywall — Test Cases
## Scope — screens.md §23 Pro / Paywall + flows/06-appearance-and-monetization.md §1.3 Paywall + feature-matrix.md Pro feature list + secure-sharing/planning.md security invariants
## Preconditions
- Sheet presentation (not full-screen interstitial) when triggered by feature tap
- Annual plan preselected with monthly equivalent shown; no fake strike-through pricing
- Close always available: 44dp glass circle top-right, plus back, scrim tap, Esc
- No countdown, no "limited time" language, no blocked back navigation
- After purchase: return to exact originating feature with state preserved
- No analytics events on paywall view, conversion, or purchase
- Entitlement cached locally as encrypted Play purchase token only

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T23-01 | Sheet opens from gated feature tap | Functional | P0 | 1. Free user taps Share on vault item<br>2. AuroraProGate sheet appears<br>3. Tap "Try Pro" | `/pro` opens as sheet over origin; hero shows iris eyebrow, `display.m` benefit, three mint check rows, price `mono.l`; two glass plan cards; annual preselected with monthly equivalent; Continue (ink), Restore (ghost), legal `body.s`; 44dp close top-right |
| T23-02 | Sheet opens from gated destination direct navigation | Functional | P0 | 1. Free user navigates to `/settings/appearance/icons`<br>2. Screen renders full-screen S10 | Same copy as sheet; `Not now` exits to caller; grid fully browsable but tiles not operable |
| T23-03 | Annual plan preselected with monthly equivalent | Functional | P0 | 1. Open `/pro` sheet<br>2. Inspect plan cards | Annual card has 2dp iris ring + check; monthly equivalent line visible; no strike-through on monthly card |
| T23-04 | Pick monthly plan changes selection | Functional | P1 | 1. Open `/pro` sheet<br>2. Tap monthly plan card | Selection moves to monthly; ring + check moves; Continue enabled |
| T23-05 | Continue launches Play billing flow | Functional | P0 | 1. Open `/pro` sheet (E1)<br>2. Tap Continue | Play billing flow launches; Continue shows spinner; footprint preserved; S15 state |
| T23-06 | Purchase success returns to origin with state preserved | Functional | P0 | 1. Complete purchase in Play flow<br>2. Observe return | Pro active (E2); origin route restored; vault selection, share form, or icon grid state identical to pre-paywall |
| T23-07 | Restore purchases finds existing subscription | Functional | P0 | 1. Subscribed user on new device opens `/pro`<br>2. Tap Restore | `queryPurchasesAsync` runs; token reconciled silently; flips to E2; "Pro is active." announced; feature works |
| T23-08 | Close via 44dp top-right dismisses sheet | Functional | P0 | 1. Open `/pro` sheet<br>2. Tap 44dp glass circle top-right | Sheet dismissed; origin re-armed; gated action still visible and re-tappable |
| T23-09 | Close via back gesture dismisses sheet | Functional | P0 | 1. Open `/pro` sheet<br>2. System back gesture | Sheet dismissed; origin re-armed; no purchase state written |
| T23-10 | Close via scrim tap dismisses sheet | Functional | P0 | 1. Open `/pro` sheet<br>2. Tap scrim behind sheet | Sheet dismissed; origin re-armed |
| T23-11 | Close via Esc key dismisses sheet | Functional | P1 | 1. Open `/pro` sheet on desktop/tablet<br>2. Press Esc | Sheet dismissed; origin re-armed |
| T23-12 | Not now on gate sheet returns to origin | Functional | P0 | 1. Free user taps Share → gate sheet<br>2. Tap "Not now" | Gate closes; origin held; selection intact; still gated; re-tappable |
| T23-13 | Purchase cancelled in Play returns to S3 | Functional | P0 | 1. Tap Continue → Play flow<br>2. Cancel in Play sheet | Back to S3; previous plan selection retained; no second confirm; no discount offer |
| T23-14 | Already subscribed (ITEM_ALREADY_OWNED) reconciles silently | Functional | P0 | 1. Subscribed user taps Continue<br>2. Play returns ALREADY_OWNED | Silent reconcile; flips to E2; acts as success; no second billing launch; no double charge |
| T23-15 | Deferred purchase (E4) shows neutral inline state | Functional | P0 | 1. Purchase enters pending state<br>2. Observe `/pro` | Neutral inline "Finishing purchase…" + `Check again` after 60s; no alarm colour; no countdown |
| T23-16 | Check again during E4 resolves to success | Functional | P0 | 1. E4 state active<br>2. Token lands → tap Check again | Flips to S16 → E2; returns to origin; "Pro is active." |
| T23-17 | Check again during E4 still pending restarts 60s window | Functional | P1 | 1. E4 state active<br>2. Tap Check again, still pending | Remains S5; 60s timeout restarts; no spinner past 8s |
| T23-18 | Close during E4 does not cancel purchase | Functional | P0 | 1. E4 state active<br>2. Tap close | Sheet dismissed; purchase continues in background; on return query finds token |
| T23-19 | Tablet ≥600dp shows centered 480dp sheet with side-by-side cards | Adaptive | P1 | 1. Open `/pro` on 720dp width device | Sheet centered 480dp; plan cards side-by-side; nav rail behind |
| T23-20 | Tablet ≥840dp shows side panel | Adaptive | P1 | 1. Open `/pro` on 900dp width device | Sheet becomes side panel; confirm stays dialog |
| T23-21 | Multi-window <360dp shows full-height sheet, one card at a time | Adaptive | P1 | 1. Open `/pro` in 320dp multi-window | Sheet full height; one plan card visible; scrollable |
| T23-22 | 200% font stacks plan cards, price never truncates, Continue 60dp visible | Accessibility | P0 | 1. Set 200% font<br>2. Open `/pro` | Plan cards stack; price `mono.l` never truncates; Continue 60dp visible without scroll; benefit rows wrap |
| T23-23 | TalkBack announces price with monthly equivalent | Accessibility | P0 | 1. Enable TalkBack<br>2. Focus annual price | "$19.99 per year, about $1.67 a month"; no timer; nothing to race |
| T23-24 | Offline shows "Connect to see plans.", Continue disabled with reason | Functional | P0 | 1. Disable network<br>2. Open `/pro` | S8 state; price line "Connect to see plans."; Continue disabled with reason shown; Restore still runs |
| T23-25 | Restore works offline for purchases made on this device | Functional | P1 | 1. Offline, previously purchased on this device<br>2. Tap Restore | Play caches purchases; restore succeeds; flips to E2 |
| T23-26 | Family sharing shows one plain line | Functional | P1 | 1. Family-shared Pro account<br>2. Open `/pro` | "Pro is shared through your Google family group. If the organizer cancels, it ends here too." |
| T23-27 | Origin memory-only resolves stale id silently to /vault | Functional | P1 | 1. Purchase succeeds<br>2. Process death → relaunch<br>3. Origin no longer resolves | Silent; never named destination; lands on `/vault` |
| T23-28 | No analytics events anywhere | Security | P0 | 1. Open/close/purchase/restore<br>2. Inspect logs | No paywall-view, conversion, purchase, or icon-change events; only Play token cached locally in encrypted prefs |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T23-29 | Prices load >300ms shows skeleton on price line only | Functional | P1 | 1. Throttle network<br>2. Open `/pro` | S1 state; skeleton on price line only; rest of content visible |
| T23-30 | Price query failed shows inline "Prices didn't load." + Retry | Functional | P0 | 1. Force price query failure<br>2. Open `/pro` | S6 state; inline "Prices didn't load." + `Retry` button; Close available |
| T23-31 | Retry after price failure re-queries | Functional | P1 | 1. S6 state<br>2. Tap Retry | Re-queries prices; on success → S3 |
| T23-32 | ITEM_UNAVAILABLE shows "That plan isn't available. Try again." | Functional | P1 | 1. Tap Continue<br>2. Play returns ITEM_UNAVAILABLE | S6; inline "That plan isn't available. Try again." |
| T23-33 | Offline during Continue shows S8 with reason | Functional | P0 | 1. Open `/pro` online<br>2. Go offline<br>3. Tap Continue | S8; Continue disabled with reason; Restore available |
| T23-34 | Purchase failed shows "The purchase didn't go through." + Try again | Functional | P0 | 1. Tap Continue<br>2. Play returns error (not cancelled) | S6; inline "The purchase didn't go through." + `Try again`, Close |
| T23-35 | Try again after purchase failure re-launches billing | Functional | P1 | 1. S6 purchase failed<br>2. Tap Try again | Re-launches Play billing flow with same plan |
| T23-36 | Restore offline shows "You're offline. This will retry when you're connected." | Functional | P1 | 1. Offline<br>2. Tap Restore | Inline "You're offline. This will retry when you're connected." |
| T23-37 | Restore nothing found shows "No purchase found on this account." + Try Pro | Functional | P1 | 1. Never purchased<br>2. Tap Restore | "No purchase found on this account." + `Try Pro` button |
| T23-38 | Pro expired (E3) gate copy "Your Pro access has ended. Your data is still here." | Functional | P0 | 1. Subscription expired<br>2. Tap gated feature (e.g., Share) | Gate sheet shows exact copy; re-purchasing same flow; nothing deleted |
| T23-39 | Refund/revocation → next query flips to E3, silent degrade | Security | P0 | 1. Refund processed in Play<br>2. App resumes → queryPurchasesAsync | Flips to E3; duress config and active disguise keep working; no data deleted |
| T23-40 | Auto-lock (S9) dismisses paywall with session | Functional | P0 | 1. Open `/pro` sheet<br>2. Wait for auto-lock threshold | In-app paywall dismissed instantly; Play sheet (system UI) keeps running; on return: lock first, reconcile entitlement, then origin if resolves else `/vault` |
| T23-41 | Home during Play sheet → billing continues, app locks per threshold | Functional | P1 | 1. Tap Continue → Play sheet open<br>2. Press Home | Billing continues in system UI; app locks per auto-lock threshold |
| T23-42 | G4 during purchase → relaunch → /lock → query on unlock | Functional | P0 | 1. Tap Continue<br>2. Kill process during billing<br>3. Relaunch | `/lock` shown; on unlock query purchases; token present → E2 + gated feature works; absent → nothing happened; billing sheet never relaunched automatically |
| T23-43 | Rate limited shows "Please wait a moment." neutral, mono.m countdown | Functional | P1 | 1. Trigger rate limit (e.g., rapid Restore taps)<br>2. Observe | "Please wait a moment." neutral tone; `mono.m` countdown; no red; no shake |
| T23-44 | Error tone: plain, short, no blame, no "oops", no exclamation marks | Content | P0 | 1. Trigger any error state<br>2. Inspect copy | Matches error tone spec; never exposes stack traces, file paths, crypto terms |
| T23-45 | Paywall never first thing new user sees | Functional | P0 | 1. Fresh install, launch app | Direct to pattern grid; no interstitial at launch; no paywall on `/vault`; no paywall after restore beyond user-tapped gate |
| T23-46 | Gate opened over origin; back pops sheet not screen | Functional | P0 | 1. Tap gated feature → gate sheet<br>2. Press back | Gate sheet pops; origin screen remains; back again exits origin |
| T23-47 | S9/G6 dismisses gate and paywall with session | Functional | P0 | 1. Gate sheet open<br>2. Auto-lock or G6 | Both dismissed with session; no paywall over `/lock` |
| T23-48 | Purchase succeeds inside gate → gate closes, original action proceeds once | Functional | P0 | 1. Gate sheet → Try Pro → purchase<br>2. Observe | Gate closes; original action (Share, etc.) proceeds exactly once; never twice |
| T23-49 | E4 while gate open shows same "Finishing purchase…" inline | Functional | P1 | 1. Gate sheet open<br>2. Purchase enters pending | Gate shows identical "Finishing purchase…" + `Check again` as `/pro` |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T23-50 | No vault data in purchase telemetry | Security | P0 | 1. Purchase Pro<br>2. Inspect Play token and local cache | Only Play purchase token cached in encrypted prefs; no email, account id, payment method, vault labels, file names, pattern data |
| T23-51 | Purchase state preserved across process death | Security | P0 | 1. Purchase succeeds<br>2. Kill process before return<br>3. Relaunch | On unlock, query finds token → E2; gated feature works; origin resolves if possible |
| T23-52 | Entitlement revocation: duress config and disguise stay active | Security | P0 | 1. Configure duress + change icon to Calculator<br>2. Revoke Pro (refund)<br>3. Verify | Duress configuration still active and enforced; Calculator icon stays; all vaults/files/camera/import/recovery/backup accessible |
| T23-53 | Sync pauses on revocation; local data intact, nothing deleted | Security | P0 | 1. Active sync + Pro<br>2. Revoke Pro<br>3. Observe | Syncing pauses; local data intact; no auto-rejoin; nothing hidden |
| T23-54 | Opening/viewing/revoking existing shares works Free | Security | P0 | 1. Pro user created shares<br>2. Pro lapses<br>3. Open My Shares, revoke one | Works; only *creating* new shares is gated |
| T23-55 | No countdown, no fake strike-through, no "limited time", no blocked back | Security | P0 | 1. Inspect `/pro` UI in all states | Zero countdown timers; no strike-through pricing; no "limited time" copy; back/close always functional |
| T23-56 | No gate ever hides existing data, blurs content, or renders over /lock or duress wipe | Security | P0 | 1. Pro lapsed + gated feature tapped<br>2. Observe | Gate sheet over origin; content never blurred/hidden; never over `/lock` or duress wipe |
| T23-57 | No analytics: no conversion event, no paywall-view event | Security | P0 | 1. Full purchase flow<br>2. Inspect network/logs | Zero analytics events; only local encrypted Play token |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T23-58 | Phone portrait: sheet at half height, scrollable | Adaptive | P1 | 1. Open `/pro` on phone portrait | Sheet half height; scrollable if needed |
| T23-59 | RTL: grid mirrors; icon art never mirrors; numbers stay LTR | Adaptive | P1 | 1. Set RTL locale<br>2. Open `/pro` | Plan cards mirror order; price numbers LTR; benefit rows mirror |
| T23-60 | Reduced motion: no bloom drift in preview; crossfade = 120ms fade | Accessibility | P1 | 1. Enable reduced motion<br>2. Open `/pro` | Theme crossfade 120ms; no spring animations; price `mono.l` stable |
| T23-61 | Battery saver: no effect on this screen | Adaptive | P2 | 1. Enable battery saver<br>2. Open `/pro` | No visual change; X2 code shows no effect |
| T23-62 | Offline: nothing changes on this screen (all values local) | Adaptive | P2 | 1. Offline + open `/pro` | N2: prices unavailable (S8) but UI structure identical |
| T23-63 | Switch accessibility: Continue 60dp target at 200% font | Accessibility | P0 | 1. 200% font + TalkBack<br>2. Focus Continue | 60dp height; visible without scroll; announced correctly |
| T23-64 | Legal text `body.s` readable at 200% font | Accessibility | P1 | 1. 200% font<br>2. Inspect legal row | Wraps to 2 lines; never truncates; 4.5:1 contrast |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T23-65 | Theme change while sheet open: sheet re-themes without reopening | Race | P1 | 1. Open `/pro` sheet<br>2. System theme changes (Light→Night) | Sheet crossfades 240ms (120ms reduced motion); enter animation finishes first; no double crossfade |
| T23-66 | Language switch with sheet open: recreation closes sheets; unsaved text dropped after confirm | Race | P1 | 1. Open `/pro` sheet<br>2. Change app language | Activity recreated; sheet dismissed; unsaved state dropped after confirm; never silently |
| T23-67 | Rotation during crossfade: animation cancels, target mode wins | Race | P1 | 1. Open `/pro` sheet<br>2. Rotate during 240ms crossfade | Animation cancels; target theme/surface wins; no mid-transition hue committed |
| T23-68 | G4 while writing entitlement cache: atomic temp+rename; last committed wins | Race | P0 | 1. Purchase succeeds<br>2. Kill process during encrypted prefs write<br>3. Relaunch | Last committed value wins; no torn state; query on unlock reconciles correctly |
| T23-69 | S9 while paywall open: in-app dismissed instantly; Play sheet continues | Race | P0 | 1. `/pro` sheet open<br>2. Auto-lock triggers | In-app paywall gone instantly; Play sheet (system UI) continues; on return: lock → reconcile → origin |
| T23-70 | Origin resolution order: token present → E2 + feature works; absent → nothing happened | Race | P0 | 1. Purchase in flight<br>2. Process death<br>3. Relaunch + unlock | Query purchases; token found → E2, gated feature works; not found → nothing happened; no auto-relaunch of billing |

## Security invariants to assert
- [ ] No countdown timer, no "limited time" language, no blocked back navigation anywhere
- [ ] Purchase state preserved across process death (G4) — query on unlock reconciles
- [ ] Entitlement revocation: duress configuration stays active/enforced; already-changed disguise icon stays; all vaults/files/camera/import/recovery/backup accessible; opening/viewing/revoking shares works; local sync data intact (syncing pauses)
- [ ] No vault data in purchase telemetry — only Play token cached locally in encrypted prefs
- [ ] No analytics events: no paywall-view, no conversion, no purchase, no icon-change metrics
- [ ] Paywall never renders over `/lock`, duress wipe, or recents cover
- [ ] No gate hides, blurs, or partial-renders existing user content as incentive
- [ ] Error copy never exposes stack traces, file paths, or crypto terms
- [ ] Close always available: 44dp top-right, back, scrim, Esc — at every point including S15 (Play sheet)
- [ ] Origin memory-only {route, kind, opaqueId}; never persisted with names; stale id resolves silently to `/vault`

## Gaps & contradictions
- **Paywall copy not in screens.md §24**: Success ("Pro is active."), already-subscribed, purchase-failed, restore-nothing-found, and family-sharing lines are only in flows/06-appearance-and-monetization.md; need sign-off so copy table stays single source (flows/06 § Gaps #4)
- **Family sharing detection rule missing**: Play family purchases surface through `queryPurchasesAsync` like any other; whether UI should ever say "shared" and what happens when organizer cancels mid-session is unstated beyond one proposed line (flows/06 § Gaps #5)
- **Notification copy contradicts complete disguise**: screens.md §25 uses "Vaultaire backup needs attention." A disguised app (label "Calculator") must not emit real app name; needs neutral string set for notification bodies while alias active (flows/06 § Gaps #1)
- **App icon alias label**: feature-matrix.md §5 leaves "Play Store policy review required before shipping" open; Calculator/Printer/Network labels plus look-alike art can read as impersonation; renamed label set may be needed (flows/06 § Gaps #3)
- **Reduce transparency OS source undocumented**: Android exposes no public system setting of that name before API 35; specified as in-app toggle that also follows system where OS reports one — confirm or drop row (flows/06 § Gaps #6)
- **FLAG_SECURE scope on paywall**: Not specified in screens.md; paywall shows no vault content but may show plan prices — confirm if FLAG_SECURE needed
- **Pricing copy exact values**: screens.md says price `mono.l` but exact dollar amounts not specified; flows/06 uses "$19.99 per year, about $1.67 a month" as TalkBack example — confirm canonical pricing strings