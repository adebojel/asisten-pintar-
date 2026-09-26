package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.control.DeviceController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssistantForegroundService : Service() {

    private lateinit var deviceController: DeviceController

    override fun onCreate() {
        super.onCreate()
        deviceController = DeviceController(applicationContext)
        createNotificationChannel()
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                _isRunning.value = false
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_TORCH -> {
                deviceController.toggleFlashlight()
                updateNotification()
            }
            ACTION_TOGGLE_SILENT -> {
                val current = deviceController.deviceStatus.value.ringerMode
                val next = if (current == android.media.AudioManager.RINGER_MODE_SILENT) {
                    android.media.AudioManager.RINGER_MODE_NORMAL
                } else {
                    android.media.AudioManager.RINGER_MODE_SILENT
                }
                deviceController.setRingerMode(next)
                updateNotification()
            }
            ACTION_OPEN_PANEL -> {
                deviceController.openInternetPanel()
            }
            else -> {
                val notification = buildForegroundNotification()
                startForeground(NOTIFICATION_ID, notification)
            }
        }

        return START_STICKY
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildForegroundNotification())
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Senter
        val torchIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_TOGGLE_TORCH
        }
        val torchPendingIntent = PendingIntent.getService(
            this,
            1,
            torchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Suara
        val silentIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_TOGGLE_SILENT
        }
        val silentPendingIntent = PendingIntent.getService(
            this,
            2,
            silentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Panel Internet
        val panelIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_OPEN_PANEL
        }
        val panelPendingIntent = PendingIntent.getService(
            this,
            3,
            panelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val st = deviceController.deviceStatus.value
        val subtitle = "Wi-Fi: ${if (st.isWifiEnabled) "ON" else "OFF"} | Senter: ${if (st.isTorchOn) "ON" else "OFF"} | Suara: ${if (st.ringerMode == android.media.AudioManager.RINGER_MODE_SILENT) "Senyap" else "Normal"}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Asisten Kontrol Aktif (Latar Belakang)")
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_launcher_foreground, "Senter", torchPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Suara", silentPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Internet", panelPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Layanan Asisten Latar Belakang",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga asisten offline dan pembaca pesan WhatsApp tetap siaga di latar belakang."
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "assistant_foreground_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_STOP = "com.example.service.action.STOP"
        const val ACTION_TOGGLE_TORCH = "com.example.service.action.TOGGLE_TORCH"
        const val ACTION_TOGGLE_SILENT = "com.example.service.action.TOGGLE_SILENT"
        const val ACTION_OPEN_PANEL = "com.example.service.action.OPEN_PANEL"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _isRunning.value = true
        }

        fun stop(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
            _isRunning.value = false
        }
    }
}
