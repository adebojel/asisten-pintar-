package com.example.parser

import android.media.AudioManager
import com.example.control.AppManager
import com.example.control.DeviceController
import java.util.Locale

sealed class CommandResult {
    data class Success(val message: String, val actionType: String) : CommandResult()
    data class ActionPrompt(val message: String, val actionType: String) : CommandResult()
    data class Unknown(val message: String) : CommandResult()
}

class OfflineCommandEngine(
    private val deviceController: DeviceController,
    private val appManager: AppManager,
    private val wakeWordManager: WakeWordManager
) {

    fun executeCommand(rawInput: String): CommandResult {
        var query = rawInput.trim().lowercase(Locale.ROOT)
        val currentWakeName = wakeWordManager.wakeName.value

        // Check for Change Wake Name command (e.g. "ganti nama panggilan jadi Garuda", "ubah nama asisten menjadi Jarvis")
        if (query.startsWith("ganti nama panggilan") || query.startsWith("ubah nama panggilan") ||
            query.startsWith("ganti nama asisten") || query.startsWith("ubah nama asisten") ||
            query.startsWith("ganti nama jadi ") || query.startsWith("ubah nama jadi ")
        ) {
            val newName = query
                .removePrefix("ganti nama panggilan menjadi ")
                .removePrefix("ganti nama panggilan jadi ")
                .removePrefix("ubah nama panggilan menjadi ")
                .removePrefix("ubah nama panggilan jadi ")
                .removePrefix("ganti nama asisten menjadi ")
                .removePrefix("ganti nama asisten jadi ")
                .removePrefix("ubah nama asisten menjadi ")
                .removePrefix("ubah nama asisten jadi ")
                .removePrefix("ganti nama jadi ")
                .removePrefix("ubah nama jadi ")
                .trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

            if (newName.isNotEmpty()) {
                wakeWordManager.setWakeName(newName)
                return CommandResult.Success(
                    "Nama panggilan berhasil diubah menjadi \"$newName\". Sekarang Anda bisa memanggil saya dengan \"Halo $newName\"!",
                    "WAKE_NAME_CHANGED"
                )
            }
        }

        // Check if query starts with or is the wake word (e.g. "Halo Kobra", "Kobra matikan senter")
        if (wakeWordManager.matchesWakeWord(query)) {
            val remainder = wakeWordManager.extractCommandAfterWakeWord(query)
            if (remainder.isBlank()) {
                return CommandResult.Success(
                    "Halo! Saya $currentWakeName, asisten siap mendengarkan. Katakan perintah Anda!",
                    "WAKE_GREETING"
                )
            } else {
                // Execute remainder command
                query = remainder.lowercase(Locale.ROOT)
            }
        }

        // 1. Wi-Fi Commands
        if (matchesAny(query, listOf("matikan wifi", "nonaktifkan wifi", "putus wifi", "wifi mati", "wifi off", "turn off wifi", "disconnect wifi"))) {
            val response = deviceController.toggleWifi(false)
            return CommandResult.Success(response, "WIFI_OFF")
        }
        if (matchesAny(query, listOf("hidupkan wifi", "nyalakan wifi", "aktifkan wifi", "sambungkan wifi", "wifi on", "wifi nyala", "turn on wifi"))) {
            val response = deviceController.toggleWifi(true)
            return CommandResult.Success(response, "WIFI_ON")
        }
        if (query == "wifi" || query == "pengaturan wifi") {
            deviceController.openWifiSettings()
            return CommandResult.ActionPrompt("Membuka pengaturan Wi-Fi...", "WIFI_SETTINGS")
        }

        // 2. Mobile Data Commands
        if (matchesAny(query, listOf("matikan data", "nonaktifkan data", "matikan data seluler", "data off", "matikan paket", "matikan kuota", "turn off data", "turn off mobile data"))) {
            val response = deviceController.handleMobileDataAction(false)
            return CommandResult.ActionPrompt(response, "MOBILE_DATA_OFF")
        }
        if (matchesAny(query, listOf("hidupkan data", "nyalakan data", "aktifkan data", "aktifkan data seluler", "data on", "data nyala", "turn on data", "turn on mobile data"))) {
            val response = deviceController.handleMobileDataAction(true)
            return CommandResult.ActionPrompt(response, "MOBILE_DATA_ON")
        }
        if (query == "data" || query == "data seluler" || query == "pengaturan data") {
            deviceController.openMobileDataSettings()
            return CommandResult.ActionPrompt("Membuka pengaturan Data Seluler...", "MOBILE_DATA_SETTINGS")
        }

        // 3. Flashlight (Senter) Commands
        if (matchesAny(query, listOf("nyalakan senter", "hidupkan senter", "senter on", "lampu nyala", "buka senter", "turn on flashlight", "turn on torch"))) {
            val (ok, msg) = deviceController.toggleFlashlight(true)
            return if (ok) CommandResult.Success(msg, "FLASHLIGHT_ON") else CommandResult.Unknown(msg)
        }
        if (matchesAny(query, listOf("matikan senter", "padamkan senter", "senter off", "lampu mati", "tutup senter", "turn off flashlight", "turn off torch"))) {
            val (ok, msg) = deviceController.toggleFlashlight(false)
            return if (ok) CommandResult.Success(msg, "FLASHLIGHT_OFF") else CommandResult.Unknown(msg)
        }
        if (query == "senter" || query == "senter hp") {
            val (ok, msg) = deviceController.toggleFlashlight()
            return if (ok) CommandResult.Success(msg, "FLASHLIGHT_TOGGLE") else CommandResult.Unknown(msg)
        }

        // 4. Sound & Ringer Mode
        if (matchesAny(query, listOf("mode senyap", "mode hening", "senyap", "hening", "diam", "silent", "silent mode", "mute"))) {
            val (ok, msg) = deviceController.setRingerMode(AudioManager.RINGER_MODE_SILENT)
            return if (ok) CommandResult.Success(msg, "SOUND_SILENT") else CommandResult.ActionPrompt(msg, "SOUND_SILENT_ERROR")
        }
        if (matchesAny(query, listOf("mode getar", "getar", "vibrate", "vibrate mode"))) {
            val (ok, msg) = deviceController.setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            return if (ok) CommandResult.Success(msg, "SOUND_VIBRATE") else CommandResult.ActionPrompt(msg, "SOUND_VIBRATE_ERROR")
        }
        if (matchesAny(query, listOf("suara normal", "mode normal", "bunyikan nada", "unmute", "normal", "suara on"))) {
            val (ok, msg) = deviceController.setRingerMode(AudioManager.RINGER_MODE_NORMAL)
            return if (ok) CommandResult.Success(msg, "SOUND_NORMAL") else CommandResult.ActionPrompt(msg, "SOUND_NORMAL_ERROR")
        }

        // 5. System Controls (Lock, Screenshot, Quick Settings)
        if (matchesAny(query, listOf("kunci layar", "kunci hp", "kunci ponsel", "matikan layar", "lock screen"))) {
            val (ok, msg) = deviceController.lockScreen()
            return if (ok) CommandResult.Success(msg, "LOCK_SCREEN") else CommandResult.ActionPrompt(msg, "LOCK_SCREEN_REQ")
        }
        if (matchesAny(query, listOf("setelan cepat", "pengaturan cepat", "panel kontrol", "buka toggle", "quick settings"))) {
            val (ok, msg) = deviceController.openQuickSettingsPanel()
            return if (ok) CommandResult.Success(msg, "QUICK_SETTINGS") else CommandResult.ActionPrompt(msg, "QUICK_SETTINGS_REQ")
        }
        if (matchesAny(query, listOf("tangkapan layar", "screenshot", "foto layar", "screen shot", "ss"))) {
            val (ok, msg) = deviceController.takeScreenshot()
            return if (ok) CommandResult.Success(msg, "SCREENSHOT") else CommandResult.ActionPrompt(msg, "SCREENSHOT_REQ")
        }

        // 6. Connectivity Settings
        if (matchesAny(query, listOf("matikan bluetooth", "nonaktifkan bluetooth", "bluetooth mati", "bluetooth off", "turn off bluetooth"))) {
            val feedback = deviceController.toggleBluetooth(false)
            return CommandResult.Success(feedback, "BLUETOOTH_OFF")
        }
        if (matchesAny(query, listOf("nyalakan bluetooth", "aktifkan bluetooth", "hidupkan bluetooth", "bluetooth nyala", "bluetooth on", "turn on bluetooth"))) {
            val feedback = deviceController.toggleBluetooth(true)
            return CommandResult.Success(feedback, "BLUETOOTH_ON")
        }
        if (matchesAny(query, listOf("bluetooth", "buka bluetooth", "pengaturan bluetooth"))) {
            deviceController.openBluetoothSettings()
            return CommandResult.ActionPrompt("Membuka setelan Bluetooth.", "BLUETOOTH")
        }
        if (matchesAny(query, listOf("mode pesawat", "pesawat", "airplane mode", "flight mode"))) {
            deviceController.openAirplaneModeSettings()
            return CommandResult.ActionPrompt("Membuka setelan Mode Pesawat.", "AIRPLANE_MODE")
        }
        if (matchesAny(query, listOf("hemat baterai", "hemat daya", "penghemat baterai", "battery saver"))) {
            deviceController.openBatterySaverSettings()
            return CommandResult.ActionPrompt("Membuka setelan Hemat Baterai.", "BATTERY_SAVER")
        }
        if (matchesAny(query, listOf("hotspot", "tethering", "berbagi koneksi", "buka hotspot"))) {
            deviceController.openHotspotSettings()
            return CommandResult.ActionPrompt("Membuka setelan Hotspot & Tethering.", "HOTSPOT")
        }

        // 7. App Launch Commands (e.g. "buka whatsapp", "buka youtube", "buka kamera", "buka [app]")
        if (query.startsWith("buka ") || query.startsWith("jalankan ") || query.startsWith("akses ") || query.startsWith("open ")) {
            val appQuery = query
                .removePrefix("buka ")
                .removePrefix("jalankan ")
                .removePrefix("akses ")
                .removePrefix("open ")
                .removePrefix("aplikasi ")
                .trim()

            if (appQuery.isNotEmpty()) {
                val (ok, msg) = appManager.launchAppByName(appQuery)
                return if (ok) {
                    CommandResult.Success(msg, "APP_LAUNCH")
                } else {
                    CommandResult.Unknown(msg)
                }
            }
        }

        if (matchesAny(query, listOf("buka aplikasi", "daftar aplikasi", "semua aplikasi", "lihat aplikasi", "list app"))) {
            return CommandResult.ActionPrompt(
                "Anda dapat melihat, mencari, dan membuka semua aplikasi ponsel di tab 'Aplikasi'.",
                "APP_LIST"
            )
        }

        // 8. Routines
        if (matchesAny(query, listOf("mode tidur", "waktu tidur", "mau tidur", "selamat malam"))) {
            deviceController.setRingerMode(AudioManager.RINGER_MODE_SILENT)
            deviceController.toggleFlashlight(false)
            deviceController.openInternetPanel()
            return CommandResult.Success(
                "Mode tidur diaktifkan: Suara disenyapkan, senter dimatikan, dan panel koneksi dibuka untuk mematikan data/Wi-Fi.",
                "ROUTINE_SLEEP"
            )
        }
        if (matchesAny(query, listOf("mode kerja", "fokus kerja", "waktu kerja"))) {
            deviceController.setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            deviceController.toggleWifi(true)
            return CommandResult.Success(
                "Mode fokus kerja diaktifkan: Mode getar disetel dan Wi-Fi disiapkan.",
                "ROUTINE_WORK"
            )
        }

        // 9. Status Query
        if (matchesAny(query, listOf("status", "status hp", "info sistem", "cek koneksi", "cek baterai", "kondisi hp"))) {
            deviceController.refreshStatus()
            val st = deviceController.deviceStatus.value
            val soundText = when (st.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "Senyap"
                AudioManager.RINGER_MODE_VIBRATE -> "Getar"
                else -> "Normal"
            }
            val msg = "Status HP Anda:\n• Jaringan: ${st.activeNetworkName}\n• Wi-Fi: ${if (st.isWifiEnabled) "Aktif" else "Mati"}\n• Baterai: ${st.batteryPercent}%\n• Suara: $soundText\n• Senter: ${if (st.isTorchOn) "Menyala" else "Mati"}"
            return CommandResult.Success(msg, "STATUS_QUERY")
        }

        // 10. Help / Greeting
        if (matchesAny(query, listOf("halo", "hai", "bantuan", "menu", "perintah", "help", "siapa kamu"))) {
            return CommandResult.Success(
                "Halo! Saya Asisten Kendali HP Offline Anda. Anda bisa memerintahkan saya:\n• \"Buka [Nama Aplikasi]\" (misal: Buka WhatsApp, YouTube, Kamera, Galeri)\n• \"Matikan Wi-Fi\" atau \"Nyalakan Wi-Fi\"\n• \"Matikan Data Seluler\" atau \"Nyalakan Data\"\n• \"Nyalakan Senter\" atau \"Matikan Senter\"\n• \"Mode Senyap\" atau \"Mode Getar\"\n• \"Kunci Layar\" atau \"Tangkapan Layar\"\n• \"Status HP\" atau \"Mode Tidur\"",
                "HELP"
            )
        }

        return CommandResult.Unknown(
            "Perintah tidak dikenali: \"$rawInput\". Coba katakan \"Buka WhatsApp\", \"Matikan Wi-Fi\", \"Matikan Data\", \"Nyalakan Senter\", atau ketik \"Bantuan\"."
        )
    }

    private fun matchesAny(input: String, keywords: List<String>): Boolean {
        return keywords.any { kw ->
            input == kw || input.contains(kw)
        }
    }
}
