package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AlertMessage
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.ChannelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.random.Random

// OkHttp, Moshi, and Sync Imports
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

enum class NetworkSyncMode {
    CLOUD,
    LOCAL_WIFI
}

data class CustomPreset(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val colorHex: String,
    val defaultTarget: String,
    val locations: List<String> = emptyList(),
    val sources: List<String> = emptyList()
)

class StageBridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    private var cloudSyncJob: Job? = null

    // --- Dynamic Preset Alerts (Worship & Media problems cards) ---
    private val _presetAlerts = MutableStateFlow<List<CustomPreset>>(emptyList())
    val presetAlerts: StateFlow<List<CustomPreset>> = _presetAlerts.asStateFlow()

    private val presetListType = Types.newParameterizedType(List::class.java, CustomPreset::class.java)
    private val presetAdapter = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter<List<CustomPreset>>(presetListType)

    // --- Device / User State ---
    private val _deviceRole = MutableStateFlow("DUAL_SIMULATOR") // "WORSHIP_MEMBER", "MEDIA_OPERATOR", "DUAL_SIMULATOR"
    val deviceRole: StateFlow<String> = _deviceRole.asStateFlow()

    private val _worshipSubRole = MutableStateFlow("Worship Leader") 
    // "Worship Leader", "Lead Vocalist", "Keyboardist", "Guitarist", "Bassist", "Drummer"
    val worshipSubRole: StateFlow<String> = _worshipSubRole.asStateFlow()

    private val _mediaSubRole = MutableStateFlow("Live Stream Operator")
    // "Live Stream Operator", "FOH Engineer", "Monitors Engineer", "Media Group", "Global"
    val mediaSubRole: StateFlow<String> = _mediaSubRole.asStateFlow()

    private val _senderName = MutableStateFlow("Worship Leader")
    val senderName: StateFlow<String> = _senderName.asStateFlow()

    // --- Worship Connection/Sync ID ---
    private val _syncGroupId = MutableStateFlow("grace_church_worship")
    val syncGroupId: StateFlow<String> = _syncGroupId.asStateFlow()

    // --- Alerting Popup Banner State ---
    private val _activeOverlayAlert = MutableStateFlow<AlertMessage?>(null)
    val activeOverlayAlert: StateFlow<AlertMessage?> = _activeOverlayAlert.asStateFlow()

    private val _visualPulseTrigger = MutableStateFlow<Long?>(null)
    val visualPulseTrigger: StateFlow<Long?> = _visualPulseTrigger.asStateFlow()

    private val _visualPulseIsCrucial = MutableStateFlow(false)
    val visualPulseIsCrucial: StateFlow<Boolean> = _visualPulseIsCrucial.asStateFlow()

    // --- Selected Active Target Group/Channel for Fast Cues ---
    private val _activeTargetGroup = MutableStateFlow("Global")
    val activeTargetGroup: StateFlow<String> = _activeTargetGroup.asStateFlow()

    // --- Font Scale multiplier for dynamic layout readability zoom ---
    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    // --- Local Wi-Fi Fast Sync & Direct Networking ---
    private val _networkSyncMode = MutableStateFlow(NetworkSyncMode.CLOUD)
    val networkSyncMode: StateFlow<NetworkSyncMode> = _networkSyncMode.asStateFlow()

    private val _localWifiSyncActive = MutableStateFlow(true)
    val localWifiSyncActive: StateFlow<Boolean> = _localWifiSyncActive.asStateFlow()

    private val _wifiPeers = MutableStateFlow<List<LocalWifiSyncManager.PeerDevice>>(emptyList())
    val wifiPeers: StateFlow<List<LocalWifiSyncManager.PeerDevice>> = _wifiPeers.asStateFlow()

    private lateinit var localWifiSyncManager: LocalWifiSyncManager

    // Unique device UUID across sessions for blocking self notifications
    val deviceId: String

    // Customizable Destination Targets / Groups
    private val _targetGroups = MutableStateFlow<List<String>>(emptyList())
    val targetGroups: StateFlow<List<String>> = _targetGroups.asStateFlow()

    // --- Sent Alert Tracking for "WhatsApp-style" Delivered / Receipt Popups ---
    private val _lastSentAlertId = MutableStateFlow<String?>(null)
    val lastSentAlertId: StateFlow<String?> = _lastSentAlertId.asStateFlow()

    val pendingSentAlert: StateFlow<AlertMessage?> by lazy {
        combine(allAlerts, _lastSentAlertId) { alerts, sentId ->
            if (sentId == null) null
            else alerts.find { it.id == sentId && it.status == "NEW" }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    // --- Core data streams from Room DB ---
    val allAlerts: StateFlow<List<AlertMessage>>
    val allChannels: StateFlow<List<ChannelStatus>>

    private val notifiedAlertIds = mutableSetOf<String>()

    // HTTP & Moshi Sync Clients
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val alertsListType = Types.newParameterizedType(List::class.java, AlertMessage::class.java)
    private val alertsAdapter = moshi.adapter<List<AlertMessage>>(alertsListType)

    init {
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)

        // Load or generate device ID
        var dId = sharedPrefs.getString("device_id", null)
        if (dId == null) {
            dId = "dev_" + Random.nextInt(100000, 999999).toString()
            sharedPrefs.edit().putString("device_id", dId).apply()
        }
        deviceId = dId

        // Load saved roles and configurations
        _deviceRole.value = sharedPrefs.getString("device_role", "DUAL_SIMULATOR") ?: "DUAL_SIMULATOR"
        _worshipSubRole.value = sharedPrefs.getString("worship_sub_role", "Worship Leader") ?: "Worship Leader"
        _mediaSubRole.value = sharedPrefs.getString("media_sub_role", "Live Stream Operator") ?: "Live Stream Operator"
        _senderName.value = sharedPrefs.getString("sender_name", "Worship Leader") ?: "Worship Leader"
        _syncGroupId.value = sharedPrefs.getString("sync_group_id", "grace_church_worship") ?: "grace_church_worship"
        _fontScale.value = sharedPrefs.getFloat("font_scale", 1.0f)

        // Load destination groups customizable list
        val defaultTargets = "Global,FOH Engineer,Live Stream Operator,Monitors Engineer,Media Group,MediaBooth,PresentationTeam"
        val targetsString = sharedPrefs.getString("target_groups", defaultTargets) ?: defaultTargets
        _targetGroups.value = targetsString.split(",").filter { it.isNotBlank() }

        val appDao = AppDatabase.getDatabase(application).appDao()
        repository = AppRepository(appDao)

        // Initialize Local Wi-Fi Sync Manager
        localWifiSyncManager = LocalWifiSyncManager(
            context = context,
            deviceId = deviceId,
            getSyncGroupId = { _syncGroupId.value },
            getSenderName = { _senderName.value },
            onAlertsReceived = { remoteAlerts ->
                viewModelScope.launch(Dispatchers.IO) {
                    syncRemoteAlertsWithRoom(remoteAlerts)
                }
            }
        )

        // Bind discovered peers flow to local StateFlow
        viewModelScope.launch {
            localWifiSyncManager.discoveredPeers.collect { peers ->
                _wifiPeers.value = peers
            }
        }

        // Retrieve and apply network mode configuration
        val defaultMode = if (sharedPrefs.getBoolean("local_wifi_sync_enabled", true)) {
            NetworkSyncMode.LOCAL_WIFI
        } else {
            NetworkSyncMode.CLOUD
        }
        val modeStr = sharedPrefs.getString("network_sync_mode", defaultMode.name) ?: defaultMode.name
        val initialMode = try { NetworkSyncMode.valueOf(modeStr) } catch (e: Exception) { defaultMode }
        _networkSyncMode.value = initialMode
        
        val wifiSyncEnabled = (initialMode == NetworkSyncMode.LOCAL_WIFI)
        _localWifiSyncActive.value = wifiSyncEnabled
        if (wifiSyncEnabled) {
            localWifiSyncManager.start()
        }

        allAlerts = repository.allAlerts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allChannels = repository.allChannels.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Populate standard mixer channels
        viewModelScope.launch {
            repository.checkAndPopulateDefaultChannels()
        }

        // Setup notification channel
        createNotificationChannel()
        loadPresetAlerts()
        // Send initial setup of quick report shortcuts inside system notification drawer
        refreshPersistentNotification()

        // Monitor incoming alerts flow to trigger real-time physical effects (sound, notification, popup vibration)
        var isFirstCollection = true
        viewModelScope.launch {
            allAlerts.collect { alerts ->
                if (isFirstCollection) {
                    // Populate initial in-memory set with existing alerts so we do not notify them again on boot
                    alerts.forEach { notifiedAlertIds.add(it.id) }
                    isFirstCollection = false
                    return@collect
                }

                // For subsequent collections, check for any NEW alert that is not already in notifiedAlertIds
                val newUnnotifiedAlerts = alerts.filter { it.status == "NEW" && !notifiedAlertIds.contains(it.id) }
                for (alert in newUnnotifiedAlerts) {
                    notifiedAlertIds.add(alert.id)
                    onNewAlertReceived(alert)
                }
            }
        }

        // Start real-time background cloud synchronization polling
        startCloudSyncLoop()
    }

    // --- Setup real-time key-value polling ---
    private fun startCloudSyncLoop() {
        cloudSyncJob?.cancel()
        cloudSyncJob = viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            while (true) {
                try {
                    pullAlertsFromCloud()
                } catch (e: Exception) {
                    Log.w("StageBridgeSync", "Failed to sync: ${e.message}")
                }
                delay(1000)
            }
        }
    }

    private suspend fun pullAlertsFromCloud() {
        if (_networkSyncMode.value != NetworkSyncMode.CLOUD) return
        val cleanGroup = _syncGroupId.value.replace(Regex("[^a-zA-Z0-9_-]"), "").lowercase()
        val url = "https://kvdb.io/buckets/sb_$cleanGroup/keys/alerts"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val remoteAlerts = alertsAdapter.fromJson(bodyString) ?: emptyList()
                        syncRemoteAlertsWithRoom(remoteAlerts)
                    }
                } else if (response.code == 404) {
                    // Initialize bucket with current local alerts if empty online
                    uploadAlertsToCloud()
                }
            }
        } catch (e: Exception) {
            Log.w("StageBridgeSync", "Cloud poll network error: ${e.message}")
        }
    }

    private suspend fun syncRemoteAlertsWithRoom(remoteAlerts: List<AlertMessage>) {
        val localAlerts = repository.allAlerts.first()
        val localMap = localAlerts.associateBy { it.id }

        // Write new remote items or updates to local Room DB
        remoteAlerts.forEach { remote ->
            val local = localMap[remote.id]
            if (local == null) {
                repository.sendAlert(remote)
            } else if (local.status != remote.status) {
                repository.updateAlertStatus(remote.id, remote.status)
            }
        }

        // Wipe local rows that were cleared from the cloud
        val remoteIds = remoteAlerts.map { it.id }.toSet()
        localAlerts.forEach { local ->
            if (!remoteIds.contains(local.id)) {
                repository.deleteAlertById(local.id)
            }
        }
    }

    fun uploadAlertsToCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanGroup = _syncGroupId.value.replace(Regex("[^a-zA-Z0-9_-]"), "").lowercase()
                val url = "https://kvdb.io/buckets/sb_$cleanGroup/keys/alerts"
                val localAlerts = repository.allAlerts.first()
                
                // Broadcast instantly via direct local Wi-Fi UDP networking (sub-millisecond latency)
                if (_localWifiSyncActive.value && ::localWifiSyncManager.isInitialized) {
                    localWifiSyncManager.broadcastAlerts(localAlerts)
                }

                if (_networkSyncMode.value != NetworkSyncMode.CLOUD) {
                    return@launch
                }

                val json = alertsAdapter.toJson(localAlerts)

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = json.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("StageBridgeSync", "Post to cloud failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("StageBridgeSync", "Post upload network exception: ${e.message}")
            }
        }
    }

    // --- Setter actions ---
    fun setDeviceRole(role: String) {
        _deviceRole.value = role
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("device_role", role).apply()
        refreshPersistentNotification()
    }

    fun setWorshipSubRole(sub: String) {
        _worshipSubRole.value = sub
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("worship_sub_role", sub).apply()
        if (_senderName.value.isEmpty() || _senderName.value == "Worship Leader") {
            setSenderName(sub)
        }
    }

    fun setMediaSubRole(sub: String) {
        _mediaSubRole.value = sub
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("media_sub_role", sub).apply()
        refreshPersistentNotification()
    }

    fun setSenderName(name: String) {
        _senderName.value = name
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("sender_name", name).apply()
    }

    fun setSyncGroupId(id: String) {
        _syncGroupId.value = id
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("sync_group_id", id).apply()
        // Immediately trigger pulling to switch buckets
        viewModelScope.launch(Dispatchers.IO) {
            pullAlertsFromCloud()
        }
    }

    fun toggleLocalWifiSync(enable: Boolean) {
        _localWifiSyncActive.value = enable
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("local_wifi_sync_enabled", enable).apply()
        
        if (enable) {
            localWifiSyncManager.start()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val alerts = repository.allAlerts.first()
                    localWifiSyncManager.broadcastAlerts(alerts)
                } catch (e: Exception) {
                    Log.e("StageBridgeVM", "Local Wi-Fi start broadcast error: ${e.message}")
                }
            }
        } else {
            localWifiSyncManager.stop()
        }
    }

    fun setNetworkSyncMode(mode: NetworkSyncMode) {
        _networkSyncMode.value = mode
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("network_sync_mode", mode.name).apply()
        
        when (mode) {
            NetworkSyncMode.CLOUD -> {
                toggleLocalWifiSync(false)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        pullAlertsFromCloud()
                    } catch (e: Exception) {
                        Log.e("StageBridgeVM", "Failed to force pull from cloud on toggle: ${e.message}")
                    }
                }
            }
            NetworkSyncMode.LOCAL_WIFI -> {
                toggleLocalWifiSync(true)
            }
        }
    }

    fun addTargetGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            val current = _targetGroups.value.toMutableList()
            if (!current.contains(trimmed)) {
                current.add(trimmed)
                _targetGroups.value = current
                val context = getApplication<Application>()
                val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putString("target_groups", current.joinToString(",")).apply()
            }
        }
    }

    fun removeTargetGroup(name: String) {
        val current = _targetGroups.value.toMutableList()
        if (current.remove(name)) {
            _targetGroups.value = current
            val context = getApplication<Application>()
            val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("target_groups", current.joinToString(",")).apply()
        }
    }

    fun editTargetGroup(oldName: String, newName: String) {
        val trimmedNew = newName.trim()
        if (trimmedNew.isNotBlank()) {
            val current = _targetGroups.value.map {
                if (it == oldName) trimmedNew else it
            }.distinct()
            _targetGroups.value = current
            val context = getApplication<Application>()
            val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("target_groups", current.joinToString(",")).apply()
            
            // If the edited group was active, update the active group name too
            if (_activeTargetGroup.value == oldName) {
                _activeTargetGroup.value = trimmedNew
            }
        }
    }

    fun setActiveTargetGroup(group: String) {
        _activeTargetGroup.value = group
    }

    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.7f, 1.8f)
        _fontScale.value = clamped
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putFloat("font_scale", clamped).apply()
    }

    fun adjustFontScale(increment: Boolean) {
        val step = 0.1f
        val newScale = _fontScale.value + (if (increment) step else -step)
        setFontScale(newScale)
    }

    // --- Alert Reporting core logic ---
    fun submitAlert(presetType: String, description: String, target: String, isCrucial: Boolean = false) {
        viewModelScope.launch {
            val alert = AlertMessage(
                senderName = _senderName.value,
                senderRole = when (_deviceRole.value) {
                    "WORSHIP_MEMBER" -> _worshipSubRole.value
                    "MEDIA_OPERATOR" -> _mediaSubRole.value
                    else -> _worshipSubRole.value
                },
                targetRole = target,
                presetType = presetType,
                description = description,
                isCrucial = isCrucial,
                status = "NEW",
                senderDeviceId = deviceId
            )
            repository.sendAlert(alert)
            _lastSentAlertId.value = alert.id
            uploadAlertsToCloud()
        }
    }

    fun cancelSentAlert(id: String) {
        viewModelScope.launch {
            repository.deleteAlertById(id)
            if (_lastSentAlertId.value == id) {
                _lastSentAlertId.value = null
            }
            uploadAlertsToCloud()
        }
    }

    fun clearPendingSentAlert() {
        _lastSentAlertId.value = null
    }

    fun acknowledgeAlert(id: String) {
        viewModelScope.launch {
            repository.updateAlertStatus(id, "ACKNOWLEDGED")
            if (_activeOverlayAlert.value?.id == id) {
                _activeOverlayAlert.value = null
            }
            uploadAlertsToCloud()
        }
    }

    fun solveAlert(id: String) {
        viewModelScope.launch {
            repository.updateAlertStatus(id, "SOLVED")
            if (_activeOverlayAlert.value?.id == id) {
                _activeOverlayAlert.value = null
            }
            uploadAlertsToCloud()
        }
    }

    fun dismissOverlay() {
        _activeOverlayAlert.value = null
    }

    fun clearAllAlerts() {
        viewModelScope.launch {
            repository.clearAlerts()
            _activeOverlayAlert.value = null
            uploadAlertsToCloud()
        }
    }

    // --- Channel operations ---
    fun toggleMuteChannel(channelId: Int, currentStatus: String) {
        viewModelScope.launch {
            val newStatus = if (currentStatus == "MUTED") "OK" else "MUTED"
            repository.updateChannelStatus(channelId, newStatus)
        }
    }

    fun updateChannelStatusManually(channelId: Int, status: String) {
        viewModelScope.launch {
            repository.updateChannelStatus(channelId, status)
        }
    }

    // --- Core dispatch triggers for Device feedback (Vibration, Notification, Popup Overlay) ---
    private fun onNewAlertReceived(alert: AlertMessage) {
        // Broadly block incoming notifications/overlays generated by our own device!
        if (alert.senderDeviceId.isNotEmpty() && alert.senderDeviceId == deviceId) {
            Log.d("StageBridgeVM", "Blocking physical feedback for self-sent alert: ${alert.id}")
            return
        }

        val currentRole = _deviceRole.value
        val currentSubRole = when (currentRole) {
            "WORSHIP_MEMBER" -> _worshipSubRole.value
            "MEDIA_OPERATOR" -> _mediaSubRole.value
            else -> ""
        }

        val isTarget = when {
            alert.targetRole == "Global" -> true
            currentRole == "MEDIA_OPERATOR" && alert.targetRole == "Media Group" -> true
            currentRole == "WORSHIP_MEMBER" && alert.targetRole == "Worship Team" -> true
            alert.targetRole == currentSubRole -> true
            currentRole == "DUAL_SIMULATOR" -> true
            else -> false
        }

        if (isTarget) {
            _activeOverlayAlert.value = alert
            _visualPulseIsCrucial.value = alert.isCrucial
            _visualPulseTrigger.value = System.currentTimeMillis()
            triggerToneSound(alert.isCrucial)
            triggerIntensityVibration(alert.isCrucial)
            showSystemPushNotification(alert)
        }
    }

    private fun triggerToneSound(isCrucial: Boolean) {
        viewModelScope.launch {
            try {
                // Play a hard sound when a troubleshooting request is received
                val gen = ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                if (isCrucial) {
                    // Crucial / emergency alert: fast, loud triple beep
                    for (i in 1..3) {
                        gen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
                        delay(350)
                    }
                } else {
                    // Standard warning alert: loud double beep
                    gen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                    delay(300)
                    gen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                }
            } catch (e: Exception) {
                Log.e("StageBridgeVM", "Could not play primary hard sound: ${e.message}")
                try {
                    // Fallback to simpler single ToneGenerator stream notification
                    val toneType = if (isCrucial) ToneGenerator.TONE_CDMA_HIGH_L else ToneGenerator.TONE_PROP_BEEP2
                    val fallbackGen = ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
                    fallbackGen.startTone(toneType, if (isCrucial) 1000 else 600)
                } catch (ex: Exception) {
                    Log.e("StageBridgeVM", "Fallback sound failed: ${ex.message}", ex)
                }
            }
        }
    }

    private fun triggerIntensityVibration(isCrucial: Boolean) {
        val context = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let { v ->
            if (v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = if (isCrucial) {
                        longArrayOf(0, 400, 150, 400, 150, 500)
                    } else {
                        longArrayOf(0, 250, 100, 250)
                    }
                    val amplitudes = if (isCrucial) {
                        intArrayOf(0, 255, 0, 255, 0, 255)
                    } else {
                        intArrayOf(0, 180, 0, 180)
                    }
                    v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(if (isCrucial) 1200 else 500)
                }
            }
        }
    }

    private fun showSystemPushNotification(alert: AlertMessage) {
        val context = getApplication<Application>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingLaunch = PendingIntent.getActivity(
            context,
            99,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val importanceType = if (alert.isCrucial) "⚠️ HIGH ALERT" else "Worship Team Alert"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("$importanceType [${alert.senderRole}]")
            .setContentText("${alert.presetType}: ${alert.description}")
            .setSubText("To: ${alert.targetRole}")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingLaunch)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Sender: ${alert.senderName} (${alert.senderRole})\n" +
                "Issue: ${alert.presetType}\n" +
                "Details: ${alert.description}\n\n" +
                "Direct your attention for immediate audio adjustment!"
            ))
            .build()

        manager.notify(alert.id.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = getApplication<Application>()
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "StageBridge Event Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent volume and EQ notifications dispatched directly from stage musicians."
                enableVibration(true)
                setShowBadge(true)
            }

            val persistentChannel = NotificationChannel(
                CHANNEL_ID_PERSISTENT,
                "StageBridge Quick Access Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent lockscreen and drawer shortcuts for rapid one-click reporting."
                setShowBadge(false)
            }

            manager.createNotificationChannel(alertsChannel)
            manager.createNotificationChannel(persistentChannel)
        }
    }

    fun refreshPersistentNotification() {
        val context = getApplication<Application>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intentLaunch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingLaunch = PendingIntent.getActivity(
            context,
            201,
            intentLaunch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pLowVolume = PendingIntent.getBroadcast(
            context, 301, Intent("com.example.stagebridge.ACTION_QUICK_LOW_VOLUME"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pReverb = PendingIntent.getBroadcast(
            context, 302, Intent("com.example.stagebridge.ACTION_QUICK_REVERB"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pFeedback = PendingIntent.getBroadcast(
            context, 303, Intent("com.example.stagebridge.ACTION_QUICK_FEEDBACK"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pFrontSpeakers = PendingIntent.getBroadcast(
            context, 304, Intent("com.example.stagebridge.ACTION_QUICK_FRONT_SPEAKERS"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_PERSISTENT)
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentTitle("StageBridge Active")
            .setContentText("Role: ${_deviceRole.value} (${_worshipSubRole.value}) — Direct tray shortcuts enabled.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingLaunch)
            .addAction(android.R.drawable.ic_lock_silent_mode, "LOW VOL", pLowVolume)
            .addAction(android.R.drawable.ic_menu_slideshow, "REVERB", pReverb)
            .addAction(android.R.drawable.ic_dialog_alert, "FEEDBACK", pFeedback)
            .addAction(android.R.drawable.ic_menu_add, "PA SPKRS", pFrontSpeakers)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "StageBridge is streaming in the background.\n" +
                "Do not quit. Click any below quick actions to report from the notifications bar:"
            ))

        manager.notify(PERSISTENT_NOTIFICATION_ID, builder.build())
    }



    fun loadPresetAlerts() {
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("preset_alerts_json", null)
        if (json != null) {
            try {
                val list = presetAdapter.fromJson(json)
                if (list != null && list.isNotEmpty()) {
                    _presetAlerts.value = list
                    return
                }
            } catch (e: Exception) {
                Log.e("StageBridgeVM", "Error loading presets, resetting: ${e.message}")
            }
        }
        // Fallback to default presets
        resetPresetsToDefault()
    }

    fun resetPresetsToDefault() {
        val defaults = listOf(
            CustomPreset(
                id = "preset_vol_low",
                title = "Volume/Low",
                description = "Stage audio signal is extremely quiet, boost gain!",
                iconName = "Mute",
                colorHex = "#00FFFF",
                defaultTarget = "Global",
                locations = listOf("Stage Center", "Stage Left", "Stage Right", "Backline Floor", "FOH Booth"),
                sources = listOf("Lead Vocal Mic", "Backup Singer 1", "Backup Singer 2", "Acoustic Guitar", "Electric Guitar", "Keyboard", "Bass Guitar", "Drummer Mix")
            ),
            CustomPreset(
                id = "preset_reverb",
                title = "Bad Reverb",
                description = "reverb and delay space is overwhelming instrument tone.",
                iconName = "Reverb",
                colorHex = "#FFFF00",
                defaultTarget = "Global",
                locations = listOf("Main PA Left", "Main PA Right", "Monitors Center", "Sidefills Left", "Sidefills Right"),
                sources = listOf("Acoustic Guitar", "Keys Synth", "Lead Vocal", "Backup Vocals", "Choir Microphones")
            ),
            CustomPreset(
                id = "preset_feedback",
                title = "Mic Feedback",
                description = "STAGE FEEDBACK TRIGGERED! Please find and notch the offending channel.",
                iconName = "Feedback",
                colorHex = "#FF0000",
                defaultTarget = "Global",
                locations = listOf("Monitors Center", "Monitors Stage Left", "Monitors Stage Right", "Main Room", "In-Ear System"),
                sources = listOf("Lead Vocal Mic", "Backup Singer 1", "Backup Singer 2", "Acoustic Guitar Mic", "Drum Setup", "Pastor Lapel", "Podium Mic")
            ),
            CustomPreset(
                id = "preset_muffled",
                title = "Muffled/EQ",
                description = "High frequencies are missing; audio is sounding excessively muffled.",
                iconName = "EQ",
                colorHex = "#FFFF00",
                defaultTarget = "Global",
                locations = listOf("Stage Center Speakers", "Left PA Array", "Right PA Array", "Keyboard Channel"),
                sources = listOf("Lead Vocal Mic", "Backup Singer Mix", "Keyboard Direct Box", "Acoustic Guitar Piezo", "Acoustic Piano Mics")
            ),
            CustomPreset(
                id = "preset_front_spk",
                title = "Front Speakers",
                description = "Audience Front speaker systems feel unequal relative to monitors.",
                iconName = "Speaker",
                colorHex = "#00FFFF",
                defaultTarget = "Global",
                locations = listOf("Front-Fills Center", "Out-Fills Left", "Out-Fills Right", "Under-Balcony Array", "Subwoofers"),
                sources = listOf("Overall Vocal Mix", "Acoustic Guitar", "Keyboard Keys", "Kick Drum Trigger", "Bass Direct DI")
            )
        )
        savePresetAlerts(defaults)
    }

    fun savePresetAlerts(presets: List<CustomPreset>) {
        _presetAlerts.value = presets
        val context = getApplication<Application>()
        val sharedPrefs = context.getSharedPreferences("stage_bridge_prefs", Context.MODE_PRIVATE)
        try {
            val json = presetAdapter.toJson(presets)
            sharedPrefs.edit().putString("preset_alerts_json", json).apply()
        } catch (e: Exception) {
            Log.e("StageBridgeVM", "Error saving presets: ${e.message}")
        }
    }

    fun addPresetAlert(preset: CustomPreset) {
        val current = _presetAlerts.value.toMutableList()
        current.add(preset)
        savePresetAlerts(current)
    }

    fun updatePresetAlert(updated: CustomPreset) {
        val current = _presetAlerts.value.map {
            if (it.id == updated.id) updated else it
        }
        savePresetAlerts(current)
    }

    fun deletePresetAlert(presetId: String) {
        val current = _presetAlerts.value.filter { it.id != presetId }
        savePresetAlerts(current)
    }

    override fun onCleared() {
        super.onCleared()
        cloudSyncJob?.cancel()
        if (::localWifiSyncManager.isInitialized) {
            localWifiSyncManager.stop()
        }
    }

    companion object {
        const val CHANNEL_ID_ALERTS = "STAGEBRIDGE_ALERTS_CHANNEL"
        const val CHANNEL_ID_PERSISTENT = "STAGEBRIDGE_PERSISTENT_CHANNEL"
        const val PERSISTENT_NOTIFICATION_ID = 88812
    }
}
