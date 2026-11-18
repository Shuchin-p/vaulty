# Sync Activity Log — Test Cases
## Scope — screens.md §31, flows §5.3, feature docs/invariants (device-sync planning §209)

## Preconditions
- User has active Pro (E2) entitlement.
- Sync enabled (Vault backup active, Drive connected).
- Sync activity logging enabled in Privacy settings (or prompt accepted on first access).
- Local, encrypted log store exists with sample event data (merges, connections, disconnects).
- Device alias defined (user-set) for test verification.

## Test cases
### Happy path
| ID | Title | Type | Priority | Steps | Expected |
|:---|:---|:---|:---|:---|:---|
| T31-01 | View log | UI | P0 | Navigate to Settings > Cloud > Sync > Activity log | Screen loads S3 Content, showing list of sync events. |
| T31-02 | Expand event | UI | P1 | Tap any row in the log | Row expands to show plain-language sentence (e.g., "Merged 12 changes"). |
| T31-03 | Filter by category | UI | P1 | Select `Merge` chip | List updates to show only Merge events; announcement confirms count. |
| T31-04 | Clear filters | UI | P2 | Tap active category chip again or `All` | All events restore to view. |
| T31-05 | Clear log | Action | P1 | Tap `Clear log` > Confirm in dialog | Screen moves to S4 Empty; all previous logs removed locally. |

### Edge & error
| ID | Title | Type | Priority | Steps | Expected |
|:---|:---|:---|:---|:---|:---|
| T31-06 | Empty log state | UI | P2 | Navigate to clear log, then return | Screen shows: "No sync activity yet." |
| T31-07 | Opt-out log | UI | P1 | Disable logging in settings > revisit screen | Screen shows: "Activity logging is off." + Enable toggle. |
| T31-08 | Decryption fail | Error | P2 | Inject corrupted log entry | Screen renders, corrupted rows show as neutral. |
| T31-09 | Log store unreadable | Error | P2 | Revoke file permissions while open | Screen shows: "The log couldn't be read." + Retry action. |
| T31-10 | 10k+ rows performance | Perf | P2 | Stress test 10,000+ entries | List is virtualized; no scroll jank; groupings by day observed. |

### Security & deniability
| ID | Title | Type | Priority | Steps | Expected |
|:---|:---|:---|:---|:---|:---|
| T31-11 | Content leak test | Sec | P0 | Scroll and expand rows | Never shows: file names, vault labels, IP addresses, record IDs. |
| T31-12 | Clear log cross-device | Sec | P0 | Clear log on Device A | Device B log remains intact; clearing is local only. |
| T31-13 | Duress isolate event | Sec | P1 | Peer device triggers duress | Device shows: "Device disconnected". No coercion/content revealed. |
| T31-14 | Identity exposure | Sec | P0 | Observe log | Only device alias appears (if user set); never account emails/Drive IDs. |

### Accessibility & adaptive
| ID | Title | Type | Priority | Steps | Expected |
|:---|:---|:---|:---|:---|:---|
| T31-15 | 200% font scale | A11y | P2 | Set 200% scaling | Rows grow, text wraps, no label truncation. |
| T31-16 | Landscape layout | Adapt | P2 | Rotate device to landscape | 1 col layout in list view; no overlap. |

### Races & lifecycle
| ID | Title | Type | Priority | Steps | Expected |
|:---|:---|:---|:---|:---|:---|
| T31-17 | Clear vs Sync race | Race | P2 | Trigger `Clear log` while syncing | Clear wipes current snapshot; any post-wipe events reappear. |
| T31-18 | G4 during log append | Race | P2 | Force stop app during logging | Data survives; appends are transactional; no partial rows. |

## Security invariants to assert
- [ ] **Never shows** file names, vault labels, IP addresses, record IDs, or Drive file IDs.
- [ ] **Local-only clearing**: confirmation required; clear does not reach Drive/peers.
- [ ] **Content Integrity**: Empty state copy is exact: "No sync activity yet."
- [ ] **Duress Wipe Semantics**: Duress on peer shows only `Device disconnected`, never reveals isolation source.
- [ ] **Privacy**: Log is encrypted at rest on device.

## Gaps & contradictions
- No contradictions found between §31 and flow §5.3.
- §31 states "Device alias if user set" but sync planning §244 notes "Device names/identifiers Never in cloud". This is consistent (alias is local/encrypted storage only).
