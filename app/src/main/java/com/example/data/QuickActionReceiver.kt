package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        Log.d("QuickActionReceiver", "Received action slot: $action")

        val (presetType, description) = when (action) {
            "com.example.stagebridge.ACTION_QUICK_LOW_VOLUME" -> {
                Pair("Volume/Low", "Volume is too low on main acoustic git & monitors")
            }
            "com.example.stagebridge.ACTION_QUICK_REVERB" -> {
                Pair("Bad Reverb", "Reverb is too muddy on vocals, please clean up EQ")
            }
            "com.example.stagebridge.ACTION_QUICK_FEEDBACK" -> {
                Pair("Mic Feedback", "WARNING: High frequency mic feedback on stage monitors!")
            }
            "com.example.stagebridge.ACTION_QUICK_FRONT_SPEAKERS" -> {
                Pair("Front Speakers", "Front-of-house main PA array sounding unbalanced")
            }
            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val alert = AlertMessage(
                    senderName = "Quick Tray",
                    senderRole = "Worship Leader",
                    targetRole = "Global",
                    presetType = presetType,
                    description = description,
                    isCrucial = true,
                    status = "NEW"
                )
                db.appDao().insertAlert(alert)
                Log.d("QuickActionReceiver", "Successfully saved quick action alert to local room DB")
            } catch (e: Exception) {
                Log.e("QuickActionReceiver", "Failed to write quick alert to database: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
