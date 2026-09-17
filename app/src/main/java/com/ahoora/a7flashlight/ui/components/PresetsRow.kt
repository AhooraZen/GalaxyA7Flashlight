package com.ahoora.a7flashlight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahoora.a7flashlight.R
import com.ahoora.a7flashlight.ui.theme.*

@Composable
fun PresetsRow(
    onAllOn: () -> Unit,
    onAllOff: () -> Unit,
    onPresetSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark.copy(alpha = 0.8f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.master_actions),
            style = Typography.titleMedium,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // All ON / All OFF buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAllOn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.both_on),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            OutlinedButton(
                onClick = onAllOff,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.both_off),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val presets = listOf(1 to "20%", 2 to "40%", 3 to "60%", 4 to "80%", 5 to "100%")
            presets.forEach { (level, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable { onPresetSelect(level) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SkyBlue
                    )
                }
            }
        }
    }
}
