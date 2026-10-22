package com.iptvcinema.tv.features.search

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.iptvcinema.tv.core.design.components.SearchKeyboardLayout

/** Builds and reads the system speech-recognition flow used by the Search mic key. */
object VoiceSearch {
    /** BCP-47 tag the recognizer should listen in, matching the on-screen keyboard. */
    fun languageTag(layout: SearchKeyboardLayout): String = when (layout) {
        SearchKeyboardLayout.Arabic -> "ar"
        SearchKeyboardLayout.English -> "en-US"
    }

    /** The first usable transcript from the recognizer's candidates, trimmed, or null. */
    fun bestTranscript(candidates: List<String>?): String? =
        candidates?.asSequence()?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }

    fun isAvailable(context: Context): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context) ||
            recognizeIntent("en-US", "").resolveActivity(context.packageManager) != null

    fun recognizeIntent(languageTag: String, prompt: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (prompt.isNotBlank()) putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
}
