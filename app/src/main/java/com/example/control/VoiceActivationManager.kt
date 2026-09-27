package com.example.control

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceActivationManager(
    private val context: Context,
    private val onSpeechStarted: (() -> Unit)? = null,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Pengenal suara tidak tersedia pada perangkat ini.")
            return
        }

        stopListening()
        onSpeechStarted?.invoke()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        onSpeechStarted?.invoke()
                    }

                    override fun onBeginningOfSpeech() {
                        onSpeechStarted?.invoke()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (rmsdB > 1.2f) {
                            onSpeechStarted?.invoke()
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        onSpeechStarted?.invoke()
                    }

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada suara yang cocok."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu bicara habis."
                            SpeechRecognizer.ERROR_AUDIO -> "Kendala pada audio perangkat."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon diperlukan."
                            else -> "Mendengarkan selesai."
                        }
                        onError(errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        onSpeechStarted?.invoke()
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim() ?: ""
                        if (spokenText.isNotEmpty()) {
                            _lastRecognizedText.value = spokenText
                            onResult(spokenText)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        onSpeechStarted?.invoke()
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            _lastRecognizedText.value = it
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
            onError("Gagal mengaktifkan mikrofon: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
    }
}
