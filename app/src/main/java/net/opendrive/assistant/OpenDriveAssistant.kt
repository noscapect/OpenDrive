package net.opendrive.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.prototux.aare.R
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.host.MicHandle
import java.util.Locale

/** One assistant per runtime, disposed with the car connection/preview. All state changes run on main. */
class OpenDriveAssistant(private val runtime: CarRuntime, parent: CoroutineScope) {
    private val context = runtime.context.applicationContext
    private val scope = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private val settings = AssistantSettings(context)
    private val conversation = AssistantConversation()
    private val tools = OpenDriveTools(runtime)
    private val main = Handler(Looper.getMainLooper())
    private val speech = AssistantSpeech(context) { error = context.getString(R.string.od_voice_unavailable) }
    private var mic: MicHandle? = null
    private var capture: Utterance? = null
    private var job: Job? = null
    private var timeout: Job? = null
    private var generation = 0
    private var closed = false
    private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        main.post {
            if (!closed && !settings.enabled) {
                cancelWork(); conversation.clear(); tools.clear(); question = ""; answer = ""; visible = false
            }
        }
    }

    init { settings.observe(settingsListener) }

    var visible by mutableStateOf(false); private set
    var listening by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var question by mutableStateOf(""); private set
    var answer by mutableStateOf(""); private set
    var error by mutableStateOf<String?>(null); private set

    /** Pressing the steering-wheel/rail microphone interrupts a reply or finishes an utterance. */
    fun activate() {
        if (closed) return
        visible = true
        if (listening) { finishRecording(); return }
        cancelWork()
        error = null
        if (!settings.enabled || !settings.hasKey) { error = context.getString(R.string.od_configure_first); return }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            error = context.getString(R.string.od_microphone_permission); return
        }
        if (runtime.ui.call != null) { error = context.getString(R.string.od_call_active); return }
        runtime.ui.assistantAppId = null
        val token = generation
        val utterance = Utterance()
        capture = utterance
        question = ""
        answer = ""
        listening = true
        runtime.ui.listening = true
        try {
            mic = runtime.microphone?.invoke({ data, offset, length ->
                if (utterance.append(data, offset, length)) main.post {
                    if (generation == token && listening) finishRecording()
                }
            }, { main.post {
                if (generation == token && listening) {
                    cancelWork(); error = context.getString(R.string.od_microphone_unavailable)
                }
            } })
            if (mic == null) { cancelWork(); error = context.getString(R.string.od_microphone_unavailable); return }
            // A head unit may accept the microphone request but never deliver PCM.
            timeout = scope.launch { delay(20_000); if (generation == token && listening) finishRecording() }
        } catch (_: Exception) { cancelWork(); error = context.getString(R.string.od_microphone_unavailable) }
    }

    fun finishRecording() {
        val utterance = capture ?: return
        capture = null
        listening = false
        runtime.ui.listening = false
        timeout?.cancel(); timeout = null
        mic?.close(); mic = null
        if (!utterance.hasSpeech) { error = context.getString(R.string.od_no_speech); return }
        busy = true
        val token = generation
        job = scope.launch {
            try {
                val client = OpenAiClient(settings.readKey())
                question = client.transcribe(utterance.wav()).take(2000)
                if (question.isBlank()) { error = context.getString(R.string.od_no_speech); return@launch }
                answer = conversation.answer(question, settings.model, settings.preferences, Locale.getDefault().toLanguageTag(),
                    OpenDriveTools.specs(), client::respond) { name, args ->
                        check(!closed && settings.enabled && settings.hasKey) { "Assistant disabled" }
                        tools.run(name, args)
                    }
                if (settings.enabled && settings.speakReplies) speech.speak(answer)
            } catch (e: CancellationException) { throw e }
            catch (e: AssistantApiException) {
                error = context.getString(when (e.status) {
                    401, 403 -> R.string.od_api_auth
                    429 -> R.string.od_api_limit
                    400, 404 -> R.string.od_api_model
                    else -> R.string.od_api_unavailable
                })
            } catch (_: Exception) { error = context.getString(R.string.od_api_unavailable) }
            finally { if (generation == token) busy = false }
        }
    }

    fun close() { cancelWork(); visible = false }
    fun forget() { cancelWork(); conversation.clear(); tools.clear(); question = ""; answer = ""; error = null }
    fun shutdown() {
        closed = true; settings.unobserve(settingsListener); close()
        conversation.clear(); tools.clear(); speech.close(); scope.cancel()
    }

    private fun cancelWork() {
        generation++
        job?.cancel(); job = null
        timeout?.cancel(); timeout = null
        capture = null
        listening = false
        runtime.ui.listening = false
        mic?.close(); mic = null
        busy = false
        speech.stop()
    }
}
