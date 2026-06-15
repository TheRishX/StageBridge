package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Alerts ---
    @Query("SELECT * FROM alert_messages ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertMessage>>

    @Query("SELECT * FROM alert_messages WHERE status = 'NEW' ORDER BY timestamp DESC")
    fun getNewAlerts(): Flow<List<AlertMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertMessage)

    @Query("UPDATE alert_messages SET status = :status WHERE id = :id")
    suspend fun updateAlertStatus(id: String, status: String)

    @Query("DELETE FROM alert_messages WHERE id = :id")
    suspend fun deleteAlertById(id: String)

    @Query("DELETE FROM alert_messages")
    suspend fun clearAllAlerts()

    // --- Channel Statuses ---
    @Query("SELECT * FROM channel_statuses ORDER BY id ASC")
    fun getAllChannels(): Flow<List<ChannelStatus>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelStatus>)

    @Query("UPDATE channel_statuses SET volume = :volume, peak = :peak, status = :status, lastUpdated = :lastUpdated WHERE id = :id")
    suspend fun updateChannelVolumeAndStatus(id: Int, volume: Float, peak: Float, status: String, lastUpdated: Long = System.currentTimeMillis())

    @Query("UPDATE channel_statuses SET status = :status, lastUpdated = :lastUpdated WHERE id = :id")
    suspend fun updateChannelStatus(id: Int, status: String, lastUpdated: Long = System.currentTimeMillis())
}
