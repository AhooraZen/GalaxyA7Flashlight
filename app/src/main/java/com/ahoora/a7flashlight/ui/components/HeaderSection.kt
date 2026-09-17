package com.ahoora.a7flashlight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahoora.a7flashlight.R
import com.ahoora.a7flashlight.ui.theme.*

@Composable
fun HeaderSection(
    isRootGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.5.dp, if (isRootGranted) NeonGreen else DangerRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlashlightOn,
                contentDescription = null,
                tint = if (isRootGranted) NeonGreen else ElectricBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.device_title),
            style = Typography.titleLarge,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.device_subtitle),
            style = Typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Root badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isRootGranted) NeonGreenGlow else Color(0x33EF4444))
                .border(
                    1.dp,
                    if (isRootGranted) NeonGreen.copy(alpha = 0.6f) else DangerRed.copy(alpha = 0.6f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
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
    }
}
