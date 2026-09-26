package com.example.parser

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class WakeWordManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("assistant_wake_prefs", Context.MODE_PRIVATE)

    private val _wakeName = MutableStateFlow(
        prefs.getString(KEY_WAKE_NAME, DEFAULT_WAKE_NAME) ?: DEFAULT_WAKE_NAME
    )
    val wakeName: StateFlow<String> = _wakeName.asStateFlow()

    private val _isWakeDetectionEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_WAKE_ENABLED, true)
    )
    val isWakeDetectionEnabled: StateFlow<Boolean> = _isWakeDetectionEnabled.asStateFlow()

    fun setWakeName(newName: String) {
        val cleanName = newName.trim().ifEmpty { DEFAULT_WAKE_NAME }
        prefs.edit().putString(KEY_WAKE_NAME, cleanName).apply()
        _wakeName.value = cleanName
    }

    fun setWakeDetectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_ENABLED, enabled).apply()
        _isWakeDetectionEnabled.value = enabled
    }

    /**
     * Checks if input contains wake word pattern like:
     * "halo kobra", "hallo kobra", "hai kobra", "hey kobra", "ok kobra", or simply "kobra"
     */
    fun matchesWakeWord(input: String): Boolean {
        if (!_isWakeDetectionEnabled.value) return false
        val currentName = _wakeName.value.lowercase(Locale.ROOT)
        val lower = input.trim().lowercase(Locale.ROOT)

        val wakePatterns = listOf(
            "hallo $currentName",
            "halo $currentName",
            "hai $currentName",
            "hey $currentName",
            "hei $currentName",
            "ok $currentName",
            "oke $currentName",
            currentName
        )

        return wakePatterns.any { pattern ->
            lower == pattern || lower.startsWith("$pattern ") || lower.startsWith("$pattern,")
        }
    }

    /**
     * Extracts the command following the wake word.
     * e.g. "Halo Kobra matikan wifi" -> "matikan wifi"
     * If user only said "Halo Kobra", returns empty string "".
     */
    fun extractCommandAfterWakeWord(input: String): String {
        val currentName = _wakeName.value.lowercase(Locale.ROOT)
        val lower = input.trim().lowercase(Locale.ROOT)

        val prefixes = listOf(
            "hallo $currentName",
            "halo $currentName",
            "hai $currentName",
            "hey $currentName",
            "hei $currentName",
            "ok $currentName",
            "oke $currentName",
            currentName
        )

        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                val remainder = input.substring(prefix.length).trim().removePrefix(",").trim()
                return remainder
            }
        }
        return input.trim()
    }

    companion object {
        const val DEFAULT_WAKE_NAME = "Kobra"
        private const val KEY_WAKE_NAME = "key_wake_name"
        private const val KEY_WAKE_ENABLED = "key_wake_enabled"
    }
}
