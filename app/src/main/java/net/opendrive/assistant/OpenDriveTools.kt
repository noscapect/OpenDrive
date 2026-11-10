package net.opendrive.assistant

import androidx.media3.common.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.host.CarTools
import net.prototux.aare.media.DefaultMediaHub
import net.prototux.aare.media.MediaConnection
import net.prototux.aare.media.isPlayableItem
import net.prototux.aare.media.subtitleText
import net.prototux.aare.media.titleText
import net.prototux.aare.ui.car.CarAppKind
import net.prototux.aare.ui.car.CarScreen
import org.json.JSONArray
import org.json.JSONObject

/** An explicit allow-list; the assistant cannot access messages, call contacts or execute arbitrary intents. */
internal class OpenDriveTools(private val runtime: CarRuntime) {
    private var results: List<MediaItem> = emptyList()
    private var resultConnection: MediaConnection? = null

    fun clear() { results = emptyList(); resultConnection = null }

    suspend fun run(name: String, args: JSONObject): JSONObject {
        if (!validate(name, args)) return failure("Unknown tool or invalid arguments")
        return when (name) {
            "car_status" -> CarTools.run(runtime, name, args)
            "car_navigate" -> CarTools.run(runtime, name, args)
            "car_media" -> {
                if (runtime.media.playback.value == null) failure("No active media player. Choose a music app or search for music first.")
                else {
                    CarTools.run(runtime, name, args)
                    JSONObject().put("ok", true).put("result", "Requested ${args.getString("action")}; check car_status to confirm playback.")
                }
            }
            "opendrive_music_search" -> search(args.getString("query"), args.getString("app"))
            "opendrive_music_play" -> play(args.getInt("number"))
            "opendrive_apps" -> JSONObject()
                .put("navigation", JSONArray(runtime.ui.apps.filter { it.kind == CarAppKind.NAVIGATION }.map { it.label }))
                .put("music", JSONArray(runtime.ui.apps.filter { it.kind == CarAppKind.MEDIA }.map { it.label }))
            else -> failure("Tool not available")
        }
    }

    private suspend fun search(query: String, appName: String): JSONObject {
        clear()
        val hub = runtime.media as? DefaultMediaHub ?: return failure("Music search is unavailable")
        val visible = runtime.ui.apps.filter { it.kind == CarAppKind.MEDIA }.map { it.packageName }.toSet()
        val apps = hub.apps.value.filter { it.packageName in visible }
        val candidates = if (appName.isBlank()) {
            val preferred = hub.playback.value?.packageName ?: runtime.ui.app(runtime.ui.activeMediaAppId)?.packageName
                ?: runtime.settings.lastMediaApp
            apps.filter { it.packageName == preferred }.ifEmpty { apps }
        } else apps.filter { it.label.equals(appName.trim(), true) }
            .ifEmpty { apps.filter { it.label.contains(appName.trim(), true) } }
        val app = candidates.singleOrNull() ?: return failure("Choose one music app: ${apps.joinToString { it.label }}")
        hub.select(app)
        if (!hub.awaitConnection(8000)) return failure("Cannot connect to ${app.label}")
        val connection = hub.connection.value ?: return failure("Music app disconnected")
        return try {
            val found = withTimeoutOrNull(10_000) { connection.search(query, 8) }
                ?: return failure("Music search timed out")
            results = found.filter { it.isPlayableItem }.take(8)
            resultConnection = connection
            runtime.ui.activeMediaAppId = CarRuntime.mediaId(app.packageName)
            JSONObject().put("app", app.label).put("results", JSONArray(results.mapIndexed { index, item ->
                JSONObject().put("number", index + 1).put("title", item.titleText.take(200)).put("artist", item.subtitleText.take(200))
            })).put("count", results.size)
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { clear(); failure("This music app could not search. Check its connection and sign-in on the phone.") }
    }

    private fun play(number: Int): JSONObject {
        val hub = runtime.media as? DefaultMediaHub ?: return failure("Music unavailable")
        val connection = resultConnection ?: return failure("Search for music first")
        if (hub.connection.value !== connection || !connection.connected) { clear(); return failure("Search results have expired") }
        val item = results.getOrNull(number - 1) ?: return failure("Choose a number from the last search results")
        connection.play(item)
        runtime.ui.activeMediaAppId = CarRuntime.mediaId(connection.app.packageName)
        runtime.ui.screen = CarScreen.App(CarRuntime.mediaId(connection.app.packageName))
        return JSONObject().put("ok", true).put("result", "Playback requested; the music app must confirm playback")
            .put("title", item.titleText.take(200))
    }

    companion object {
        fun specs(): JSONArray = JSONArray().apply {
            val carSpecs = CarTools.specs()
            for (i in 0 until carSpecs.length()) {
                val spec = carSpecs.getJSONObject(i)
                val name = spec.getString("name")
                if (name !in setOf("car_status", "car_navigate", "car_media")) continue
                put(function(name, spec.getString("description"), spec.getJSONObject("parameters").getJSONObject("properties")))
            }
            put(function("opendrive_apps", "List the available navigation and music apps.", JSONObject()))
            put(function("opendrive_music_search", "Search an installed music or podcast app. Returns numbered playable results; does not play yet.",
                JSONObject().put("query", string("Artist, title or podcast to search for"))
                    .put("app", string("Music app name, or empty string to use the current app. Ask if ambiguous."))))
            put(function("opendrive_music_play", "Request playback of a numbered item from the last music search. Never guess a result number.",
                JSONObject().put("number", JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 8))))
        }

        private fun string(description: String) = JSONObject().put("type", "string").put("description", description)
        private fun function(name: String, description: String, properties: JSONObject): JSONObject = JSONObject()
            .put("type", "function").put("name", name).put("description", description).put("strict", true)
            .put("parameters", JSONObject().put("type", "object").put("properties", properties)
                .put("required", JSONArray(properties.keys().asSequence().toList())).put("additionalProperties", false))

        internal fun validate(name: String, args: JSONObject): Boolean {
            val keys = args.keys().asSequence().toSet()
            fun text(key: String) = args.opt(key) is String && args.getString(key).isNotBlank() && args.getString(key).length <= 500
            return when (name) {
                "car_status", "opendrive_apps" -> keys.isEmpty()
                "car_navigate" -> keys == setOf("destination") && text("destination")
                "car_media" -> keys == setOf("action") && args.opt("action") in setOf("play", "pause", "next", "previous")
                "opendrive_music_search" -> keys == setOf("query", "app") && text("query") &&
                    args.opt("app") is String && args.getString("app").length <= 200
                "opendrive_music_play" -> keys == setOf("number") && args.opt("number") is Int && args.getInt("number") in 1..8
                else -> false
            }
        }

        private fun failure(message: String) = JSONObject().put("ok", false).put("error", message)
    }
}
