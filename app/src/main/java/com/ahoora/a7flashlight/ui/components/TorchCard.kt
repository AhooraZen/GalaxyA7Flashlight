package com.ahoora.a7flashlight.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahoora.a7flashlight.R
import com.ahoora.a7flashlight.ui.theme.*

@Composable
fun TorchCard(
    title: String,
    description: String,
    icon: ImageVector,
    isOn: Boolean,
    level: Int,
    onToggle: (Boolean) -> Unit,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isOn) NeonGreen.copy(alpha = 0.8f) else BorderSubtle,
        label = "cardBorder"
    )

    val cardBackground = if (isOn) {
        Brush.verticalGradient(
            colors = listOf(SurfaceCard, SurfaceDark)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(SurfaceCard.copy(alpha = 0.6f), SurfaceDark.copy(alpha = 0.6f))
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackground)
            .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            // Header Row: Icon, Title, Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isOn) NeonGreenGlow else BorderSubtle.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isOn) NeonGreen else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOn) TextPrimary else TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = Typography.bodyMedium,
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                // Master Toggle Switch
                Switch(
                    checked = isOn,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BgDark,
                        checkedTrackColor = NeonGreen,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceDark,
                        uncheckedBorderColor = BorderSubtle
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Brightness level text info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.brightness_level, level, level * 20),
                    style = Typography.labelSmall,
                    color = if (isOn) SkyBlue else TextDisabled,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (isOn) stringResource(R.string.status_on) else stringResource(R.string.status_off),
                    style = Typography.labelSmall,
                    color = if (isOn) NeonGreen else TextDisabled,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5-Level Discrete Slider
            Slider(
                value = level.toFloat(),
                onValueChange = { newLevel ->
                    onLevelChange(newLevel.toInt().coerceIn(1, 5))
                },
                valueRange = 1f..5f,
                steps = 3, // 5 values: 1, 2, 3, 4, 5 -> 3 steps between
                enabled = isOn,
                colors = SliderDefaults.colors(
                    thumbColor = if (isOn) NeonGreen else TextDisabled,
                    activeTrackColor = if (isOn) ElectricBlue else BorderSubtle,
                    inactiveTrackColor = SurfaceDark,
                    activeTickColor = BgDark,
                    inactiveTickColor = BorderSubtle
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Step Indicator Markers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(1, 2, 3, 4, 5).forEach { step ->
                    val isSelected = step == level && isOn
                    Text(
                        text = "${step * 20}%",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NeonGreen else TextDisabled
                    )
                }
            }
        }
    }
}
