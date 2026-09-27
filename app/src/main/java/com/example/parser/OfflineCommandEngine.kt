package com.example.parser

import android.media.AudioManager
import com.example.control.AppManager
import com.example.control.DeviceController
import java.util.Locale

sealed class CommandResult {
    data class Success(
        val message: String,
        val actionType: String,
        val followUpQuestion: String = "Langkah selanjutnya apa yang ingin Anda lakukan?",
        val suggestions: List<String> = listOf("Gulir ke Bawah", "Buka YouTube", "Buka WhatsApp", "Mode Senyap")
    ) : CommandResult()

    data class ActionPrompt(
        val message: String,
        val actionType: String,
        val followUpQuestion: String = "Silakan tentukan langkah selanjutnya:",
        val suggestions: List<String> = listOf("Lanjutkan", "Buka Beranda", "Batal")
    ) : CommandResult()

    data class Unknown(
        val message: String,
        val suggestions: List<String> = listOf("Buka YouTube", "Buka WhatsApp", "Gulir ke Bawah", "Nyalakan Senter")
    ) : CommandResult()
}

class OfflineCommandEngine(
    private val deviceController: DeviceController,
    private val appManager: AppManager,
    private val wakeWordManager: WakeWordManager
) {

    fun executeCommand(rawInput: String): CommandResult {
        var query = rawInput.trim().lowercase(Locale.ROOT)
        val currentWakeName = wakeWordManager.wakeName.value

        // Check for Change Wake Name command (e.g. "ganti nama panggilan jadi Garuda")
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
                    "WAKE_NAME_CHANGED",
                    "Nama baru aktif. Apa perintah Anda selanjutnya?",
                    listOf("Buka YouTube", "Buka WhatsApp", "Gulir ke Bawah", "Nyalakan Senter")
                )
            }
        }

        // Check if query starts with or is the wake word (e.g. "Halo Kobra", "Kobra matikan senter")
        if (wakeWordManager.matchesWakeWord(query)) {
            val remainder = wakeWordManager.extractCommandAfterWakeWord(query)
            if (remainder.isBlank()) {
                return CommandResult.Success(
                    "Halo! Saya $currentWakeName, asisten siap mendengarkan. Katakan perintah Anda!",
                    "WAKE_GREETING",
                    "Apa yang ingin Anda lakukan sekarang?",
                    listOf("Buka YouTube", "Buka WhatsApp", "Gulir ke Bawah", "Matikan Wi-Fi")
                )
            } else {
                query = remainder.lowercase(Locale.ROOT)
            }
        }

        // 0. Stop Speaking / Silence Command
        if (matchesAny(query, listOf("setop", "stop", "diam", "cukup", "berhenti", "tutup suara", "jangan bicara", "heningkan", "hentikan suara"))) {
            return CommandResult.Success("Baik, suara dihentikan.", "STOP_SPEECH", "", emptyList())
        }

        // 0.1 Update App Command
        if (matchesAny(query, listOf("periksa pembaruan", "update aplikasi", "cek update", "perbarui aplikasi", "cek pembaruan", "update studio"))) {
            return CommandResult.Success(
                "Memeriksa pembaruan build Google AI Studio terbaru untuk aplikasi ini.",
                "APP_UPDATE",
                "Pembaruan sedang diperiksa. Apa langkah selanjutnya?",
                listOf("Unduh & Pasang APK", "Buka Beranda", "Status HP")
            )
        }

        // 0.2 Hands-Free Scrolling (Gulir Layar)
        if (matchesAny(query, listOf("gulir ke bawah", "scroll down", "geser ke bawah", "scroll bawah", "turun ke bawah", "gulir bawah", "geser bawah", "layar ke bawah"))) {
            val (ok, msg) = deviceController.scrollDown()
            return CommandResult.Success(
                msg,
                "SCROLL_DOWN",
                "Layar digulir ke bawah. Mau gulir lagi atau buka video/chat?",
                listOf("Gulir ke Bawah", "Gulir ke Atas", "Ketik Pesan", "Kembali ke Beranda")
            )
        }
        if (matchesAny(query, listOf("gulir ke atas", "scroll up", "geser ke atas", "scroll atas", "naik ke atas", "gulir atas", "geser atas", "layar ke atas"))) {
            val (ok, msg) = deviceController.scrollUp()
            return CommandResult.Success(
                msg,
                "SCROLL_UP",
                "Layar digulir ke atas. Mau gulir lagi atau langkah berikutnya?",
                listOf("Gulir ke Bawah", "Gulir ke Atas", "Kembali", "Buka Beranda")
            )
        }

        // 0.3 Typing Text into active field (Ketik / Tulis Teks)
        if (query.startsWith("ketik ") || query.startsWith("tulis ") || query.startsWith("ketikkan ") ||
            query.startsWith("tulis teks ") || query.startsWith("masukkan teks ") || query.startsWith("tulis pesan ")
        ) {
            val textToType = query
                .removePrefix("tulis pesan ")
                .removePrefix("tulis teks ")
                .removePrefix("masukkan teks ")
                .removePrefix("ketikkan ")
                .removePrefix("ketik ")
                .removePrefix("tulis ")
                .trim()
            val (ok, msg) = deviceController.typeText(textToType)
            return CommandResult.Success(
                msg,
                "TYPE_TEXT",
                "Teks sudah diketik. Mau langsung kirim sekarang atau gulir layar?",
                listOf("Kirim Pesan", "Ketik Teks Lain", "Gulir ke Bawah", "Kembali ke Beranda")
            )
        }

        // 0.4 Send / Submit Button (Kirim Pesan / Tekan Kirim)
        if (matchesAny(query, listOf("kirim pesan", "kirim chat", "tekan kirim", "pencet kirim", "kirim teks", "kirimkan", "send message", "klik kirim"))) {
            val (ok, msg) = deviceController.clickSendOrSubmit()
            return CommandResult.Success(
                msg,
                "CLICK_SEND",
                "Pesan telah dikirim. Mau balas chat lain, buka YouTube, atau kembali?",
                listOf("Gulir ke Bawah", "Buka YouTube", "Kembali ke Beranda")
            )
        }

        // 0.5 YouTube Search & Play
        if (query.startsWith("cari di youtube ") || query.startsWith("telusuri youtube ") ||
            query.startsWith("buka youtube cari ") || query.startsWith("putar di youtube ") ||
            query.startsWith("cari video ") || (query.startsWith("putar ") && !query.startsWith("putus"))
        ) {
            val yQuery = query
                .removePrefix("cari di youtube ")
                .removePrefix("telusuri youtube ")
                .removePrefix("buka youtube cari ")
                .removePrefix("putar di youtube ")
                .removePrefix("cari video ")
                .removePrefix("putar ")
                .trim()
            val (ok, msg) = deviceController.searchYouTube(yQuery)
            return CommandResult.Success(
                msg,
                "YOUTUBE_SEARCH",
                "YouTube dibuka untuk \"$yQuery\". Mau gulir video atau cari video lain?",
                listOf("Gulir ke Bawah", "Gulir ke Atas", "Cari Video Lain", "Kembali ke Beranda")
            )
        }

        // 0.6 WhatsApp Direct Send
        if (query.startsWith("kirim pesan whatsapp ke ") || query.startsWith("kirim whatsapp ke ") ||
            query.startsWith("chat whatsapp ke ") || query.startsWith("chat whatsapp ")
        ) {
            val remainder = query
                .removePrefix("kirim pesan whatsapp ke ")
                .removePrefix("kirim whatsapp ke ")
                .removePrefix("chat whatsapp ke ")
                .removePrefix("chat whatsapp ")
                .trim()
            val parts = remainder.split(" ", limit = 2)
            val target = parts.getOrNull(0) ?: ""
            val message = parts.getOrNull(1) ?: "Halo"
            val (ok, msg) = deviceController.sendWhatsAppDirect(target, message)
            return CommandResult.Success(
                msg,
                "WHATSAPP_DIRECT",
                "WhatsApp terbuka untuk $target. Mau langsung kirim sekarang?",
                listOf("Kirim Pesan", "Ketik Teks", "Kembali ke Beranda")
            )
        }

        // 0.7 Click Element by Text (Klik / Tekan Tombol)
        if (query.startsWith("klik ") || query.startsWith("tekan tombol ") || query.startsWith("tekan ") || query.startsWith("pencet ")) {
            val targetElement = query
                .removePrefix("klik tombol ")
                .removePrefix("tekan tombol ")
                .removePrefix("klik ")
                .removePrefix("tekan ")
                .removePrefix("pencet ")
                .trim()
            val (ok, msg) = deviceController.clickElement(targetElement)
            return CommandResult.Success(
                msg,
                "CLICK_ELEMENT",
                "Elemen \"$targetElement\" diklik. Apa langkah selanjutnya?",
                listOf("Gulir ke Bawah", "Kembali", "Buka Beranda")
            )
        }

        // 0.8 System Navigation (Kembali, Beranda, Notifikasi, Recents)
        if (matchesAny(query, listOf("kembali", "back", "tombol kembali", "layar sebelumnya"))) {
            val (ok, msg) = deviceController.goBack()
            return CommandResult.Success(
                msg,
                "NAV_BACK",
                "Kembali ke layar sebelumnya. Langkah selanjutnya apa?",
                listOf("Buka YouTube", "Buka WhatsApp", "Buka Beranda")
            )
        }
        if (matchesAny(query, listOf("beranda", "home", "layar utama", "ke beranda", "kembali ke beranda"))) {
            val (ok, msg) = deviceController.goHome()
            return CommandResult.Success(
                msg,
                "NAV_HOME",
                "Membuka layar utama. Aplikasi apa yang ingin Anda buka sekarang?",
                listOf("Buka YouTube", "Buka WhatsApp", "Nyalakan Senter", "Mode Senyap")
            )
        }
        if (matchesAny(query, listOf("notifikasi", "buka notifikasi", "tarik notifikasi", "panel notifikasi"))) {
            val (ok, msg) = deviceController.openNotifications()
            return CommandResult.Success(
                msg,
                "NAV_NOTIFICATIONS",
                "Panel notifikasi dibuka. Mau gulir notifikasi atau kembali?",
                listOf("Gulir ke Bawah", "Kembali", "Buka Beranda")
            )
        }
        if (matchesAny(query, listOf("aplikasi terakhir", "recent apps", "riwayat aplikasi"))) {
            val (ok, msg) = deviceController.openRecents()
            return CommandResult.Success(
                msg,
                "NAV_RECENTS",
                "Daftar aplikasi terakhir dibuka. Mau beralih aplikasi atau kembali?",
                listOf("Kembali", "Buka Beranda")
            )
        }

        // 1. Wi-Fi Commands
        if (matchesAny(query, listOf("matikan wifi", "nonaktifkan wifi", "putus wifi", "wifi mati", "wifi off", "turn off wifi", "disconnect wifi"))) {
            val response = deviceController.toggleWifi(false)
            return CommandResult.Success(
                response,
                "WIFI_OFF",
                "Wi-Fi telah dimatikan. Apa langkah selanjutnya?",
                listOf("Buka YouTube", "Buka WhatsApp", "Nyalakan Senter", "Mode Senyap")
            )
        }
        if (matchesAny(query, listOf("hidupkan wifi", "nyalakan wifi", "aktifkan wifi", "sambungkan wifi", "wifi on", "wifi nyala", "turn on wifi"))) {
            val response = deviceController.toggleWifi(true)
            return CommandResult.Success(
                response,
                "WIFI_ON",
                "Wi-Fi berhasil diaktifkan. Mau buka YouTube atau periksa status koneksi?",
                listOf("Buka YouTube", "Buka WhatsApp", "Status HP", "Buka Beranda")
            )
        }
        if (query == "wifi" || query == "pengaturan wifi") {
            deviceController.openWifiSettings()
            return CommandResult.ActionPrompt("Membuka pengaturan Wi-Fi...", "WIFI_SETTINGS")
        }

        // 2. Mobile Data Commands
        if (matchesAny(query, listOf("matikan data", "nonaktifkan data", "matikan data seluler", "data off", "matikan paket", "matikan kuota", "turn off data", "turn off mobile data"))) {
            val response = deviceController.handleMobileDataAction(false)
            return CommandResult.ActionPrompt(
                response,
                "MOBILE_DATA_OFF",
                "Data seluler disesuaikan. Mau beralih ke Wi-Fi atau matikan senter?",
                listOf("Nyalakan Wi-Fi", "Nyalakan Senter", "Mode Senyap", "Buka Beranda")
            )
        }
        if (matchesAny(query, listOf("hidupkan data", "nyalakan data", "aktifkan data", "aktifkan data seluler", "data on", "data nyala", "turn on data", "turn on mobile data"))) {
            val response = deviceController.handleMobileDataAction(true)
            return CommandResult.ActionPrompt(
                response,
                "MOBILE_DATA_ON",
                "Panel data dibuka. Mau buka WhatsApp atau YouTube?",
                listOf("Buka WhatsApp", "Buka YouTube", "Status HP")
            )
        }
        if (query == "data" || query == "data seluler" || query == "pengaturan data") {
            deviceController.openMobileDataSettings()
            return CommandResult.ActionPrompt("Membuka pengaturan Data Seluler...", "MOBILE_DATA_SETTINGS")
        }

        // 3. Flashlight (Senter) Commands
        if (matchesAny(query, listOf("nyalakan senter", "hidupkan senter", "senter on", "lampu nyala", "buka senter", "turn on flashlight", "turn on torch"))) {
            val (ok, msg) = deviceController.toggleFlashlight(true)
            return if (ok) CommandResult.Success(
                msg,
                "FLASHLIGHT_ON",
                "Senter menyala terang. Apa langkah selanjutnya?",
                listOf("Matikan Senter", "Kunci Layar", "Buka Kamera", "Status HP")
            ) else CommandResult.Unknown(msg)
        }
        if (matchesAny(query, listOf("matikan senter", "padamkan senter", "senter off", "lampu mati", "tutup senter", "turn off flashlight", "turn off torch"))) {
            val (ok, msg) = deviceController.toggleFlashlight(false)
            return if (ok) CommandResult.Success(
                msg,
                "FLASHLIGHT_OFF",
                "Senter telah dimatikan. Mau kunci layar atau buka aplikasi?",
                listOf("Kunci Layar", "Buka YouTube", "Buka WhatsApp", "Mode Senyap")
            ) else CommandResult.Unknown(msg)
        }
        if (query == "senter" || query == "senter hp") {
            val (ok, msg) = deviceController.toggleFlashlight()
            return if (ok) CommandResult.Success(
                msg,
                "FLASHLIGHT_TOGGLE",
                "Senter telah diubah. Apa langkah berikutnya?",
                listOf("Nyalakan Senter", "Matikan Senter", "Kunci Layar")
            ) else CommandResult.Unknown(msg)
        }

        // 4. Bluetooth Commands
        if (matchesAny(query, listOf("matikan bluetooth", "nonaktifkan bluetooth", "bluetooth off", "bluetooth mati", "turn off bluetooth"))) {
            val msg = deviceController.toggleBluetooth(false)
            return CommandResult.Success(
                msg,
                "BLUETOOTH_OFF",
                "Bluetooth disesuaikan. Langkah selanjutnya?",
                listOf("Nyalakan Wi-Fi", "Mode Senyap", "Status HP")
            )
        }
        if (matchesAny(query, listOf("hidupkan bluetooth", "nyalakan bluetooth", "aktifkan bluetooth", "bluetooth on", "bluetooth nyala", "turn on bluetooth"))) {
            val msg = deviceController.toggleBluetooth(true)
            return CommandResult.Success(
                msg,
                "BLUETOOTH_ON",
                "Bluetooth diaktifkan. Mau buka aplikasi musik atau YouTube?",
                listOf("Buka YouTube", "Status HP", "Buka Beranda")
            )
        }

        // 5. Sound / Ringer Mode Commands
        if (matchesAny(query, listOf("mode senyap", "diamkan hp", "silent mode", "senyapkan", "mode silent", "hening", "mute"))) {
            val (ok, msg) = deviceController.setRingerMode(AudioManager.RINGER_MODE_SILENT)
            return CommandResult.Success(
                msg,
                "SOUND_SILENT",
                "HP kini dalam mode senyap. Ingin kunci layar atau atur mode tidur?",
                listOf("Kunci Layar", "Mode Tidur", "Matikan Senter")
            )
        }
        if (matchesAny(query, listOf("mode getar", "getarkan hp", "vibrate mode", "aktifkan getar", "getar saja"))) {
            val (ok, msg) = deviceController.setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            return CommandResult.Success(
                msg,
                "SOUND_VIBRATE",
                "Mode getar aktif. Apa langkah berikutnya?",
                listOf("Buka WhatsApp", "Kunci Layar", "Status HP")
            )
        }
        if (matchesAny(query, listOf("mode suara", "suara normal", "dering normal", "bunyikan hp", "unmute", "suara aktif", "mode dering"))) {
            val (ok, msg) = deviceController.setRingerMode(AudioManager.RINGER_MODE_NORMAL)
            return CommandResult.Success(
                msg,
                "SOUND_NORMAL",
                "Suara HP kembali normal. Langkah selanjutnya apa?",
                listOf("Buka YouTube", "Buka WhatsApp", "Status HP")
            )
        }

        // 6. Security / Lock & Screenshot
        if (matchesAny(query, listOf("kunci layar", "kunci hp", "lock screen", "kunci sekarang", "matikan layar"))) {
            val (ok, msg) = deviceController.lockScreen()
            return if (ok) CommandResult.Success(
                msg,
                "LOCK_SCREEN",
                "Layar dikunci.",
                listOf("Buka Beranda")
            ) else CommandResult.ActionPrompt(msg, "LOCK_SCREEN_REQUEST")
        }
        if (matchesAny(query, listOf("tangkapan layar", "screenshot", "foto layar", "screen shot", "ambil layar"))) {
            val (ok, msg) = deviceController.takeScreenshot()
            return if (ok) CommandResult.Success(
                msg,
                "TAKE_SCREENSHOT",
                "Tangkapan layar diambil. Mau kirim ke WhatsApp atau buka galeri?",
                listOf("Buka WhatsApp", "Buka Galeri", "Buka Beranda")
            ) else CommandResult.ActionPrompt(msg, "SCREENSHOT_REQUEST")
        }

        // 7. Launch Apps (Buka YouTube, WhatsApp, Kamera, dll.)
        if (query.startsWith("buka ") || query.startsWith("jalankan ") || query.startsWith("start ") || query.startsWith("open ")) {
            val appQuery = query
                .removePrefix("buka ")
                .removePrefix("jalankan ")
                .removePrefix("start ")
                .removePrefix("open ")
                .trim()

            val isLaunched = appManager.launchAppByName(appQuery)
            return if (isLaunched) {
                CommandResult.Success(
                    "Membuka aplikasi \"$appQuery\". Asisten tetap standby di latar belakang.",
                    "APP_LAUNCH",
                    "Aplikasi \"$appQuery\" aktif. Langkah selanjutnya: gulir layar, cari sesuatu, atau ketik teks?",
                    listOf("Gulir ke Bawah", "Gulir ke Atas", "Ketik Pesan", "Kembali ke Beranda")
                )
            } else {
                CommandResult.Unknown("Tidak menemukan aplikasi \"$appQuery\" di perangkat ini.")
            }
        }

        // 8. Automation Routines
        if (matchesAny(query, listOf("mode tidur", "waktu tidur", "mau tidur", "selamat malam"))) {
            deviceController.setRingerMode(AudioManager.RINGER_MODE_SILENT)
            deviceController.toggleFlashlight(false)
            deviceController.openInternetPanel()
            return CommandResult.Success(
                "Mode tidur diaktifkan: Suara disenyapkan, senter dimatikan, dan panel koneksi dibuka.",
                "ROUTINE_SLEEP",
                "Mode tidur aktif. Ingin kunci layar sekarang?",
                listOf("Kunci Layar", "Matikan Wi-Fi", "Mode Senyap")
            )
        }
        if (matchesAny(query, listOf("mode kerja", "fokus kerja", "waktu kerja"))) {
            deviceController.setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            deviceController.toggleWifi(true)
            return CommandResult.Success(
                "Mode fokus kerja diaktifkan: Mode getar disetel dan Wi-Fi disiapkan.",
                "ROUTINE_WORK",
                "Fokus kerja aktif. Mau buka WhatsApp atau periksa baterai?",
                listOf("Buka WhatsApp", "Buka YouTube", "Status HP")
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
            return CommandResult.Success(
                msg,
                "STATUS_QUERY",
                "Informasi status ditampilkan. Apa yang ingin Anda sesuaikan?",
                listOf("Nyalakan Senter", "Mode Senyap", "Buka YouTube", "Buka WhatsApp")
            )
        }

        // 10. Help / Greeting
        if (matchesAny(query, listOf("halo", "hai", "bantuan", "menu", "perintah", "help", "siapa kamu"))) {
            return CommandResult.Success(
                "Halo! Saya Asisten Kendali HP Offline Anda. Anda bisa memerintahkan saya:\n• \"Gulir ke bawah\" / \"Gulir ke atas\"\n• \"Cari di YouTube [lagu/video]\"\n• \"Ketik [pesan Anda]\" dan \"Kirim pesan\"\n• \"Buka [Nama Aplikasi]\" (misal: YouTube, WhatsApp, Kamera)\n• \"Matikan Wi-Fi\" atau \"Nyalakan Wi-Fi\"\n• \"Nyalakan Senter\" atau \"Mode Senyap\"\n• \"Kunci Layar\" atau \"Tangkapan Layar\"",
                "HELP",
                "Pilihlah salah satu perintah berikut atau ucapkan langsung:",
                listOf("Buka YouTube", "Buka WhatsApp", "Gulir ke Bawah", "Nyalakan Senter")
            )
        }

        return CommandResult.Unknown(
            "Perintah tidak dikenali: \"$rawInput\". Coba katakan \"Gulir ke bawah\", \"Cari di YouTube\", \"Buka WhatsApp\", atau \"Nyalakan Senter\".",
            listOf("Buka YouTube", "Buka WhatsApp", "Gulir ke Bawah", "Nyalakan Senter")
        )
    }

    private fun matchesAny(input: String, keywords: List<String>): Boolean {
        return keywords.any { kw ->
            input == kw || input.contains(kw)
        }
    }
}
