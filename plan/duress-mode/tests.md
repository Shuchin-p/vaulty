# Duress Mode — Test Specification

## Unit Tests
- `AttemptThrottlerTest`: Verify persistence through "restart" (mocking SharedPreferences), check exponential backoff timing.
- `DuressWipeProtocolTest`: Create test `.dat` files, trigger wipe, verify 4-pass overwrite (read back files), verify file deletion.
- `DuressLaunchStateMachineTest`: Verify state transitions (`Idle` -> `Wiping` -> `DecoyLaunch`), verify analytics disabled.

## Integration Tests (Android InstrumentedTest)
- `DuressFlowTest`:
    - Launch app (direct to pattern).
    - Setup duress decoy.
    - Trigger duress.
    - Verify: Decoy vault opens, other files deleted (not just `file.delete()`, but checking `file.exists()` is false).
    - Verify: Sync workers cancelled (`WorkManager.getWorkInfosByTag`).

## Security Tests
- Verify `.dat` files were overwritten (forensic simulation).
- Check `SharedPreferences` for plaintext patterns (should not exist).
- Test constant-time hash comparison (timing side-channel).
- Android Lifecycle: Trigger wipe -> App crash/kill -> Restart -> Verify wipe completed.

## Edge Cases
- Wipe interruption (process killed).
- Sync isolation failure.
- Decoy vault is the ONLY vault (should reject duress setup).
