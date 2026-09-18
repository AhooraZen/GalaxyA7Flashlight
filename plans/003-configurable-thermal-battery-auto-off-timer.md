# Plan 003: Configurable Thermal & Battery Auto-Off Watchdog Timer with Custom Duration

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 6774816..HEAD -- app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt app/src/main/java/com/ahoora/a7flashlight/MainActivity.kt app/src/main/res/values/strings.xml app/src/main/res/values-fa/strings.xml`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status
- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: plans/002-bugfixes-lifecycle-performance-haptics.md
- **Category**: direction
- **Planned at**: commit `6774816`, 2026-09-18
- **Issue**: none

## Why this matters
The Samsung Galaxy A7 2018 (Exynos 7885) S2MU005 PMIC chip drives high current directly to both LEDs at maximum brightness (levels 5 / PMIC value 31). Leaving the flashlight turned on unattended in a pocket or on a table causes rapid battery drain, severe device overheating, and potential physical LED degradation.
Adding a user-configurable Auto-Off Watchdog Timer with preset options (30s, 1m, 2m, 3m, 5m, 10m, Never) AND a **Custom Duration picker (N seconds, N minutes, N hours)** with persistent storage (`SharedPreferences`) and an animated countdown chip in the OLED UI gives the user complete safety and precision.

## Current state
- `app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt`: StateFlows exist only for rear/front on/level states. No timer or watchdog coroutine exists.
- `app/src/main/java/com/ahoora/a7flashlight/ui/components/HeaderSection.kt`: Contains root status badge, but no timer status or configuration chip.
- `app/src/main/res/values/strings.xml` and `values-fa/strings.xml`: Do not contain timer strings.

## Commands you will need
| Purpose | Command | Expected on success |
|---|---|---|
| Build | `./gradlew assembleRelease` | `BUILD SUCCESSFUL` |
| Lint | `./gradlew lint` | `BUILD SUCCESSFUL` |

## Scope
**In scope**:
- `app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt`
- `app/src/main/java/com/ahoora/a7flashlight/ui/components/TimerDialog.kt` (new)
- `app/src/main/java/com/ahoora/a7flashlight/ui/components/HeaderSection.kt`
- `app/src/main/java/com/ahoora/a7flashlight/MainActivity.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-fa/strings.xml`

**Out of scope**:
- Quick Settings tile service (handled in Plan 004)
- Strobe / SOS mode (handled in Plan 005)

## Git workflow
- Branch: `advisor/003-auto-off-timer`
- Commit per step; message style: `feat: <description>`

## Steps

### Step 1: Add Auto-Off Timer Logic & State with Custom Seconds in `TorchManager.kt`
1. In `TorchManager.kt`, define timer state:
   ```kotlin
   // Timer duration in total seconds (0 = Never / Disabled)
   private val _autoOffSeconds = MutableStateFlow(180) // default 3 minutes
   val autoOffSeconds: StateFlow<Int> = _autoOffSeconds.asStateFlow()
   
   private val _remainingSeconds = MutableStateFlow<Int?>(null)
   val remainingSeconds: StateFlow<Int?> = _remainingSeconds.asStateFlow()
   
   private var timerJob: Job? = null
   
   fun setAutoOffSeconds(seconds: Int, context: Context? = null) {
       _autoOffSeconds.value = seconds.coerceAtLeast(0)
       context?.getSharedPreferences("torch_prefs", Context.MODE_PRIVATE)?.edit()
           ?.putInt("auto_off_seconds", _autoOffSeconds.value)?.apply()
       
       if (_isRearOn.value || _isFrontOn.value) {
           restartTimer()
       }
   }
   
   fun restartTimer() {
       timerJob?.cancel()
       val duration = _autoOffSeconds.value
       if (duration <= 0) {
           _remainingSeconds.value = null
           return
       }
       _remainingSeconds.value = duration
       timerJob = scope.launch {
           var left = duration
           while (isActive && left > 0) {
               delay(1000L)
               left--
               _remainingSeconds.value = left
           }
           if (isActive && left == 0) {
               close()
               _remainingSeconds.value = null
           }
       }
   }
   
   fun stopTimer() {
       timerJob?.cancel()
       timerJob = null
       _remainingSeconds.value = null
   }
   ```
2. In `setRearTorch`, `setFrontTorch`, `setBoth`:
   - If turning ON (any torch is ON) -> call `restartTimer()`.
   - If turning OFF (both rear and front are OFF) -> call `stopTimer()`.
3. In `close()`: Call `stopTimer()`.

**Verify**: `./gradlew compileReleaseKotlin` → `BUILD SUCCESSFUL`

### Step 2: Create `TimerDialog.kt` with Presets & Custom N Duration Input
1. Create `app/src/main/java/com/ahoora/a7flashlight/ui/components/TimerDialog.kt`:
   - Sleek dark OLED modal dialog with rounded corners (22.dp) and Cyan glow borders.
   - Quick Presets: 30s, 1m, 2m, 3m, 5m, 10m, Never.
   - **Custom Duration Section**:
     - Number input field (e.g. `15`)
     - Unit dropdown / segmented toggle: Seconds (ثانیه), Minutes (دقیقه), Hours (ساعت).
     - "Set Custom / اعمال تایمر دلخواه" button calculating total seconds `N * unitMultiplier` and calling `TorchManager.setAutoOffSeconds(customSeconds, context)`.
2. In `HeaderSection.kt`:
   - Add an interactive Timer chip next to or below the Root status badge:
     - Shows timer icon (`Icons.Default.Timer`).
     - When active & counting down: displays `⏳ 02:45` in Electric Cyan.
     - When inactive: displays configured setting (e.g., `⏱ 3m` or `⏱ 45s` or `⏱ 1h` or `⏱ Off`).
     - On tap: opens `TimerDialog`.

**Verify**: `./gradlew compileReleaseKotlin` → `BUILD SUCCESSFUL`

### Step 3: Add English & Persian Localized Strings
1. In `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="timer_title">Auto-Off Timer</string>
   <string name="timer_desc">Automatically turns off flashlight to save battery and prevent overheating</string>
   <string name="timer_30s">30 Seconds</string>
   <string name="timer_1m">1 Minute</string>
   <string name="timer_2m">2 Minutes</string>
   <string name="timer_3m">3 Minutes</string>
   <string name="timer_5m">5 Minutes</string>
   <string name="timer_10m">10 Minutes</string>
   <string name="timer_never">Never (Disabled)</string>
   <string name="timer_custom">Custom Duration</string>
   <string name="timer_unit_sec">Sec</string>
   <string name="timer_unit_min">Min</string>
   <string name="timer_unit_hr">Hour</string>
   <string name="timer_set_custom">Set Custom Timer</string>
   <string name="timer_active_format">%02d:%02d</string>
   ```
2. In `app/src/main/res/values-fa/strings.xml`:
   ```xml
   <string name="timer_title">تایمر خاموشی خودکار</string>
   <string name="timer_desc">خاموش کردن خودکار چراغ‌قوه جهت حفظ باتری و جلوگیری از داغ شدن</string>
   <string name="timer_30s">۳۰ ثانیه</string>
   <string name="timer_1m">۱ دقیقه</string>
   <string name="timer_2m">۲ دقیقه</string>
   <string name="timer_3m">۳ دقیقه</string>
   <string name="timer_5m">۵ دقیقه</string>
   <string name="timer_10m">۱۰ دقیقه</string>
   <string name="timer_never">غیرفعال (همیشه روشن)</string>
   <string name="timer_custom">زمان دلخواه</string>
   <string name="timer_unit_sec">ثانیه</string>
   <string name="timer_unit_min">دقیقه</string>
   <string name="timer_unit_hr">ساعت</string>
   <string name="timer_set_custom">تنظیم تایمر دلخواه</string>
   <string name="timer_active_format">%02d:%02d</string>
   ```

**Verify**: `./gradlew assembleRelease` → `BUILD SUCCESSFUL`

## Test plan
- Select 30s preset; verify flashlight turns off after 30s.
- Select Custom Duration "45" + "Sec"; verify 45s countdown.
- Select Custom Duration "2" + "Hour"; verify 7200s countdown.
- Turn off flashlight manually; verify countdown stops and resets.
- Verify timer selection persists across app restarts.

## Done criteria
- [ ] `./gradlew assembleRelease` exits 0.
- [ ] Presets and custom N seconds/minutes/hours duration work.
- [ ] Countdown chip displays remaining time.
- [ ] Preference saved in `SharedPreferences`.
- [ ] `plans/README.md` updated.

## STOP conditions
- If custom number input parsing throws `NumberFormatException`.
