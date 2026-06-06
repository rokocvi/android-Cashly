package com.example.projektmobpravi.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceInputHelper(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun startListening(
        languageTag: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onReadyForSpeech: () -> Unit = {}
    ) {
        destroy()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Prepoznavanje govora nije dostupno na ovom uređaju")
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = onReadyForSpeech()
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                onResult(matches?.firstOrNull().orEmpty())
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH       -> "Nije prepoznat govor, pokušaj ponovo"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Isteklo je vrijeme, pokušaj ponovo"
                    SpeechRecognizer.ERROR_NETWORK        -> "Provjeri internet vezu"
                    SpeechRecognizer.ERROR_AUDIO          -> "Greška mikrofona"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Nedostaje dozvola za mikrofon"
                    else                                  -> "Greška prepoznavanja ($error)"
                }
                onError(msg)
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer?.startListening(intent)
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}
