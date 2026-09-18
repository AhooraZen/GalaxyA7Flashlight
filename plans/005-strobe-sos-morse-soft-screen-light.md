# Plan 005: Strobe Mode, Emergency SOS Morse Beacon & Soft Screen Light

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 6774816..HEAD -- app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt app/src/main/java/com/ahoora/a7flashlight/MainActivity.kt app/src/main/java/com/ahoora/a7flashlight/ui/components/PresetsRow.kt`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status
- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: plans/002-bugfixes-lifecycle-performance-haptics.md
- **Category**: direction
- **Planned at**: commit `6774816`, 2026-09-18
- **Issue**: none

## Why this matters
1. Outdoor safety & emergencies require rapid visual signaling: Strobe (1-15 Hz) for self-defense/visibility and international Morse code SOS (`... --- ...`) beaconing.
2. In pitch dark environments or during close-up reading/selfies, direct physical LEDs are blindingly bright. A Soft Screen Light (full-screen OLED white/warm amber lantern with brightness control) provides gentle, diffused ambient light.

## Current state
- `app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt`: Only supports static continuous on/off writes.
- `app/src/main/java/com/ahoora/a7flashlight/ui/components/PresetsRow.kt`: Contains master on/off and percentage chips only.
- `app/src/main/res/values/strings.xml`: Lacks strobe and screen light strings.

## Commands you will need
| Purpose | Command | Expected on success |
|---|---|---|
| Build | `./gradlew assembleRelease` | `BUILD SUCCESSFUL` |
| Lint | `./gradlew lint` | `BUILD SUCCESSFUL` |

## Scope
**In scope**:
- `app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt`
- `app/src/main/java/com/ahoora/a7flashlight/ui/components/StrobeSosSection.kt` (new)
- `app/src/main/java/com/ahoora/a7flashlight/ui/components/ScreenLightDialog.kt` (new)
- `app/src/main/java/com/ahoora/a7flashlight/MainActivity.kt`
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-fa/strings.xml`

**Out of scope**:
- Hardware PMIC flash strobe driver (we use software coroutine loop through `libsu` stream which has <1ms latency)

## Git workflow
- Branch: `advisor/005-strobe-sos-screen-light`
- Commit per step; message style: `feat: <description>`

## Steps

### Step 1: Implement Strobe & Morse SOS Coroutine Engine in `TorchManager.kt`
1. In `TorchManager.kt`:
   ```kotlin
   enum class SpecialMode { NONE, STROBE, SOS }
   
   private val _specialMode = MutableStateFlow(SpecialMode.NONE)
   val specialMode: StateFlow<SpecialMode> = _specialMode.asStateFlow()
   
   private val _strobeHz = MutableStateFlow(5)
   val strobeHz: StateFlow<Int> = _strobeHz.asStateFlow()
   
   private var specialJob: Job? = null
   
   fun startStrobe(hz: Int = _strobeHz.value) {
       stopSpecialMode()
       _specialMode.value = SpecialMode.STROBE
       _strobeHz.value = hz.coerceIn(1, 15)
       
       val delayMs = (1000L / (_strobeHz.value * 2)).coerceAtLeast(30L)
       specialJob = scope.launch {
           while (isActive) {
               Shell.cmd("echo 31 > $REAR_LED_SYSFS; echo 15 > $FRONT_LED_SYSFS").submit()
               delay(delayMs)
               Shell.cmd("echo 0 > $REAR_LED_SYSFS; echo 0 > $FRONT_LED_SYSFS").submit()
               delay(delayMs)
           }
       }
   }
   
   fun startSos() {
       stopSpecialMode()
       _specialMode.value = SpecialMode.SOS
       // Morse SOS: 3 short (150ms), 3 long (450ms), 3 short (150ms), pause 1200ms
       val dot = 150L
       val dash = 450L
       val elementGap = 150L
       val letterGap = 400L
       val wordGap = 1200L
       
       val sequence = listOf(
           dot, dot, dot,       // S
           dash, dash, dash,    // O
           dot, dot, dot        // S
       )
       
       specialJob = scope.launch {
           while (isActive) {
               // ... S
               repeat(3) { flash(dot); delay(elementGap) }
               delay(letterGap)
               // --- O
               repeat(3) { flash(dash); delay(elementGap) }
               delay(letterGap)
               // ... S
               repeat(3) { flash(dot); delay(elementGap) }
               delay(wordGap)
           }
       }
   }
   
   fun stopSpecialMode() {
       specialJob?.cancel()
       specialJob = null
       _specialMode.value = SpecialMode.NONE
       close()
   }
   ```

**Verify**: `./gradlew compileReleaseKotlin` → `BUILD SUCCESSFUL`

### Step 2: Implement `ScreenLightDialog.kt` (Soft Ambient Light)
1. Create `app/src/main/java/com/ahoora/a7flashlight/ui/components/ScreenLightDialog.kt`:
   - Fullscreen dialog that sets `WindowManager.LayoutParams.screenBrightness = 1.0f`.
   - Color selection: Pure White (`#FFFFFF`), Warm Amber (`#FFE4A0`), Soft Yellow (`#FFF8DC`), Candlelight (`#FF9900`).
   - Drag slider to dim screen illumination smoothly.
   - Restores original brightness on dismiss.

**Verify**: `./gradlew compileReleaseKotlin` → `BUILD SUCCESSFUL`

### Step 3: Implement `StrobeSosSection.kt` and Integrate into `MainActivity.kt`
1. Create `app/src/main/java/com/ahoora/a7flashlight/ui/components/StrobeSosSection.kt`:
   - Modern OLED card with:
     - "Strobe" toggle button + Frequency Slider (1 Hz to 15 Hz).
     - "SOS Emergency" pulse button with blinking warning indicator.
     - "Screen Light" launcher button with lantern icon.
2. In `MainActivity.kt`: Place `StrobeSosSection` below `PresetsRow`.

**Verify**: `./gradlew compileReleaseKotlin` → `BUILD SUCCESSFUL`

### Step 4: Add Bilingual Localized Strings
1. In `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="strobe_title">Strobe &amp; Emergency</string>
   <string name="strobe_btn">Strobe</string>
   <string name="strobe_freq">%1$d Hz</string>
   <string name="sos_btn">SOS Signal</string>
   <string name="screen_light_btn">Screen Soft Light</string>
   <string name="screen_light_title">Soft Screen Lantern</string>
   ```
2. In `app/src/main/res/values-fa/strings.xml`:
   ```xml
   <string name="strobe_title">حالت‌های اضطراری و چشمک‌زن</string>
   <string name="strobe_btn">چشمک‌زن (استروب)</string>
   <string name="strobe_freq">%1$d هرتز</string>
   <string name="sos_btn">سیگنال اضطراری SOS</string>
   <string name="screen_light_btn">نور ملایم صفحه</string>
   <string name="screen_light_title">فانوس ملایم نمایشگر</string>
   ```

**Verify**: `./gradlew assembleRelease` → `BUILD SUCCESSFUL`

## Test plan
- Tap Strobe button: verify rapid blinking at chosen frequency (e.g. 5 Hz).
- Adjust Strobe slider: verify frequency changes smoothly.
- Tap SOS button: verify international Morse SOS pattern (... --- ...).
- Tap Screen Light button: verify full-screen white/amber lantern appears with max screen brightness.
- Tap close or dismiss: verify screen brightness returns to system default and LEDs turn off.

## Done criteria
- [ ] `./gradlew assembleRelease` exits 0.
- [ ] Strobe and SOS modes run smoothly in coroutine scope without UI blocking.
- [ ] Screen Light dialog adjusts window brightness and dismisses cleanly.
- [ ] Full bilingual support in English and Persian.
- [ ] `plans/README.md` updated.

## STOP conditions
- If rapid shell execution in strobe loop hangs `libsu` shell process (verify `delayMs` has a safe 30ms floor).
