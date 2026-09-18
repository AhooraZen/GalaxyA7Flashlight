package com.ahoora.a7flashlight.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahoora.a7flashlight.R
import com.ahoora.a7flashlight.data.TorchManager
import com.ahoora.a7flashlight.ui.theme.*

@Composable
fun StrobeSosSection(
    specialMode: TorchManager.SpecialMode,
    strobeHz: Int,
    onStrobeToggle: (Boolean) -> Unit,
    onStrobeHzChange: (Int) -> Unit,
    onSosToggle: (Boolean) -> Unit,
    onScreenLightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isStrobeActive = specialMode == TorchManager.SpecialMode.STROBE
    val isSosActive = specialMode == TorchManager.SpecialMode.SOS

    val animatedBorder by animateColorAsState(
        targetValue = when {
            isSosActive -> DangerRed.copy(alpha = 0.9f)
            isStrobeActive -> ElectricCyan.copy(alpha = 0.9f)
            else -> BorderSubtle
        },
        animationSpec = tween(durationMillis = 250),
        label = "strobeBorder"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceCard)
            .border(1.5.dp, animatedBorder, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.strobe_title),
                style = Typography.titleMedium,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Strobe & SOS Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Strobe Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStrobeToggle(!isStrobeActive)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStrobeActive) ElectricCyan else DeepMidnight,
                        contentColor = if (isStrobeActive) PureBlack else ElectricCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isStrobeActive) ElectricCyan else BorderSubtle
                        )
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = if (isStrobeActive) PureBlack else ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.strobe_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // SOS Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSosToggle(!isSosActive)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSosActive) DangerRed else DeepMidnight,
                        contentColor = if (isSosActive) PureBlack else DangerRed
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isSosActive) DangerRed else BorderSubtle
                        )
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isSosActive) PureBlack else DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.sos_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Strobe frequency slider (1 to 15 Hz)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.strobe_btn),
                    style = Typography.labelSmall,
                    color = if (isStrobeActive) ElectricCyan else TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.strobe_freq, strobeHz),
                    style = Typography.labelSmall,
                    color = if (isStrobeActive) ElectricCyan else TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = strobeHz.toFloat(),
                onValueChange = { newHz ->
                    val hzInt = newHz.toInt().coerceIn(1, 15)
                    if (hzInt != strobeHz) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStrobeHzChange(hzInt)
                    }
                },
                valueRange = 1f..15f,
                steps = 13,
                colors = SliderDefaults.colors(
                    thumbColor = if (isStrobeActive) ElectricCyan else TextMuted,
                    activeTrackColor = if (isStrobeActive) ElectricCyan else BorderSubtle,
                    inactiveTrackColor = DeepMidnight,
                    activeTickColor = PureBlack,
                    inactiveTickColor = BorderSubtle
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Screen Soft Light Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onScreenLightClick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepMidnight,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LightMode,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.screen_light_btn),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
