package com.example.ui

import android.content.Context
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.AlertMessage
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets

class LocalWifiSyncManager(
    private val context: Context,
    private val deviceId: String,
    private val getSyncGroupId: () -> String,
    private val getSenderName: () -> String,
    private val onAlertsReceived: (List<AlertMessage>) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: DatagramSocket? = null
    private val port = 18888

    private val _discoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val discoveredPeers = _discoveredPeers.asStateFlow()

    private var isRunning = false
    private var listeningJob: Job? = null
    private var broadcastJob: Job? = null
    private var peerCleanupJob: Job? = null

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val alertsListType = Types.newParameterizedType(List::class.java, AlertMessage::class.java)
    private val alertsAdapter = moshi.adapter<List<AlertMessage>>(alertsListType)
    private val messageAdapter = moshi.adapter(WifiSyncMessage::class.java)

    data class PeerDevice(
        val ipAddress: String,
        val deviceId: String,
        val name: String,
        val syncGroupId: String,
        val lastSeen: Long
    )

    data class WifiSyncMessage(
        val type: String, // "DISCOVERY_PING", "ALERTS_DATA"
        val deviceId: String,
        val senderName: String,
        val syncGroupId: String,
        val payload: String? = null
    )

    fun start() {
        if (isRunning) return
        isRunning = true
        Log.i("LocalWifiSync", "Starting Local Wi-Fi Sync Manager...")

        listeningJob = scope.launch {
            listenForPackets()
        }
        broadcastJob = scope.launch {
            broadcastDiscoveryLoop()
        }
        peerCleanupJob = scope.launch {
            peerCleanupLoop()
        }
    }

    fun stop() {
        isRunning = false
        listeningJob?.cancel()
        broadcastJob?.cancel()
        peerCleanupJob?.cancel()
        socket?.close()
        socket = null
        _discoveredPeers.value = emptyList()
        Log.i("LocalWifiSync", "Local Wi-Fi Sync Manager stopped.")
    }

    private fun getBroadcastAddress(): InetAddress {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp: DhcpInfo? = wifiManager.dhcpInfo
            if (dhcp != null && dhcp.ipAddress != 0) {
                val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
                val quads = ByteArray(4)
                for (k in 0..3) {
                    quads[k] = ((broadcast shr (k * 8)) and 0xFF).toByte()
                }
                return InetAddress.getByAddress(quads)
            }
        } catch (e: Exception) {
            Log.w("LocalWifiSync", "Failed to retrieve local subnet broadcast: ${e.message}")
        }
        return InetAddress.getByName("255.255.255.255")
    }

    private suspend fun listenForPackets() {
        while (isRunning) {
            try {
                if (socket == null || socket?.isClosed == true) {
                    socket = DatagramSocket(port).apply {
                        broadcast = true
                        reuseAddress = true
                    }
                }
                val buffer = ByteArray(8192)
                val packet = DatagramPacket(buffer, buffer.size)
                
                withContext(Dispatchers.IO) {
                    socket?.receive(packet)
                }

                val senderIp = packet.address.hostAddress ?: continue
                val dataString = String(packet.data, 0, packet.length, StandardCharsets.UTF_8).trim()
                
                Log.d("LocalWifiSync", "Received packet of length ${packet.length} from $senderIp")

                val message = try {
                    messageAdapter.fromJson(dataString)
                } catch (e: Exception) {
                    null
                }

                if (message != null && message.deviceId != deviceId && message.syncGroupId == getSyncGroupId()) {
                    when (message.type) {
                        "DISCOVERY_PING" -> {
                            handleDiscoveryPing(senderIp, message)
                        }
                        "ALERTS_DATA" -> {
                            Log.d("LocalWifiSync", "Received local Wi-Fi alerts from $senderIp")
                            message.payload?.let { json ->
                                try {
                                    val alerts = alertsAdapter.fromJson(json)
                                    if (alerts != null) {
                                        onAlertsReceived(alerts)
                                    }
                                } catch (e: Exception) {
                                    Log.e("LocalWifiSync", "Error parsing fast sync JSON: ${e.message}")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.w("LocalWifiSync", "Socket read error, reconnecting in 2s... ${e.message}")
                    socket?.close()
                    socket = null
                    delay(2000)
                }
            }
        }
    }

    private fun handleDiscoveryPing(ip: String, msg: WifiSyncMessage) {
        val currentPeers = _discoveredPeers.value.toMutableList()
        val existingIndex = currentPeers.indexOfFirst { it.deviceId == msg.deviceId }
        val updatedPeer = PeerDevice(
            ipAddress = ip,
            deviceId = msg.deviceId,
            name = msg.senderName,
            syncGroupId = msg.syncGroupId,
            lastSeen = System.currentTimeMillis()
        )
        if (existingIndex >= 0) {
            currentPeers[existingIndex] = updatedPeer
        } else {
            currentPeers.add(updatedPeer)
            Log.i("LocalWifiSync", "New Wi-Fi peer discovered: ${msg.senderName} ($ip)")
        }
        _discoveredPeers.value = currentPeers
    }

    private suspend fun broadcastDiscoveryLoop() {
        while (isRunning) {
            try {
                sendPing()
            } catch (e: Exception) {
                Log.w("LocalWifiSync", "Failed to send discovery ping: ${e.message}")
            }
            delay(1500)
        }
    }

    private fun sendPing() {
        val pingMsg = WifiSyncMessage(
            type = "DISCOVERY_PING",
            deviceId = deviceId,
            senderName = getSenderName(),
            syncGroupId = getSyncGroupId()
        )
        val json = messageAdapter.toJson(pingMsg) ?: return
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        
        val socketToUse = socket ?: return
        
        scope.launch {
            try {
                val broadcastAddr = getBroadcastAddress()
                val packet = DatagramPacket(bytes, bytes.size, broadcastAddr, port)
                socketToUse.send(packet)
            } catch (e: Exception) {
                try {
                    val packetFallback = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), port)
                    socketToUse.send(packetFallback)
                } catch (ex: Exception) {
                    Log.e("LocalWifiSync", "Broadcast failure: ${ex.message}")
                }
            }
        }
    }

    fun broadcastAlerts(alerts: List<AlertMessage>) {
        if (!isRunning) return
        scope.launch {
            try {
                val jsonAlerts = alertsAdapter.toJson(alerts) ?: return@launch
                val syncMsg = WifiSyncMessage(
                    type = "ALERTS_DATA",
                    deviceId = deviceId,
                    senderName = getSenderName(),
                    syncGroupId = getSyncGroupId(),
                    payload = jsonAlerts
                )
                val json = messageAdapter.toJson(syncMsg) ?: return@launch
                val bytes = json.toByteArray(StandardCharsets.UTF_8)

                val socketToUse = socket ?: return@launch
                val peers = _discoveredPeers.value

                // Send directly to discovered peer IPs (unicast) for 100% reliability
                peers.forEach { peer ->
                    try {
                        val ip = InetAddress.getByName(peer.ipAddress)
                        val packet = DatagramPacket(bytes, bytes.size, ip, port)
                        socketToUse.send(packet)
                        Log.d("LocalWifiSync", "Unicasted fast sync to ${peer.name} ($peer.ipAddress)")
                    } catch (e: Exception) {
                        Log.e("LocalWifiSync", "Error sending unicast to ${peer.ipAddress}: ${e.message}")
                    }
                }

                // Also send a general broadcast so any newly connected device receives it instantly
                try {
                    val broadcastAddr = getBroadcastAddress()
                    val packet = DatagramPacket(bytes, bytes.size, broadcastAddr, port)
                    socketToUse.send(packet)
                } catch (e: Exception) {
                    try {
                        val packetFallback = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), port)
                        socketToUse.send(packetFallback)
                    } catch (ex: Exception) {
                        Log.e("LocalWifiSync", "Data broadcast failed: ${ex.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalWifiSync", "Failed to broadcast alerts: ${e.message}")
            }
        }
    }

    private suspend fun peerCleanupLoop() {
        while (isRunning) {
            delay(3000)
            val now = System.currentTimeMillis()
            val activePeers = _discoveredPeers.value.filter { now - it.lastSeen < 6000 }
            if (activePeers.size != _discoveredPeers.value.size) {
                _discoveredPeers.value = activePeers
                Log.d("LocalWifiSync", "Cleaned up old peers. Active peers count: ${activePeers.size}")
            }
        }
    }
}
