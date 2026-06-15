package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AlertMessage
import com.example.data.ChannelStatus
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

// Style Constants for Professional Polish Light M3 Theme
private val StageDarkBg = Color(0xFFFDF8FD)
private val CardBg = Color(0xFFFFFFFF)
private val CardBorderColor = Color(0xFFCAC4D0)
private val NeonCyan = Color(0xFF6750A4)
private val NeonRed = Color(0xFFBA1A1A)
private val NeonYellow = Color(0xFFED9300)
private val NeonGreen = Color(0xFF2E7D32)
private val TextPrimary = Color(0xFF1D1B20)
private val TextSecondary = Color(0xFF49454F)

@Composable
fun StageBridgeScreen(
    viewModel: StageBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()
    val worshipSubRole by viewModel.worshipSubRole.collectAsStateWithLifecycle()
    val mediaSubRole by viewModel.mediaSubRole.collectAsStateWithLifecycle()
    val senderName by viewModel.senderName.collectAsStateWithLifecycle()
    val syncGroupId by viewModel.syncGroupId.collectAsStateWithLifecycle()
    val activeTargetGroup by viewModel.activeTargetGroup.collectAsStateWithLifecycle()
    val activeOverlayAlert by viewModel.activeOverlayAlert.collectAsStateWithLifecycle()
    val targetGroups by viewModel.targetGroups.collectAsStateWithLifecycle()
    val pendingSentAlert by viewModel.pendingSentAlert.collectAsStateWithLifecycle()
    val localWifiSyncActive by viewModel.localWifiSyncActive.collectAsStateWithLifecycle()
    val wifiPeers by viewModel.wifiPeers.collectAsStateWithLifecycle()
    val networkSyncMode by viewModel.networkSyncMode.collectAsStateWithLifecycle()

    val alertsList by viewModel.allAlerts.collectAsStateWithLifecycle()
    val presetAlerts by viewModel.presetAlerts.collectAsStateWithLifecycle()
    val visualPulseTrigger by viewModel.visualPulseTrigger.collectAsStateWithLifecycle()
    val visualPulseIsCrucial by viewModel.visualPulseIsCrucial.collectAsStateWithLifecycle()

    // Configuration flags
    var showAdvancedDispatchPreset by remember { mutableStateOf<CustomPreset?>(null) }
    var showAddOrEditPresetDialog by remember { mutableStateOf<CustomPreset?>(null) }
    var isCreatingNewPreset by remember { mutableStateOf(false) }
    var showConfigurePresetDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var customAlertMessageText by remember { mutableStateOf("") }
    var customAlertPresetName by remember { mutableStateOf("Custom Issue") }

    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density,
            fontScale = LocalDensity.current.fontScale * fontScale
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(StageDarkBg, Color(0xFFFFF9FC), StageDarkBg)
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. StageBridge Header
                AppHeaderSection(
                    deviceRole = deviceRole,
                    syncGroupId = syncGroupId,
                    networkSyncMode = networkSyncMode,
                    onNetworkSyncModeChange = { viewModel.setNetworkSyncMode(it) },
                    onSyncGroupIdChange = { viewModel.setSyncGroupId(it) },
                    onRoleChange = { viewModel.setDeviceRole(it) },
                    fontScale = fontScale,
                    onFontScaleIncrease = { viewModel.adjustFontScale(true) },
                    onFontScaleDecrease = { viewModel.adjustFontScale(false) },
                    onSettingsClick = { showSettingsDialog = true }
                )

            // Dynamic view occupied completely by the high-vis Role panels
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                RoleLeftPanel(
                    deviceRole = deviceRole,
                    worshipSubRole = worshipSubRole,
                    senderName = senderName,
                    onUpdateWorshipSubRole = { viewModel.setWorshipSubRole(it) },
                    onUpdateSenderName = { viewModel.setSenderName(it) },
                    presetAlerts = presetAlerts,
                    targetGroups = targetGroups,
                    activeTargetGroup = activeTargetGroup,
                    onActiveTargetGroupChange = { viewModel.setActiveTargetGroup(it) },
                    onAddGroup = { viewModel.addTargetGroup(it) },
                    onEditGroup = { old, new -> viewModel.editTargetGroup(old, new) },
                    onRemoveGroup = { viewModel.removeTargetGroup(it) },
                    onQuickReportClick = { preset ->
                        viewModel.submitAlert(
                            presetType = preset.title,
                            description = preset.description,
                            target = activeTargetGroup,
                            isCrucial = (preset.title == "Mic Feedback")
                        )
                    },
                    onLongPressReportClick = { preset ->
                        showAdvancedDispatchPreset = preset
                    },
                    onEditPresetClick = { showAddOrEditPresetDialog = it },
                    onAddPresetClick = { isCreatingNewPreset = true },
                    onCustomReportClick = { showConfigurePresetDialog = true },
                    alertsList = alertsList,
                    onAcknowledge = { viewModel.acknowledgeAlert(it) },
                    onSolve = { viewModel.solveAlert(it) },
                    onClearAll = { viewModel.clearAllAlerts() },
                    mediaSubRole = mediaSubRole,
                    onUpdateMediaSubRole = { viewModel.setMediaSubRole(it) }
                )
            }
        }

        // Custom Quick Note Creator Dialog
        if (showConfigurePresetDialog) {
            CustomPresetConfigureDialog(
                defaultName = customAlertPresetName,
                defaultText = customAlertMessageText,
                targetGroups = targetGroups,
                onDismiss = { showConfigurePresetDialog = false },
                onConfirm = { name, text, target, isCrucial ->
                    viewModel.submitAlert(name, text, target, isCrucial)
                    showConfigurePresetDialog = false
                    customAlertMessageText = ""
                }
            )
        }

        // Advanced Dispatch Dialog with customizable contextual options (triggered on Long-press!):
        showAdvancedDispatchPreset?.let { preset ->
            AdvancedDispatchDialog(
                preset = preset,
                targetGroups = targetGroups,
                onDismiss = { showAdvancedDispatchPreset = null },
                onSend = { target, isCrucial, customDesc ->
                    viewModel.submitAlert(preset.title, customDesc, target, isCrucial)
                    showAdvancedDispatchPreset = null
                },
                onConfigureCard = {
                    showAddOrEditPresetDialog = preset
                    showAdvancedDispatchPreset = null
                }
            )
        }

        // Dynamic Add / Edit Preset Card Dialog:
        if (isCreatingNewPreset || showAddOrEditPresetDialog != null) {
            AddOrEditPresetDialog(
                presetToEdit = showAddOrEditPresetDialog,
                targetGroups = targetGroups,
                onDismiss = {
                    showAddOrEditPresetDialog = null
                    isCreatingNewPreset = false
                },
                onSave = { updatedOrNew ->
                    if (showAddOrEditPresetDialog != null) {
                        viewModel.updatePresetAlert(updatedOrNew)
                    } else {
                        viewModel.addPresetAlert(updatedOrNew)
                    }
                    showAddOrEditPresetDialog = null
                    isCreatingNewPreset = false
                },
                onDelete = { presetId ->
                    viewModel.deletePresetAlert(presetId)
                    showAddOrEditPresetDialog = null
                    isCreatingNewPreset = false
                }
            )
        }

        // Real-time track pending sent alert popup (Like WhatsApp delivered/read statuses)
        pendingSentAlert?.let { alert ->
            PendingSentAlertPopUp(
                alert = alert,
                onCancel = { viewModel.cancelSentAlert(alert.id) },
                onDismiss = { viewModel.clearPendingSentAlert() }
            )
        }

        // 4. LOUD DISPATCH EMERGENCY OVERLAY WITH INTENSE VIBRATION
        activeOverlayAlert?.let { alert ->
            StageEmergencyOverlay(
                alert = alert,
                onAcknowledge = { viewModel.acknowledgeAlert(alert.id) },
                onSolve = { viewModel.solveAlert(alert.id) },
                onDismiss = { viewModel.dismissOverlay() }
            )
        }

        // 5. SETTINGS OVERLAY PANEL FOR ACCESSIBILITY AND TARGETS
        if (showSettingsDialog) {
            SettingsDialog(
                currentName = senderName,
                onUpdateName = { viewModel.setSenderName(it) },
                currentRole = deviceRole,
                onUpdateRole = { viewModel.setDeviceRole(it) },
                currentWorshipSubRole = worshipSubRole,
                onUpdateWorshipSubRole = { viewModel.setWorshipSubRole(it) },
                currentMediaSubRole = mediaSubRole,
                onUpdateMediaSubRole = { viewModel.setMediaSubRole(it) },
                targetGroups = targetGroups,
                onAddTargetGroup = { viewModel.addTargetGroup(it) },
                onRemoveTargetGroup = { viewModel.removeTargetGroup(it) },
                localWifiSyncActive = localWifiSyncActive,
                onToggleLocalWifiSync = { viewModel.setNetworkSyncMode(if (it) NetworkSyncMode.LOCAL_WIFI else NetworkSyncMode.CLOUD) },
                wifiPeers = wifiPeers,
                onDismiss = { showSettingsDialog = false }
            )
        }

        // 6. BRIEF INTERFACE VISUAL PULSE OVERLAY ON INCOMING TROUBLESHOOTING ALERT
        VisualPulseOverlay(triggerTime = visualPulseTrigger, isCrucial = visualPulseIsCrucial)
    }
}
}

// ==========================================
// COMPOSABLE COMPONENT SECTIONS
// ==========================================

@Composable
fun AppHeaderSection(
    deviceRole: String,
    syncGroupId: String,
    networkSyncMode: NetworkSyncMode,
    onNetworkSyncModeChange: (NetworkSyncMode) -> Unit,
    onSyncGroupIdChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    fontScale: Float,
    onFontScaleIncrease: () -> Unit,
    onFontScaleDecrease: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Branding with Settings Icon and Font Scale Adjuster
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (networkSyncMode == NetworkSyncMode.LOCAL_WIFI) NeonGreen else NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "StageBridge",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("app_header_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Device Settings Options",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))

                    // Font zoom control toolbar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEADDFF).copy(alpha = 0.35f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = onFontScaleDecrease,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text(
                                "A-",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${(fontScale * 100).toInt()}%",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        IconButton(
                            onClick = onFontScaleIncrease,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text(
                                "A+",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Interactive Network Mode Toggle Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val nextMode = if (networkSyncMode == NetworkSyncMode.CLOUD) NetworkSyncMode.LOCAL_WIFI else NetworkSyncMode.CLOUD
                            onNetworkSyncModeChange(nextMode)
                        }
                        .background(
                            if (networkSyncMode == NetworkSyncMode.LOCAL_WIFI) NeonGreen.copy(alpha = 0.15f)
                            else NeonCyan.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.CenterVertically)
                            .clip(CircleShape)
                            .background(
                                if (networkSyncMode == NetworkSyncMode.LOCAL_WIFI) NeonGreen.copy(alpha = pulseAlpha)
                                else NeonCyan.copy(alpha = pulseAlpha)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (networkSyncMode == NetworkSyncMode.LOCAL_WIFI) "📡 DIRECT WI-FI" else "☁️ CLOUD ACTIVE",
                        color = if (networkSyncMode == NetworkSyncMode.LOCAL_WIFI) NeonGreen else NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time Group Sync configuration field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF3EDF7))
                    .border(BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TEAM SYNC ID:",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = syncGroupId,
                    onValueChange = onSyncGroupIdChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFF21005D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sync_code_input")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Professional Segment buttons to switch between Cloud and Local Wifi sync
            Text(
                text = "NETWORK SYNC MODE:",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3EDF7))
                    .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(10.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    NetworkSyncMode.CLOUD to "☁️ CLOUD SYNC",
                    NetworkSyncMode.LOCAL_WIFI to "📡 LOCAL WI-FI"
                ).forEach { (mode, label) ->
                    val isSelected = networkSyncMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNetworkSyncModeChange(mode) }
                            .background(if (isSelected) Color(0xFFEADDFF) else Color.Transparent)
                            .padding(vertical = 8.dp)
                            .testTag("mode_${mode.name.lowercase()}_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF21005D) else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (networkSyncMode == NetworkSyncMode.CLOUD) {
                    "☁️ Synchronizes across different cellular & external networks via cloud."
                } else {
                    "📡 Direct low-latency offline linking with zero internet dependence."
                },
                color = if (networkSyncMode == NetworkSyncMode.CLOUD) NeonCyan else NeonGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Professional segment buttons for assigning active device role
            Text(
                text = "ASSIGN DEVICE ACTIVE PROFILE:",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3EDF7))
                    .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(10.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val roles = listOf(
                    Triple("WORSHIP_MEMBER", "🎤 WORSHIP", "Send Cues"),
                    Triple("DUAL_SIMULATOR", "🔄 SIM PREVIEW", "Cohesively Inspect"),
                    Triple("MEDIA_OPERATOR", "🎛️ MEDIA BOOTH", "Receive alerts")
                )

                roles.forEach { r ->
                    val isSelected = deviceRole == r.first
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRoleChange(r.first) }
                            .background(if (isSelected) Color(0xFFEADDFF) else Color.Transparent)
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("role_${r.first.lowercase()}_tab"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = r.second,
                            color = if (isSelected) Color(0xFF21005D) else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = r.third,
                            color = if (isSelected) Color(0xFF21005D).copy(alpha = 0.8f) else TextSecondary,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// TabRowSection is removed gracefully on user's request.

@Composable
fun RoleLeftPanel(
    deviceRole: String,
    worshipSubRole: String,
    senderName: String,
    onUpdateWorshipSubRole: (String) -> Unit,
    onUpdateSenderName: (String) -> Unit,
    presetAlerts: List<CustomPreset>,
    targetGroups: List<String>,
    activeTargetGroup: String,
    onActiveTargetGroupChange: (String) -> Unit,
    onAddGroup: (String) -> Unit,
    onEditGroup: (String, String) -> Unit,
    onRemoveGroup: (String) -> Unit,
    onQuickReportClick: (CustomPreset) -> Unit,
    onLongPressReportClick: (CustomPreset) -> Unit,
    onEditPresetClick: (CustomPreset) -> Unit,
    onAddPresetClick: () -> Unit,
    onCustomReportClick: () -> Unit,
    alertsList: List<AlertMessage>,
    onAcknowledge: (String) -> Unit,
    onSolve: (String) -> Unit,
    onClearAll: () -> Unit,
    mediaSubRole: String,
    onUpdateMediaSubRole: (String) -> Unit
) {
    when (deviceRole) {
        "WORSHIP_MEMBER" -> {
            WorshipSenderDashboard(
                worshipSubRole = worshipSubRole,
                senderName = senderName,
                onUpdateWorshipSubRole = onUpdateWorshipSubRole,
                onUpdateSenderName = onUpdateSenderName,
                presetAlerts = presetAlerts,
                targetGroups = targetGroups,
                activeTargetGroup = activeTargetGroup,
                onActiveTargetGroupChange = onActiveTargetGroupChange,
                onAddGroup = onAddGroup,
                onEditGroup = onEditGroup,
                onRemoveGroup = onRemoveGroup,
                onQuickReportClick = onQuickReportClick,
                onLongPressReportClick = onLongPressReportClick,
                onEditPresetClick = onEditPresetClick,
                onAddPresetClick = onAddPresetClick,
                onCustomReportClick = onCustomReportClick
            )
        }
        "MEDIA_OPERATOR" -> {
            MediaReceiverConsole(
                mediaSubRole = mediaSubRole,
                onUpdateMediaSubRole = onUpdateMediaSubRole,
                alertsList = alertsList,
                onAcknowledge = onAcknowledge,
                onSolve = onSolve,
                onClearAll = onClearAll
            )
        }
        else -> {
            // DUAL SIMULATOR (Side-by-side splitscreen preview operations on one screen)
            Column(modifier = Modifier.fillMaxSize()) {
                ScrollableWorshipPanel(
                    worshipSubRole = worshipSubRole,
                    senderName = senderName,
                    onUpdateWorshipSubRole = onUpdateWorshipSubRole,
                    onUpdateSenderName = onUpdateSenderName,
                    presetAlerts = presetAlerts,
                    targetGroups = targetGroups,
                    activeTargetGroup = activeTargetGroup,
                    onActiveTargetGroupChange = onActiveTargetGroupChange,
                    onAddGroup = onAddGroup,
                    onEditGroup = onEditGroup,
                    onRemoveGroup = onRemoveGroup,
                    onQuickReportClick = onQuickReportClick,
                    onLongPressReportClick = onLongPressReportClick,
                    onEditPresetClick = onEditPresetClick,
                    onAddPresetClick = onAddPresetClick,
                    onCustomReportClick = onCustomReportClick,
                    modifier = Modifier.weight(1.1f)
                )
 
                Spacer(modifier = Modifier.height(12.dp))
 
                HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f), thickness = 1.dp)
 
                Spacer(modifier = Modifier.height(10.dp))
 
                Box(modifier = Modifier.weight(0.9f)) {
                    MediaReceiverConsole(
                        mediaSubRole = mediaSubRole,
                        onUpdateMediaSubRole = onUpdateMediaSubRole,
                        alertsList = alertsList,
                        onAcknowledge = onAcknowledge,
                        onSolve = onSolve,
                        onClearAll = onClearAll
                    )
                }
            }
        }
    }
}
 
// ==========================================
// SENDER VIEW: WORSHIP TEAM PANEL
// ==========================================
 
@Composable
fun WorshipSenderDashboard(
    worshipSubRole: String,
    senderName: String,
    onUpdateWorshipSubRole: (String) -> Unit,
    onUpdateSenderName: (String) -> Unit,
    presetAlerts: List<CustomPreset>,
    targetGroups: List<String>,
    activeTargetGroup: String,
    onActiveTargetGroupChange: (String) -> Unit,
    onAddGroup: (String) -> Unit,
    onEditGroup: (String, String) -> Unit,
    onRemoveGroup: (String) -> Unit,
    onQuickReportClick: (CustomPreset) -> Unit,
    onLongPressReportClick: (CustomPreset) -> Unit,
    onEditPresetClick: (CustomPreset) -> Unit,
    onAddPresetClick: () -> Unit,
    onCustomReportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        WorshipMusicianConfigCard(
            worshipSubRole = worshipSubRole,
            senderName = senderName,
            onUpdateWorshipSubRole = onUpdateWorshipSubRole,
            onUpdateSenderName = onUpdateSenderName
        )
 
        Spacer(modifier = Modifier.height(16.dp))

        ActiveTargetGroupBar(
            targetGroups = targetGroups,
            activeTargetGroup = activeTargetGroup,
            onActiveTargetGroupChange = onActiveTargetGroupChange,
            onAddGroup = onAddGroup,
            onEditGroup = onEditGroup,
            onRemoveGroup = onRemoveGroup
        )

        Spacer(modifier = Modifier.height(16.dp))
 
        WorshipPresetsGridSection(
            senderRole = worshipSubRole,
            presetAlerts = presetAlerts,
            onQuickReportClick = onQuickReportClick,
            onLongPressReportClick = onLongPressReportClick,
            onEditPresetClick = onEditPresetClick,
            onAddPresetClick = onAddPresetClick,
            onCustomReportClick = onCustomReportClick
        )
    }
}
 
@Composable
fun ScrollableWorshipPanel(
    worshipSubRole: String,
    senderName: String,
    onUpdateWorshipSubRole: (String) -> Unit,
    onUpdateSenderName: (String) -> Unit,
    presetAlerts: List<CustomPreset>,
    targetGroups: List<String>,
    activeTargetGroup: String,
    onActiveTargetGroupChange: (String) -> Unit,
    onAddGroup: (String) -> Unit,
    onEditGroup: (String, String) -> Unit,
    onRemoveGroup: (String) -> Unit,
    onQuickReportClick: (CustomPreset) -> Unit,
    onLongPressReportClick: (CustomPreset) -> Unit,
    onEditPresetClick: (CustomPreset) -> Unit,
    onAddPresetClick: () -> Unit,
    onCustomReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STAGE SEND CONSOLE",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
 
            // Fast identity badge
            Text(
                text = "⚡ $worshipSubRole ($senderName)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            WorshipMusicianConfigCard(
                worshipSubRole = worshipSubRole,
                senderName = senderName,
                onUpdateWorshipSubRole = onUpdateWorshipSubRole,
                onUpdateSenderName = onUpdateSenderName
            )
            Spacer(modifier = Modifier.height(10.dp))

            ActiveTargetGroupBar(
                targetGroups = targetGroups,
                activeTargetGroup = activeTargetGroup,
                onActiveTargetGroupChange = onActiveTargetGroupChange,
                onAddGroup = onAddGroup,
                onEditGroup = onEditGroup,
                onRemoveGroup = onRemoveGroup
            )

            Spacer(modifier = Modifier.height(10.dp))

            WorshipPresetsGridSection(
                senderRole = worshipSubRole,
                presetAlerts = presetAlerts,
                onQuickReportClick = onQuickReportClick,
                onLongPressReportClick = onLongPressReportClick,
                onEditPresetClick = onEditPresetClick,
                onAddPresetClick = onAddPresetClick,
                onCustomReportClick = onCustomReportClick
            )
        }
    }
}

@Composable
fun WorshipMusicianConfigCard(
    worshipSubRole: String,
    senderName: String,
    onUpdateWorshipSubRole: (String) -> Unit,
    onUpdateSenderName: (String) -> Unit
) {
    val instruments = listOf(
        "Worship Leader", "Lead Vocalist", "Keyboardist", "Guitarist", "Bassist", "Drummer"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "STAGE MUSICIAN IDENTITY:",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown mock or inline select list. We make scrollable row of choices for one-tap role select!
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3EDF7))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Group: $worshipSubRole",
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = NeonCyan
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        instruments.forEach { inst ->
                            DropdownMenuItem(
                                text = { Text(inst, color = TextPrimary) },
                                onClick = {
                                    onUpdateWorshipSubRole(inst)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Sender display name input field
                OutlinedTextField(
                    value = senderName,
                    onValueChange = onUpdateSenderName,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sender_name_input"),
                    label = { Text("Your Call Name", fontSize = 10.sp, color = TextSecondary) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary
                    )
                )
            }
        }
    }
}

@Composable
fun ActiveTargetGroupBar(
    targetGroups: List<String>,
    activeTargetGroup: String,
    onActiveTargetGroupChange: (String) -> Unit,
    onAddGroup: (String) -> Unit,
    onEditGroup: (String, String) -> Unit,
    onRemoveGroup: (String) -> Unit
) {
    var isManaging by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialogForGroup by remember { mutableStateOf<String?>(null) }
    var newGroupName by remember { mutableStateOf("") }
    var editGroupName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🎯 TARGET NOTIFICATION GROUP",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Cues go to: $activeTargetGroup",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            TextButton(
                onClick = { isManaging = !isManaging },
                colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
            ) {
                Icon(
                    imageVector = if (isManaging) Icons.Default.Close else Icons.Default.Edit,
                    contentDescription = "Manage Groups",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isManaging) "Done" else "Manage Channels",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!isManaging) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                targetGroups.forEach { group ->
                    item {
                        val isSelected = group == activeTargetGroup
                        FilterChip(
                            selected = isSelected,
                            onClick = { onActiveTargetGroupChange(group) },
                            label = {
                                Text(
                                    text = group,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEADDFF),
                                selectedLabelColor = Color(0xFF21005D),
                                containerColor = Color(0xFFF3EDF7),
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = CardBorderColor.copy(alpha = 0.5f),
                                selectedBorderColor = NeonCyan
                            )
                        )
                    }
                }

                item {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Custom Group",
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize list / Delete default groups:",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+ Add Group", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                targetGroups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Group",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = group,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    editGroupName = group
                                    showEditDialogForGroup = group
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename group",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = { onRemoveGroup(group) },
                                enabled = targetGroups.size > 1,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove group",
                                    tint = if (targetGroups.size > 1) NeonRed else Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newGroupName = ""
            },
            title = { Text("Create New Group", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            onAddGroup(newGroupName)
                            showAddDialog = false
                            newGroupName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newGroupName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    showEditDialogForGroup?.let { originalName ->
        AlertDialog(
            onDismissRequest = { showEditDialogForGroup = null },
            title = { Text("Rename Group", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = editGroupName,
                    onValueChange = { editGroupName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editGroupName.isNotBlank()) {
                            onEditGroup(originalName, editGroupName)
                            showEditDialogForGroup = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Rename", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialogForGroup = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WorshipPresetsGridSection(
    senderRole: String,
    presetAlerts: List<CustomPreset>,
    onQuickReportClick: (CustomPreset) -> Unit,
    onLongPressReportClick: (CustomPreset) -> Unit,
    onEditPresetClick: (CustomPreset) -> Unit,
    onAddPresetClick: () -> Unit,
    onCustomReportClick: () -> Unit
) {
    Text(
        text = "RAPID ONE-TOUCH PROBLEMS (TAP TO QUICK SEND | EDIT DISPATCH CARD):",
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Grid of cards
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .height(310.dp)
            .testTag("presets_grid"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = true
    ) {
        items(presetAlerts) { item ->
            // Customize text slightly depending on active keyboardist, drum role etc.
            val customizedItem = when (senderRole) {
                "Keyboardist" -> {
                    if (item.title == "Volume/Low") item.copy(description = "Synth Nord Stage keys are sounding too low in my direct monitor.")
                    else item
                }
                "Drummer" -> {
                    if (item.title == "Volume/Low") item.copy(description = "Drum monitoring loop is completely quiet, boost monitors.")
                    else item
                }
                else -> item
            }

            PresetButtonCard(
                preset = customizedItem,
                onClick = { onQuickReportClick(customizedItem) },
                onLongClick = { onLongPressReportClick(customizedItem) },
                onEditClick = { onEditPresetClick(customizedItem) }
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAddPresetClick() }
                    .testTag("new_preset_card"),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New Card",
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+ Add Card",
                        color = NeonGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize problems",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onCustomReportClick() }
                    .testTag("custom_preset_card"),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Edit Preset",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Custom Note",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Write description",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetButtonCard(
    preset: CustomPreset,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isEmergency = preset.title == "Mic Feedback"
    val presetColor = try {
        Color(android.graphics.Color.parseColor(preset.colorHex))
    } catch (e: Exception) {
        NeonCyan
    }
    val presetIcon = when (preset.iconName) {
        "Mute" -> Icons.Filled.VolumeMute
        "Reverb" -> Icons.Filled.LeakAdd
        "Feedback" -> Icons.Filled.Warning
        "EQ" -> Icons.Filled.GraphicEq
        "Speaker" -> Icons.Filled.Speaker
        else -> Icons.Filled.Help
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("preset_${preset.title.lowercase().replace("/", "_")}"),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(
            width = if (isEmergency) (1.5.dp * pulseBorder) else 1.dp,
            color = if (isEmergency) NeonRed.copy(alpha = pulseBorder) else CardBorderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = presetIcon,
                    contentDescription = preset.title,
                    tint = presetColor,
                    modifier = Modifier.size(24.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3EDF7))
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Card",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    if (isEmergency) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonRed)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("DANGER", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(presetColor.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("HOLD", color = presetColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column {
                Text(
                    text = preset.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Target: ${preset.defaultTarget}",
                    color = TextSecondary,
                    fontSize = 8.sp,
                )
            }
        }
    }
}

@Composable
fun MediaServiceAssignmentCard(
    currentDesignation: String,
    onUpdateDesignation: (String) -> Unit
) {
    val operatorSubRoles = listOf(
        "Live Stream Operator", "FOH Engineer", "Monitors Engineer", "Media Group", "Global"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "MEDIA OPERATOR ROUTING CONSOLE:",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            var expanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF3EDF7))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📡 Operating: $currentDesignation",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Drop",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(CardBg)
                ) {
                    operatorSubRoles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role, color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                onUpdateDesignation(role)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// RECEIVER VIEW: MEDIA BOOTH PANEL
// ==========================================

@Composable
fun MediaReceiverConsole(
    mediaSubRole: String,
    onUpdateMediaSubRole: (String) -> Unit,
    alertsList: List<AlertMessage>,
    onAcknowledge: (String) -> Unit,
    onSolve: (String) -> Unit,
    onClearAll: () -> Unit
) {
    // Filter alerts list based on current Media SubRole Routing to demonstrate real assignments!
    val filteredAlerts = remember(alertsList, mediaSubRole) {
        alertsList.filter { alert ->
            alert.targetRole == "Global" ||
            alert.targetRole == "Media Group" ||
            alert.targetRole == mediaSubRole
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Embed the Operator Assignment dropdown directly at the top of the Booth Console!
        MediaServiceAssignmentCard(
            currentDesignation = mediaSubRole,
            onUpdateDesignation = onUpdateMediaSubRole
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📥 STAGE ALERTS QUEUE",
                    color = NeonRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Role: Receiving for $mediaSubRole",
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }

            if (filteredAlerts.isNotEmpty()) {
                Text(
                    text = "CLEAR ALL",
                    color = NeonRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onClearAll() }
                        .padding(6.dp)
                        .testTag("clear_all_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredAlerts.isEmpty()) {
            EmptyAlertsState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("alerts_queue_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAlerts) { alert ->
                    AlertItemRow(
                        alert = alert,
                        onAcknowledge = { onAcknowledge(alert.id) },
                        onSolve = { onSolve(alert.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertItemRow(
    alert: AlertMessage,
    onAcknowledge: () -> Unit,
    onSolve: () -> Unit
) {
    val isCrucial = alert.isCrucial
    val outlineColor = when (alert.status) {
        "NEW" -> if (isCrucial) NeonRed else NeonYellow
        "ACKNOWLEDGED" -> NeonYellow.copy(alpha = 0.5f)
        else -> CardBorderColor
    }

    val dynamicOpacity = if (alert.status == "ACKNOWLEDGED") 0.75f else 1.0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_item_${alert.id}")
            .drawBehind {
                // Flash alert state
            },
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, outlineColor),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .drawWithContent {
                    drawContent()
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (alert.status == "NEW") NeonRed else NeonYellow)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${alert.presetType} Alert",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "From: ${alert.senderName} (${alert.senderRole})",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Relative time badge
                val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(alert.timestamp))
                Text(
                    text = timeString,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = alert.description,
                color = TextPrimary,
                fontSize = 13.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Target recipient chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF3EDF7))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Target: ${alert.targetRole}",
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Operator Action triggers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (alert.status == "NEW") {
                        Button(
                            onClick = onAcknowledge,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_ack_${alert.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonYellow.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            border = BorderStroke(1.dp, NeonYellow)
                        ) {
                            Text("ACKNOWLEDGE", color = NeonYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onSolve,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_solve_${alert.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        border = BorderStroke(1.dp, NeonGreen)
                    ) {
                        Text("SOLVED", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyAlertsState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
        border = BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Balanced",
                tint = NeonGreen,
                modifier = Modifier
                    .size(40.dp)
                    .padding(3.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Audio Mix Balanced",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "0 urgent stage issues active. The worship team is streaming clean.",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}

// ==========================================
// INTERACTIVE OVERLAYS & SHEETS
// ==========================================

@Composable
fun CustomPresetConfigureDialog(
    defaultName: String,
    defaultText: String,
    targetGroups: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, details: String, target: String, isCrucial: Boolean) -> Unit
) {
    var presetName by remember { mutableStateOf(defaultName) }
    var issueText by remember { mutableStateOf(defaultText) }
    var isCrucialState by remember { mutableStateOf(false) }

    var selectedTargetIndex by remember { mutableStateOf(0) }

    val safeIndex = if (targetGroups.isNotEmpty()) {
        selectedTargetIndex.coerceIn(0, targetGroups.lastIndex)
    } else {
        0
    }

    val activeTarget = if (targetGroups.isNotEmpty()) {
        targetGroups[safeIndex]
    } else {
        "Global"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Edit Note",
                    tint = NeonCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write Custom Note", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Issue Name (1-2 Words)", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_note_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor,
                        focusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = issueText,
                    onValueChange = { issueText = it },
                    label = { Text("Describe details clearly to operator", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("custom_note_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor,
                        focusedTextColor = TextPrimary
                    )
                )

                Text("TARGET OF ALARM:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                var expandedTarget by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF3EDF7))
                        .clickable { expandedTarget = !expandedTarget }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = activeTarget, color = TextPrimary, fontSize = 12.sp)
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Drop", tint = NeonCyan)
                    }
                    DropdownMenu(
                        expanded = expandedTarget,
                        onDismissRequest = { expandedTarget = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        targetGroups.forEachIndexed { i, t ->
                            DropdownMenuItem(
                                text = { Text(t, color = TextPrimary) },
                                onClick = {
                                    selectedTargetIndex = i
                                    expandedTarget = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Deem critical emergency?", color = TextPrimary, fontSize = 13.sp)
                    Checkbox(
                        checked = isCrucialState,
                        onCheckedChange = { isCrucialState = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeonRed, uncheckedColor = TextSecondary),
                        modifier = Modifier.testTag("custom_is_crucial_check")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (presetName.isNotBlank() && issueText.isNotBlank()) {
                        onConfirm(presetName, issueText, activeTarget, isCrucialState)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.testTag("custom_note_submit_btn")
            ) {
                Text("SEND ALARM ⚡", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("custom_note_cancel_btn")) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardBg
    )
}

// ==========================================
// EMERGENCY POPUP OVERLAY SCREEN
// ==========================================

@Composable
fun StageEmergencyOverlay(
    alert: AlertMessage,
    onAcknowledge: () -> Unit,
    onSolve: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emergency")
    val redFlashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .border(
                    width = 6.dp,
                    color = NeonRed.copy(alpha = if (alert.isCrucial) redFlashAlpha else 0.4f)
                )
                .testTag("emergency_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large flashing warning triangle
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "Alert",
                    tint = NeonRed,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (alert.isCrucial) "⚠️ URGENT AUDIO DISPATCH" else "STAGE CUE RECEIVED",
                    color = NeonRed,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(NeonRed.copy(alpha = 0.2f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Sender: ${alert.senderName} (${alert.senderRole})",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(2.dp, NeonRed.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = alert.presetType,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = alert.description,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Loud audio warnings and device vibration will pulse until an operator takes corrective actions.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAcknowledge,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("overlay_ack_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonYellow),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "ACKNOWLEDGE 🖐️",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = onSolve,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("overlay_solved_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "MIX ADJUSTED! ✅",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("overlay_ignore_btn")
                ) {
                    Text("Mute overlay temporarily", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    currentName: String,
    onUpdateName: (String) -> Unit,
    currentRole: String,
    onUpdateRole: (String) -> Unit,
    currentWorshipSubRole: String,
    onUpdateWorshipSubRole: (String) -> Unit,
    currentMediaSubRole: String,
    onUpdateMediaSubRole: (String) -> Unit,
    targetGroups: List<String>,
    onAddTargetGroup: (String) -> Unit,
    onRemoveTargetGroup: (String) -> Unit,
    localWifiSyncActive: Boolean,
    onToggleLocalWifiSync: (Boolean) -> Unit,
    wifiPeers: List<LocalWifiSyncManager.PeerDevice>,
    onDismiss: () -> Unit
) {
    var deviceNameInput by remember { mutableStateOf(currentName) }
    var selectedRole by remember { mutableStateOf(currentRole) }
    var subRoleInput by remember { mutableStateOf(
        if (currentRole == "WORSHIP_MEMBER") currentWorshipSubRole else currentMediaSubRole
    ) }
    
    var newGroupInput by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Device & Role Profiles", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Device Identity Name
                Text("USER DETAILS & DEVICE CALLSIGN", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                
                OutlinedTextField(
                    value = deviceNameInput,
                    onValueChange = {
                        deviceNameInput = it
                        onUpdateName(it)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("settings_name_input"),
                    label = { Text("Display/User Name", fontSize = 11.sp, color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        focusedLabelColor = NeonCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        unfocusedLabelColor = TextSecondary,
                        unfocusedBorderColor = CardBorderColor
                    )
                )
                
                // Section 2: Profile Selection
                Text("DEVICE ACTIVE ROLE & PROFILE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                
                val roles = listOf(
                    "WORSHIP_MEMBER" to "Worship Musician",
                    "MEDIA_OPERATOR" to "Media Operator Console",
                    "DUAL_SIMULATOR" to "Dual/Tester Simulator"
                )
                
                var expandedRoleDropdown by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3EDF7))
                        .clickable { expandedRoleDropdown = !expandedRoleDropdown }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = roles.find { it.first == selectedRole }?.second ?: selectedRole,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = NeonCyan)
                    }
                    
                    DropdownMenu(
                        expanded = expandedRoleDropdown,
                        onDismissRequest = { expandedRoleDropdown = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        roles.forEach { (roleKey, roleLabel) ->
                            DropdownMenuItem(
                                text = { Text(roleLabel, color = TextPrimary) },
                                onClick = {
                                    selectedRole = roleKey
                                    onUpdateRole(roleKey)
                                    expandedRoleDropdown = false
                                }
                            )
                        }
                    }
                }
                
                // Section 3: Sub-role / Operating Console customization
                if (selectedRole != "DUAL_SIMULATOR") {
                    Text(
                        text = if (selectedRole == "WORSHIP_MEMBER") "STAGE SUB-ROLE (INSTRUMENT / VOCAL)" else "MEDIA CONSOLE DESIGNATION",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    
                    OutlinedTextField(
                        value = subRoleInput,
                        onValueChange = {
                            subRoleInput = it
                            if (selectedRole == "WORSHIP_MEMBER") {
                                onUpdateWorshipSubRole(it)
                            } else {
                                onUpdateMediaSubRole(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("settings_sub_role_input"),
                        label = { Text("E.g. Lead Vocals, PresentationTeam, MediaBooth", fontSize = 11.sp, color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            focusedLabelColor = NeonCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            unfocusedLabelColor = TextSecondary,
                            unfocusedBorderColor = CardBorderColor
                        )
                    )
                }
                
                HorizontalDivider(color = CardBorderColor.copy(alpha = 0.4f), thickness = 1.dp)
                
                // Section 4: Target Groups customization
                Text("MANAGE INTERACTIVE TEAMS & GROUPS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                
                // Text input + Add button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newGroupInput,
                        onValueChange = { newGroupInput = it },
                        modifier = Modifier.weight(1f).testTag("new_group_input"),
                        label = { Text("Add custom target (e.g. MediaBooth)", fontSize = 11.sp, color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            focusedLabelColor = NeonCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            unfocusedLabelColor = TextSecondary,
                            unfocusedBorderColor = CardBorderColor
                        )
                    )
                    
                    Button(
                        onClick = {
                            if (newGroupInput.isNotBlank()) {
                                onAddTargetGroup(newGroupInput)
                                newGroupInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_group_btn")
                    ) {
                        Text("+ Add", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                
                // List of current target groups
                Text("Current Recipients List (Tap '❌' to remove):", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3EDF7))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    targetGroups.forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🎯 $group", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            
                            if (group != "Global") {
                                IconButton(
                                    onClick = { onRemoveTargetGroup(group) },
                                    modifier = Modifier.size(24.dp).testTag("delete_group_btn_$group")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove group",
                                        tint = NeonRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Text("(Default)", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = CardBorderColor.copy(alpha = 0.4f), thickness = 1.dp)

                // Section 5: Local WiFi Fast Connections
                Text("CO-LOCATED DIRECT WI-FI CONNECTION (OFFLINE FAST SYNC)", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3EDF7))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Local Link",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Bypasses slow cloud servers. If devices are on the same Wi-Fi, they connect automatically with near-zero latency.",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                    
                    Switch(
                        checked = localWifiSyncActive,
                        onCheckedChange = onToggleLocalWifiSync,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                if (localWifiSyncActive) {
                    Text(
                        text = if (wifiPeers.isEmpty()) "📡 Scanning for local devices on same network..." else "📡 Connected Local Devices (${wifiPeers.size}):",
                        color = if (wifiPeers.isEmpty()) TextSecondary else NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (wifiPeers.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            wifiPeers.forEach { peer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE8E0F5))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = peer.name,
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "IP: ${peer.ipAddress} | ID: ${peer.deviceId.take(8)}",
                                            color = TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NeonGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text("ACTIVE", color = NeonGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "Ensure other devices open this app and use the same Worship Connection/Sync ID! No cloud pairing setup is required.",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.testTag("settings_done_btn")
            ) {
                Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CardBg
    )
}

@Composable
fun PendingSentAlertPopUp(
    alert: AlertMessage,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent accidental dismissal outside of dialogue */ },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "beacon_pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1100, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "beacon_scale"
                )
                
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RssFeed,
                        contentDescription = "Broadcasting Alert",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Text(
                    text = "📡 TICKET ACTIVE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = NeonCyan
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                    border = BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RAPID PRESET:",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (alert.isCrucial) "⚠️ CRUCIAL" else "STANDARD",
                                color = if (alert.isCrucial) NeonRed else NeonGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = alert.presetType,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            text = alert.description,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Recipient: ${alert.targetRole}",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.5f)))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = NeonCyan,
                        strokeWidth = 2.dp
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Awaiting Recipient Acknowledge...",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "This receipt pop-up will close automatically once the receiver device clicks Acknowledge or Fix.",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                    modifier = Modifier.weight(1f).testTag("cancel_sent_alert_btn")
                ) {
                    Text("🗑️ Recall Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.weight(1f).testTag("dismiss_sent_alert_popup_btn")
                ) {
                    Text("Hide Popup", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = CardBg
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedDispatchDialog(
    preset: CustomPreset,
    targetGroups: List<String>,
    onDismiss: () -> Unit,
    onSend: (target: String, isCrucial: Boolean, description: String) -> Unit,
    onConfigureCard: () -> Unit
) {
    var selectedTargetIndex by remember { mutableStateOf(0) }
    
    val safeIndex = if (targetGroups.isNotEmpty()) {
        selectedTargetIndex.coerceIn(0, targetGroups.lastIndex)
    } else {
        0
    }
    
    val activeTarget = if (targetGroups.isNotEmpty()) {
        targetGroups[safeIndex]
    } else {
        preset.defaultTarget
    }

    val quickOptions = remember(preset) {
        val list = mutableListOf<String>()
        
        // 1. Direct clear general description
        list.add(preset.description)
        
        // 2. Primary Location + Source focus
        val loc1 = if (preset.locations.isNotEmpty()) preset.locations[0] else "Center Stage"
        val src1 = if (preset.sources.isNotEmpty()) preset.sources[0] else "Lead Vocals"
        list.add("[$loc1] $src1: ${preset.description}")
        
        // 3. Secondary focus
        val loc2 = if (preset.locations.size > 1) preset.locations[1] else "Stage Left"
        val src2 = if (preset.sources.size > 1) preset.sources[1] else "Acoustic Guitar"
        list.add("[$loc2] $src2: ${preset.description}")
        
        // 4. Keyboard / Alternate focus
        val loc3 = if (preset.locations.size > 2) preset.locations[2] else "Stage Right"
        val src3 = if (preset.sources.size > 2) preset.sources[2] else "Keyboard / Piano"
        list.add("[$loc3] $src3: ${preset.description}")
        
        // 5. Critical Emergency Warning
        list.add("⚠️ [URGENT ATTENTION REQUIRED] ${preset.description}")
        
        list.take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "⚡ INSTANT ONE-TAP SEND",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = preset.title,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                
                IconButton(
                    onClick = onConfigureCard,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF3EDF7), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure Preset Locations/Sources",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Recipient Pick
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🎯 SEND NOTIFICATION TO CHANNEL / RECEIVER:",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        targetGroups.forEachIndexed { index, group ->
                            val isSelected = selectedTargetIndex == index
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFEADDFF) else Color(0xFFF3EDF7),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF21005D) else CardBorderColor.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .clickable { selectedTargetIndex = index }
                            ) {
                                Text(
                                    text = group,
                                    color = if (isSelected) Color(0xFF21005D) else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = CardBorderColor.copy(alpha = 0.3f), thickness = 1.dp)

                // Section 2: Big 4 to 5 Quick Dispatch Options Lists
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "👉 SELECT READY-MADE DISPATCH (TAP TO SEND INSTANTLY):",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    quickOptions.forEachIndexed { optIndex, optionText ->
                        val isUrgent = optionText.contains("⚠️") || preset.title == "Mic Feedback"
                        val containerBg = if (isUrgent) Color(0xFFFFF2F2) else Color(0xFFF9F6FC)
                        val borderCol = if (isUrgent) NeonRed.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.3f)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSend(activeTarget, isUrgent, optionText)
                                },
                            colors = CardDefaults.cardColors(containerColor = containerBg),
                            border = BorderStroke(1.dp, borderCol),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "${optIndex + 1}.",
                                        color = if (isUrgent) NeonRed else NeonCyan,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = optionText,
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 24.sp
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Send Option",
                                    tint = if (isUrgent) NeonRed else NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "CLOSE DISPATCH ASSISTANT",
                    color = NeonRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        },
        containerColor = CardBg
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditPresetDialog(
    presetToEdit: CustomPreset?,
    targetGroups: List<String>,
    onDismiss: () -> Unit,
    onSave: (CustomPreset) -> Unit,
    onDelete: (String) -> Unit
) {
    val isEditMode = presetToEdit != null
    
    var title by remember { mutableStateOf(presetToEdit?.title ?: "") }
    var description by remember { mutableStateOf(presetToEdit?.description ?: "") }
    var iconName by remember { mutableStateOf(presetToEdit?.iconName ?: "Help") }
    var colorHex by remember { mutableStateOf(presetToEdit?.colorHex ?: "#00FFFF") }
    var defaultTarget by remember { mutableStateOf(presetToEdit?.defaultTarget ?: "Global") }
    
    var locationsText by remember { mutableStateOf(presetToEdit?.locations?.joinToString(", ") ?: "Stage Center, Stage Left, Stage Right") }
    var sourcesText by remember { mutableStateOf(presetToEdit?.sources?.joinToString(", ") ?: "Lead Vocal, Backup Singer, Acoustic Guitar, Keyboard, Drums") }

    val iconOptions = listOf("Mute", "Reverb", "Feedback", "EQ", "Speaker", "Help")
    val colorOptions = listOf(
        "#00FFFF" to "Neon Cyan",
        "#FFFF00" to "Neon Yellow",
        "#FF0000" to "Neon Red",
        "#00FF00" to "Neon Green",
        "#FF00FF" to "Magenta",
        "#5D00FF" to "Deep Violet"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        title = {
            Text(
                text = if (isEditMode) "✏️ Edit Problem Card Options" else "➕ Add New One-Touch Problem Card",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Problem Title (e.g. Wireless Battery)", fontSize = 11.sp, color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag("preset_title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor
                    )
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Default Dispatch Message", fontSize = 11.sp, color = TextSecondary) },
                    placeholder = { Text("e.g. Battery levels have dropped below safe threshold! Need replacement.") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor
                    )
                )

                // Standard Operator dropdown or single-choice list
                Text("DEFAULT RECIPIENT OPERATOR:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    targetGroups.forEach { group ->
                        val isSelected = defaultTarget == group
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFEADDFF) else Color.Transparent)
                                .border(BorderStroke(1.dp, if (isSelected) Color(0xFF21005D) else CardBorderColor), RoundedCornerShape(8.dp))
                                .clickable { defaultTarget = group }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = group, color = if (isSelected) Color(0xFF21005D) else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Preset Icon Names
                Text("CARD GLYPH / ICON STYLE:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    iconOptions.forEach { name ->
                        val isSelected = iconName == name
                        val vector = when (name) {
                            "Mute" -> Icons.Filled.VolumeMute
                            "Reverb" -> Icons.Filled.LeakAdd
                            "Feedback" -> Icons.Filled.Warning
                            "EQ" -> Icons.Filled.GraphicEq
                            "Speaker" -> Icons.Filled.Speaker
                            else -> Icons.Filled.Help
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .border(BorderStroke(1.dp, if (isSelected) NeonCyan else CardBorderColor), RoundedCornerShape(16.dp))
                                .clickable { iconName = name }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = vector, contentDescription = name, tint = if (isSelected) NeonCyan else Color.Gray, modifier = Modifier.size(16.dp))
                            Text(text = name, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Preset Colors Selection
                Text("CARD HIGH-VISIBILITY BORDER COLOR:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colorOptions.forEach { (hex, optionLabel) ->
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        val composeColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { NeonCyan }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) composeColor.copy(alpha = 0.15f) else Color.Transparent)
                                .border(BorderStroke(2.dp, if (isSelected) composeColor else composeColor.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
                                .clickable { colorHex = hex }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(composeColor))
                            Text(text = optionLabel, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = CardBorderColor.copy(alpha = 0.3f), thickness = 1.dp)

                // Customizable Option Lists: Locations
                Text(
                    text = "CUSTOMIZABLE LOCATIONS QUESTION (COMMA SEPARATED):",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = locationsText,
                    onValueChange = { locationsText = it },
                    placeholder = { Text("e.g. Center Stage, Left side, Main PA, FOH Booth") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor
                    )
                )

                // Customizable Option Lists: Sources/Mics
                Text(
                    text = "CUSTOMIZABLE WHOSE / SOURCES QUESTION (COMMA SEPARATED):",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = sourcesText,
                    onValueChange = { sourcesText = it },
                    placeholder = { Text("e.g. Lead Singer, Back Voc 1, Acoustic DI, Keyboardist") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor
                    )
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditMode) {
                    OutlinedButton(
                        onClick = { onDelete(presetToEdit!!.id) },
                        border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🗑️ Delete Card", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, CardBorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Button(
                    onClick = {
                        if (title.isNotBlank() && description.isNotBlank()) {
                            val locationsList = locationsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val sourcesList = sourcesText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val newOrUpdated = CustomPreset(
                                id = presetToEdit?.id ?: "preset_" + System.currentTimeMillis().toString(),
                                title = title,
                                description = description,
                                iconName = iconName,
                                colorHex = colorHex,
                                defaultTarget = defaultTarget,
                                locations = locationsList,
                                sources = sourcesList
                            )
                            onSave(newOrUpdated)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    enabled = title.isNotBlank() && description.isNotBlank(),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(text = if (isEditMode) "💾 Save Changes" else "➕ Create Card", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        },
        containerColor = CardBg
    )
}

@Composable
fun VisualPulseOverlay(
    triggerTime: Long?,
    isCrucial: Boolean
) {
    if (triggerTime == null) return

    var animationProgress by remember(triggerTime) { mutableStateOf(0f) }

    LaunchedEffect(triggerTime) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animationProgress = value
        }
    }

    if (animationProgress < 1f) {
        val alpha = (1f - animationProgress) * 0.75f
        val edgePulseColor = if (isCrucial) NeonRed else NeonCyan

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val strokeWidth = 24.dp.toPx() * (1f - animationProgress)
                    if (strokeWidth > 0) {
                        drawRect(
                            color = edgePulseColor,
                            style = Stroke(width = strokeWidth),
                            alpha = alpha
                        )
                    }

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, edgePulseColor.copy(alpha = alpha * 0.2f)),
                            radius = size.maxDimension * 0.7f
                        ),
                        alpha = alpha
                    )
                }
                .pointerInput(triggerTime) {}
        )
    }
}

