package com.ahoora.a7flashlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahoora.a7flashlight.data.TorchManager
import com.ahoora.a7flashlight.ui.components.HeaderSection
import com.ahoora.a7flashlight.ui.components.PresetsRow
import com.ahoora.a7flashlight.ui.components.TorchCard
import com.ahoora.a7flashlight.ui.theme.BgDark
import com.ahoora.a7flashlight.ui.theme.GalaxyA7FlashlightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GalaxyA7FlashlightTheme {
                FlashlightApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TorchManager.close()
    }
}

@Composable
fun FlashlightApp() {
    val isRearOn by TorchManager.isRearOn.collectAsStateWithLifecycle()
    val rearLevel by TorchManager.rearLevel.collectAsStateWithLifecycle()
    val isFrontOn by TorchManager.isFrontOn.collectAsStateWithLifecycle()
    val frontLevel by TorchManager.frontLevel.collectAsStateWithLifecycle()
    val isRootGranted by TorchManager.isRootGranted.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BgDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // App Header
            HeaderSection(isRootGranted = isRootGranted)

            Spacer(modifier = Modifier.height(24.dp))

            // Rear Flash Card
            TorchCard(
                title = stringResource(R.string.rear_flash_title),
                description = stringResource(R.string.rear_flash_desc),
                icon = Icons.Default.CameraAlt,
                isOn = isRearOn,
                level = rearLevel,
                onToggle = { enabled ->
                    TorchManager.setRearTorch(enabled)
                },
                onLevelChange = { newLevel ->
                    TorchManager.setRearTorch(true, newLevel)
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Front Selfie Flash Card
            TorchCard(
                title = stringResource(R.string.front_flash_title),
                description = stringResource(R.string.front_flash_desc),
                icon = Icons.Default.Face,
                isOn = isFrontOn,
                level = frontLevel,
                onToggle = { enabled ->
                    TorchManager.setFrontTorch(enabled)
                },
                onLevelChange = { newLevel ->
                    TorchManager.setFrontTorch(true, newLevel)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Master presets & bulk actions
            PresetsRow(
                onAllOn = {
                    TorchManager.setBoth(true)
                },
                onAllOff = {
                    TorchManager.setBoth(false)
                },
                onPresetSelect = { level ->
                    TorchManager.setAllLevel(level)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
