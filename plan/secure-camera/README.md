# Secure Camera — Android Planning Package

## Feature Overview

**Secure Camera** is Vaultaire's built-in photo and video capture system that operates entirely within an unlocked vault. The camera captures images/video directly into encrypted storage (`.vlt` files) without ever writing to external storage, MediaStore, or the system gallery. This eliminates the "import and delete" workflow that leaves forensic traces on iOS (Recently Deleted, iCloud Photos) and Android (MediaStore, Google Photos backup).

---

## Website Claims Summary (from vaultaire.app/features/secure-camera/)

| Claim | Source | Status |
|-------|--------|--------|
| Camera lives inside the vault | ✅ | Implemented: `SecureCameraManager.kt` |
| Uses front/rear lens, tap-to-focus, flash/torch, optional audio | ⚠️ | Partially: CameraX configs exist, UI controls needed |
| Every capture goes through a **review step** | ❌ | **Missing** — critical UX requirement |
| Kept capture is encrypted into the open vault | ✅ | Implemented: AES-256-GCM via `EncryptedOutputStream` |
| Never adds to iPhone camera roll / Android MediaStore | ✅ | Architecture supports: captures to app-internal files |
| Import-and-delete leaves a trail (Recently Deleted, iCloud) | ✅ | Addressed by zero-temp-file architecture |
| Video: temporary MP4 → encrypt → 3-pass overwrite → delete | ⚠️ | **Gap**: current code uses ParcelFileDescriptor pipe but no overwrite |
| Camera permission required; microphone separate & optional | ⚠️ | Permissions in manifest, runtime handling needed |
| Captures JPEG at max sensor dimensions, quality over speed | ⚠️ | Need to configure `ImageCapture` for max resolution |
| Thumbnail built after encryption for vault grid | ❌ | **Missing** — thumbnail generation pipeline needed |
| Works offline (capture, review, encryption on-device) | ✅ | Architecture supports this |
| Hidden vault / duress mode / device sync integration | ⚠️ | Vault selection logic exists, sync not yet implemented |

---

## Status Dashboard

| Component | Status | Notes |
|-----------|--------|-------|
| **Core encryption pipeline** | ✅ Done | `EncryptedOutputStream` + `RandomAccessEncryptedSourceImpl` |
| **Photo capture (CameraX → encrypted .vlt)** | ✅ Done | `SecureCameraManager.captureSecurePhoto()` |
| **Video capture (CameraX → pipe → encrypted .vlt)** | ✅ Done | `SecureCameraManager.startSecureVideoRecording()` |
| **Review screen (preview captured media before encrypt)** | ❌ **Missing** | **Critical gap** — website mandates this step |
| **CameraX Preview/Viewfinder UI** | ❌ Missing | Need Compose `PreviewView` integration |
| **Camera controls (lens switch, focus tap, flash/torch)** | ❌ Missing | Need UI + CameraX config |
| **Permission handling (CAMERA, RECORD_AUDIO)** | ⚠️ Partial | Manifest declares, runtime request logic needed |
| **Thumbnail generation** | ❌ Missing | Need post-encryption thumbnail extractor |
| **Zero-temp-file guarantee (video overwrite)** | ❌ Missing | Current pipe approach avoids temp file, but no overwrite on cancel |
| **FLAG_SECURE on preview/recents** | ❌ Missing | Critical for screenshot prevention |
| **RAM hygiene (buffer wipe after encrypt)** | ⚠️ Partial | `image.close()` called, but byte arrays not wiped |
| **Portrait/landscape orientation handling** | ❌ Missing | Website: "preview and capture follow device rotation" |

---

## Quick Navigation

| Document | Purpose |
|----------|---------|
| [`planning.md`](planning.md) | Product Owner perspective: user stories, flows, acceptance criteria, UX |
| [`implementation.md`](implementation.md) | Tech Lead/Architect: architecture, CameraX configs, pipelines, security |
| [`tests.md`](tests.md) | Test specifications: unit, integration, security, UI, edge cases |

---

## Key Technical Decisions (from website + existing code)

1. **Zero-temp-file architecture**: Photo captures go ImageProxy → memory → `EncryptedOutputStream` → `.vlt`. Video uses `ParcelFileDescriptor.createPipe()` → `EncryptedOutputStream` → `.vlt`. No file ever hits disk unencrypted.

2. **AES-256-GCM with chunked streaming**: `EncryptedOutputStream` uses 64KB chunks, each with unique IV derived from master IV + chunk index. Header stores magic, version, salt (placeholder), master IV, chunk size, file size placeholder, header auth tag placeholder.

3. **Review step is mandatory**: User must explicitly "Keep" or "Retake" after capture. Nothing encrypts until "Keep". This is a **hard UX requirement** from the website.

4. **No MediaStore/gallery artifacts**: Files written to `context.filesDir` (app-internal). `android:allowBackup="false"` + `dataExtractionRules` exclude all domains. CameraX output targets app-internal files.

5. **Video temp file handling**: Website acknowledges video *must* use temp file on iOS (AVAssetWriter). Android CameraX `VideoCapture` can write to `ParcelFileDescriptor` pipe, avoiding temp file entirely — but we must handle cleanup on cancel/crash.

6. **Three-pass overwrite for video temp files** (iOS): On Android, our pipe approach avoids this, but if we ever use a temp file (e.g., for Media3 playback), we need secure deletion.

7. **Microphone permission is optional**: Video records silently if denied; show "audio-off" badge.

8. **FLAG_SECURE**: Must be set on camera Activity/Fragment to prevent screenshots and recent-apps thumbnails.

---

## Open Questions

1. **Review step implementation**: How to show captured image/video before encryption without writing to disk?
   - Option A: Capture → hold `ImageProxy` in memory → display via `ImageView`/`PreviewView` → on "Keep", encrypt
   - Option B: Capture → encrypt to `.vlt` → decrypt for review → on "Retake", delete `.vlt` (defeats zero-temp-file)
   - **Recommendation**: Option A — hold `ImageProxy` (for photo) or buffer video frames, display via `Bitmap`/`ImageProxy.toBitmap()`

2. **Video review**: Video is large; can't hold in RAM. Options:
   - Record to encrypted `.vlt` via pipe, then decrypt-stream for review playback
   - Record to temp file (excluded from backup, Data Protection), review, then encrypt + overwrite
   - **Recommendation**: Record to encrypted `.vlt` via pipe (current impl), use `Media3` with custom `DataSource` to decrypt-stream for review

3. **Thumbnail generation**: When? After "Keep", decrypt first frame/keyframe, generate thumbnail, store encrypted in vault metadata.

4. **Orientation**: CameraX handles rotation via `ImageCapture.setTargetRotation()`. Need to store orientation in file header or EXIF inside encrypted payload.

---

## References

- [Vaultaire Secure Camera (iOS)](https://vaultaire.app/features/secure-camera/)
- [Vaultaire Security Architecture](https://vaultaire.app/features/security-features/)
- Existing code: `SecureCameraManager.kt`, `EncryptedOutputStream.kt`, `RandomAccessEncryptedSourceImpl.kt`
- CameraX 1.4.0 documentation
- Android `FLAG_SECURE` documentation