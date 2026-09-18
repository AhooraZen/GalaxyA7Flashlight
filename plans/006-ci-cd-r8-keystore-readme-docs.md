# Plan 006: CI/CD Pipeline Fix, Release R8 Minification & Root Documentation

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 6774816..HEAD -- app/build.gradle.kts .github/workflows/build_apk.yml`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status
- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: dx
- **Planned at**: commit `6774816`, 2026-09-18
- **Issue**: none

## Why this matters
1. The GitHub Actions workflow currently deletes and recreates tag `v1.3.0` on every single push to `main`, which breaks git release history and fails if multiple commits are pushed.
2. `isMinifyEnabled = false` inflates APK size. Enabling R8/ProGuard shrinks unused resources and code while `libsu` rules protect native shell hooks.
3. The repository root is missing a `README.md`, leaving external users without documentation on device compatibility (Galaxy A7 2018 / Exynos 7885), hardware sysfs node mappings, root requirements, and build instructions.

## Current state
- `.github/workflows/build_apk.yml:43-53`: Hardcodes `gh release delete v1.3.0` and `gh release create v1.3.0` on every push to `main`.
- `app/build.gradle.kts:39`: `isMinifyEnabled = false`.
- Root directory `/home/ahoura/projects/GalaxyA7Flashlight`: Lacks `README.md`.

## Commands you will need
| Purpose | Command | Expected on success |
|---|---|---|
| Build Release | `./gradlew assembleRelease` | `BUILD SUCCESSFUL` |
| ProGuard / R8 Test | `ls -la app/build/outputs/apk/release/app-release.apk` | file exists, smaller size |

## Scope
**In scope**:
- `.github/workflows/build_apk.yml`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `README.md` (new)

**Out of scope**:
- Source Kotlin files in `app/src/main/java/`

## Git workflow
- Branch: `advisor/006-ci-cd-r8-readme`
- Commit per step; message style: `chore: <description>`

## Steps

### Step 1: Update `.github/workflows/build_apk.yml`
1. Separate PR verification from Release publishing:
   - On `push` to `main` and `pull_request`: Compile and run `./gradlew assembleRelease --stacktrace` and upload the APK as an artifact.
   - On `push` with tags `tags: ['v*']` or `workflow_dispatch`: Create a GitHub Release attaching the generated release APK using the dynamic tag name (`${{ github.ref_name }}`).

**Verify**: Check YAML syntax validity with `git diff` or Python yaml parser.

### Step 2: Enable R8 / ProGuard Minification in `app/build.gradle.kts`
1. In `app/build.gradle.kts`:
   ```kotlin
   buildTypes {
       release {
           isMinifyEnabled = true
           isShrinkResources = true
           signingConfig = signingConfigs.getByName("release")
           proguardFiles(
               getDefaultProguardFile("proguard-android-optimize.txt"),
               "proguard-rules.pro"
           )
       }
   }
   ```
2. In `app/proguard-rules.pro`: Add keep rules for `libsu` and Compose models:
   ```proguard
   -keep class com.topjohnwu.superuser.** { *; }
   ```

**Verify**: `./gradlew assembleRelease` → `BUILD SUCCESSFUL`

### Step 3: Create Comprehensive `README.md`
1. Create `README.md` at repository root:
   - Project overview: High-performance, zero-lag root flashlight for Samsung Galaxy A7 (2018) (SM-A750F / Exynos 7885) on LineageOS 18.1 / AOSP.
   - Hardware control architecture: S2MU005 PMIC sysfs nodes (`/sys/class/leds/leds-sec1/brightness`, `/sys/class/leds/leds-sec2/brightness`) and Camera HAL virtual nodes (`/sys/class/camera/flash/*_torch_flash`).
   - Key features: Independent Dual Torch (Rear + Front Selfie LED), 5-level discrete hardware brightness, Quick Settings Tiles & Long-press Popup Dialog, Auto-Off Thermal Watchdog, Strobe & SOS beacon, Soft OLED screen light, 100% OLED black theme, English & Persian localization.
   - Installation & Magisk Root instructions.
   - Building from source instructions (`./gradlew assembleRelease`).

**Verify**: `test -f README.md && echo "OK"` → `OK`

## Test plan
- Verify `./gradlew assembleRelease` succeeds with R8 minification enabled.
- Verify APK size is reduced.
- Verify `README.md` accurately documents all features and hardware nodes.

## Done criteria
- [ ] `./gradlew assembleRelease` exits 0 with minification active.
- [ ] `.github/workflows/build_apk.yml` handles dynamic tags for releases without overwriting.
- [ ] `README.md` exists and is complete.
- [ ] `plans/README.md` updated.

## STOP conditions
- If R8 minification strips required `libsu` native reflection classes (verify keep rules in `proguard-rules.pro`).
