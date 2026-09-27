package com.example.control

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.example.service.AssistantAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceStatus(
    val isWifiEnabled: Boolean = false,
    val isWifiConnected: Boolean = false,
    val isMobileDataConnected: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isTorchOn: Boolean = false,
    val ringerMode: Int = AudioManager.RINGER_MODE_NORMAL, // 0=SILENT, 1=VIBRATE, 2=NORMAL
    val batteryPercent: Int = 100,
    val isBatterySaverOn: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isDndAccessGranted: Boolean = false,
    val activeNetworkName: String = "Offline"
)

class DeviceController(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter ?: @Suppress("DEPRECATION") android.bluetooth.BluetoothAdapter.getDefaultAdapter()

    private val _deviceStatus = MutableStateFlow(DeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    private var rearCameraId: String? = null

    init {
        initCamera()
        refreshStatus()
    }

    private fun initCamera() {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    rearCameraId = id
                    return@forEach
                }
            }
        } catch (_: Exception) {
            // Ignore camera discovery failure
        }
    }

    fun refreshStatus() {
        val isWifiOn = try {
            wifiManager?.isWifiEnabled == true
        } catch (_: Exception) {
            false
        }

        var isWifiConn = false
        var isCellularConn = false
        var activeNet = "Tidak Ada Koneksi"

        try {
            val activeNetwork = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    isWifiConn = true
                    activeNet = "Wi-Fi Terhubung"
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    isCellularConn = true
                    activeNet = "Data Seluler Aktif"
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    activeNet = "Ethernet"
                }
            }
        } catch (_: Exception) {
            // Fallback
        }

        val ringer = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        val isSaver = powerManager?.isPowerSaveMode == true
        val isAcc = AssistantAccessibilityService.isAccessibilityServiceEnabled(context)
        val isDnd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }

        val batteryPercent = try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        } catch (_: Exception) {
            100
        }

        val isBtOn = try {
            bluetoothAdapter?.isEnabled == true
        } catch (_: Exception) {
            false
        }

        _deviceStatus.value = _deviceStatus.value.copy(
            isWifiEnabled = isWifiOn,
            isWifiConnected = isWifiConn,
            isMobileDataConnected = isCellularConn,
            isBluetoothEnabled = isBtOn,
            ringerMode = ringer,
            batteryPercent = batteryPercent,
            isBatterySaverOn = isSaver,
            isAccessibilityEnabled = isAcc,
            isDndAccessGranted = isDnd,
            activeNetworkName = activeNet
        )
    }

    /**
     * Wi-Fi handling:
     * On Android 9 and lower: programmatic setWifiEnabled
     * On Android 10+ (Q): Open system connectivity panel or Wi-Fi settings
     */
    fun toggleWifi(enable: Boolean): String {
        vibrate()
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            val success = wifiManager?.setWifiEnabled(enable) == true
            refreshStatus()
            if (success) {
                if (enable) "Wi-Fi berhasil dinyalakan." else "Wi-Fi berhasil dimatikan."
            } else {
                openInternetPanel()
                "Membuka panel pengaturan Wi-Fi..."
            }
        } else {
            // Android 10+ standard policy
            openInternetPanel()
            if (enable) {
                "Membuka panel koneksi untuk menyalakan Wi-Fi."
            } else {
                "Membuka panel koneksi untuk mematikan Wi-Fi."
            }
        }
    }

    fun openInternetPanel() {
        vibrate()
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            openWifiSettings()
        }
    }

    fun openWifiSettings() {
        vibrate()
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignored
        }
    }

    /**
     * Mobile Data Handling:
     * Android secures Mobile Data toggle behind system permission.
     * The official and fastest method on Android is opening the Internet Connectivity Panel
     * or Mobile Network Settings.
     */
    fun handleMobileDataAction(enable: Boolean): String {
        vibrate()
        openInternetPanel()
        return if (enable) {
            "Membuka panel internet untuk menyalakan Data Seluler."
        } else {
            "Membuka panel internet untuk menonaktifkan Data Seluler."
        }
    }

    fun openMobileDataSettings() {
        vibrate()
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (_: Exception) {
                openInternetPanel()
            }
        }
    }

    /**
     * Senter (Flashlight / Torch)
     */
    fun toggleFlashlight(forceState: Boolean? = null): Pair<Boolean, String> {
        vibrate()
        val targetState = forceState ?: !_deviceStatus.value.isTorchOn
        val camId = rearCameraId
        if (camId == null) {
            return Pair(false, "Kamera dengan flash tidak ditemukan pada perangkat ini.")
        }
        return try {
            cameraManager?.setTorchMode(camId, targetState)
            _deviceStatus.value = _deviceStatus.value.copy(isTorchOn = targetState)
            Pair(
                true,
                if (targetState) "Senter berhasil dinyalakan." else "Senter berhasil dimatikan."
            )
        } catch (e: CameraAccessException) {
            Pair(false, "Gagal mengakses senter: ${e.localizedMessage}")
        } catch (e: Exception) {
            Pair(false, "Terjadi kendala saat menyalakan senter.")
        }
    }

    /**
     * Suara & Mode DND
     */
    fun setRingerMode(mode: Int): Pair<Boolean, String> {
        vibrate()
        val am = audioManager ?: return Pair(false, "Audio Manager tidak tersedia.")
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (mode == AudioManager.RINGER_MODE_SILENT && notificationManager?.isNotificationPolicyAccessGranted == false) {
                    openDndSettings()
                    return Pair(
                        false,
                        "Izin Akses 'Jangan Ganggu' (DND) diperlukan untuk mode senyap. Membuka pengaturan..."
                    )
                }
            }
            am.ringerMode = mode
            _deviceStatus.value = _deviceStatus.value.copy(ringerMode = mode)
            val desc = when (mode) {
                AudioManager.RINGER_MODE_SILENT -> "Mode senyap diaktifkan."
                AudioManager.RINGER_MODE_VIBRATE -> "Mode getar diaktifkan."
                else -> "Mode suara normal diaktifkan."
            }
            Pair(true, desc)
        } catch (e: SecurityException) {
            openDndSettings()
            Pair(false, "Izin ubah kebijakan suara diperlukan. Silakan izinkan akses.")
        } catch (_: Exception) {
            Pair(false, "Gagal mengatur mode suara.")
        }
    }

    fun openDndSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // Ignored
            }
        }
    }

    fun openAccessibilitySettings() {
        vibrate()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignored
        }
    }

    fun toggleBluetooth(enable: Boolean): String {
        vibrate()
        return try {
            if (bluetoothAdapter == null) {
                "Bluetooth tidak didukung pada perangkat ini."
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    @Suppress("DEPRECATION")
                    val success = if (enable) bluetoothAdapter.enable() else bluetoothAdapter.disable()
                    refreshStatus()
                    if (success) {
                        if (enable) "Bluetooth berhasil diaktifkan." else "Bluetooth berhasil dimatikan."
                    } else {
                        openBluetoothSettings()
                        "Membuka setelan Bluetooth..."
                    }
                } else {
                    // On Android 12+, direct enable/disable requires BLUETOOTH_CONNECT or system intent
                    openBluetoothSettings()
                    if (enable) {
                        "Membuka setelan untuk mengaktifkan Bluetooth."
                    } else {
                        "Membuka setelan untuk mematikan Bluetooth."
                    }
                }
            }
        } catch (e: SecurityException) {
            openBluetoothSettings()
            "Izin Bluetooth diperlukan. Membuka pengaturan Bluetooth..."
        } catch (e: Exception) {
            openBluetoothSettings()
            "Membuka setelan Bluetooth..."
        }
    }

    fun openBluetoothSettings() {
        vibrate()
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignored
        }
    }

    fun openBatterySaverSettings() {
        vibrate()
        val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignored
        }
    }

    fun openAirplaneModeSettings() {
        vibrate()
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignored
        }
    }

    fun openHotspotSettings() {
        vibrate()
        val intent = Intent("android.settings.TETHER_SETTINGS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (_: Exception) {
                // Ignored
            }
        }
    }

    // Accessibility Global Actions
    fun lockScreen(): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            if (success) {
                Pair(true, "Layar ponsel berhasil dikunci.")
            } else {
                Pair(false, "Gagal mengunci layar melalui Layanan Aksesibilitas.")
            }
        } else {
            openAccessibilitySettings()
            Pair(
                false,
                "Aktifkan 'Layanan Asisten Kendali HP' di Pengaturan Aksesibilitas untuk mengunci layar."
            )
        }
    }

    fun openQuickSettingsPanel(): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null) {
            val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            if (success) {
                Pair(true, "Membuka Panel Setelan Cepat HP.")
            } else {
                Pair(false, "Gagal membuka panel setelan cepat.")
            }
        } else {
            openAccessibilitySettings()
            Pair(
                false,
                "Aktifkan 'Layanan Asisten Kendali HP' di Aksesibilitas untuk membuka panel sistem."
            )
        }
    }

    fun takeScreenshot(): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            if (success) {
                Pair(true, "Tangkapan layar sedang diambil...")
            } else {
                Pair(false, "Gagal mengambil tangkapan layar.")
            }
        } else {
            openAccessibilitySettings()
            Pair(
                false,
                "Aktifkan 'Layanan Asisten Kendali HP' di Aksesibilitas untuk fitur tangkapan layar."
            )
        }
    }

    // Hands-Free Gestures and App Controls
    fun scrollDown(): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null) {
            val success = service.scrollDown()
            if (success) {
                Pair(true, "Layar berhasil digulir ke bawah.")
            } else {
                Pair(false, "Tidak dapat menggulir layar saat ini.")
            }
        } else {
            openAccessibilitySettings()
            Pair(false, "Aktifkan 'Layanan Asisten Kendali HP' di Aksesibilitas agar asisten dapat menggulir layar.")
        }
    }

    fun scrollUp(): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null) {
            val success = service.scrollUp()
            if (success) {
                Pair(true, "Layar berhasil digulir ke atas.")
            } else {
                Pair(false, "Tidak dapat menggulir layar saat ini.")
            }
        } else {
            openAccessibilitySettings()
            Pair(false, "Aktifkan 'Layanan Asisten Kendali HP' di Aksesibilitas agar asisten dapat menggulir layar.")
        }
    }

    fun typeText(text: String): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null) {
            val success = service.typeText(text)
            if (success) {
                Pair(true, "Teks \"$text\" berhasil diketik.")
            } else {
                Pair(false, "Tidak menemukan kolom input teks yang aktif di layar.")
            }
        } else {
            openAccessibilitySettings()
            Pair(false, "Aktifkan 'Layanan Asisten Kendali HP' di Aksesibilitas agar asisten dapat mengetik teks.")
        }
    }

    fun clickSendOrSubmit(): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null) {
            val success = service.clickSendOrSubmit()
            if (success) {
                Pair(true, "Tombol kirim/cari berhasil ditekan.")
            } else {
                Pair(false, "Tidak menemukan tombol kirim atau cari di layar saat ini.")
            }
        } else {
            openAccessibilitySettings()
            Pair(false, "Aktifkan 'Layanan Asisten Kendali HP' di Aksesibilitas agar asisten dapat menekan tombol.")
        }
    }

    fun clickElement(targetText: String): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null) {
            val success = service.clickElementByText(targetText)
            if (success) {
                Pair(true, "Berhasil mengklik \"$targetText\".")
            } else {
                Pair(false, "Tidak menemukan elemen atau tombol dengan teks \"$targetText\".")
            }
        } else {
            openAccessibilitySettings()
            Pair(false, "Aktifkan 'Layanan Asisten Kendali HP' di Aksesibilitas untuk mengklik elemen layar.")
        }
    }

    fun goBack(): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null) {
            val success = service.performBack()
            if (success) Pair(true, "Kembali ke layar sebelumnya.") else Pair(false, "Gagal kembali.")
        } else {
            openAccessibilitySettings()
            Pair(false, "Aktifkan 'Layanan Asisten Kendali HP' di Aksesibilitas.")
        }
    }

    fun goHome(): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null) {
            val success = service.performHome()
            if (success) Pair(true, "Membuka layar utama / beranda.") else Pair(false, "Gagal membuka beranda.")
        } else {
            openAccessibilitySettings()
            Pair(false, "Aktifkan 'Layanan Asisten Kendali HP' di Aksesibilitas.")
        }
    }

    fun openRecents(): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null) {
            val success = service.performRecents()
            if (success) Pair(true, "Membuka daftar aplikasi terakhir.") else Pair(false, "Gagal membuka riwayat aplikasi.")
        } else {
            openAccessibilitySettings()
            Pair(false, "Aktifkan 'Layanan Asisten Kendali HP' di Aksesibilitas.")
        }
    }

    fun openNotifications(): Pair<Boolean, String> {
        vibrate()
        val service = AssistantAccessibilityService.instance
        return if (service != null) {
            val success = service.performNotifications()
            if (success) Pair(true, "Membuka panel notifikasi.") else Pair(false, "Gagal membuka panel notifikasi.")
        } else {
            openAccessibilitySettings()
            Pair(false, "Aktifkan 'Layanan Asisten Kendali HP' di Aksesibilitas.")
        }
    }

    fun searchYouTube(query: String): Pair<Boolean, String> {
        vibrate()
        val encoded = android.net.Uri.encode(query)
        val appIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("vnd.youtube://results?search_query=$encoded")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(appIntent)
            Pair(true, "Membuka YouTube dan mencari \"$query\".")
        } catch (_: Exception) {
            try {
                context.startActivity(webIntent)
                Pair(true, "Membuka pencarian YouTube untuk \"$query\".")
            } catch (_: Exception) {
                Pair(false, "Tidak dapat membuka aplikasi YouTube.")
            }
        }
    }

    fun sendWhatsAppDirect(phoneOrName: String, message: String): Pair<Boolean, String> {
        vibrate()
        val cleanNumber = phoneOrName.replace(Regex("[^0-9]"), "")
        val encodedMsg = android.net.Uri.encode(message)
        val uriStr = if (cleanNumber.length >= 7) {
            val formatted = if (cleanNumber.startsWith("0")) "62" + cleanNumber.substring(1) else cleanNumber
            "https://api.whatsapp.com/send?phone=$formatted&text=$encodedMsg"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMsg"
        }

        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uriStr)).apply {
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            Pair(true, "Membuka obrolan WhatsApp dengan pesan siap kirim.")
        } catch (_: Exception) {
            // Fallback without package restriction
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uriStr)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                Pair(true, "Membuka WhatsApp untuk mengirim pesan.")
            } catch (_: Exception) {
                Pair(false, "Aplikasi WhatsApp tidak terpasang di perangkat.")
            }
        }
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(30)
                }
            }
        } catch (_: Exception) {
            // Ignore vibration error
        }
    }
}
