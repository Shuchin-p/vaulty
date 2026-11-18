# Flows, Actions & States

> `screens.md` says what each screen *looks like*. This folder says what each screen **does**:
> every way in, every way out, every action, every failure, and every state it can be in.

This is the layer that turns a visual system into a testable product specification.

---

## 1. Read in this order

| # | File | What it covers |
|---|---|---|
| 0 | [`_TEMPLATE.md`](./_TEMPLATE.md) | The authoring contract: state codes, screen template, invariants, canonical routes |
| 0 | [`00-state-model.md`](./00-state-model.md) | Three tiers of state, precedence, session lifecycle, persistence, process death, overlay stack, mandatory states per screen class |
| 1 | [`01-access-and-recovery.md`](./01-access-and-recovery.md) | Splash, new device, onboarding, lock, recovery, reset, restore |
| 2 | [`02-vault-core.md`](./02-vault-core.md) | Vault home, timeline, collection, search, viewer, import, selection |
| 3 | [`03-camera-and-notifications.md`](./03-camera-and-notifications.md) | Camera, photo/video review, permissions, notifications, widgets |
| 4 | [`04-security-settings.md`](./04-security-settings.md) | Settings hub, security, duress, decoy guidance, hidden vaults, privacy, about |
| 5 | [`05-cloud-and-sharing.md`](./05-cloud-and-sharing.md) | Backup, sync, sync log, my shares, share creation, phrase, open, shared vault |
| 6 | [`06-appearance-and-monetization.md`](./06-appearance-and-monetization.md) | Appearance, app icons, paywall, entitlement gates |
| 7 | [`07-system-states.md`](./07-system-states.md) | Network, storage, entitlement, permissions, viewports, accessibility, device pressure, lifecycle |
| 8 | [`08-coverage.md`](./08-coverage.md) | Coverage matrix, per-class state budgets, intentional omissions, severity-ranked gap roll-up |

---

## 2. The four questions every screen answers

1. **How did I get here?** Every route, notification, deep link, and lifecycle trigger.
2. **How do I leave?** Back, gesture, timeout, process death, forced exit.
3. **What can I do?** Every control, its precondition, its effect, its failure.
4. **What if it isn't normal?** Empty, partial, offline, gated, denied, locked, full, fatal.

A screen that cannot answer all four is not specified.

---

## 3. State codes at a glance

**Screen:** `S0` initial · `S1` loading · `S2` refreshing · `S3` content · `S4` empty ·
`S5` partial · `S6` recoverable error · `S7` fatal · `S8` offline/stale · `S9` locked ·
`S10` Pro-gated · `S11` permission-gated · `S12` unsupported · `S13` selection/editing ·
`S14` confirming · `S15` committing · `S16` success · `S17` undo window

**Environment:** `G` session · `N` network · `D` storage · `E` entitlement · `P` permission ·
`V` viewport · `A` accessibility · `X` device pressure

Full definitions: [`_TEMPLATE.md §2`](./_TEMPLATE.md). Combination rules:
[`00-state-model.md §2`](./00-state-model.md).

---

## 4. How to use this in practice

| Role | Use |
|---|---|
| **Design review** | A screen is done when its state table has no empty cells that apply and no state without an exit |
| **Implementation** | Each row of Actions is a test case; each row of States is a screenshot fixture |
| **QA** | The state injector (`00 §10`) forces any state; the coverage matrix is the test plan |
| **Security review** | Check the Data contract and the Flow atlas for each security screen first |

Design-system rule carried forward: **no state may be represented by color alone**, and
**no security state may differ visually between the honest and the coerced path**.

---

## 5. Maintenance

- Adding a screen: copy the template, add the route to `_TEMPLATE.md §5`, add it to the right
  domain file, and add a coverage row.
- Changing behavior: update the domain file **and** `screens.md` in the same change.
- Adding a state code: add it to `_TEMPLATE.md §2` first, then use it. Codes are referenced by
  number in tests and screenshots, so they are append-only — never renumber.
- Anything that needs a new component goes in the domain file's `## New components needed`, then
  into `../components.md`. Never invent a component inline.

---

## 6. What this folder deliberately does not cover

- Cryptographic design. The flows assume the primitives described in the feature docs.
- Server APIs. There is no content server.
- Copy style. That lives in `../screens.md §24` and `../README.md §2`.
- Motion values. Those are tokens in `../foundations.md §8`.
