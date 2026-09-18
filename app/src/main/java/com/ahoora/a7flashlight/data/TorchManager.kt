package com.ahoora.a7flashlight.data

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import android.util.Log
import com.ahoora.a7flashlight.service.FrontTorchTileService
import com.ahoora.a7flashlight.service.MasterTorchTileService
import com.ahoora.a7flashlight.service.RearTorchTileService
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object TorchManager {
    private const val TAG = "TorchManager"
    private const val PREFS_NAME = "torch_prefs"
    private const val KEY_AUTO_OFF_SECONDS = "auto_off_seconds"

    private var appContext: Context? = null

    // Primary Linux kernel S2MU005 LED subsystem nodes (Direct PMIC Control)
    const val REAR_LED_SYSFS = "/sys/class/leds/leds-sec1/brightness"
    const val FRONT_LED_SYSFS = "/sys/class/leds/leds-sec2/brightness"

    // Secondary Camera Flash HAL virtual nodes
    const val REAR_CAMERA_SYSFS = "/sys/class/camera/flash/rear_torch_flash"
    const val FRONT_CAMERA_SYSFS = "/sys/class/camera/flash/front_torch_flash"

    // Rear brightness mapping (max 31): 0, Level 1 (6), Level 2 (12), Level 3 (18), Level 4 (24), Level 5 (31)
    private val REAR_LEVEL_MAP = mapOf(
        0 to 0,
        1 to 6,
        2 to 12,
        3 to 18,
        4 to 24,
        5 to 31
    )

    // Rear camera HAL level mapping
    private val REAR_CAMERA_MAP = mapOf(
        0 to "0",
        1 to "1001",
        2 to "1002",
        3 to "1004",
        4 to "1006",
        5 to "1009"
    )

    // Front brightness mapping (max 15): 0, Level 1 (3), Level 2 (6), Level 3 (9), Level 4 (12), Level 5 (15)
    private val FRONT_LEVEL_MAP = mapOf(
        0 to 0,
        1 to 3,
        2 to 6,
        3 to 9,
        4 to 12,
        5 to 15
    )

    enum class SpecialMode { NONE, STROBE, SOS }

    private val _isRearOn = MutableStateFlow(false)
    val isRearOn: StateFlow<Boolean> = _isRearOn.asStateFlow()

    private val _rearLevel = MutableStateFlow(5)
    val rearLevel: StateFlow<Int> = _rearLevel.asStateFlow()

    private val _isFrontOn = MutableStateFlow(false)
    val isFrontOn: StateFlow<Boolean> = _isFrontOn.asStateFlow()

    private val _frontLevel = MutableStateFlow(5)
    val frontLevel: StateFlow<Int> = _frontLevel.asStateFlow()

    private val _isRootGranted = MutableStateFlow(false)
    val isRootGranted: StateFlow<Boolean> = _isRootGranted.asStateFlow()

    // Timer duration in total seconds (0 = Never / Disabled)
    private val _autoOffSeconds = MutableStateFlow(180) // default 3 minutes
    val autoOffSeconds: StateFlow<Int> = _autoOffSeconds.asStateFlow()

    private val _remainingSeconds = MutableStateFlow<Int?>(null)
    val remainingSeconds: StateFlow<Int?> = _remainingSeconds.asStateFlow()

    private val _specialMode = MutableStateFlow(SpecialMode.NONE)
    val specialMode: StateFlow<SpecialMode> = _specialMode.asStateFlow()

    private val _strobeHz = MutableStateFlow(5)
    val strobeHz: StateFlow<Int> = _strobeHz.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var timerJob: Job? = null
    private var specialJob: Job? = null

    init {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
        requestRoot()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        initPrefs(context)
        requestListeningState()
    }

    fun initPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _autoOffSeconds.value = prefs.getInt(KEY_AUTO_OFF_SECONDS, 180)
    }

    fun requestListeningState() {
        val ctx = appContext ?: return
        try {
            TileService.requestListeningState(ctx, ComponentName(ctx, MasterTorchTileService::class.java))
            TileService.requestListeningState(ctx, ComponentName(ctx, RearTorchTileService::class.java))
            TileService.requestListeningState(ctx, ComponentName(ctx, FrontTorchTileService::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting tile listening state", e)
        }
    }

    fun requestRoot() {
        scope.launch {
            try {
                val isRoot = Shell.getShell().isRoot
                _isRootGranted.value = isRoot
                Log.d(TAG, "libsu Root Shell status: isRoot=$isRoot")
                if (isRoot) {
                    val out = Shell.cmd("cat $REAR_LED_SYSFS", "cat $FRONT_LED_SYSFS").exec().out
                    if (out.isNotEmpty()) {
                        val rearVal = out.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
                        val frontVal = out.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                        _isRearOn.value = rearVal > 0
                        _isFrontOn.value = frontVal > 0
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error obtaining root shell", e)
                _isRootGranted.value = false
            }
        }
    }

    fun setAutoOffSeconds(seconds: Int, context: Context? = null) {
        val valid = seconds.coerceAtLeast(0)
        _autoOffSeconds.value = valid
        val ctx = context ?: appContext
        ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()
            ?.putInt(KEY_AUTO_OFF_SECONDS, valid)?.apply()

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

    fun setRearTorch(enabled: Boolean, level: Int = _rearLevel.value) {
        val clamped = level.coerceIn(1, 5)
        if (_isRearOn.value == enabled && _rearLevel.value == clamped && _specialMode.value == SpecialMode.NONE) {
            return
        }
        if (_specialMode.value != SpecialMode.NONE) {
            specialJob?.cancel()
            specialJob = null
            _specialMode.value = SpecialMode.NONE
        }

        _isRearOn.value = enabled
        _rearLevel.value = clamped

        val brightness = if (enabled) REAR_LEVEL_MAP[clamped] ?: 31 else 0
        val camVal = if (enabled) REAR_CAMERA_MAP[clamped] ?: "1009" else "0"

        val cmd = if (enabled) {
            "echo $camVal > $REAR_CAMERA_SYSFS; echo $brightness > $REAR_LED_SYSFS"
        } else {
            "echo 0 > $REAR_CAMERA_SYSFS; echo 0 > $REAR_LED_SYSFS"
        }
        Shell.cmd(cmd).submit { result ->
            if (!result.isSuccess) {
                Log.e(TAG, "Rear command failed: $cmd, code=${result.code}")
            }
        }

        if (enabled || _isFrontOn.value) {
            restartTimer()
        } else {
            stopTimer()
        }
        requestListeningState()
    }

    fun setFrontTorch(enabled: Boolean, level: Int = _frontLevel.value) {
        val clamped = level.coerceIn(1, 5)
        if (_isFrontOn.value == enabled && _frontLevel.value == clamped && _specialMode.value == SpecialMode.NONE) {
            return
        }
        if (_specialMode.value != SpecialMode.NONE) {
            specialJob?.cancel()
            specialJob = null
            _specialMode.value = SpecialMode.NONE
        }

        _isFrontOn.value = enabled
        _frontLevel.value = clamped

        val brightness = if (enabled) FRONT_LEVEL_MAP[clamped] ?: 15 else 0
        val camVal = if (enabled) "1" else "0"

        val cmd = if (enabled) {
            "echo $camVal > $FRONT_CAMERA_SYSFS; echo $brightness > $FRONT_LED_SYSFS"
        } else {
            "echo 0 > $FRONT_CAMERA_SYSFS; echo 0 > $FRONT_LED_SYSFS"
        }

        Shell.cmd(cmd).submit { result ->
            if (!result.isSuccess) {
                Log.e(TAG, "Front command failed: $cmd, code=${result.code}")
            }
        }

        if (enabled || _isRearOn.value) {
            restartTimer()
        } else {
            stopTimer()
        }
        requestListeningState()
    }

    fun setBoth(enabled: Boolean, level: Int = 5) {
        val clamped = level.coerceIn(1, 5)
        if (_specialMode.value != SpecialMode.NONE) {
            specialJob?.cancel()
            specialJob = null
            _specialMode.value = SpecialMode.NONE
        }

        _isRearOn.value = enabled
        _rearLevel.value = clamped
        _isFrontOn.value = enabled
        _frontLevel.value = clamped

        val rearBright = if (enabled) REAR_LEVEL_MAP[clamped] ?: 31 else 0
        val rearCam = if (enabled) REAR_CAMERA_MAP[clamped] ?: "1009" else "0"
        val frontBright = if (enabled) FRONT_LEVEL_MAP[clamped] ?: 15 else 0
        val frontCam = if (enabled) "1" else "0"

        val cmd = if (enabled) {
            "echo $rearCam > $REAR_CAMERA_SYSFS; echo $rearBright > $REAR_LED_SYSFS; echo $frontCam > $FRONT_CAMERA_SYSFS; echo $frontBright > $FRONT_LED_SYSFS"
        } else {
            "echo 0 > $REAR_CAMERA_SYSFS; echo 0 > $FRONT_CAMERA_SYSFS; echo 0 > $REAR_LED_SYSFS; echo 0 > $FRONT_LED_SYSFS"
        }
        Shell.cmd(cmd).submit()

        if (enabled) {
            restartTimer()
        } else {
            stopTimer()
        }
        requestListeningState()
    }

    fun applyPresetToActiveOrBoth(level: Int) {
        val clamped = level.coerceIn(1, 5)
        val rearActive = _isRearOn.value
        val frontActive = _isFrontOn.value

        if (!rearActive && !frontActive) {
            setBoth(true, clamped)
        } else {
            if (rearActive) setRearTorch(true, clamped)
            if (frontActive) setFrontTorch(true, clamped)
        }
    }

    fun startStrobe(hz: Int = _strobeHz.value) {
        stopTimer()
        specialJob?.cancel()
        _isRearOn.value = false
        _isFrontOn.value = false
        _specialMode.value = SpecialMode.STROBE
        val clampedHz = hz.coerceIn(1, 15)
        _strobeHz.value = clampedHz

        val delayMs = (1000L / (clampedHz * 2)).coerceAtLeast(30L)
        specialJob = scope.launch {
            while (isActive) {
                Shell.cmd("echo 31 > $REAR_LED_SYSFS; echo 15 > $FRONT_LED_SYSFS").submit()
                delay(delayMs)
                Shell.cmd("echo 0 > $REAR_LED_SYSFS; echo 0 > $FRONT_LED_SYSFS").submit()
                delay(delayMs)
            }
        }
        requestListeningState()
    }

    fun startSos() {
        stopTimer()
        specialJob?.cancel()
        _isRearOn.value = false
        _isFrontOn.value = false
        _specialMode.value = SpecialMode.SOS

        val dot = 150L
        val dash = 450L
        val elementGap = 150L
        val letterGap = 400L
        val wordGap = 1200L

        specialJob = scope.launch {
            while (isActive) {
                // ... S
                repeat(3) {
                    flash(dot)
                    delay(elementGap)
                }
                delay(letterGap)
                // --- O
                repeat(3) {
                    flash(dash)
                    delay(elementGap)
                }
                delay(letterGap)
                // ... S
                repeat(3) {
                    flash(dot)
                    delay(elementGap)
                }
                delay(wordGap)
            }
        }
        requestListeningState()
    }

    private suspend fun flash(durationMs: Long) {
        Shell.cmd("echo 31 > $REAR_LED_SYSFS; echo 15 > $FRONT_LED_SYSFS").submit()
        delay(durationMs)
        Shell.cmd("echo 0 > $REAR_LED_SYSFS; echo 0 > $FRONT_LED_SYSFS").submit()
    }

    fun stopSpecialMode() {
        specialJob?.cancel()
        specialJob = null
        _specialMode.value = SpecialMode.NONE
        close()
    }

    fun close() {
        stopTimer()
        if (specialJob != null) {
            specialJob?.cancel()
            specialJob = null
            _specialMode.value = SpecialMode.NONE
        }
        _isRearOn.value = false
        _isFrontOn.value = false
        Shell.cmd(
            "echo 0 > $REAR_CAMERA_SYSFS; echo 0 > $FRONT_CAMERA_SYSFS; echo 0 > $REAR_LED_SYSFS; echo 0 > $FRONT_LED_SYSFS"
        ).submit()
        requestListeningState()
    }
}
