package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AssistantApp
import com.example.control.AppItem
import com.example.control.AppManager
import com.example.control.DeviceController
import com.example.control.DeviceStatus
import com.example.control.NetworkStateMonitor
import com.example.control.NetworkStatus
import com.example.data.database.entity.AutomationRoutineEntity
import com.example.data.database.entity.CommandLogEntity
import com.example.parser.CommandResult
import com.example.parser.OfflineCommandEngine
import com.example.service.AssistantForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val actionType: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AssistantApp).repository
    val deviceController = DeviceController(application)
    val appManager = AppManager(application)
    val wakeWordManager = com.example.parser.WakeWordManager(application)
    private val networkMonitor = NetworkStateMonitor(application)
    private val commandEngine = OfflineCommandEngine(deviceController, appManager, wakeWordManager)

    val wakeName: StateFlow<String> = wakeWordManager.wakeName
    val isWakeDetectionEnabled: StateFlow<Boolean> = wakeWordManager.isWakeDetectionEnabled

    val voiceActivationManager = com.example.control.VoiceActivationManager(
        context = application,
        onResult = { spokenText ->
            submitCommand(spokenText)
        },
        onError = { _ -> }
    )
    val isVoiceListening: StateFlow<Boolean> = voiceActivationManager.isListening

    fun setWakeName(name: String) {
        wakeWordManager.setWakeName(name)
        val msg = "Nama panggilan diubah menjadi \"$name\". Panggil asisten dengan \"Halo $name\"."
        speak(msg)
    }

    fun setWakeDetectionEnabled(enabled: Boolean) {
        wakeWordManager.setWakeDetectionEnabled(enabled)
    }

    fun startVoiceListening() {
        voiceActivationManager.startListening()
    }

    fun stopVoiceListening() {
        voiceActivationManager.stopListening()
    }

    fun openVoiceInputSettings() {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                getApplication<Application>().startActivity(fallback)
            } catch (_: Exception) {
                val sysIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                getApplication<Application>().startActivity(sysIntent)
            }
        }
    }

    val deviceStatus: StateFlow<DeviceStatus> = deviceController.deviceStatus

    val networkStatus: StateFlow<NetworkStatus> = networkMonitor.networkStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkStatus.Unavailable
    )

    val logs: StateFlow<List<CommandLogEntity>> = repository.logs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val routines: StateFlow<List<AutomationRoutineEntity>> = repository.routines.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    val isForegroundServiceRunning: StateFlow<Boolean> = AssistantForegroundService.isRunning

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "Halo! Saya Asisten Offline Kendali HP Anda. Anda dapat mengetik atau menyuarakan perintah seperti \"Buka WhatsApp\", \"Buka YouTube\", \"Matikan Wi-Fi\", \"Matikan Data Seluler\", \"Nyalakan Senter\", atau \"Mode Senyap\"."
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        initTts(application)
        loadInstalledApps()
        startBackgroundService()
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appManager.getInstalledApps()
            _installedApps.value = apps
        }
    }

    fun launchApp(appItem: AppItem) {
        val success = appManager.launchAppByPackage(appItem.packageName)
        if (success) {
            postDirectAction("Buka Aplikasi", "Membuka ${appItem.name}...", "APP_LAUNCH")
        }
    }

    fun startBackgroundService() {
        try {
            AssistantForegroundService.start(getApplication())
        } catch (_: Exception) {}
    }

    fun stopBackgroundService() {
        try {
            AssistantForegroundService.stop(getApplication())
        } catch (_: Exception) {}
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(getApplication<Application>().packageName) == true
        } else {
            true
        }
    }

    fun openBatteryOptimizationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun initTts(app: Application) {
        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("id", "ID"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                isTtsReady = true
            }
        }
    }

    fun toggleTts() {
        _isTtsEnabled.value = !_isTtsEnabled.value
        if (!_isTtsEnabled.value) {
            tts?.stop()
        }
    }

    private fun speak(text: String) {
        if (_isTtsEnabled.value && isTtsReady) {
            // Strip bullet points or technical markup for clean voice
            val cleanText = text.replace("•", "").replace("\n", ". ")
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "assistant_speech")
        }
    }

    fun submitCommand(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(isUser = true, text = trimmed)
        _messages.value = _messages.value + userMsg

        val result = commandEngine.executeCommand(trimmed)
        val (responseMsg, actionType, isSuccess) = when (result) {
            is CommandResult.Success -> Triple(result.message, result.actionType, true)
            is CommandResult.ActionPrompt -> Triple(result.message, result.actionType, true)
            is CommandResult.Unknown -> Triple(result.message, "UNKNOWN", false)
        }

        val botMsg = ChatMessage(
            isUser = false,
            text = responseMsg,
            actionType = actionType
        )
        _messages.value = _messages.value + botMsg

        speak(responseMsg)
        deviceController.refreshStatus()

        viewModelScope.launch {
            repository.logCommand(
                query = trimmed,
                response = responseMsg,
                actionType = actionType,
                isSuccess = isSuccess
            )
        }
    }

    // Direct Controls
    fun onToggleWifi(enable: Boolean) {
        val feedback = deviceController.toggleWifi(enable)
        postDirectAction("Ubah Wi-Fi (${if (enable) "Nyalakan" else "Matikan"})", feedback, "WIFI_TOGGLE")
    }

    fun onOpenInternetPanel() {
        deviceController.openInternetPanel()
        postDirectAction("Panel Internet", "Membuka Panel Koneksi Internet untuk mengatur Wi-Fi dan Data Seluler.", "INTERNET_PANEL")
    }

    fun onOpenWifiSettings() {
        deviceController.openWifiSettings()
    }

    fun onOpenMobileDataSettings() {
        deviceController.openMobileDataSettings()
    }

    fun onToggleFlashlight() {
        val (ok, feedback) = deviceController.toggleFlashlight()
        postDirectAction("Senter", feedback, if (ok) "FLASHLIGHT_TOGGLE" else "FLASHLIGHT_ERROR")
    }

    fun onSetRingerMode(mode: Int) {
        val (_, feedback) = deviceController.setRingerMode(mode)
        postDirectAction("Mode Suara", feedback, "SOUND_CHANGE")
    }

    fun onLockScreen() {
        val (_, feedback) = deviceController.lockScreen()
        postDirectAction("Kunci Layar", feedback, "LOCK_SCREEN")
    }

    fun onOpenQuickSettings() {
        val (_, feedback) = deviceController.openQuickSettingsPanel()
        postDirectAction("Setelan Cepat", feedback, "QUICK_SETTINGS")
    }

    fun onTakeScreenshot() {
        val (_, feedback) = deviceController.takeScreenshot()
        postDirectAction("Tangkapan Layar", feedback, "SCREENSHOT")
    }

    fun onOpenBluetooth() = deviceController.openBluetoothSettings()
    fun onOpenBatterySaver() = deviceController.openBatterySaverSettings()
    fun onOpenAirplaneMode() = deviceController.openAirplaneModeSettings()
    fun onOpenHotspot() = deviceController.openHotspotSettings()
    fun onOpenAccessibilitySettings() = deviceController.openAccessibilitySettings()
    fun onOpenDndSettings() = deviceController.openDndSettings()
    fun onRefreshStatus() = deviceController.refreshStatus()

    fun executeRoutine(routine: AutomationRoutineEntity) {
        // Execute actions defined in routine
        if (routine.wifiAction == "ON") {
            deviceController.toggleWifi(true)
        } else if (routine.wifiAction == "OFF") {
            deviceController.toggleWifi(false)
        }

        if (routine.mobileDataAction == "OFF_PANEL" || routine.mobileDataAction == "ON_PANEL") {
            deviceController.openInternetPanel()
        }

        when (routine.soundModeAction) {
            "SILENT" -> deviceController.setRingerMode(AudioManager.RINGER_MODE_SILENT)
            "VIBRATE" -> deviceController.setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            "NORMAL" -> deviceController.setRingerMode(AudioManager.RINGER_MODE_NORMAL)
        }

        if (routine.flashlightAction == "ON") {
            deviceController.toggleFlashlight(true)
        } else if (routine.flashlightAction == "OFF") {
            deviceController.toggleFlashlight(false)
        }

        val feedback = "Rutinitas \"${routine.name}\" berhasil dijalankan."
        postDirectAction("Rutinitas: ${routine.name}", feedback, "ROUTINE_EXEC")
    }

    fun createCustomRoutine(
        name: String,
        desc: String,
        wifi: String,
        mobileData: String,
        sound: String,
        flashlight: String
    ) {
        viewModelScope.launch {
            repository.insertRoutine(
                AutomationRoutineEntity(
                    name = name,
                    description = desc,
                    iconKey = "custom",
                    wifiAction = wifi,
                    mobileDataAction = mobileData,
                    soundModeAction = sound,
                    flashlightAction = flashlight,
                    isEnabled = true
                )
            )
            val feedback = "Rutinitas baru \"$name\" berhasil disimpan."
            postDirectAction("Tambah Rutinitas", feedback, "ROUTINE_ADD")
        }
    }

    fun deleteRoutine(routine: AutomationRoutineEntity) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
        }
    }

    fun clearCommandLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // WhatsApp Voice Announcement Integration
    val whatsappAlerts: StateFlow<List<com.example.service.WhatsAppAlert>> =
        com.example.service.WhatsAppNotificationListenerService.alertHistory

    val isReadWhatsAppMessages: StateFlow<Boolean> =
        com.example.service.WhatsAppNotificationListenerService.isReadMessagesEnabled

    val isReadWhatsAppCallers: StateFlow<Boolean> =
        com.example.service.WhatsAppNotificationListenerService.isReadCallerNameEnabled

    fun setReadWhatsAppMessages(enabled: Boolean) {
        com.example.service.WhatsAppNotificationListenerService.isReadMessagesEnabled.value = enabled
    }

    fun setReadWhatsAppCallers(enabled: Boolean) {
        com.example.service.WhatsAppNotificationListenerService.isReadCallerNameEnabled.value = enabled
    }

    fun isNotificationListenerGranted(): Boolean {
        return com.example.service.WhatsAppNotificationListenerService.isNotificationListenerEnabled(getApplication())
    }

    fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            // Ignored
        }
    }

    fun testSimulateWhatsAppMessage(sender: String = "Budi Santoso", message: String = "Halo, apakah kamu besok ada waktu?") {
        val alert = com.example.service.WhatsAppAlert(
            senderOrCaller = sender,
            content = message,
            isCall = false
        )
        com.example.service.WhatsAppNotificationListenerService.postSimulatedAlert(alert)
        speak("Uji coba: Pesan WhatsApp dari $sender: $message")
    }

    fun testSimulateWhatsAppCall(caller: String = "Siti Rahmawati") {
        val alert = com.example.service.WhatsAppAlert(
            senderOrCaller = caller,
            content = "Panggilan suara WhatsApp masuk",
            isCall = true
        )
        com.example.service.WhatsAppNotificationListenerService.postSimulatedAlert(alert)
        speak("Uji coba: Panggilan WhatsApp masuk dari $caller")
    }

    fun speakCustomText(text: String) {
        speak(text)
    }

    private fun postDirectAction(actionLabel: String, feedback: String, actionType: String) {
        deviceController.refreshStatus()
        speak(feedback)
        viewModelScope.launch {
            repository.logCommand(
                query = actionLabel,
                response = feedback,
                actionType = actionType,
                isSuccess = true
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
