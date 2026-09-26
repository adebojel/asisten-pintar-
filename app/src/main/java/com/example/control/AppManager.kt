package com.example.control

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import java.util.Locale

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null
)

class AppManager(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    fun getInstalledApps(): List<AppItem> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = packageManager.queryIntentActivities(intent, 0)
            resolveInfos.mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                val label = ri.loadLabel(packageManager)?.toString() ?: pkg
                AppItem(
                    name = label,
                    packageName = pkg,
                    icon = try { ri.loadIcon(packageManager) } catch (_: Exception) { null }
                )
            }.distinctBy { it.packageName }.sortedBy { it.name.lowercase(Locale.ROOT) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun launchAppByPackage(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun launchAppByName(query: String): Pair<Boolean, String> {
        val q = query.trim().lowercase(Locale.ROOT)

        // General fallback shortcuts in Indonesian
        if (q == "kamera" || q.contains("camera")) {
            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(cameraIntent)
                return Pair(true, "Membuka Kamera...")
            } catch (_: Exception) {}
        }

        if (q == "telepon" || q == "dialer" || q == "panggilan") {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(dialIntent)
                return Pair(true, "Membuka Telepon...")
            } catch (_: Exception) {}
        }

        if (q == "pesan" || q == "sms") {
            val smsIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(smsIntent)
                return Pair(true, "Membuka Pesan...")
            } catch (_: Exception) {}
        }

        if (q == "galeri" || q == "foto") {
            val galleryIntent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(galleryIntent)
                return Pair(true, "Membuka Galeri...")
            } catch (_: Exception) {}
        }

        if (q == "browser" || q == "internet" || q == "chrome" || q == "web") {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(browserIntent)
                return Pair(true, "Membuka Browser...")
            } catch (_: Exception) {}
        }

        // Search installed apps by name or package match
        val allApps = getInstalledApps()
        // 1. Exact match on app name
        var match = allApps.firstOrNull { it.name.equals(q, ignoreCase = true) }
        // 2. Contains match
        if (match == null) {
            match = allApps.firstOrNull { it.name.lowercase(Locale.ROOT).contains(q) }
        }
        // 3. Package name match
        if (match == null) {
            match = allApps.firstOrNull { it.packageName.lowercase(Locale.ROOT).contains(q) }
        }

        if (match != null) {
            val success = launchAppByPackage(match.packageName)
            return if (success) {
                Pair(true, "Membuka aplikasi ${match.name}...")
            } else {
                Pair(false, "Gagal meluncurkan aplikasi ${match.name}.")
            }
        }

        return Pair(false, "Aplikasi \"$query\" tidak ditemukan pada ponsel Anda.")
    }
}
