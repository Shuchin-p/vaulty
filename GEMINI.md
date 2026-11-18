# AGENTS.md — Autonomous Engineering & UI Harness for VaultApp

Welcome to the **VaultApp** repository (`Vaultaire`, package `com.vaultaire.android`).
This document sets the mandatory operational invariants, architectural rules, and verification standards for all AI agents, subagents, and automated workflows operating in this workspace.

---

## 1. Core Operating Principles

1. **Always Verify End-to-End**: Never mark a task complete until code compiles with `./gradlew assembleDebug`, installs onto attached emulators (`emulator-5554`, `emulator-5556`), and is verified using `android layout` or `android screen capture`.
2. **Autonomous Execution Loop**: Fix compiler errors, deprecations, layout clipping, and runtime crashes autonomously in a closed loop. Do not stop to ask for confirmation on safe incremental edits or test runs.
3. **Structured Reasoning**: Utilize sequential thinking (`sequentialthinking`) for complex navigation flows, state synchronizations, and security invariants.
4. **Code Quality & Comments**: All Kotlin code must follow modern Compose idioms with comprehensive documentation and comments. Any Python tooling must strictly follow the PEP 8 style guide.

---

## 2. Design System & UI Invariants (Aurora Only)

VaultApp uses a bespoke design language called **Aurora**. Material 3 components at the screen level are strictly prohibited.

- **Prohibited Components**:
  - `androidx.compose.material3.Card`
  - `androidx.compose.material3.Scaffold`
  - `androidx.compose.material3.Switch`
  - `androidx.compose.material3.TextField` / `OutlinedTextField`
  - `androidx.compose.material3.Button` / `ElevatedButton`
- **Mandatory Replacements**:
  - `AuroraGlassCard(tint, raised, onClick, modifier)`
  - `AuroraBackground { ... }` or `AuroraSecureSurface { ... }`
  - `AuroraSwitch(checked, onCheckedChange)`
  - `BasicTextField` with custom Aurora container styling
  - `AuroraButton(text, variant = AuroraButtonVariant.Primary / Secondary / Ghost / Danger, onClick)`
  - `AuroraIconButton(icon, contentDescription, onClick)`
  - `AuroraTopBar(title, eyebrow, leading, actions)`
- **Typography & Dimensions**:
  - Always use `AuroraTheme.typography` (e.g. `AuroraType.headlineL`, `titleM`, `bodyM`, `monoS`, `labelM`).
  - Minimum touch target for all interactive elements is **48 dp** (`AuroraDimens.minTouchTarget`).
  - Use `statusBarsPadding()` and `navigationBarsPadding()` on root containers to guarantee inset safety.

---

## 3. Security & Plausible Deniability Invariants

1. **FLAG_SECURE**: Must be active on all sensitive screens via `AuroraSecureSurface`:
   - Pattern setup and unlock screens (`/onboarding/pattern`, `/lock`)
   - Recovery phrase display and intake screens (`/onboarding/phrase`, `/recovery/*`)
   - File details / media decrypter (`/vault/file/*`)
   - Camera capture and photo/video review (`/vault/camera/*`)
   - Share phrase generation and shared phrase intake (`/vault/share/phrase`, `/shared/open`, `/shared/*`)
2. **Zero Plaintext at Rest**: Decryption happens strictly in-memory. No cached plaintext files or unencrypted thumbnails on disk.
3. **No Recipient Identity**: In sharing flows, shares are 12-word cryptographic phrases. Never add email, phone, contact picker, or recipient profile fields.

---

## 4. Android Toolchain & Automation Harness

- **Android CLI**: Located at `C:\Users\pshuc\AppData\AndroidCLI\android.exe`.
  - Layout Inspection: `android layout --device <serial>`
  - Visual Capture: `android screen capture -o <file.png> --device <serial>`
  - Compose Preview rendering: `android preview`
  - Documentation Search: `android docs search "<topic>"`
- **ADB**: Located at `C:\Users\pshuc\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- **Emulators**:
  - `emulator-5554`: Primary validation device (Pixel 8 / API 34).
  - `emulator-5556`: Secondary / recipient validation device (Pixel 7 / API 34).
- **Logcat Audit**: Always verify zero exceptions via:
  ```powershell
  & $adb -s <device> logcat -d -s AndroidRuntime:E
  ```

---

## 5. Agent Skills Catalog

The repository provides modular skills under `.agents/skills/`:
- `vault-verification`: Multi-pass layout, security surface, and interaction tests.
- `android-cli`: Official toolchain interaction instructions.
- `adaptive`: Multi-window, foldables, and tablet layout handling.
- `jetpack-compose-performance`: Recomposition optimization and layout stability.
- `r8-analyzer`: R8 optimization and shrinking rules audit.
- `edge-to-edge`: WindowInsets and system bar edge-to-edge compliance.
- `navigation-3`: Type-safe navigation and scene management.
- `anr-audit-and-fix`: Main thread jank and ANR prevention.

---

## 6. Definition of Done (DoD)

A task is only complete when:
1. `./gradlew.bat assembleDebug` succeeds with 0 errors and 0 unresolved references.
2. The APK is deployed to the target emulator.
3. Live layout hierarchy is inspected and matches spec bounding boxes, labels, and touch targets.
4. Logcat reports 0 `AndroidRuntime:E` crashes.
5. All newly authored code contains clean, professional documentation comments.
