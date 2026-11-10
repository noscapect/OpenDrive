package net.opendrive.assistant

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Uses the existing media audio path to the car; focus is released when speech ends or is interrupted. */
internal class AssistantSpeech(context: Context, private val failed: () -> Unit) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stop()
        }.build()
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    private var closed = false

    fun speak(text: String) {
        if (closed) return
        pending = text.take(3000)
        if (ready) { play(); return }
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            main.post {
                if (closed) return@post
                if (status != TextToSpeech.SUCCESS) { pending = null; failed(); return@post }
                tts?.setAudioAttributes(attributes)
                val language = tts?.setLanguage(Locale.getDefault())
                if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    pending = null; failed(); return@post
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { main.post { audio.abandonAudioFocusRequest(focus) } }
                    @Deprecated("Android callback")
                    override fun onError(id: String?) { main.post { audio.abandonAudioFocusRequest(focus); failed() } }
                })
                ready = true
                play()
            }
        }
    }

    private fun play() {
        val text = pending ?: return
        pending = null
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { failed(); return }
        if (tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "opendrive-reply") != TextToSpeech.SUCCESS) {
            audio.abandonAudioFocusRequest(focus); failed()
        }
    }

    fun stop() { pending = null; tts?.stop(); audio.abandonAudioFocusRequest(focus) }
    fun close() { closed = true; stop(); tts?.shutdown(); tts = null }
}
