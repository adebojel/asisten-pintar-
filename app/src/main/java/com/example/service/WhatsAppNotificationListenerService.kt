package com.example.service

import android.app.Notification
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class WhatsAppAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderOrCaller: String,
    val content: String,
    val isCall: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class WhatsAppNotificationListenerService : NotificationListenerService() {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("id", "ID"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                isTtsReady = true
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        // Check if package is WhatsApp or WhatsApp Business
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val category = notification.category

        if (title.isEmpty() && text.isEmpty()) return

        // Detect if this is an incoming call or message
        val isCallCategory = category == Notification.CATEGORY_CALL
        val isCallKeyword = text.contains("panggilan", ignoreCase = true) ||
                text.contains("incoming call", ignoreCase = true) ||
                text.contains("memanggil", ignoreCase = true) ||
                title.contains("panggilan", ignoreCase = true)

        val isCall = isCallCategory || isCallKeyword

        val alert = WhatsAppAlert(
            senderOrCaller = title.ifEmpty { "Seseorang" },
            content = text,
            isCall = isCall
        )

        // Broadcast to UI
        serviceScope.launch {
            _latestAlerts.emit(alert)
            val currentList = _alertHistory.value.toMutableList()
            currentList.add(0, alert)
            if (currentList.size > 50) currentList.removeAt(currentList.lastIndex)
            _alertHistory.value = currentList
        }

        // Voice out if enabled
        if (isCall && isReadCallerNameEnabled.value) {
            speakIndonesian("Panggilan WhatsApp masuk dari $title")
        } else if (!isCall && isReadMessagesEnabled.value) {
            val announcement = if (text.isNotBlank()) {
                "Pesan WhatsApp baru dari $title: $text"
            } else {
                "Pesan WhatsApp baru dari $title"
            }
            speakIndonesian(announcement)
        }
    }

    fun speakIndonesian(textToSpeak: String) {
        if (!isTtsReady || tts == null) {
            initTts()
        }
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "wa_alert_${System.currentTimeMillis()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        var instance: WhatsAppNotificationListenerService? = null
            private set

        private val serviceScope = CoroutineScope(Dispatchers.Main)

        private val _latestAlerts = MutableSharedFlow<WhatsAppAlert>(extraBufferCapacity = 5)
        val latestAlerts: SharedFlow<WhatsAppAlert> = _latestAlerts.asSharedFlow()

        private val _alertHistory = MutableStateFlow<List<WhatsAppAlert>>(emptyList())
        val alertHistory: StateFlow<List<WhatsAppAlert>> = _alertHistory.asStateFlow()

        val isReadMessagesEnabled = MutableStateFlow(true)
        val isReadCallerNameEnabled = MutableStateFlow(true)

        fun isNotificationListenerEnabled(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(packageName)
        }

        fun postSimulatedAlert(alert: WhatsAppAlert) {
            serviceScope.launch {
                val currentList = _alertHistory.value.toMutableList()
                currentList.add(0, alert)
                _alertHistory.value = currentList
                _latestAlerts.emit(alert)
                instance?.let { srv ->
                    if (alert.isCall) {
                        srv.speakIndonesian("Uji coba: Panggilan WhatsApp masuk dari ${alert.senderOrCaller}")
                    } else {
                        srv.speakIndonesian("Uji coba: Pesan WhatsApp dari ${alert.senderOrCaller}: ${alert.content}")
                    }
                }
            }
        }
    }
}
