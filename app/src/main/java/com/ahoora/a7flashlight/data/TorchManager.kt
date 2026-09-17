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

    const val REAR_NODE = "/sys/class/camera/flash/rear_torch_flash"
    const val FRONT_NODE = "/sys/class/camera/flash/front_torch_flash"

    private val LEVEL_MAP = mapOf(
        0 to "0",
        1 to "1001",
        2 to "1002",
        3 to "1004",
        4 to "1006",
        5 to "1009"
    )

    private val _isRearOn = MutableStateFlow(false)
    val isRearOn: StateFlow<Boolean> = _isRearOn.asStateFlow()

    private val _rearLevel = MutableStateFlow(3)
    val rearLevel: StateFlow<Int> = _rearLevel.asStateFlow()

    private val _isFrontOn = MutableStateFlow(false)
    val isFrontOn: StateFlow<Boolean> = _isFrontOn.asStateFlow()

    private val _frontLevel = MutableStateFlow(3)
    val frontLevel: StateFlow<Int> = _frontLevel.asStateFlow()

    private val _isRootGranted = MutableStateFlow(false)
    val isRootGranted: StateFlow<Boolean> = _isRootGranted.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        Shell.enableVerboseLogging = true
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(15)
        )
        requestRoot()
    }

    fun requestRoot() {
        scope.launch {
            try {
                // Triggers Magisk Superuser prompt on first run
                val isRoot = Shell.getShell().isRoot
                _isRootGranted.value = isRoot
                Log.d(TAG, "libsu Shell initialized, isRoot=$isRoot")
            } catch (e: Exception) {
                Log.e(TAG, "Error obtaining root shell via libsu", e)
                _isRootGranted.value = false
            }
        }
    }

    private fun writeSysfs(node: String, value: String) {
        val cmd = "echo $value > $node"
        Log.d(TAG, "Submitting command: $cmd")
        Shell.cmd(cmd).submit { result ->
            if (!result.isSuccess) {
                Log.e(TAG, "Command failed: $cmd, code=${result.code}")
            }
        }
    }

    fun setRearTorch(enabled: Boolean, level: Int = _rearLevel.value) {
        val clamped = level.coerceIn(1, 5)
        _isRearOn.value = enabled
        _rearLevel.value = clamped

        val raw = if (enabled) LEVEL_MAP[clamped] ?: "1" else "0"
        writeSysfs(REAR_NODE, raw)
    }

    fun setFrontTorch(enabled: Boolean, level: Int = _frontLevel.value) {
        val clamped = level.coerceIn(1, 5)
        _isFrontOn.value = enabled
        _frontLevel.value = clamped

        val raw = if (enabled) LEVEL_MAP[clamped] ?: "1" else "0"
        writeSysfs(FRONT_NODE, raw)
    }

    fun setBoth(enabled: Boolean, level: Int = 3) {
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
        writeSysfs(REAR_NODE, "0")
        writeSysfs(FRONT_NODE, "0")
    }
}
