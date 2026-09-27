package com.example.control

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AssistantCoordinator {
    var onFloatingMicClicked: (() -> Unit)? = null
    var onFloatingActionSubmitted: ((String) -> Unit)? = null

    private val _floatingStatusText = MutableStateFlow("Asisten Standby - Siap kendalikan HP")
    val floatingStatusText: StateFlow<String> = _floatingStatusText.asStateFlow()

    private val _floatingFollowUp = MutableStateFlow("Langkah selanjutnya apa yang ingin Anda lakukan?")
    val floatingFollowUp: StateFlow<String> = _floatingFollowUp.asStateFlow()

    private val _floatingSuggestions = MutableStateFlow<List<String>>(
        listOf("Gulir ke Bawah", "Buka YouTube", "Buka WhatsApp", "Nyalakan Senter")
    )
    val floatingSuggestions: StateFlow<List<String>> = _floatingSuggestions.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isFloatingServiceRunning = MutableStateFlow(false)
    val isFloatingServiceRunning: StateFlow<Boolean> = _isFloatingServiceRunning.asStateFlow()

    fun updateStatus(text: String, followUp: String? = null, suggestions: List<String>? = null) {
        _floatingStatusText.value = text
        if (followUp != null) {
            _floatingFollowUp.value = followUp
        }
        if (suggestions != null && suggestions.isNotEmpty()) {
            _floatingSuggestions.value = suggestions
        }
    }

    fun setListening(listening: Boolean) {
        _isListening.value = listening
    }

    fun setFloatingServiceRunning(running: Boolean) {
        _isFloatingServiceRunning.value = running
    }
}
