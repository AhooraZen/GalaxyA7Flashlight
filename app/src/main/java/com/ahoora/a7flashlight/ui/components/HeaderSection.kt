package com.ahoora.a7flashlight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahoora.a7flashlight.R
import com.ahoora.a7flashlight.data.TorchManager
import com.ahoora.a7flashlight.ui.theme.*
import java.util.Locale

@Composable
fun HeaderSection(
    isRootGranted: Boolean,
    autoOffSeconds: Int,
    remainingSeconds: Int?,
    onTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(2.dp, if (isRootGranted) NeonGreen else DangerRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = if (isRootGranted) NeonGreen else ElectricCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.device_title),
            style = Typography.titleLarge,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = stringResource(R.string.device_subtitle),
            style = Typography.bodyMedium,
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status row: Root badge + Timer countdown chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Interactive Root status badge (tap to request root if not granted)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isRootGranted) NeonGreenGlow else DangerRedGlow)
                    .border(
                        1.dp,
                        if (isRootGranted) NeonGreen.copy(alpha = 0.5f) else DangerRed.copy(alpha = 0.5f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        if (!isRootGranted) {
                            TorchManager.requestRoot()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isRootGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isRootGranted) NeonGreen else DangerRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(if (isRootGranted) R.string.root_status_granted else R.string.root_status_denied),
                    color = if (isRootGranted) NeonGreen else DangerRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Interactive Auto-Off Timer chip (tap to open TimerDialog)
            val isCountingDown = remainingSeconds != null && remainingSeconds > 0
            val timerText = if (isCountingDown) {
                val rem = remainingSeconds ?: 0
                val min = rem / 60
                val sec = rem % 60
                String.format(Locale.US, "%02d:%02d", min, sec)
            } else {
                when {
                    autoOffSeconds <= 0 -> stringResource(R.string.timer_off)
                    autoOffSeconds < 60 -> "${autoOffSeconds}s"
                    autoOffSeconds < 3600 -> "${autoOffSeconds / 60}m"
                    else -> "${autoOffSeconds / 3600}h"
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isCountingDown) ElectricCyanGlow else SurfaceCard)
                    .border(
                        1.dp,
                        if (isCountingDown) ElectricCyan.copy(alpha = 0.7f) else BorderSubtle,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onTimerClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isCountingDown) Icons.Default.HourglassTop else Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (isCountingDown) ElectricCyan else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timerText,
                    color = if (isCountingDown) ElectricCyan else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isCountingDown) FontWeight.Bold else FontWeight.SemiBold
                )
            }
        }
    }
}
