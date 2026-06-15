package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(private val appDao: AppDao) {

    val allAlerts: Flow<List<AlertMessage>> = appDao.getAllAlerts()
    val newAlerts: Flow<List<AlertMessage>> = appDao.getNewAlerts()
    val allChannels: Flow<List<ChannelStatus>> = appDao.getAllChannels()

    suspend fun sendAlert(alert: AlertMessage) {
        appDao.insertAlert(alert)
    }

    suspend fun updateAlertStatus(id: String, status: String) {
        appDao.updateAlertStatus(id, status)
    }

    suspend fun deleteAlertById(id: String) {
        appDao.deleteAlertById(id)
    }

    suspend fun clearAlerts() {
        appDao.clearAllAlerts()
    }

    suspend fun updateChannelVolume(id: Int, volume: Float, peak: Float, status: String) {
        appDao.updateChannelVolumeAndStatus(id, volume, peak, status)
    }

    suspend fun updateChannelStatus(id: Int, status: String) {
        appDao.updateChannelStatus(id, status)
    }

    suspend fun checkAndPopulateDefaultChannels() {
        // Query the first list of channels to check if empty
        val currentChannels = appDao.getAllChannels().first()
        if (currentChannels.isEmpty()) {
            val defaults = listOf(
                ChannelStatus(1, "Lead Vocal", 0.82f, 0.85f, "OK", "Main Beta58 Vocal mic"),
                ChannelStatus(2, "Backing Vocal", 0.70f, 0.74f, "OK", "Alt vocal SM58 mic"),
                ChannelStatus(3, "Acoustic Git", 0.65f, 0.68f, "OK", "DI Box with tone preamp"),
                ChannelStatus(4, "Electric Git", 0.55f, 0.59f, "OK", "Kemper Stereo Output"),
                ChannelStatus(5, "Nord Stage 4", 0.78f, 0.84f, "OK", "Stereo DI Box lines"),
                ChannelStatus(6, "Bass Guitar", 0.60f, 0.64f, "OK", "Sub-bass active DI"),
                ChannelStatus(7, "Drum Set", 0.80f, 0.92f, "OK", "Overheads, Snare & Kick sub-mix"),
                ChannelStatus(8, "Pulpit Lapel", 0.00f, 0.00f, "MUTED", "Speaker Sennheiser clip")
            )
            appDao.insertChannels(defaults)
        }
    }
}
