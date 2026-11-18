# Vaultaire — Build Conventions for Agents

## Build commands (Windows, from D:\CodeWork\VaultApp\android)
- Compile:      `.\gradlew.bat :app:compileDebugKotlin --console=plain`
- Full APK:     `.\gradlew.bat :app:assembleDebug --console=plain`
- Unit tests:   `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- One test:     `.\gradlew.bat :app:testDebugUnitTest --tests "com.vaultaire.android.<pkg>.<Class>" --console=plain`
- Install/run:  `android run --debug` (or `android run --apks app\build\outputs\apk\debug\app-debug.apk --device <serial>`)

Baseline at start of battle phase: compileDebugKotlin + assembleDebug are GREEN. Keep them green after every change.

## Ownership rules (avoid merge conflicts between parallel agents)
- `app/build.gradle.kts`, root `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`,
  `AndroidManifest.xml`: OWNED BY ORCHESTRATOR. If you need a new dependency/permission, report it — do not edit.
  (Exception: the test-infra agent may add test-only deps in a dedicated pass.)
- `app/src/main/res/values/*.xml`: add NEW files (e.g. `strings_feature.xml`) instead of editing existing ones.
- `ui/theme/Theme.kt`, `MainActivity.kt`, `VaultaireApp.kt`, `VaultaireApplication.kt`: foundation files.
  Feature agents add their own screens under `ui/<feature>/` and register destinations by editing
  `ui/VaultaireApp.kt` ONLY inside clearly-marked TODO regions. If you must edit a foundation file,
  keep the change surgical and mention it in your report.
- Engine code: each feature agent owns its own package(s) (see its task brief). Do not edit another
  feature's packages; report cross-feature API needs instead.

## Package layout
- Source root: `app/src/main/java/com/vaultaire/android/` → packages `com.vaultaire.android.*`
- Engines (existing, to be completed per plan):
  - `crypto/` (KeyDerivationEngine, EnvelopeEncryptionEngine, DeviceSaltManager, MemoryHygiene,
    PatternUtils, PatternPoint, RecoveryPhraseEngine)
  - `storage/` (VaultStorageEngine, RandomAccessEncryptedSourceImpl, EncryptedOutputStream)
  - `vault/` (VaultHeader, VaultManager)
  - `camera/` (SecureCameraManager), `backup/` (ZeroKnowledgeBackupEngine),
    `sharing/` (EncryptedShareEngine), `disguise/` (AppDisguiseManager), `data/pricing/model/Entitlement.kt`
- UI (to be built): `com.vaultaire.android.ui.<feature>` with `*Screen.kt` + `*ViewModel.kt`,
  shared components in `ui/components/`, theme in `ui/theme/`.
- Navigation: NavHost lives in `ui/VaultaireApp.kt` (navigation-compose).
- Unit tests: `app/src/test/java/com/vaultaire/android/...` (JUnit4 + kotlinx-coroutines-test).
- Instrumentation/journey tests: `app/src/androidTest/java/...`.

## Stack (already in build.gradle.kts)
Kotlin 2.0.21, AGP 8.7.2, compileSdk 35, minSdk 26, Compose (BOM 2024.11.00) + Material3 +
navigation-compose 2.8.4, CameraX 1.4.0, Media3 1.5.0, Coil3 0.0? (3.0.4), Room 2.6.1 (KSP),
Play Billing 7.1.1, security-crypto 1.1.0-alpha06, coroutines 1.9.0. Report if you need more.

## Code style
- Kotlin official style, no wildcard imports in NEW files (existing engine files may keep them).
- Coroutines for async; no blocking calls on main thread.
- Security-sensitive code: wipe secrets (MemoryHygiene), no logging of secret material, prefer
  hardware-backed Keystore. Add KDoc referencing the plan requirement it implements.
- Every public engine class needs a unit test in `app/src/test` per its plan's tests.md.

## Plans
Feature specifications live in `plan/<feature>/` (README.md, planning.md, implementation.md, tests.md).
UI screen specs: `plan/tests/screens/*.md`. Design tokens/components: `plan/design-system/`.