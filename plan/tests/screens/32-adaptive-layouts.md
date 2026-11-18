# Adaptive Layouts — Test Cases
## Scope — screens.md §32, components.md §1 (Nav), _archive/responsive-breakpoints.md
## Preconditions — 
- Screen sizes: Compact (<600dp), Medium (600–840dp), Expanded (>840dp)
- Modes: Hinge-aware foldables, Tabletop (grid bottom), Landscape camera (controls right), TV/Desktop form factors.
- Density: All tests validated at 100% and 200% font scales.

## Test cases
### Happy path
| ID | Title | Screen | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|---|
| T32-01 | Compact Nav | Home | Nav | P0 | Check navigation at 400dp | Floating nav bar (phone) |
| T32-02 | Tablet Nav | Home | Nav | P0 | Check navigation at 700dp | Navigation rail (84dp wide) |
| T32-03 | Desktop Nav | Home | Nav | P0 | Check navigation at 1000dp | Permanent nav rail |
| T32-04 | Foldable Tabletop | Lock | Pattern | P1 | Fold to 90 degrees | Pattern grid on bottom half, instructions on top |
| T32-05 | Landscape Camera | Camera | Camera | P1 | Rotate to landscape | Controls on right, shutter bottom-right, no cutout overlap |

### Edge & error
| ID | Title | Screen | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|---|
| T32-06 | Small Screen Grid | Lock | Pattern | P0 | Set < 600dp | Pattern grid centered, single pane |
| T32-07 | Foldable Split | Lock | Pattern | P1 | Place fold over grid | Never place grid across fold |
| T32-08 | Max Desktop Width | Home | Layout | P2 | Set window 1200dp | 3 columns, max content width 1040dp |

### Security & deniability
| ID | Title | Screen | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|---|
| T32-09 | Deniability Lock | Lock | Pattern | P0 | Use unknown pattern | Neutral vault, no error, no shake, no counter |
| T32-10 | Secure Pane | Lock | Pattern | P0 | Check FLAG_SECURE | Grid area secured, solid recents cover |

### Accessibility & adaptive
| ID | Title | Screen | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|---|
| T32-11 | 200% Scale Flow | Settings | Access | P1 | Set 200% font scale | No overlap, buttons grow min 60dp |
| T32-12 | TalkBack Focus | Lock | Access | P0 | Explore grid via TalkBack | Explore individual dots via ExploreByTouchHelper |

## Security invariants to assert
- Pattern grid: Never split across a fold; always centered in a single pane; never rendered as a split UI.
- Landscape camera: Controls move to right edge; shutter is bottom-right; no overlap with display cutout.
- Security flags: FLAG_SECURE applied to grid (lock), viewer (media), camera (controls), recovery/share, decoy, QR handoff.
- Deniability: No error, shake, or counter for unknown pattern; always neutral resolution.

## Gaps & contradictions
- §32 "Adaptive layouts" (screens.md) table does not explicitly define the 3rd column behavior for "Settings" or "Camera" at > 840dp, though the text implies three-pane.
- Screens.md §32 table vs components.md: Rail is 84dp; ensure tablet nav is strictly aligned to the 84dp spec.
