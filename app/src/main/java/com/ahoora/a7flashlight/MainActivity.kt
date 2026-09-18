package com.ahoora.a7flashlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahoora.a7flashlight.data.TorchManager
import com.ahoora.a7flashlight.ui.components.HeaderSection
import com.ahoora.a7flashlight.ui.components.PresetsRow
import com.ahoora.a7flashlight.ui.components.ScreenLightDialog
import com.ahoora.a7flashlight.ui.components.StrobeSosSection
import com.ahoora.a7flashlight.ui.components.TimerDialog
import com.ahoora.a7flashlight.ui.components.TorchCard
import com.ahoora.a7flashlight.ui.theme.ElectricCyan
import com.ahoora.a7flashlight.ui.theme.GalaxyA7FlashlightTheme
import com.ahoora.a7flashlight.ui.theme.NeonGreen
import com.ahoora.a7flashlight.ui.theme.PureBlack

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TorchManager.init(this)
        setContent {
            GalaxyA7FlashlightTheme {
                FlashlightApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        TorchManager.requestRoot()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            TorchManager.close()
        }
    }
}

@Composable
fun FlashlightApp() {
    val isRearOn by TorchManager.isRearOn.collectAsStateWithLifecycle()
    val rearLevel by TorchManager.rearLevel.collectAsStateWithLifecycle()
    val isFrontOn by TorchManager.isFrontOn.collectAsStateWithLifecycle()
    val frontLevel by TorchManager.frontLevel.collectAsStateWithLifecycle()
    val isRootGranted by TorchManager.isRootGranted.collectAsStateWithLifecycle()
    val autoOffSeconds by TorchManager.autoOffSeconds.collectAsStateWithLifecycle()
    val remainingSeconds by TorchManager.remainingSeconds.collectAsStateWithLifecycle()
    val specialMode by TorchManager.specialMode.collectAsStateWithLifecycle()
    val strobeHz by TorchManager.strobeHz.collectAsStateWithLifecycle()

    var showTimerDialog by remember { mutableStateOf(false) }
    var showScreenLightDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PureBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            // App Header
            HeaderSection(
                isRootGranted = isRootGranted,
                autoOffSeconds = autoOffSeconds,
                remainingSeconds = remainingSeconds,
                onTimerClick = { showTimerDialog = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Rear Flash Card (Neon Green Accent)
            TorchCard(
                title = stringResource(R.string.rear_flash_title),
                description = stringResource(R.string.rear_flash_desc),
                icon = Icons.Default.CameraAlt,
                isOn = isRearOn,
                level = rearLevel,
                accentColor = NeonGreen,
                onToggle = { enabled ->
                    TorchManager.setRearTorch(enabled)
                },
                onLevelChange = { newLevel ->
                    TorchManager.setRearTorch(true, newLevel)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Front Selfie Flash Card (Electric Cyan Accent)
            TorchCard(
                title = stringResource(R.string.front_flash_title),
                description = stringResource(R.string.front_flash_desc),
                icon = Icons.Default.Face,
                isOn = isFrontOn,
                level = frontLevel,
                accentColor = ElectricCyan,
                onToggle = { enabled ->
                    TorchManager.setFrontTorch(enabled)
                },
                onLevelChange = { newLevel ->
                    TorchManager.setFrontTorch(true, newLevel)
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Master presets & bulk actions
            PresetsRow(
                onAllOn = {
                    TorchManager.setBoth(true)
                },
                onAllOff = {
                    TorchManager.setBoth(false)
                },
                onPresetSelect = { level ->
                    TorchManager.applyPresetToActiveOrBoth(level)
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Strobe & SOS Emergency Section
            StrobeSosSection(
                specialMode = specialMode,
                strobeHz = strobeHz,
                onStrobeToggle = { enable ->
                    if (enable) TorchManager.startStrobe(strobeHz) else TorchManager.stopSpecialMode()
                },
                onStrobeHzChange = { hz ->
                    TorchManager.startStrobe(hz)
                },
                onSosToggle = { enable ->
                    if (enable) TorchManager.startSos() else TorchManager.stopSpecialMode()
                },
                onScreenLightClick = {
                    showScreenLightDialog = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Dialogs
        if (showTimerDialog) {
            TimerDialog(
                currentSeconds = autoOffSeconds,
                onDismiss = { showTimerDialog = false }
            )
        }

        if (showScreenLightDialog) {
            ScreenLightDialog(
                onDismiss = { showScreenLightDialog = false }
            )
        }
    }
}
