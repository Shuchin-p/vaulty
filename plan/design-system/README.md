# Vaultaire Aurora — Design System

> **Soft light. Hard encryption.**
> A warm, glassy interface wrapped around a zero-knowledge vault.
> This folder is the single source of truth for Vaultaire Android's visual language, components, and screen blueprints.

**Status:** v1.0 — proposed
**Applies to:** every feature under `plan/` (lock, vaults, camera, recovery, sharing, backup, sync, disguise, duress)
**Preview:** open [`preview.html`](./preview.html) in a browser
**Tokens:** [`tokens.json`](./tokens.json) · **Compose:** [`kotlin/`](./kotlin)

---

## 1. The idea

The reference direction is a **soft lavender world of frosted glass**: oversized type, squircle cards, pastel data blocks, a floating dark pill in the nav, and a quiet background full of slow light. It feels personal, not corporate. Nothing about it looks like a "secure enclave".

That is the point. Vaultaire should feel like a place you keep your life, not a bunker you visit.

But the app makes hard promises: no accounts, no catalog of vaults, no readable cloud data, no coercion tells. So the visual system has one governing rule:

> **Every soft surface has a hard backing.** Glass is a finish, never a dependency. Remove transparency, motion, blur, or color and the product still works, still passes contrast, and still reveals nothing.

Aurora is therefore built in two layers:

| Layer | What it is | Can be removed? |
|---|---|---|
| **Aurora Glass** | The default look — lavender canvas, frosted cards, pastel tints, ambient light | Yes — degrades to Solid Mode |
| **Vault Core** | Structure, hierarchy, semantics, focus order, secure-screen behavior | No — structural |

---

## 2. Design principles

1. **Calm is a security feature.**
   No red alarms, no padlocks, no hacker chrome. Fear makes people disable protections. Confidence makes them keep them.

2. **Content carries the color. Chrome stays quiet.**
   Chrome is ink, white glass, and one iris accent. Photos, documents, and pastel data blocks provide the color. Never tint user content.

3. **One decision per screen.**
   Unlock. Keep or retake. Save phrase. Share or cancel. Hiding secondary actions behind a "more" button is better than showing six equal choices.

4. **Motion explains. It never decorates a secret.**
   Motion shows where a sheet came from and that an action landed. Nothing bounces on a duress wipe. Nothing celebratory happens around credentials.

5. **Indistinguishable where it matters.**
   Duress unlock, decoy vault, wrong-pattern empty state, and "no vault here" must be visually identical to their ordinary counterparts. Any unique styling is a coercion tell.

6. **Glass is optional; legibility is not.**
   Every glass component has a solid fallback, and text is measured against the worst-case backdrop, not the pretty one.

7. **Thumb first, then eyes.**
   Primary actions sit in the bottom third. Destructive actions never sit under the thumb's resting arc.

8. **No dead ends, no dark patterns.**
   Every empty state offers the next safe action. Pro gates explain value, never hold data hostage.

---

## 3. What this system covers

| Area | Document |
|---|---|
| Color, type, spacing, shape, glass, elevation, motion, icons, imagery | [`foundations.md`](./foundations.md) |
| ~30 components with anatomy, states, and Compose APIs | [`components.md`](./components.md) |
| All screens and flows, mapped to plan features | [`screens.md`](./screens.md) |
| Per-screen entry points, actions, failures, and every state | [`flows/`](./flows/README.md) |
| Accessibility, secure surfaces, FLAG_SECURE, haptics | [`accessibility.md`](./accessibility.md) |
| Feature → surface traceability | [`feature-matrix.md`](./feature-matrix.md) |
| W3C design tokens (light + night) | [`tokens.json`](./tokens.json) |
| Compose theme + primitives | [`kotlin/AuroraTheme.kt`](./kotlin/AuroraTheme.kt), [`kotlin/AuroraComponents.kt`](./kotlin/AuroraComponents.kt) |
| Visual style guide with three device mockups | [`preview.html`](./preview.html) |

> The former `plan/ui-design/` folder is **deprecated** and intentionally not referenced by this system. Do not mix the two token sets.

---

## 4. The 60-second visual summary

- **Canvas:** lavender `#C9C3E6` with a soft vertical wash and thin white orbit lines.
- **Surfaces:** white glass at 62% fill, 28dp blur, 1dp white border, 28–32dp squircle radius.
- **Ink:** near-black `#17151F`; secondary `#4A4557`; tertiary `#6B6578`.
- **Brand:** iris `#7C6BF2`, used sparingly for focus, progress, and selection.
- **Pastels:** mint, rose, peach, butter, sky, lilac, sand — data blocks and category tints only.
- **Type:** Instrument Sans (display/UI), Roboto Mono (secrets, counts, timers). Display is tight and large; labels are small and confident.
- **Primary action:** solid ink pill (`#17151F`), white label. Never a colored gradient button.
- **Nav:** floating glass capsule, ink pill for the active destination, line icons elsewhere.
- **Depth:** shadows are lavender-tinted and soft; no hard black drop shadows.
- **Motion:** springs for touch, 240–440ms; ambient light drifts over 45s; zero motion for duress.

---

## 5. Compose adoption

```kotlin
AuroraTheme(
    mode = AuroraMode.Glass,      // Glass | Solid (auto-fallback)
    night = false,                 // Light-first; Night is a first-class theme
) {
    AuroraScaffold(
        background = AuroraBackground.Orbit,
        bottomBar = { AuroraNavBar(...) },
    ) { padding ->
        VaultHomeScreen(contentPadding = padding)
    }
}
```

Rules for engineers:

1. **Never hardcode a hex value.** Use `AuroraTheme.colors.*`, `AuroraTheme.type.*`, `AuroraTheme.shapes.*`.
2. **Never place text on `glassSubtle` over media.** Use `glassRaised` + a 20% ink scrim, or a solid tint card.
3. **Every glass component must render correctly in Solid Mode.** Add a screenshot test for both modes.
4. **Every screen must survive 200% font scale** without clipping a primary action.
5. **FLAG_SECURE is a screen property, not a component property.** See `accessibility.md`.
6. **No new corner radii, font sizes, or pastels** without adding a token first.

---

## 6. Definition of done for a screen

- [ ] Uses only Aurora tokens and components
- [ ] Passes AA contrast in Glass, Solid, and Night modes
- [ ] Passes at 85%, 100%, 150%, 200% font scale
- [ ] Has loading, empty, error, offline, and locked states
- [ ] Pro-gated states show value and a safe exit
- [ ] FLAG_SECURE correctly applied to sensitive surfaces
- [ ] TalkBack order follows visual order; all targets ≥ 48dp
- [ ] Reduced-motion variant tested
- [ ] No unique visual treatment on duress/decoy paths
- [ ] Screenshot tests for phone, foldable, and tablet widths

---

## 7. Naming

- **Aurora** — the design system.
- **Aurora Glass** / **Aurora Solid** / **Aurora Night** — the three surface modes.
- **Vault Core** — the non-negotiable structural and security rules.
- **Orbit** — the thin white background line motif.
