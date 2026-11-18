# Secure Camera — Test Specifications

## Test Strategy

| Layer | Framework | Scope |
|-------|-----------|-------|
| Unit Tests | JUnit 4 + MockK + Turbine | Crypto, encryption pipeline, memory hygiene, CameraX config builders |
| Integration Tests | AndroidX Test + Robolectric | CameraManager logic, permission flows, vault integration |
| Instrumented Tests | AndroidJUnitRunner + Espresso + CameraX Test | Real CameraX on device/emulator, end-to-end capture |
| Security Tests | Custom + forensic tools | Disk analysis, RAM dumps, MediaStore queries, screenshot tests |
| UI Tests | Compose Test + Espresso | Camera preview, controls, review screen, navigation |

---

## Unit Tests

### Encryption Pipeline (`EncryptedOutputStream`)

| Test ID | Description | Given | When | Then |
|---------|-------------|-------|------|------|
| UT-ENC-01 | Header written correctly | Fresh EncryptedOutputStream | First `write()` | Header: magic="VAULT1", version=1, reserved=0, salt=32B, masterIV=12B, chunkSize=64KB, fileSize=0, headerTag=16B |
| UT-ENC-02 | Chunk encryption unique IVs | 130KB data (3 chunks) | `write()` ×3 + `close()` | Each chunk decrypts with unique IV (masterIV ⊕ chunkIndex) |
| UT-ENC-03 | GCM auth tag verification | Valid .vlt file | Decrypt via RandomAccessEncryptedSource | All chunks authenticate; tampered chunk throws AEADBadTagException |
| UT-ENC-04 | File size updated in header | EncryptedOutputStream with 100KB data | `close()` | Header fileSize = 100KB (plaintext size) |
| UT-ENC-05 | Empty file handling | EncryptedOutputStream | `close()` without `write()` | Valid header written, 0 chunks, fileSize=0 |
| UT-ENC-06 | Large file (1GB) | EncryptedOutputStream | Stream 1GB in 64KB writes | Completes without OOM; fileSize correct |
| UT-ENC-07 | Concurrent writes thread safety | Multiple threads writing same stream | Parallel `write()` calls | No data corruption; sequential chunk indices |

### Decryption Pipeline (`RandomAccessEncryptedSourceImpl`)

| Test ID | Description | Given | When | Then |
|---------|-------------|-------|------|------|
| UT-DEC-01 | Sequential read | Valid .vlt, 3 chunks | `readAt(0, buf, 0, total)` | Returns all plaintext bytes |
| UT-DEC-02 | Random access read | Valid .vlt, 3 chunks | `readAt(chunk1Start, buf, 0, chunkSize)` | Returns correct chunk data |
| UT-DEC-03 | Partial chunk read | Valid .vlt | `readAt(offset, buf, 0, len)` where len < chunkSize | Returns exact requested bytes |
| UT-DEC-04 | Cross-chunk read | Valid .vlt | `readAt(chunk0End - 10, buf, 0, 30)` | Returns 10 bytes from chunk0 + 20 from chunk1 |
| UT-DEC-05 | Read past EOF | Valid .vlt | `readAt(totalLength, buf, 0, 10)` | Returns -1 |
| UT-DEC-06 | Invalid header magic | File with wrong magic | `RandomAccessEncryptedSourceImpl()` | Throws IllegalArgumentException |
| UT-DEC-07 | Tampered chunk detection | Valid .vlt with 1 bit flipped in chunk 1 | `readAt(chunk1Start, ...)` | Throws AEADBadTagException |
| UT-DEC-08 | Wrong key detection | Valid .vlt, wrong master key | `readAt(0, ...)` | Throws AEADBadTagException |

### Memory Hygiene (`MemoryHygiene`)

| Test ID | Description | Given | When | Then |
|---------|-------------|-------|------|------|
| UT-MEM-01 | Wipe byte array | ByteArray with known data | `MemoryHygiene.wipe(bytes)` | All bytes = 0 |
| UT-MEM-02 | Wipe null safe | null input | `MemoryHygiene.wipe(null)` | No exception |
| UT-MEM-03 | Wipe SecretKey | SecretKey with encoded bytes | `MemoryHygiene.wipeKey(key)` | Key.encoded all zeros |
| UT-MEM-04 | Buffer wiped after encrypt | SecureCameraManager.capturePhoto | Photo captured and encrypted | Captured ByteArray all zeros (verify via test hook) |

### CameraX Configuration Builders

| Test ID | Description | Given | When | Then |
|---------|-------------|-------|------|------|
| UT-CAM-01 | Photo config max quality | ImageCapture.Builder | Configured for Secure Camera | `captureMode=MAXIMIZE_QUALITY`, `jpegQuality=100`, `targetResolution=max` |
| UT-CAM-02 | Video config highest quality | Recorder.Builder | Configured for Secure Camera | `qualitySelector=HIGHEST→HD→SD` |
| UT-CAM-03 | Lens selector front | CameraSelector.Builder | `requireLensFacing(FRONT)` | Selector filters to front camera only |
| UT-CAM-04 | Lens selector back | CameraSelector.Builder | `requireLensFacing(BACK)` | Selector filters to back camera only |

### Key Derivation & Envelope

| Test ID | Description | Given | When | Then |
|---------|-------------|-------|------|------|
| UT-CRY-01 | PBKDF2 600k iterations | KeyDerivationEngine | `deriveVaultKey(pattern)` | Uses 600,000 iterations, SHA-512, 256-bit output |
| UT-CRY-02 | Device salt persistence | DeviceSaltManager | First call → second call | Returns same salt; stored in SharedPreferences |
| UT-CRY-03 | Master key wrap/unwrap | EnvelopeEncryptionEngine | `wrapMasterKey` → `unwrapMasterKey` | Returns identical key bytes |
| UT-CRY-04 | Wrong VDK fails unwrap | EnvelopeEncryptionEngine | `unwrapMasterKey` with wrong key | Throws AEADBadTagException |

---

## Integration Tests

### SecureCameraManager (Robolectric + MockK)

| Test ID | Description | Setup | Action | Verify |
|---------|-------------|-------|--------|--------|
| IT-CAM-01 | Photo capture success | Mock ImageCapture, mock keyProvider | `captureSecurePhoto()` | onSuccess called; EncryptedOutputStream.write called with image bytes |
| IT-CAM-02 | Photo capture error | Mock ImageCapture throws exception | `captureSecurePhoto()` | onError called with exception |
| IT-CAM-03 | Photo capture wipes buffer | Mock ImageCapture returns ImageProxy | `captureSecurePhoto()` | MemoryHygiene.wipe called on captured bytes |
| IT-CAM-04 | Video recording start | Mock VideoCapture, mock ParcelFileDescriptor | `startSecureVideoRecording()` | Returns Recording; pipe created; encryption coroutine started |
| IT-CAM-05 | Video recording finalize success | Mock Finalize event no error | Recording callback | writeSide closed; encryptionJob joined; onRecordingFinished(true) |
| IT-CAM-06 | Video recording finalize error | Mock Finalize event has error | Recording callback | onRecordingFinished(false); partial .vlt deleted |
| IT-CAM-07 | Video cancel deletes partial file | Recording started, then cancel | `recording.close()` + cleanup | Target .vlt file deleted |

### Vault Integration

| Test ID | Description | Setup | Action | Verify |
|---------|-------------|-------|--------|--------|
| IT-VLT-01 | Capture writes to vault dir | VaultManager with unlocked vault | `captureSecurePhoto(targetVaultId)` | .vlt created in vault directory |
| IT-VLT-02 | Thumbnail generated after keep | VaultStorageEngine + thumbnail gen | Photo kept | Thumbnail bytes in vault metadata |
| IT-VLT-03 | Multiple vaults isolated | Two vaults unlocked sequentially | Capture in vault A, then vault B | Files in separate directories; keys not shared |

### Permission Flows

| Test ID | Description | Setup | Action | Verify |
|---------|-------------|-------|--------|--------|
| IT-PERM-01 | Camera permission granted | ActivityResultRegistry mocked | Request camera → granted | Camera preview starts |
| IT-PERM-02 | Camera permission denied | ActivityResultRegistry mocked | Request camera → denied | Rationale shown; settings link offered |
| IT-PERM-03 | Mic permission optional | Video mode, mic denied | Start recording | Recording proceeds; audio-off badge shown |

---

## Instrumented Tests (Device/Emulator)

### CameraX End-to-End

| Test ID | Description | Device Requirements | Steps | Assertions |
|---------|-------------|---------------------|-------|------------|
| INST-CAM-01 | Photo capture round-trip | API 26+, camera | Open camera → capture → review → keep → decrypt → verify image | Decrypted image matches captured (pixel comparison, allow JPEG artifacts) |
| INST-CAM-02 | Video capture round-trip | API 26+, camera, mic | Record 5s video → review → keep → decrypt → verify | Decrypted video playable; duration ~5s; audio track present |
| INST-CAM-03 | Video capture silent | API 26+, camera, mic denied | Record 5s video (no mic) → keep → decrypt | Decrypted video has no audio track; duration ~5s |
| INST-CAM-04 | Lens switch | Device with front+back | Open camera → switch lens → capture both | Both captures succeed; front/back metadata correct |
| INST-CAM-05 | Flash modes | Device with flash | Capture with auto/on/off flash | Flash fires per mode (visual check or EXIF) |
| INST-CAM-06 | Torch toggle | Device with torch | Video mode → toggle torch → record | Torch state matches toggle |
| INST-CAM-07 | Tap to focus | Any | Tap preview → capture | Focus metering point set (verify via Camera2 API if possible) |
| INST-CAM-08 | Device rotation | Any | Rotate during preview → capture | Captured image upright (EXIF orientation correct) |
| INST-CAM-09 | App backgrounded during capture | Any | Start capture → home button → reopen | Capture cancelled; no partial .vlt; buffers released |
| INST-CAM-10 | Storage full simulation | Any | Fill storage → attempt capture | IOException caught; user-facing error shown |

### Review Screen Tests

| Test ID | Description | Steps | Assertions |
|---------|-------------|-------|------------|
| INST-REV-01 | Photo review shows captured image | Capture → review screen | ImageView displays captured photo (not live preview) |
| INST-REV-02 | Photo retake discards | Capture → review → retake | No .vlt created; back to camera preview |
| INST-REV-03 | Photo keep encrypts | Capture → review → keep | .vlt created; decryptable; thumbnail in grid |
| INST-REV-04 | Video review plays | Record → review screen | PlayerView shows recorded video; seek works |
| INST-REV-05 | Video retake deletes | Record → review → retake | .vlt deleted; back to camera preview |
| INST-REV-06 | Video keep finalizes | Record → review → keep | .vlt complete; thumbnail generated |

---

## Security Tests

### Disk Forensics (No Unencrypted Files)

| Test ID | Description | Tool/Method | Pass Criteria |
|---------|-------------|-------------|---------------|
| SEC-DISK-01 | No JPEG/MP4 in app files | `adb shell find /data/data/com.vaultaire.android -type f \( -name "*.jpg" -o -name "*.jpeg" -o -name "*.mp4" -o -name "*.mov" \)` | Zero results |
| SEC-DISK-02 | No media in MediaStore | `adb shell content query --uri content://media/external/images/media --projection _id,title --where "title LIKE '%vaultaire%'"` | Zero results |
| SEC-DISK-03 | No media in DCIM/Pictures | `adb shell ls /sdcard/DCIM /sdcard/Pictures /sdcard/Movies` | No vaultaire files |
| SEC-DISK-04 | .vlt files only in vault dir | `adb shell find /data/data/com.vaultaire.android/files -name "*.vlt"` | All .vlt files under `files/vaults/` |
| SEC-DISK-05 | .vlt header valid | Custom script: parse each .vlt header | All have magic="VAULT1", valid structure |
| SEC-DISK-06 | .vlt decrypts with correct key | RandomAccessEncryptedSourceImpl | All chunks authenticate; plaintext recovered |
| SEC-DISK-07 | .vlt fails with wrong key | RandomAccessEncryptedSourceImpl with wrong key | AEADBadTagException on first chunk |

### RAM Security

| Test ID | Description | Tool/Method | Pass Criteria |
|---------|-------------|-------------|---------------|
| SEC-RAM-01 | ImageProxy buffer released | Capture photo → heap dump → analyze ByteArray | No ImageProxy byte buffers retained after `image.close()` |
| SEC-RAM-02 | Captured bytes wiped | Capture → keep → heap dump → search for plaintext | No full plaintext image in heap (only encrypted chunks) |
| SEC-RAM-03 | Master key not in heap | Vault unlocked → heap dump → search for MVK bytes | MasterVaultKey.encoded not found in plaintext |
| SEC-RAM-04 | Video buffers not retained | Record video → stop → heap dump | No large ByteArrays matching video frame sizes |

### Screenshot / Recents Protection

| Test ID | Description | Tool/Method | Pass Criteria |
|---------|-------------|-------------|---------------|
| SEC-SCR-01 | Screenshot blocked | `adb shell screencap /sdcard/test.png` during camera preview | Resulting PNG is black/blank |
| SEC-SCR-02 | Recent apps thumbnail blank | Open camera → home → recent apps | Thumbnail shows secure placeholder (not preview) |
| SEC-SCR-03 | Screen recording blocked | `adb shell screenrecord /sdcard/test.mp4` during camera | Resulting video is black frames |

### Temp File Cleanup

| Test ID | Description | Steps | Pass Criteria |
|---------|-------------|-------|---------------|
| SEC-TMP-01 | Video cancel → no temp files | Start video → cancel → list app files | No .mp4, .tmp, .part files |
| SEC-TMP-02 | App kill mid-video → cleanup on restart | Record → kill app → restart → list files | No orphaned partial .vlt (or auto-deleted on unlock) |
| SEC-TMP-03 | Photo capture error → no residue | Force ImageCapture error → check files | No partial .vlt |

---

## UI Tests (Compose + Espresso)

### Camera Preview Screen

| Test ID | Description | Steps | Assertions |
|---------|-------------|-------|------------|
| UI-CAM-01 | Preview renders | Launch camera screen | PreviewView visible; camera feed showing |
| UI-CAM-02 | Controls visible | Launch camera screen | Lens switch, mode toggle, flash, shutter all visible |
| UI-CAM-03 | Lens switch works | Tap lens switch | Camera feed changes (front↔back) |
| UI-CAM-04 | Mode toggle works | Tap photo/video toggle | Shutter ↔ Record button; UI updates |
| UI-CAM-05 | Flash cycle works | Photo mode → tap flash 3x | Icon cycles: Auto → On → Off → Auto |
| UI-CAM-06 | Torch toggle works | Video mode, rear cam → tap torch | Torch on/off; icon updates |
| UI-CAM-07 | Tap to focus | Tap preview at (x,y) | Focus indicator appears at tap point |
| UI-CAM-08 | FLAG_SECURE enforced | Screenshot during preview | Screenshot is blank |

### Review Screen

| Test ID | Description | Steps | Assertions |
|---------|-------------|-------|------------|
| UI-REV-01 | Photo review shows image | Capture → review | ImageView shows captured photo |
| UI-REV-02 | Video review plays | Record → review | PlayerView shows video; controls work |
| UI-REV-03 | Retake button works | Review → tap Retake | Returns to camera; no .vlt |
| UI-REV-04 | Keep button works | Review → tap Keep | Encrypts; returns to vault grid |
| UI-REV-05 | Audio off badge shown | Record no mic → review | "Audio off" badge visible |

### Navigation

| Test ID | Description | Steps | Assertions |
|---------|-------------|-------|------------|
| UI-NAV-01 | Vault → Camera → Vault | Unlock vault → camera → keep → back | Back at vault grid with new item |
| UI-NAV-02 | Camera → Back → Vault | Camera → back button/gesture | Returns to vault grid; no capture |
| UI-NAV-03 | Lock during camera → auth | Camera → lock app → unlock | Camera closed; requires re-auth |

---

## Edge Case Tests

| Test ID | Category | Scenario | Expected |
|---------|----------|----------|----------|
| EDGE-01 | Camera | No camera hardware | Graceful error: "No camera available" |
| EDGE-02 | Camera | Camera in use by other app | CameraAccessException caught; retry offered |
| EDGE-03 | Storage | Internal storage full | IOException caught; "Free up space" dialog |
| EDGE-04 | Memory | OOM on large capture | OutOfMemoryError caught; "Image too large" |
| EDGE-05 | Lifecycle | Rotate during capture | Capture completes; review upright |
| EDGE-06 | Lifecycle | Rotate during review | Review locks orientation or rotates correctly |
| EDGE-07 | Permissions | Camera permission revoked in settings | Next open shows rationale/settings link |
| EDGE-08 | Permissions | Mic permission revoked mid-video | Recording stops; silent portion saved |
| EDGE-09 | Vault | Vault locked during capture (timeout) | Capture cancelled; buffers wiped |
| EDGE-10 | Vault | Switch vault mid-capture | Not allowed (camera bound to current vault) |
| EDGE-11 | Video | Record 0 seconds (tap record+stop instantly) | Minimum duration enforced or handled gracefully |
| EDGE-12 | Video | Record > 4GB (FAT32 limit) | Not applicable (internal storage ext4/f2fs) |
| EDGE-13 | Corrupt | .vlt file truncated | Open fails with clear error; not crash |
| EDGE-14 | Corrupt | .vlt header magic wrong | Open fails with "Invalid vault file" |

---

## Performance Benchmarks (Manual/Automated)

| Benchmark | Target | Measurement Method |
|-----------|--------|-------------------|
| Photo capture → review | < 500ms (flagship), < 1000ms (mid) | SystemClock.elapsedRealtime() in test |
| Photo encrypt (12MP) | < 200ms | EncryptOutputStream timing |
| Video start latency | < 300ms | Record button → Recording callback |
| Encryption throughput | > 100 MB/s | Stream 100MB / time |
| Memory peak (photo) | < 100 MB | `Debug.getNativeHeapAllocatedSize()` |
| Memory peak (video) | < 50 MB | Same during 30s recording |
| Battery (10 min video) | < 5% drain | BatteryManager before/after |

---

## CI/CD Integration

```yaml
# .github/workflows/secure-camera-tests.yml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run unit tests
        run: ./gradlew testDebugUnitTest --tests "*camera*" --tests "*crypto*" --tests "*storage*"

  instrumented-tests:
    runs-on: macos-latest  # For Android emulator
    steps:
      - uses: actions/checkout@v4
      - name: Start emulator
        run: |
          emulator -avd test -no-window -no-audio -no-boot-anim &
          adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'
      - name: Run instrumented tests
        run: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vaultaire.android.camera.SecureCameraInstrumentedTest

  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build debug APK
        run: ./gradlew assembleDebug
      - name: Run security test suite
        run: |
          adb install app/build/outputs/apk/debug/app-debug.apk
          ./scripts/run-security-tests.sh  # Custom script for disk/RAM/screenshot tests
```

---

## Test Data & Fixtures

| Fixture | Description | Location |
|---------|-------------|----------|
| `test_pattern_1.json` | Valid 6-point pattern | `test/fixtures/` |
| `test_vault.vlt` | Pre-encrypted vault file | `test/fixtures/` |
| `test_master_key.bin` | 32-byte AES key | `test/fixtures/` |
| `sample_photo.jpg` | 12MP test photo | `test/fixtures/` |
| `sample_video.mp4` | 10s 1080p test video | `test/fixtures/` |
| `corrupted.vlt` | .vlt with flipped bit | `test/fixtures/` |

---

## Coverage Targets

| Package | Line Coverage | Branch Coverage |
|---------|--------------|-----------------|
| `com.vaultaire.android.camera` | 90% | 85% |
| `com.vaultaire.android.storage` | 95% | 90% |
| `com.vaultaire.android.crypto` | 95% | 90% |
| `com.vaultaire.android.vault` | 90% | 85% |

**Enforce in CI:**
```kotlin
// build.gradle.kts
tasks.withType<Test> {
    testLogging { events("passed", "skipped", "failed") }
}

tasks.named("testDebugUnitTest") {
    doLast {
        val report = file("build/reports/jacoco/testDebugUnitTest/html/index.html")
        if (report.exists()) {
            // Parse and fail if coverage < targets
        }
    }
}
```

---

## Sign-Off Checklist

Before merge to main:

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Instrumented tests pass on API 26, 28, 30, 33, 35
- [ ] Security tests pass (disk, RAM, screenshot, temp files)
- [ ] UI tests pass on phone + tablet
- [ ] Performance benchmarks within targets
- [ ] No `TODO`/`FIXME` in SecureCameraManager, EncryptedOutputStream
- [ ] MemoryHygiene.wipe() called on all sensitive buffers
- [ ] FLAG_SECURE set on CameraActivity
- [ ] No MediaStore writes in any code path
- [ ] `allowBackup=false` + `dataExtractionRules` exclude all domains
- [ ] Documentation updated (README, planning, implementation)