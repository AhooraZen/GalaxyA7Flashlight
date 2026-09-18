package com.ahoora.a7flashlight.ui.quickcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahoora.a7flashlight.R
import com.ahoora.a7flashlight.data.TorchManager
import com.ahoora.a7flashlight.ui.theme.*

class QuickControlDialogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TorchManager.init(this)
        TorchManager.requestRoot()

        setContent {
            GalaxyA7FlashlightTheme {
                QuickControlPopup(
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
fun QuickControlPopup(
    onDismiss: () -> Unit
) {
    val isRearOn by TorchManager.isRearOn.collectAsStateWithLifecycle()
    val rearLevel by TorchManager.rearLevel.collectAsStateWithLifecycle()
    val isFrontOn by TorchManager.isFrontOn.collectAsStateWithLifecycle()
    val frontLevel by TorchManager.frontLevel.collectAsStateWithLifecycle()

    // Full screen overlay with semi-transparent scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating compact OLED card dialog
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .background(DeepMidnight)
                .border(1.5.dp, BorderHighlight, RoundedCornerShape(26.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* consume click inside card */ }
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.quick_control_title),
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.quick_control_subtitle),
                                style = Typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rear Flash Control Row
                CompactControlRow(
                    title = stringResource(R.string.rear_flash_title),
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

                Spacer(modifier = Modifier.height(14.dp))

                // Front Selfie Flash Control Row
                CompactControlRow(
                    title = stringResource(R.string.front_flash_title),
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

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Action Buttons Row ("All ON", "All OFF")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { TorchManager.setBoth(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen.copy(alpha = 0.15f),
                            contentColor = NeonGreen
                        ),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = stringResource(R.string.both_on),
                            style = Typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { TorchManager.setBoth(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerRed.copy(alpha = 0.15f),
                            contentColor = DangerRed
                        ),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = stringResource(R.string.both_off),
                            style = Typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactControlRow(
    title: String,
    icon: ImageVector,
    isOn: Boolean,
    level: Int,
    accentColor: Color,
    onToggle: (Boolean) -> Unit,
    onLevelChange: (Int) -> Unit
) {
    val animatedBorder by animateColorAsState(
        targetValue = if (isOn) accentColor.copy(alpha = 0.7f) else BorderSubtle,
        animationSpec = tween(durationMillis = 200),
        label = "rowBorder"
    )

    val animatedBg by animateColorAsState(
        targetValue = if (isOn) SurfaceCardActive else SurfaceCard,
        animationSpec = tween(durationMillis = 200),
        label = "rowBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(animatedBg)
            .border(1.dp, animatedBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isOn) accentColor.copy(alpha = 0.15f) else DeepMidnight)
                        .border(
                            1.dp,
                            if (isOn) accentColor.copy(alpha = 0.5f) else BorderSubtle,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isOn) accentColor else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isOn) stringResource(R.string.brightness_level, level, level * 20) else stringResource(R.string.status_off),
                        style = Typography.labelSmall,
                        fontSize = 11.sp,
                        color = if (isOn) accentColor else TextMuted
                    )
                }

                Switch(
                    checked = isOn,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DeepMidnight,
                        uncheckedBorderColor = BorderSubtle
                    )
                )
            }

            if (isOn) {
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = level.toFloat(),
                    onValueChange = { newLevel ->
                        onLevelChange(newLevel.toInt().coerceIn(1, 5))
                    },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = DeepMidnight,
                        activeTickColor = PureBlack,
                        inactiveTickColor = BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
