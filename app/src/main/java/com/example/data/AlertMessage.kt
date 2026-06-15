package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "alert_messages")
data class AlertMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val senderRole: String, // "Worship Leader", "Vocalist", "Keyboardist", "Guitarist", "Bass", "Drummer"
    val targetRole: String, // "Live Stream Operator", "FOH Engineer", "Monitors", "Media Group", "Global"
    val presetType: String, // "Volume/Low", "Volume/High", "Bad Reverb", "Muffled/EQ", "Mic Feedback", "Front Speakers"
    val description: String, // e.g. "Lead Vocal Reverb is too dry"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "NEW", // "NEW", "ACKNOWLEDGED", "SOLVED"
    val isCrucial: Boolean = false,
    val senderDeviceId: String = ""
)
