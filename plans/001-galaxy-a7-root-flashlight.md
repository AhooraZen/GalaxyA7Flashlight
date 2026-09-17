# Plan 001: Simple Zero-Lag Root Flashlight Control for Samsung Galaxy A7 (2018)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git status`
> Verify working tree is clean.

## Status
- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: direction
- **Planned at**: commit `init`, 2026-09-18
- **Issue**: none

## Why this matters
Samsung Galaxy A7 2018 (a7y18lte / Exynos 7885 running LineageOS 18.1 with Root) features both a rear camera flash and a dedicated physical front selfie LED flash. Standard Android flashlight apps only toggle the rear flash at a single intensity and cannot control the front selfie flash or fine-tune multi-level brightness. This project provides a sleek, lag-free Jetpack Compose root utility that controls both LEDs with 5 precise hardware intensity levels.

## Current state & Hardware Discoveries
Verified sysfs nodes and value mappings:
- **Rear Torch Sysfs Path**: `/sys/class/camera/flash/rear_torch_flash`
- **Front Selfie Torch Sysfs Path**: `/sys/class/camera/flash/front_torch_flash`
- **Hardware Values**:
  - `0`: Turn OFF
  - `1`: Turn ON default
  - `1001`: Level 1 (Lowest brightness)
  - `1002`: Level 2
  - `1004`: Level 3 (Medium brightness)
  - `1006`: Level 4
  - `1009`: Level 5 (Maximum hardware brightness)

## Performance & Zero-Lag Architecture (Crucial)
Spawning a new `su` process via `Runtime.getRuntime().exec("su -c echo ...")` on every slider change introduces 100-200ms fork latency and causes severe UI stutter.
**Zero-lag solution**:
- Maintain a single persistent `su` process in a background thread or Coroutine Dispatcher (IO).
- Keep its `OutputStreamWriter` open.
- On toggle or slider drag, directly write `"echo " + value + " > " + path + "\n"` and call `flush()`.
- Latency is sub-millisecond, completely smooth during slider drags.
- Fallback: If root is denied or process fails, log error and attempt Android Camera2 `CameraManager.setTorchMode()` for rear camera.

## UI/UX Design System (Jetpack Compose)
- **Palette**: Dark OLED (`#0A0F1D`), Surface Cards (`#141D32`), Neon Green Accent (`#00FF66`), Cyan/Electric Blue Glow (`#007AFF` / `#00E5FF`).
- **Cards**:
  1. **Header**: Device status chip (Root active, Samsung A7 2018), Language switch button (English / فارسی).
  2. **Rear Flash Card**: Title, master toggle switch with glow, 5-level slider with percentage labels.
  3. **Front Selfie Flash Card**: Title, master toggle switch with glow, 5-level slider with percentage labels.
  4. **Quick Presets**: 25%, 50%, 75%, 100% one-touch buttons for both torches.
- **Bilingual**: Full localization in English (`values/strings.xml`) and Persian (`values-fa/strings.xml`).

## Scope
**In scope**:
- `settings.gradle.kts`, `build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/ahoora/a7flashlight/MainActivity.kt`
- `app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt`
- `app/src/main/java/com/ahoora/a7flashlight/ui/theme/Color.kt`
- `app/src/main/java/com/ahoora/a7flashlight/ui/theme/Theme.kt`
- `app/src/main/java/com/ahoora/a7flashlight/ui/components/TorchCard.kt`
- `app/src/main/java/com/ahoora/a7flashlight/ui/components/HeaderSection.kt`
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-fa/strings.xml`
- `.github/workflows/build_apk.yml`

## Steps
### Step 1: Root & App Gradle Configuration
Create Gradle configuration with AGP 8.2+, Kotlin 1.9.22, Compose Compiler 1.5.8, Material3, and Java 17 compatibility.

### Step 2: Zero-Lag Root Engine (TorchManager.kt)
Implement singleton `TorchManager` maintaining an active root shell stream writing directly to `/sys/class/camera/flash/rear_torch_flash` and `/sys/class/camera/flash/front_torch_flash`.

### Step 3: Modern Compose UI & Bilingual Localization
Implement dark mode glassmorphism UI with reactive StateFlow/Compose States, rear and front controls, and full English & Persian resources.

### Step 4: GitHub Actions CI/CD Pipeline
Add `.github/workflows/build_apk.yml` building the APK with Java 17 and uploading debug APK artifacts.

### Step 5: Git Commit & Remote Repository Push
Use `gh repo create GalaxyA7Flashlight --public --source=. --remote=origin --push` to push code and trigger CI build.

## Done criteria
- [ ] All Kotlin and Gradle files created with zero syntax errors.
- [ ] Strings localized in both English and Persian.
- [ ] GitHub Actions workflow file exists and is valid YAML.
- [ ] Repository pushed to GitHub with active CI run.

## STOP conditions
- If `gh auth status` indicates unauthenticated state.
- If sysfs paths cannot be accessed or written.
