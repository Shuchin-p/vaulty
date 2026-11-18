# Vaultaire Aurora — Test Specification Index

One test-case spec per screen family from `plan/design-system/screens.md`. Each spec contains:
- Preconditions
- Test cases in 5 groups (Happy path / Edge & error / Security & deniability / Accessibility & adaptive / Races & lifecycle)
- Security invariants checklist
- Gaps & contradictions (plan inconsistencies flagged, no invented features)

| # | Spec file | Screen (§ in `screens.md`) | Source docs |
|---|-----------|----------------------------|-------------|
| 1 | `01-entry-splash.md` | §1 Entry / Splash | §1, flows/01, flows/00, pattern-encryption, security-architecture |
| 2 | `02-onboarding.md` | §2 Onboarding | §2, flows/01, pattern-encryption, secret-phrase |
| 3 | `03-pattern-lock.md` | §3 Pattern Lock + §3.1 deniability policy | §3, flows/01, flows/04, flows/00, pattern-encryption, hidden-vaults, plausible-deniability, duress-mode |
| 4 | `04-vault-home.md` | §4 Vault Home | §4, §24, flows/02, flows/00, encrypted-cloud-backup, security-architecture |
| 5 | `05-timeline.md` | §5 Timeline | §5, flows/02, encrypted-cloud-backup, secure-sharing |
| 6 | `06-collection.md` | §6 Collection (Vault Detail) | §6, flows/02, secure-sharing, hidden-vaults |
| 7 | `07-search.md` | §7 Search | §7, §24, flows/02, secret-phrase, plausible-deniability |
| 8 | `08-file-viewer.md` | §8 File Viewer | §8, §24, §9, flows/02, components §6, secure-camera, secure-sharing |
| 9 | `09-selection-mode.md` | §9 Selection Mode | §9, §24, flows/02, secure-sharing |
| 10 | `10-settings-hub.md` | §10 Settings Hub | §10, flows/04, flows/06, components (lists), security-architecture, duress-mode |
| 11 | `11-security-settings.md` | §11 Security Settings | §11, flows/04, flows/01, pattern-encryption, secret-phrase, duress-mode, hidden-vaults |
| 12 | `12-duress-setup.md` | §12 Duress Setup (Pro) | §12, §29, flows/04, duress-mode, plausible-deniability, hidden-vaults |
| 13 | `13-hidden-vaults-explainer.md` | §13 Hidden Vaults Explainer | §13, flows/04, hidden-vaults, plausible-deniability |
| 14 | `14-encrypted-backup.md` | §14 Encrypted Backup | §14, §24, flows/05, encrypted-cloud-backup, secret-phrase |
| 15 | `15-device-sync.md` | §15 Device Sync (Pro) | §15, §24, flows/05, device-sync, plausible-deniability, duress-mode |
| 16 | `16-secure-sharing.md` | §16 Secure Sharing (16.1–16.4) | §16, §24, flows/05, secure-sharing, secret-phrase, plausible-deniability |
| 17 | `17-app-icons.md` | §17 App Icons (Pro) | §17, flows/06, flows/01, app-icons, plausible-deniability |
| 18 | `18-appearance.md` | §18 Appearance | §18, flows/06, accessibility, ease-of-use, plausible-deniability |
| 19 | `19-about-privacy.md` | §19 About / Privacy | §19, §30, flows/04, flows/07, security-architecture, plausible-deniability |
| 20 | `20-recovery-phrase-display.md` | §20 Recovery Phrase Display | §20, §24, flows/01, secret-phrase, pattern-encryption |
| 21 | `21-recovery-phrase-entry.md` | §21 Recovery Phrase Entry | §21, §24, flows/01, secret-phrase, pattern-encryption |
| 22 | `22-pattern-reset.md` | §22 Pattern Reset | §22, §11, §2, flows/01, pattern-encryption, secret-phrase, hidden-vaults |
| 23 | `23-pro-paywall.md` | §23 Pro / Paywall | §23, §24, flows/06, feature-matrix, secure-sharing |
| 24 | `24-system-states.md` | §24 Empty / Error / System States | §24, flows/07, flows/00, security-architecture, plausible-deniability |
| 25 | `25-notifications.md` | §25 Notifications | §25, §24, flows/03, encrypted-cloud-backup, device-sync, plausible-deniability |
| 26 | `26-widgets-qss.md` | §26 Widgets and Quick Settings | §26, §24, flows/03, flows/06, ease-of-use, plausible-deniability |
| 27 | `27-restore-import.md` | §27 Restore / Import (27.1–27.3) | §27, §24, flows/01, flows/02, flows/05, encrypted-cloud-backup, ease-of-use, secret-phrase |
| 28 | `28-my-shares.md` | §28 My Shares and Revocation | §28, §16, §24, flows/05, secure-sharing, plausible-deniability |
| 29 | `29-decoy-guidance.md` | §29 Decoy Believability Guidance | §29, §24, flows/04, plausible-deniability, duress-mode |
| 30 | `30-privacy-security-info.md` | §30 Privacy and Security Info | §30, §19, §24, flows/04, flows/07, security-architecture, plausible-deniability, ease-of-use |
| 31 | `31-sync-activity-log.md` | §31 Sync Activity Log | §31, §24, flows/05, device-sync, plausible-deniability |
| 32 | `32-adaptive-layouts.md` | §32 Adaptive Layouts | §32, §3 (foldable rule), components (responsive), accessibility, responsive-breakpoints, flows/08 |
| 33 | `33-security-matrix.md` | §33 Screen-level Security Matrix | §33, §3 (deniability policy), flows/00, security-architecture, plausible-deniability, duress-mode, secure-sharing, secret-phrase |

---

## Cross-cutting invariants enforced in every spec

1. **No feature invention** — only behaviors already in `screens.md` and flow docs
2. **Exact copy quotes** — expected results use verbatim strings from §24 and screen sections
3. **Every action names a failure path**
4. **Security invariants** — deniability (unknown pattern = neutral empty vault, duress = pixel-identical success), FLAG_SECURE scope, clipboard policy, recents cover, no vault identity leakage
5. **Gaps section flags contradictions** — e.g., §3.1 error-vs-deniability conflict, "Vault label" row vs deniability, share error copy, notification content rules

---

## How to run / extend

```bash
# All specs are Markdown tables; grep by ID pattern:
grep -n "T03-" plan/tests/screens/03-pattern-lock.md

# Add a new screen: copy _TEMPLATE.md from design-system/flows/ and follow the authoring contract
```

Total: **33 specs, ~1,800 test cases**.