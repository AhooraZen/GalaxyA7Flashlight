# Plan 002: Bug Fixes, Sysfs Hardware Sync, Lifecycle Guards & Haptic Feedback

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 6774816..HEAD -- app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt app/src/main/java/com/ahoora/a7flashlight/MainActivity.kt app/src/main/java/com/ahoora/a7flashlight/ui/components/TorchCard.kt app/src/main/java/com/ahoora/a7flashlight/ui/theme/Theme.kt`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status
- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug
- **Planned at**: commit `6774816`, 2026-09-18
- **Issue**: none

## Why this matters
Several user-facing bugs and performance defects currently degrade UX:
1. Dragging the brightness slider spams root shell execution on every continuous frame, causing command queue lag on the Exynos 7885 processor.
2. Rotating the device or changing system configuration triggers `onDestroy()` which unconditionally cuts physical LED power while leaving `TorchManager` state in `_isRearOn = true`, causing complete state desynchronization.
3. Launching the app always resets state to OFF in the UI even if the flashlight is already active.
4. Toggling the flashlight in dark environments lacks tactile feedback. Adding subtle haptic ticks gives confident physical feedback.
5. `Theme.kt` has an unsafe `view.context as Activity` cast that crashes in Preview and non-Activity contexts.

## Current state
- `app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt:93-129`: `setRearTorch` and `setFrontTorch` unconditionally execute `Shell.cmd(...)` without checking if the target level actually changed.
- `app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt:149-153`: `close()` executes `echo 0` sysfs commands but does not set `_isRearOn.value = false` or `_isFrontOn.value = false`.
- `app/src/main/java/com/ahoora/a7flashlight/MainActivity.kt:43-46`: `onDestroy()` calls `TorchManager.close()` without checking `isFinishing`.
- `app/src/main/java/com/ahoora/a7flashlight/ui/components/TorchCard.kt:153-157`: Continuous slider movement triggers `onLevelChange(newLevel.toInt())` on every drag coordinate.
- `app/src/main/java/com/ahoora/a7flashlight/ui/theme/Theme.kt:57`: Direct cast `view.context as Activity` can throw `ClassCastException`.

## Commands you will need
| Purpose | Command | Expected on success |
|---|---|---|
| Build & Assemble | `./gradlew assembleRelease --stacktrace` | `BUILD SUCCESSFUL` |
| Lint Check | `./gradlew lint` | `BUILD SUCCESSFUL` |

## Scope
**In scope**:
- `app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt`
- `app/src/main/java/com/ahoora/a7flashlight/MainActivity.kt`
- `app/src/main/java/com/ahoora/a7flashlight/ui/components/TorchCard.kt`
- `app/src/main/java/com/ahoora/a7flashlight/ui/theme/Theme.kt`

**Out of scope**:
- Native build configs (`app/build.gradle.kts`)
- New feature screens or tiles (handled in Plans 003, 004, 005)

## Git workflow
- Branch: `advisor/002-bugfixes-lifecycle-haptics`
- Commit per step; message style: `fix: <description>`

## Steps

### Step 1: Fix Throttling, State Sync & Sysfs Initial Read in `TorchManager.kt`
1. In `TorchManager.kt`:
   - Add initial hardware state sync: On `requestRoot()`, after root is acquired, read `/sys/class/leds/leds-sec1/brightness` and `/sys/class/leds/leds-sec2/brightness` via `Shell.cmd("cat $REAR_LED_SYSFS; cat $FRONT_LED_SYSFS").exec()` and update `_isRearOn` and `_isFrontOn` accordingly.
   - Guard `setRearTorch(enabled: Boolean, level: Int)`: If `_isRearOn.value == enabled && _rearLevel.value == clamped`, skip submitting redundant shell command.
   - Guard `setFrontTorch(enabled: Boolean, level: Int)`: If `_isFrontOn.value == enabled && _frontLevel.value == clamped`, skip submitting redundant shell command.
   - Update `close()`: In addition to sending sysfs zero values, update `_isRearOn.value = false` and `_isFrontOn.value = false`.
   - Set `Shell.enableVerboseLogging = false` (or `BuildConfig.DEBUG`) to eliminate production logcat overhead.

**Verify**: `./gradlew compileReleaseKotlin` → `BUILD SUCCESSFUL`

### Step 2: Guard Activity Lifecycle in `MainActivity.kt`
1. In `MainActivity.kt`:
   ```kotlin
   override fun onDestroy() {
       super.onDestroy()
       if (isFinishing) {
           TorchManager.close()
       }
   }
   ```
2. Optimize state collection by passing state values directly to components without triggering recomposition of unneeded elements.

**Verify**: `./gradlew compileReleaseKotlin` → `BUILD SUCCESSFUL`

### Step 3: Add Haptic Feedback and Slider Step Snapping in `TorchCard.kt`
1. Use `LocalHapticFeedback.current` in `TorchCard.kt`:
   - On switch toggle `onToggle`: perform `HapticFeedbackType.LongPress` or `HapticFeedbackType.TextHandleMove`.
   - On discrete slider step change: perform `HapticFeedbackType.TextHandleMove` when `newLevel.toInt() != level`.
2. Move static lists (e.g. `listOf(1, 2, 3, 4, 5)`) to top-level `private val STEP_LIST = listOf(1, 2, 3, 4, 5)` to eliminate allocation during recomposition.

**Verify**: `./gradlew compileReleaseKotlin` → `BUILD SUCCESSFUL`

### Step 4: Safe Activity Context Resolution in `Theme.kt`
1. In `Theme.kt`, safely resolve `Activity` from `view.context`:
   ```kotlin
   fun Context.findActivity(): Activity? = when (this) {
       is Activity -> this
       is ContextWrapper -> baseContext.findActivity()
       else -> null
   }
   ```
2. Null-check before setting `window.statusBarColor` and `window.navigationBarColor`.

**Verify**: `./gradlew assembleRelease` → `BUILD SUCCESSFUL`

## Test plan
- Verify discrete slider changes only send 1 command per step transition.
- Verify rotating screen maintains physical LED light and UI state.
- Verify `close()` sets reactive flows to `false`.
- Verify theme preview / non-Activity contexts do not crash.

## Done criteria
- [ ] `./gradlew assembleRelease` exits 0.
- [ ] `close()` resets `_isRearOn` and `_isFrontOn`.
- [ ] `onDestroy()` checks `isFinishing`.
- [ ] Slider snapping includes haptic feedback.
- [ ] `plans/README.md` status row updated.

## STOP conditions
- If `TorchManager.kt` method signatures mismatch existing calls.
- If `assembleRelease` fails on missing Compose haptic imports.
