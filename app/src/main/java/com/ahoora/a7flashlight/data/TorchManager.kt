package com.ahoora.a7flashlight.data

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object TorchManager {
    private const val TAG = "TorchManager"

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

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        Shell.enableVerboseLogging = true
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
        requestRoot()
    }

    fun requestRoot() {
        scope.launch {
            try {
                val isRoot = Shell.getShell().isRoot
                _isRootGranted.value = isRoot
                Log.d(TAG, "libsu Root Shell status: isRoot=$isRoot")
            } catch (e: Exception) {
                Log.e(TAG, "Error obtaining root shell", e)
                _isRootGranted.value = false
            }
        }
    }

    fun setRearTorch(enabled: Boolean, level: Int = _rearLevel.value) {
        val clamped = level.coerceIn(1, 5)
        _isRearOn.value = enabled
        _rearLevel.value = clamped

        val brightness = if (enabled) REAR_LEVEL_MAP[clamped] ?: 31 else 0
        val camVal = if (enabled) REAR_CAMERA_MAP[clamped] ?: "1009" else "0"

        val cmd = "echo $brightness > $REAR_LED_SYSFS; echo $camVal > $REAR_CAMERA_SYSFS"
        Shell.cmd(cmd).submit { result ->
            if (!result.isSuccess) {
                Log.e(TAG, "Rear command failed: $cmd, code=${result.code}")
            }
        }
    }

    fun setFrontTorch(enabled: Boolean, level: Int = _frontLevel.value) {
        val clamped = level.coerceIn(1, 5)
        _isFrontOn.value = enabled
        _frontLevel.value = clamped

        val brightness = if (enabled) FRONT_LEVEL_MAP[clamped] ?: 15 else 0
        val camVal = if (enabled) "1" else "0"

        // Set direct PMIC LED brightness and trigger camera node
        val cmd = if (enabled) {
            "echo $camVal > $FRONT_CAMERA_SYSFS; echo $brightness > $FRONT_LED_SYSFS"
        } else {
            "echo 0 > $FRONT_LED_SYSFS; echo 0 > $FRONT_CAMERA_SYSFS"
        }

        Shell.cmd(cmd).submit { result ->
            if (!result.isSuccess) {
                Log.e(TAG, "Front command failed: $cmd, code=${result.code}")
            }
        }
    }

    fun setBoth(enabled: Boolean, level: Int = 5) {
        setRearTorch(enabled, level)
        setFrontTorch(enabled, level)
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

    fun close() {
        Shell.cmd(
            "echo 0 > $REAR_LED_SYSFS; echo 0 > $REAR_CAMERA_SYSFS; echo 0 > $FRONT_LED_SYSFS; echo 0 > $FRONT_CAMERA_SYSFS"
        ).submit()
    }
}
