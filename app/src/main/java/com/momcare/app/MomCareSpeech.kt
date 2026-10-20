package com.momcare.app

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object MomCareSpeech {
    @Volatile
    private var engine: TextToSpeech? = null

    @Volatile
    private var ready = false

    @Synchronized
    fun speak(context: Context, message: String) {
        val existing = engine

        if (existing != null) {
            if (ready) {
                existing.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "momcare_voice"
                )
            }
            return
        }

        engine = TextToSpeech(context.applicationContext) { status ->
            synchronized(this) {
                val tts = engine ?: return@synchronized
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts.setLanguage(Locale.getDefault())
                    ready = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                    if (ready) {
                        tts.speak(
                            message,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "momcare_voice"
                        )
                    }
                }
            }
        }
    }
}
