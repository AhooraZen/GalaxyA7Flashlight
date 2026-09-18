package com.ahoora.a7flashlight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ahoora.a7flashlight.R
import com.ahoora.a7flashlight.data.TorchManager
import com.ahoora.a7flashlight.ui.theme.*

private enum class TimeUnit(val multiplier: Int) {
    SEC(1),
    MIN(60),
    HR(3600)
}

@Composable
fun TimerDialog(
    currentSeconds: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var customInput by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(TimeUnit.MIN) }
    var inputError by remember { mutableStateOf(false) }

    val presets = remember {
        listOf(
            30 to R.string.timer_30s,
            60 to R.string.timer_1m,
            120 to R.string.timer_2m,
            180 to R.string.timer_3m,
            300 to R.string.timer_5m,
            600 to R.string.timer_10m,
            0 to R.string.timer_never
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(DeepMidnight)
                .border(1.5.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.timer_title),
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.timer_desc),
                    style = Typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Presets list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (sec, strRes) ->
                        val isSelected = currentSeconds == sec
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) SurfaceCardActive else SurfaceCard)
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricCyan else BorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    TorchManager.setAutoOffSeconds(sec, context)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(strRes),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ElectricCyan else TextPrimary
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ElectricCyan)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Duration Divider & Section
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.timer_custom),
                    style = Typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Custom input row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = {
                            customInput = it.filter { ch -> ch.isDigit() }.take(5)
                            inputError = false
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = inputError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            cursorColor = ElectricCyan
                        ),
                        placeholder = {
                            Text(text = "15", color = TextMuted, fontSize = 13.sp)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Unit Chips (Sec, Min, Hour)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TimeUnit.values().forEach { unit ->
                            val isUnitSelected = selectedUnit == unit
                            val labelRes = when (unit) {
                                TimeUnit.SEC -> R.string.timer_unit_sec
                                TimeUnit.MIN -> R.string.timer_unit_min
                                TimeUnit.HR -> R.string.timer_unit_hr
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isUnitSelected) ElectricCyan else SurfaceCard)
                                    .border(
                                        1.dp,
                                        if (isUnitSelected) ElectricCyan else BorderSubtle,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedUnit = unit }
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    fontSize = 11.sp,
                                    fontWeight = if (isUnitSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isUnitSelected) PureBlack else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val num = customInput.toIntOrNull()
                        if (num != null && num > 0) {
                            val totalSec = num * selectedUnit.multiplier
                            TorchManager.setAutoOffSeconds(totalSec, context)
                            onDismiss()
                        } else {
                            inputError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = PureBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.timer_set_custom),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
