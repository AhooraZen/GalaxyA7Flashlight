package com.ahoora.a7flashlight.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    accentColor: Color,
    onToggle: (Boolean) -> Unit,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBorder by animateColorAsState(
        targetValue = if (isOn) accentColor.copy(alpha = 0.9f) else BorderSubtle,
        animationSpec = tween(durationMillis = 250),
        label = "cardBorder"
    )

    val animatedBg by animateColorAsState(
        targetValue = if (isOn) SurfaceCardActive else SurfaceCard,
        animationSpec = tween(durationMillis = 250),
        label = "cardBg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(animatedBg)
            .border(1.5.dp, animatedBorder, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            // Header Row: Icon, Title, Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isOn) accentColor.copy(alpha = 0.15f) else DeepMidnight)
                        .border(
                            1.dp,
                            if (isOn) accentColor.copy(alpha = 0.6f) else BorderSubtle,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isOn) accentColor else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (isOn) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = Typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Master Toggle Switch
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

            Spacer(modifier = Modifier.height(18.dp))

            // Brightness level text info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.brightness_level, level, level * 20),
                    style = Typography.labelSmall,
                    color = if (isOn) accentColor else TextMuted,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (isOn) stringResource(R.string.status_on) else stringResource(R.string.status_off),
                    style = Typography.labelSmall,
                    color = if (isOn) accentColor else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5-Level Discrete Slider
            Slider(
                value = level.toFloat(),
                onValueChange = { newLevel ->
                    onLevelChange(newLevel.toInt().coerceIn(1, 5))
                },
                valueRange = 1f..5f,
                steps = 3,
                enabled = isOn,
                colors = SliderDefaults.colors(
                    thumbColor = if (isOn) accentColor else TextMuted,
                    activeTrackColor = if (isOn) accentColor else BorderSubtle,
                    inactiveTrackColor = DeepMidnight,
                    activeTickColor = PureBlack,
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
                        color = if (isSelected) accentColor else TextMuted
                    )
                }
            }
        }
    }
}
