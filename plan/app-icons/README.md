# App Icons Feature - Planning Package

## Overview
Vaultaire Pro allows users to replace the standard Home Screen icon with one of seven utility icons (Calculator, Printer, Units, Device Info, Level, Sound Meter, Network), maintaining privacy and discretion against casual glances while preserving full vault security behind the pattern unlock screen.

This planning package bridges the gap between the official Vaultaire feature specifications (originating from iOS) and the native Android implementation using `activity-alias` component state switching.

---

## Website Claims Summary vs. Android Reality

| Feature Aspect | Official Website (iOS) | Current Android Implementation | Proposed Android Alignment |
|---|---|---|---|
| **Icons Offered** | 7 utility icons + Default Vaultaire | 6 legacy icons (Calculator, Printer, Notes, Scanner, Weather, Files) | **7 official utility icons** (Calculator, Printer, Units, Device Info, Level, Sound Meter, Network) + Default Vaultaire |
| **Name Under Icon** | Stays "Vaultaire" (iOS limitation) | Activity-alias labels currently set to utility names | **Configurable / Aligned**: Set alias labels to match utility names or "Vaultaire" |
| **Confirmation** | iOS system alert per icon change | Direct switch without custom dialog | Confirmation dialog matching UX alert |
| **Pro Gating** | Free preview, Pro requirement | Unenforced / partial stub | Strict Pro license enforcement via `Entitlement` |
| **Technical Mechanism** | `UIApplication.setAlternateIconName` | `PackageManager.setComponentEnabledSetting` | `PackageManager.setComponentEnabledSetting` with `DONT_KILL_APP` and state reconciliation |

---

## Status Dashboard

| Document | Scope | Status |
|---|---|---|
| [`planning.md`](./planning.md) | Product Owner: User Stories, Flows, Acceptance Criteria, UX | **Ready for Review** |
| [`implementation.md`](./implementation.md) | Tech Lead / Architect: Manifest, PackageManager, Reconciliation, Edge Cases | **Ready for Review** |
| [`tests.md`](./tests.md) | QA / Testing: Unit, Integration, UI, Security & Edge Cases | **Ready for Review** |

---

## Quick Navigation
- [Product Planning & User Experience](./planning.md)
- [Technical Architecture & Implementation Guide](./implementation.md)
- [Test Specifications & QA Plan](./tests.md)
