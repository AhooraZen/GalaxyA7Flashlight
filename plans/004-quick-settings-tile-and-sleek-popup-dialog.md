# Plan 004: Quick Settings Tiles & Sleek Floating Popup Dialog on Long-Press

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 6774816..HEAD -- app/src/main/AndroidManifest.xml app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status
- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: plans/002-bugfixes-lifecycle-performance-haptics.md
- **Category**: direction
- **Planned at**: commit `6774816`, 2026-09-18
- **Issue**: none

## Why this matters
Users frequently need immediate flashlight access in the dark without unlocking the phone and navigating to the app launcher.
This plan introduces:
1. Dedicated Quick Settings Tiles in the Android notification shade (`TileService`) for one-tap toggling.
2. Long-pressing the Quick Settings tile triggers a lightweight, translucent floating OLED popup (`QuickControlDialogActivity`) that lets the user choose between Rear and Front LEDs and drag brightness levels in real-time without leaving their current screen.

## Current state
- `app/src/main/AndroidManifest.xml:10-27`: Only `MainActivity` is registered. No `TileService` or `QS_TILE_PREFERENCES` receiver exists.
- `app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt`: Controls hardware but has no listener mechanism to notify Android System `TileService.requestListeningState()`.

## Commands you will need
| Purpose | Command | Expected on success |
|---|---|---|
| Build | `./gradlew assembleRelease` | `BUILD SUCCESSFUL` |
| Lint | `./gradlew lint` | `BUILD SUCCESSFUL` |

## Scope
**In scope**:
- `app/src/main/java/com/ahoora/a7flashlight/service/MasterTorchTileService.kt` (new)
- `app/src/main/java/com/ahoora/a7flashlight/service/RearTorchTileService.kt` (new)
- `app/src/main/java/com/ahoora/a7flashlight/service/FrontTorchTileService.kt` (new)
- `app/src/main/java/com/ahoora/a7flashlight/ui/quickcontrol/QuickControlDialogActivity.kt` (new)
- `app/src/main/java/com/ahoora/a7flashlight/data/TorchManager.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-fa/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/drawable/` icons for tiles

**Out of scope**:
- Lockscreen widget / lockscreen camera gesture remapping

## Git workflow
- Branch: `advisor/004-qs-tiles-floating-popup`
- Commit per step; message style: `feat: <description>`

## Steps

### Step 1: Implement Quick Settings TileServices
1. Create `app/src/main/java/com/ahoora/a7flashlight/service/MasterTorchTileService.kt`:
   - Extends `android.service.quicksettings.TileService`.
   - In `onStartListening()`, observe `TorchManager.isRearOn` and `TorchManager.isFrontOn`. Update `qsTile.state = if (anyOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE` and `qsTile.updateTile()`.
   - In `onClick()`: If any torch is ON, turn all OFF (`TorchManager.setBoth(false)`). If all OFF, turn ON last active or rear torch. Update tile state immediately.
2. Create `RearTorchTileService.kt` and `FrontTorchTileService.kt` following the same pattern for dedicated individual LED toggling.
3. In `TorchManager.kt`, invoke `TileService.requestListeningState(context, ComponentName(context, MasterTorchTileService::class.java))` whenever state changes so tiles in the shade update synchronously.

**Verify**: `./gradlew compileReleaseKotlin` → `BUILD SUCCESSFUL`

### Step 2: Implement Sleek Floating OLED Popup (`QuickControlDialogActivity.kt`)
1. Create `app/src/main/java/com/ahoora/a7flashlight/ui/quickcontrol/QuickControlDialogActivity.kt`:
   - Extends `ComponentActivity`.
   - Uses Jetpack Compose with a compact, floating OLED card dialog:
     - Backdrop: semi-transparent scrim (`#CC030712`).
     - Card shape: `RoundedCornerShape(26.dp)`, border with Neon/Cyan glow (`BorderHighlight`), background `DeepMidnight`.
     - Compact Header: "Quick Flashlight / کنترل سریع" with close icon.
     - Rear Flash control row (compact icon, title, switch, mini 5-step discrete slider).
     - Front Selfie Flash control row (compact icon, title, switch, mini 5-step discrete slider).
     - "All ON" / "All OFF" quick buttons.
     - Changing switches or sliders triggers `TorchManager` immediately with haptic feedback.
   - Clicking outside the dialog card or pressing Back calls `finish()`.
2. Add translucent theme in `app/src/main/res/values/themes.xml`:
   ```xml
   <style name="Theme.GalaxyA7Flashlight.Dialog" parent="android:Theme.Material.NoActionBar">
       <item name="android:windowIsTranslucent">true</item>
       <item name="android:windowBackground">@android:color/transparent</item>
       <item name="android:windowContentOverlay">@null</item>
       <item name="android:windowNoTitle">true</item>
       <item name="android:statusBarColor">@android:color/transparent</item>
       <item name="android:navigationBarColor">@android:color/transparent</item>
   </style>
   ```

**Verify**: `./gradlew compileReleaseKotlin` → `BUILD SUCCESSFUL`

### Step 3: Register Tiles and Long-Press Intent Filter in `AndroidManifest.xml`
1. In `app/src/main/AndroidManifest.xml`:
   ```xml
   <!-- Quick Settings Tiles -->
   <service
       android:name=".service.MasterTorchTileService"
       android:icon="@android:drawable/ic_dialog_info"
       android:label="@string/tile_master_title"
       android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"
       android:exported="true">
       <intent-filter>
           <action android:name="android.service.quicksettings.action.QS_TILE" />
       </intent-filter>
   </service>

   <service
       android:name=".service.RearTorchTileService"
       android:icon="@android:drawable/ic_dialog_info"
       android:label="@string/tile_rear_title"
       android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"
       android:exported="true">
       <intent-filter>
           <action android:name="android.service.quicksettings.action.QS_TILE" />
       </intent-filter>
   </service>

   <service
       android:name=".service.FrontTorchTileService"
       android:icon="@android:drawable/ic_dialog_info"
       android:label="@string/tile_front_title"
       android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"
       android:exported="true">
       <intent-filter>
           <action android:name="android.service.quicksettings.action.QS_TILE" />
       </intent-filter>
   </service>

   <!-- Floating Dialog Activity triggered on QS Tile Long-Press -->
   <activity
       android:name=".ui.quickcontrol.QuickControlDialogActivity"
       android:theme="@style/Theme.GalaxyA7Flashlight.Dialog"
       android:excludeFromRecents="true"
       android:noHistory="true"
       android:exported="true">
       <intent-filter>
           <action android:name="android.service.quicksettings.action.QS_TILE_PREFERENCES" />
           <category android:name="android.intent.category.DEFAULT" />
       </intent-filter>
   </activity>
   ```

**Verify**: `./gradlew assembleRelease` → `BUILD SUCCESSFUL`

### Step 4: Add Bilingual Localized Strings
1. In `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="tile_master_title">A7 Flashlight</string>
   <string name="tile_rear_title">Rear Flash</string>
   <string name="tile_front_title">Front Flash</string>
   <string name="quick_control_title">Quick Controls</string>
   ```
2. In `app/src/main/res/values-fa/strings.xml`:
   ```xml
   <string name="tile_master_title">چراغ‌قوه A7</string>
   <string name="tile_rear_title">فلش پشت</string>
   <string name="tile_front_title">فلش جلو</string>
   <string name="quick_control_title">کنترل سریع</string>
   ```

**Verify**: `./gradlew assembleRelease` → `BUILD SUCCESSFUL`

## Test plan
- Add Quick Settings tile to notification panel in LineageOS 18.1.
- Tap tile: verify torch turns ON immediately and tile turns active/highlighted.
- Tap tile again: verify torch turns OFF and tile returns to inactive.
- Long-press tile: verify `QuickControlDialogActivity` opens instantly as a floating popup dialog over the current screen.
- Drag sliders inside popup: verify hardware brightness adjusts smoothly in real-time.
- Tap outside popup: verify dialog dismisses cleanly without leaving orphaned activity.

## Done criteria
- [ ] `./gradlew assembleRelease` exits 0.
- [ ] TileServices declared with `BIND_QUICK_SETTINGS_TILE`.
- [ ] Long-press `QS_TILE_PREFERENCES` launches translucent floating Compose dialog.
- [ ] Real-time control works without opening the full application.
- [ ] `plans/README.md` updated.

## STOP conditions
- If Android system does not launch `QuickControlDialogActivity` on long-press (verify `QS_TILE_PREFERENCES` intent filter and `exported="true"`).
- If translucent dialog background appears solid black on older Android versions.
