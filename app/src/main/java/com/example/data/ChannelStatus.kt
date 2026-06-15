package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channel_statuses")
data class ChannelStatus(
    @PrimaryKey val id: Int,
    val name: String,
    val volume: Float,
    val peak: Float,
    val status: String, // "OK", "UNDER_LIMIT" (Low Volume), "OVER_LIMIT" (Clipping), "BAD_EQ" (Muffled/Feedback), "MUTED"
    val assignedRole: String, // e.g. "Main Mic", "Instruments", "Percussion"
    val lastUpdated: Long = System.currentTimeMillis()
)
