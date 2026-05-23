package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.SmsLog
import com.example.data.model.SmsTemplate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SmsBomberScreen(
    viewModel: SmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // UI Input field state bindings
    val targetNumber by viewModel.targetNumber.collectAsState()
    val messagePattern by viewModel.messagePattern.collectAsState()
    val repeatCount by viewModel.repeatCount.collectAsState()
    val delayMs by viewModel.delayMs.collectAsState()
    val templateName by viewModel.templateName.collectAsState()

    // Status state bindings
    val isSending by viewModel.isSending.collectAsState()
    val currentCount by viewModel.currentCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isSimulatorMode by viewModel.isSimulatorMode.collectAsState()

    // History & template flows
    val savedTemplates by viewModel.savedTemplates.collectAsState()
    val executionLogs by viewModel.executionLogs.collectAsState()

    // Permission state handler
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasSmsPermission = granted
            if (granted) {
                viewModel.setSimulatorMode(false)
            }
        }
    )

    // Layout configuration
    var selectedTab by remember { mutableStateOf(0) } // 0 = Terminal Log, 1 = Preset Payloads

    // Dark cyberpunk color values mapped locally for convenient references
    val primaryCyan = Color(0xFF00FFCC)
    val neonPink = Color(0xFFFF2A6D)
    val amberAlert = Color(0xFFFFE600)
    val bgDark = Color(0xFF070C16)
    val surfaceDark = Color(0xFF0F172E)
    val cardDark = Color(0xFF16223F)
    val borderCyan = Color(0xFF1A384F)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark),
        containerColor = bgDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -- HEADER TERMINAL TITLE --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SMS ARRAY CO-EXTENSOR",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = primaryCyan,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "TACTICAL STRESS BLASTER CONTROL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                }

                // SIMULATOR TOGGLE SLIDER
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardDark)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable {
                            if (isSimulatorMode) {
                                // Requesting actual permission if trying to go real mode
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.SEND_SMS
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    viewModel.setSimulatorMode(false)
                                } else {
                                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                }
                            } else {
                                viewModel.setSimulatorMode(true)
                            }
                        }
                ) {
                    Text(
                        text = if (isSimulatorMode) "SIMULATOR" else "CARRIER",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (isSimulatorMode) amberAlert else primaryCyan
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isSimulatorMode) amberAlert else primaryCyan)
                    )
                }
            }

            // -- ACTIVE RUN STATISTICS WIDGET --
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                border = BorderStroke(1.dp, if (isSending) neonPink else borderCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSending) neonPink else Color(0xFF475569))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSending) "FIRE CONTROL PROTOCOL ACTIVE" else "LAUNCH MODULE READY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSending) neonPink else Color(0xFF94A3B8)
                            )
                        }

                        // Progress percentage text
                        if (isSending && totalCount > 0) {
                            val percent = (currentCount * 100) / totalCount
                            Text(
                                text = "$percent%",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = neonPink
                            )
                        }
                    }

                    // Progress indicator
                    if (isSending && totalCount > 0) {
                        val progress = currentCount.toFloat() / totalCount.toFloat()
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = neonPink,
                            trackColor = Color(0xFF1E293B)
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF1E293B),
                            trackColor = Color(0xFF1E293B)
                        )
                    }

                    // Numeric diagnostics panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("STATUS CONSOLE", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                            Text(
                                text = statusMessage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSending) primaryCyan else Color.White,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("BOMBS DISPATCHED", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                            Text(
                                text = "$currentCount / $totalCount",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSending) neonPink else primaryCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // -- TACTICAL RATE COMPLIANCE PANEL --
            val maxPerMinute by viewModel.maxPerMinute.collectAsState()
            val maxPerHour by viewModel.maxPerHour.collectAsState()
            val currentMinCount by viewModel.currentMinCount.collectAsState()
            val currentHourCount by viewModel.currentHourCount.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                border = BorderStroke(1.dp, borderCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RATE ENFORCEMENT & SAFETY PROTOCOL",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryCyan,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    // Numeric gauges of current vs max
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Minute Gauge
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = cardDark),
                            border = BorderStroke(1.dp, borderCyan)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "MINUTE LIMITER",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B)
                                )
                                val limitMinInt = maxPerMinute.toIntOrNull() ?: 1
                                val progressMin = (currentMinCount.toFloat() / limitMinInt.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progressMin },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = if (progressMin >= 0.9f) neonPink else primaryCyan,
                                    trackColor = Color(0xFF0F172E)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$currentMinCount / $maxPerMinute sent",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentMinCount >= limitMinInt) neonPink else Color.White
                                    )
                                }
                            }
                        }

                        // Hour Gauge
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = cardDark),
                            border = BorderStroke(1.dp, borderCyan)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "HOUR LIMITER",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B)
                                )
                                val limitHourInt = maxPerHour.toIntOrNull() ?: 1
                                val progressHour = (currentHourCount.toFloat() / limitHourInt.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progressHour },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = if (progressHour >= 0.9f) neonPink else primaryCyan,
                                    trackColor = Color(0xFF0F172E)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$currentHourCount / $maxPerHour sent",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentHourCount >= limitHourInt) neonPink else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Configuration input fields to dynamically change limit thresholds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = maxPerMinute,
                            onValueChange = { viewModel.setMaxPerMinute(it) },
                            label = { Text("Max/Min") },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("max_per_minute_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !isSending,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryCyan,
                                unfocusedBorderColor = borderCyan,
                                focusedLabelColor = primaryCyan,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        OutlinedTextField(
                            value = maxPerHour,
                            onValueChange = { viewModel.setMaxPerHour(it) },
                            label = { Text("Max/Hour") },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("max_per_hour_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !isSending,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryCyan,
                                unfocusedBorderColor = borderCyan,
                                focusedLabelColor = primaryCyan,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }

            // -- PARAMETER CONFIG PANEL --
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                border = BorderStroke(1.dp, borderCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "RAY PAYLOAD SPECK",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryCyan
                    )

                    // Target Receiver Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = targetNumber,
                            onValueChange = { viewModel.setTargetNumber(it) },
                            label = { Text("Target Receiver (Phone Number)") },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("target_number_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            enabled = !isSending,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryCyan,
                                unfocusedBorderColor = borderCyan,
                                focusedLabelColor = primaryCyan,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Demo number injector icon button
                        OutlinedButton(
                            onClick = { viewModel.setTargetNumber("+15555551234") },
                            enabled = !isSending,
                            modifier = Modifier.height(56.dp),
                            border = BorderStroke(1.dp, borderCyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryCyan),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("DEMO", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }

                    // Message Pattern input
                    OutlinedTextField(
                        value = messagePattern,
                        onValueChange = { viewModel.setMessagePattern(it) },
                        label = { Text("Blast Message Pattern") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("message_pattern_input"),
                        enabled = !isSending,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryCyan,
                            unfocusedBorderColor = borderCyan,
                            focusedLabelColor = primaryCyan,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )

                    // Legendary Placeholders Tip
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardDark),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Variables: {index} = current packet, {rand} = randomized code, {time} = timestamp",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    // Numeric Row: Repeat amount & delay interval
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = repeatCount,
                            onValueChange = { viewModel.setRepeatCount(it) },
                            label = { Text("Bomb Count") },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("repeat_count_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !isSending,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryCyan,
                                unfocusedBorderColor = borderCyan,
                                focusedLabelColor = primaryCyan,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        OutlinedTextField(
                            value = delayMs,
                            onValueChange = { viewModel.setDelayMs(it) },
                            label = { Text("Delay (ms)") },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("delay_ms_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !isSending,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryCyan,
                                unfocusedBorderColor = borderCyan,
                                focusedLabelColor = primaryCyan,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )
                    }

                    // PERMISSION WARNER FOR NON-SIMULATOR
                    if (!isSimulatorMode && !hasSmsPermission) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, neonPink), RoundedCornerShape(6.dp))
                                .background(Color(0xFF3F1321))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Sms Permission Alert",
                                tint = neonPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "App requires SEND_SMS permission. Tap here to request.",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                    }
                            )
                        }
                    }

                    // Fire / Stop trigger buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isSending) {
                            Button(
                                onClick = { viewModel.startSending(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("fire_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryCyan,
                                    contentColor = bgDark
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Trigger Blast"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FIRE ARRAY",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.stopSending() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("stop_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = neonPink,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop Blast"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "HALT ATTACK",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            // -- HISTORIC TERMINAL NAVIGATION TABS --
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = bgDark,
                contentColor = primaryCyan,
                divider = { Spacer(modifier = Modifier.height(1.dp).background(borderCyan)) },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = primaryCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "TERMINAL LOGS",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "PRESET LOADS",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            // -- LOWER VIEW SWITCHBOARD CONTAINER --
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(BorderStroke(1.dp, borderCyan), RoundedCornerShape(12.dp))
                    .background(surfaceDark)
                    .padding(10.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TERMINAL TRANSMISSION LOGS PANEL
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE SYSTEM IO FLOW (MAX 200)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF64748B)
                                )
                                if (executionLogs.isNotEmpty()) {
                                    Text(
                                        text = "CLEAR LOGS",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = neonPink,
                                        modifier = Modifier
                                            .clickable { viewModel.clearAllLogs() }
                                            .padding(4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            if (executionLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Logs empty Icon",
                                            tint = Color(0xFF334155),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "IO Line Quiet. No packets recorded.",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(executionLogs) { log ->
                                        TerminalLogLineItem(log = log, onDelete = {
                                            viewModel.deleteLogById(log.id)
                                        })
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // PRESETS CREATION & LOADER PANEL
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "QUICK LOADOUT LOADS / TEMPLATES",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )

                            // Quick Template Save Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = templateName,
                                    onValueChange = { viewModel.setTemplateName(it) },
                                    label = { Text("Template Name") },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.White),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("template_name_input"),
                                    singleLine = true,
                                    enabled = !isSending,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryCyan,
                                        unfocusedBorderColor = borderCyan,
                                        focusedLabelColor = primaryCyan,
                                        unfocusedLabelColor = Color(0xFF94A3B8)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.saveCurrentAsTemplate() },
                                    enabled = !isSending,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(cardDark)
                                        .border(BorderStroke(1.dp, borderCyan), RoundedCornerShape(4.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Save Preset",
                                        tint = primaryCyan
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (savedTemplates.isEmpty()) {
                                // Default sample presets info
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setTargetNumber("+15555551234")
                                            viewModel.setMessagePattern("STRESS-TEST: OTP Packet {index} Code: {rand}")
                                            viewModel.setRepeatCount("20")
                                            viewModel.setDelayMs("500")
                                        },
                                    colors = CardDefaults.cardColors(containerColor = cardDark),
                                    border = BorderStroke(1.dp, borderCyan)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "⚡ TAP TO LOAD SAMPLE PAYLOAD",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = primaryCyan
                                        )
                                        Text(
                                            text = "Generate a sample high-frequency 20 message burst with 500ms delay. Excellent for carrier performance simulation verification.",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(savedTemplates) { template ->
                                        TemplateRowItem(
                                            template = template,
                                            onLoad = { viewModel.loadTemplate(template) },
                                            onDelete = { viewModel.deleteTemplate(template.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TERMINAL LOG LINE ITEM COMPOSABLE
@Composable
fun TerminalLogLineItem(
    log: SmsLog,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SS", Locale.ROOT) }
    val timeFormatted = formatter.format(Date(log.timestamp))

    val statusColor = when (log.status) {
        "SENT" -> Color(0xFF10B981)
        "SIMULATED_SENT" -> Color(0xFF00FFCC)
        "SIMULATED_FAILED" -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    val statusLabel = when (log.status) {
        "SENT" -> "[REAL_OK]"
        "SIMULATED_SENT" -> "[SIMU_OK]"
        "SIMULATED_FAILED" -> "[SIMU_ER]"
        else -> "[REAL_ER]"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(0.5.dp, Color(0xFF1E293B)), RoundedCornerShape(4.dp))
            .background(Color(0xFF0C1322))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = timeFormatted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )

            Text(
                text = statusLabel,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )

            Text(
                text = "-> ${log.targetNumber} | ${log.messageText}",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFFE2E8F0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete item",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// PRESET LOADS ROW COMPOSABLE/ITEM
@Composable
fun TemplateRowItem(
    template: SmsTemplate,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFF22365A)), RoundedCornerShape(8.dp))
            .background(Color(0xFF111C33))
            .clickable { onLoad() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF00FFCC),
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Target: ${template.targetNumber} | Count: ${template.repeatCount} | Delay: ${template.delayMs}ms",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete template",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
