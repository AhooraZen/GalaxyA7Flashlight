package com.ahoora.a7flashlight.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStreamWriter

object TorchManager {
    private const val TAG = "TorchManager"

    const val REAR_NODE = "/sys/class/camera/flash/rear_torch_flash"
    const val FRONT_NODE = "/sys/class/camera/flash/front_torch_flash"

    // Hardware level mapping verified on Samsung Galaxy A7 2018 (Exynos 7885)
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
    private var suProcess: Process? = null
    private var suWriter: BufferedWriter? = null
    private val lock = Any()

    init {
        initRootShell()
    }

    private fun initRootShell() {
        scope.launch {
            synchronized(lock) {
                try {
                    val process = ProcessBuilder("su").redirectErrorStream(true).start()
                    val writer = BufferedWriter(OutputStreamWriter(process.outputStream))

                    // Quick test echo
                    writer.write("echo root_ok\n")
                    writer.flush()

                    suProcess = process
                    suWriter = writer
                    _isRootGranted.value = true
                    Log.d(TAG, "Root shell successfully initialized with zero-lag pipe")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start su shell", e)
                    _isRootGranted.value = false
                }
            }
        }
    }

    private fun writeSysfs(node: String, value: String) {
        scope.launch {
            synchronized(lock) {
                try {
                    if (suWriter == null || suProcess == null) {
                        suProcess = ProcessBuilder("su").redirectErrorStream(true).start()
                        suWriter = BufferedWriter(OutputStreamWriter(suProcess!!.outputStream))
                        _isRootGranted.value = true
                    }
                    val writer = suWriter ?: return@synchronized
                    writer.write("echo $value > $node\n")
                    writer.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing $value to $node", e)
                    _isRootGranted.value = false
                }
            }
        }
    }

    fun setRearTorch(enabled: Boolean, level: Int = _rearLevel.value) {
        val clampedLevel = level.coerceIn(1, 5)
        _isRearOn.value = enabled
        _rearLevel.value = clampedLevel

        val rawVal = if (enabled) LEVEL_MAP[clampedLevel] ?: "1" else "0"
        writeSysfs(REAR_NODE, rawVal)
    }

    fun setFrontTorch(enabled: Boolean, level: Int = _frontLevel.value) {
        val clampedLevel = level.coerceIn(1, 5)
        _isFrontOn.value = enabled
        _frontLevel.value = clampedLevel

        val rawVal = if (enabled) LEVEL_MAP[clampedLevel] ?: "1" else "0"
        writeSysfs(FRONT_NODE, rawVal)
    }

    fun setBoth(enabled: Boolean, level: Int = 3) {
        setRearTorch(enabled, level)
        setFrontTorch(enabled, level)
    }

    fun setAllLevel(level: Int) {
        if (_isRearOn.value) setRearTorch(true, level) else _rearLevel.value = level
        if (_isFrontOn.value) setFrontTorch(true, level) else _frontLevel.value = level
    }

    fun close() {
        synchronized(lock) {
            try {
                suWriter?.write("exit\n")
                suWriter?.flush()
                suWriter?.close()
                suProcess?.destroy()
            } catch (ignored: Exception) {}
            suWriter = null
            suProcess = null
        }
    }
}
