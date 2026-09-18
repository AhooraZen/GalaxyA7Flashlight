# Galaxy A7 (2018) Root Flashlight

A high-performance, zero-lag root flashlight application designed specifically for the **Samsung Galaxy A7 2018** (`SM-A750F` / `SM-A750FN` / `SM-A750G` / `SM-A750N`) powered by the **Samsung Exynos 7885** chipset on **LineageOS 18.1+ / AOSP** custom ROMs.

---

## Overview

On AOSP and LineageOS custom ROMs for Exynos 7885 devices, standard Android `CameraManager` torch APIs often suffer from severe latency, camera server crashes, or missing hardware brightness steps.

This utility bypasses high-level camera services by interfacing directly with the **S2MU005 PMIC** kernel LED subsystem and hardware sysfs nodes using TopJohnWu's `libsu` root shell engine, delivering instant, zero-latency toggling and discrete multi-level brightness control.

---

## Hardware Architecture & Sysfs Mapping

The Samsung Galaxy A7 (2018) uses dedicated LED driver channels mapped through the Linux kernel sysfs interface:

| Component | PMIC Sysfs Node | Range | Camera HAL Sysfs Node | Range |
|---|---|---|---|---|
| **Rear Camera Flash** | `/sys/class/leds/leds-sec1/brightness` | `0` – `31` | `/sys/class/camera/flash/rear_torch_flash` | `0`, `1001`–`1009` |
| **Front Selfie Flash** | `/sys/class/leds/leds-sec2/brightness` | `0` – `15` | `/sys/class/camera/flash/front_torch_flash` | `0`, `1` |

### Brightness Level Mapping

- **Rear Flash (Level 1–5)**:
  - Level 1: `6` (20%)
  - Level 2: `12` (40%)
  - Level 3: `18` (60%)
  - Level 4: `24` (80%)
  - Level 5: `31` (100% max hardware output)
- **Front Selfie Flash (Level 1–5)**:
  - Level 1: `3` (20%)
  - Level 2: `6` (40%)
  - Level 3: `9` (60%)
  - Level 4: `12` (80%)
  - Level 5: `15` (100% max hardware output)

---

## Features

- **Independent Dual Torch Control**: Control Rear Flash and Front Selfie Flash separately or simultaneously.
- **5-Level Discrete Hardware Brightness**: Smooth step adjustment directly driving PMIC current levels.
- **Zero-Lag libsu Root Shell**: Direct native root execution via `com.github.topjohnwu.libsu:core` with async coroutine dispatch.
- **Master Quick Presets**: Instant bulk control buttons (All ON, All OFF, 20%, 40%, 60%, 80%, 100%).
- **Pure OLED Black UI**: 100% `#000000` background to minimize battery drain on Super AMOLED panels, accented with Neon Green (`#00E676`) and Electric Cyan (`#00E5FF`).
- **Bilingual Localization**: Full support for English and Persian (فارسی).
- **R8 / ProGuard Minified**: Stripped unused code and resources, targeting only `arm64-v8a` ABI for compact APK footprint.

---

## Prerequisites & Installation

### Requirements
1. **Device**: Samsung Galaxy A7 2018 (Exynos 7885 platform).
2. **ROM**: LineageOS 18.1 / 19.1 / 20 / 21, AOSP, or any rooted One UI / custom ROM.
3. **Root**: Magisk v24.0+ or KernelSU / APatch with root granted.

### Installation
1. Download the latest `GalaxyA7Flashlight-arm64-release.apk` from [Releases](https://github.com/AhooraZen/GalaxyA7Flashlight/releases).
2. Install APK on your device.
3. Open application and grant Superuser permission when prompted by Magisk / KernelSU.

---

## Building from Source

### Requirements
- JDK 17 (Eclipse Temurin or OpenJDK)
- Android SDK (API 34, Build-Tools 34.0.0)

### Build Release APK
```bash
# Clone the repository
git clone https://github.com/AhooraZen/GalaxyA7Flashlight.git
cd GalaxyA7Flashlight

# Build optimized release APK (arm64-v8a)
./gradlew assembleRelease
```

The signed release APK will be generated at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## CI/CD Pipeline

Automated GitHub Actions workflow (`.github/workflows/build_apk.yml`):
- **Pull Requests & Commits to `main`**: Compiles release APK and uploads build artifact.
- **Git Tags (`v*`) & Manual Dispatch**: Automatically publishes a new GitHub Release with attached `arm64-v8a` release APK.

---

## License

Open-source under the Apache License 2.0.
