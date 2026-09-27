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
import com.example.data.database.entity.FavoriteAppEntity
import com.example.parser.CommandResult
import com.example.parser.OfflineCommandEngine
import android.speech.tts.UtteranceProgressListener
import com.example.control.AssistantCoordinator
import com.example.service.AssistantForegroundService
import com.example.service.FloatingAssistantService
import com.example.update.AppUpdateManager
import com.example.update.UpdateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val actionType: String? = null,
    val followUpQuestion: String? = null,
    val suggestions: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

private data class CommandExecInfo(
    val message: String,
    val actionType: String,
    val isSuccess: Boolean,
    val followUp: String,
    val suggestions: List<String>
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
        onSpeechStarted = {
            stopSpeaking()
        },
        onResult = { spokenText ->
            stopSpeaking()
            submitCommand(spokenText)
        },
        onError = { _ -> }
    )
    val isVoiceListening: StateFlow<Boolean> = voiceActivationManager.isListening
    val recognizedVoiceText: StateFlow<String> = voiceActivationManager.lastRecognizedText

    private val assistantPrefs = application.getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)
    private val _isAutoMicOnLaunchEnabled = MutableStateFlow(
        assistantPrefs.getBoolean("auto_mic_on_launch", true)
    )
    val isAutoMicOnLaunchEnabled: StateFlow<Boolean> = _isAutoMicOnLaunchEnabled.asStateFlow()

    private val _currentSuggestions = MutableStateFlow<List<String>>(
        listOf("Gulir ke Bawah", "Buka YouTube", "Buka WhatsApp", "Nyalakan Senter")
    )
    val currentSuggestions: StateFlow<List<String>> = _currentSuggestions.asStateFlow()

    private val _currentFollowUpQuestion = MutableStateFlow<String>(
        "Langkah selanjutnya apa yang ingin Anda lakukan?"
    )
    val currentFollowUpQuestion: StateFlow<String> = _currentFollowUpQuestion.asStateFlow()

    private val _isFloatingOverlayEnabled = MutableStateFlow(
        assistantPrefs.getBoolean("floating_overlay_enabled", false)
    )
    val isFloatingOverlayEnabled: StateFlow<Boolean> = _isFloatingOverlayEnabled.asStateFlow()
    val isFloatingServiceRunning: StateFlow<Boolean> = AssistantCoordinator.isFloatingServiceRunning

    fun canDrawOverlays(): Boolean {
        return FloatingAssistantService.isOverlayPermissionGranted(getApplication())
    }

    fun openOverlayPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${getApplication<Application>().packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                getApplication<Application>().startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun setFloatingOverlayEnabled(enabled: Boolean) {
        _isFloatingOverlayEnabled.value = enabled
        assistantPrefs.edit().putBoolean("floating_overlay_enabled", enabled).apply()
        val context = getApplication<Application>()
        val intent = Intent(context, FloatingAssistantService::class.java)
        if (enabled) {
            if (canDrawOverlays()) {
                context.startService(intent)
            } else {
                openOverlayPermissionSettings()
            }
        } else {
            context.stopService(intent)
        }
    }

    val appUpdateManager = AppUpdateManager(application)
    val updateStatus: StateFlow<UpdateStatus> = appUpdateManager.updateStatus
    val customApkUrl: StateFlow<String> = appUpdateManager.customApkUrl
    val isAutoCheckUpdateEnabled: StateFlow<Boolean> = appUpdateManager.isAutoCheckEnabled
    val currentVersionName: String = appUpdateManager.currentVersionName
    val currentVersionCode: Long = appUpdateManager.currentVersionCode

    fun checkForUpdates(onFinished: ((UpdateStatus) -> Unit)? = null) {
        appUpdateManager.checkForUpdates(viewModelScope, onFinished)
    }

    fun downloadAndInstallApk(url: String? = null) {
        val downloadUrl = if (!url.isNullOrBlank()) url
        else if (appUpdateManager.customApkUrl.value.isNotBlank()) appUpdateManager.customApkUrl.value
        else appUpdateManager.studioSharedUrl

        appUpdateManager.downloadAndInstallApk(downloadUrl, viewModelScope)
    }

    fun setCustomApkUrl(url: String) {
        appUpdateManager.setCustomApkUrl(url)
    }

    fun setAutoCheckUpdateEnabled(enabled: Boolean) {
        appUpdateManager.setAutoCheckEnabled(enabled)
    }

    fun openStudioLive(useSharedUrl: Boolean = false) {
        appUpdateManager.openStudioInBrowser(useSharedUrl)
    }

    fun openInstallPermissionSettings() {
        appUpdateManager.openInstallPermissionSettings()
    }

    fun canRequestPackageInstalls(): Boolean {
        return appUpdateManager.canRequestPackageInstalls()
    }

    fun resetUpdateStatus() {
        appUpdateManager.resetStatus()
    }

    fun setAutoMicOnLaunchEnabled(enabled: Boolean) {
        _isAutoMicOnLaunchEnabled.value = enabled
        assistantPrefs.edit().putBoolean("auto_mic_on_launch", enabled).apply()
        val msg = if (enabled) {
            "Mikrofon otomatis saat aplikasi dibuka diaktifkan."
        } else {
            "Mikrofon otomatis saat aplikasi dibuka dinonaktifkan."
        }
        postDirectAction("Pengaturan Mic", msg, "AUTO_MIC_SETTING")
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun setWakeName(name: String) {
        wakeWordManager.setWakeName(name)
        val msg = "Nama panggilan diubah menjadi \"$name\". Panggil asisten dengan \"Halo $name\"."
        speak(msg)
    }

    fun setWakeDetectionEnabled(enabled: Boolean) {
        wakeWordManager.setWakeDetectionEnabled(enabled)
    }

    fun startVoiceListening() {
        stopSpeaking()
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

    val favoriteApps: StateFlow<List<FavoriteAppEntity>> = repository.favoriteApps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteAppItems: StateFlow<List<AppItem>> = combine(
        repository.favoriteApps,
        _installedApps
    ) { favorites, installed ->
        val installedMap = installed.associateBy { it.packageName }
        favorites.map { fav ->
            installedMap[fav.packageName] ?: AppItem(
                name = fav.name,
                packageName = fav.packageName,
                icon = null
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
    private var onSpeechDoneCallback: (() -> Unit)? = null

    init {
        initTts(application)
        loadInstalledApps()
        startBackgroundService()
        if (appUpdateManager.isAutoCheckEnabled.value) {
            checkForUpdates()
        }

        // Connect AssistantCoordinator for Floating Overlay
        AssistantCoordinator.onFloatingMicClicked = {
            if (isVoiceListening.value) {
                stopVoiceListening()
            } else {
                startVoiceListening()
            }
        }
        AssistantCoordinator.onFloatingActionSubmitted = { actionCmd ->
            submitCommand(actionCmd)
        }

        // Sync listening state to Floating Overlay
        viewModelScope.launch {
            isVoiceListening.collect { listening ->
                AssistantCoordinator.setListening(listening)
            }
        }

        // Start floating overlay if previously enabled
        if (_isFloatingOverlayEnabled.value && canDrawOverlays()) {
            try {
                val intent = Intent(application, FloatingAssistantService::class.java)
                application.startService(intent)
            } catch (_: Exception) {}
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appManager.getInstalledApps()
            _installedApps.value = apps
            repository.ensureDefaultFavorites(apps)
        }
    }

    fun toggleFavorite(appItem: AppItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val isNowFav = repository.toggleFavorite(appItem.packageName, appItem.name)
            val msg = if (isNowFav) {
                "${appItem.name} ditambahkan ke Akses Cepat"
            } else {
                "${appItem.name} dihapus dari Akses Cepat"
            }
            postDirectAction("Akses Cepat", msg, "FAVORITE_TOGGLE")
        }
    }

    fun removeFavorite(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeFavorite(packageName)
        }
    }

    fun updateFavorites(selectedApps: List<AppItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setFavorites(selectedApps)
            postDirectAction("Akses Cepat", "${selectedApps.size} aplikasi disimpan ke Akses Cepat", "FAVORITE_SAVED")
        }
    }

    fun resetDefaultFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultFavorites(_installedApps.value)
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

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        AssistantCoordinator.setListening(false)
                    }

                    override fun onDone(utteranceId: String?) {
                        viewModelScope.launch(Dispatchers.Main) {
                            val cb = onSpeechDoneCallback
                            onSpeechDoneCallback = null
                            cb?.invoke()
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        viewModelScope.launch(Dispatchers.Main) {
                            val cb = onSpeechDoneCallback
                            onSpeechDoneCallback = null
                            cb?.invoke()
                        }
                    }
                })
            }
        }
    }

    fun toggleTts() {
        _isTtsEnabled.value = !_isTtsEnabled.value
        if (!_isTtsEnabled.value) {
            tts?.stop()
        }
    }

    private fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (_isTtsEnabled.value && isTtsReady) {
            onSpeechDoneCallback = onDone
            val cleanText = text.replace("•", "").replace("\n", ". ")
            val uId = "speech_${System.currentTimeMillis()}"
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, uId)
        } else {
            viewModelScope.launch(Dispatchers.Main) {
                kotlinx.coroutines.delay(600)
                onDone?.invoke()
            }
        }
    }

    fun submitCommand(input: String) {
        stopSpeaking()
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(isUser = true, text = trimmed)
        _messages.value = _messages.value + userMsg

        val result = commandEngine.executeCommand(trimmed)
        val execInfo = when (result) {
            is CommandResult.Success -> CommandExecInfo(
                result.message,
                result.actionType,
                true,
                result.followUpQuestion,
                result.suggestions
            )
            is CommandResult.ActionPrompt -> CommandExecInfo(
                result.message,
                result.actionType,
                true,
                result.followUpQuestion,
                result.suggestions
            )
            is CommandResult.Unknown -> CommandExecInfo(
                result.message,
                "UNKNOWN",
                false,
                "Perintah tidak dikenali. Pilih salah satu:",
                result.suggestions
            )
        }

        _currentFollowUpQuestion.value = execInfo.followUp
        _currentSuggestions.value = execInfo.suggestions
        AssistantCoordinator.updateStatus(execInfo.message, execInfo.followUp, execInfo.suggestions)

        val botMsg = ChatMessage(
            isUser = false,
            text = execInfo.message,
            actionType = execInfo.actionType,
            followUpQuestion = execInfo.followUp,
            suggestions = execInfo.suggestions
        )
        _messages.value = _messages.value + botMsg

        val fullSpeech = if (execInfo.followUp.isNotBlank() && execInfo.actionType != "STOP_SPEECH" && execInfo.actionType != "LOCK_SCREEN") {
            "${execInfo.message}. ${execInfo.followUp}"
        } else {
            execInfo.message
        }

        if (execInfo.actionType == "STOP_SPEECH") {
            stopSpeaking()
        } else if (execInfo.actionType == "APP_UPDATE") {
            speak(fullSpeech) {
                if (execInfo.actionType != "LOCK_SCREEN") {
                    startVoiceListening()
                }
            }
            checkForUpdates()
        } else {
            // Speak confirmation & question, then immediately re-activate listening!
            speak(fullSpeech) {
                if (execInfo.actionType != "LOCK_SCREEN") {
                    startVoiceListening()
                }
            }
        }
        deviceController.refreshStatus()

        viewModelScope.launch {
            repository.logCommand(
                query = trimmed,
                response = execInfo.message,
                actionType = execInfo.actionType,
                isSuccess = execInfo.isSuccess
            )
        }
    }

    // Direct Controls
    fun onToggleWifi(enable: Boolean) {
        val feedback = deviceController.toggleWifi(enable)
        postDirectAction("Ubah Wi-Fi (${if (enable) "Nyalakan" else "Matikan"})", feedback, "WIFI_TOGGLE")
    }

    fun onToggleBluetooth(enable: Boolean) {
        val feedback = deviceController.toggleBluetooth(enable)
        postDirectAction("Ubah Bluetooth (${if (enable) "Nyalakan" else "Matikan"})", feedback, "BLUETOOTH_TOGGLE")
    }

    fun onToggleMobileData(enable: Boolean) {
        val feedback = deviceController.handleMobileDataAction(enable)
        postDirectAction("Ubah Data Seluler (${if (enable) "Nyalakan" else "Matikan"})", feedback, "MOBILE_DATA_TOGGLE")
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
