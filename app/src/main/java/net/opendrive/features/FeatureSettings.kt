package net.opendrive.features

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

data class Destination(val label: String = "", val address: String = "")
data class WebFavorite(val label: String = "", val url: String = "")
data class HomeAction(val label: String = "", val entity: String = "", val service: String = "light.turn_on")

data class FeatureConfig(
    val cockpitHome: Boolean = true,
    val tiles: List<String> = listOf("navigation", "music", "ev", "favorites"),
    val destinations: List<Destination> = List(3) { Destination() },
    val websites: List<WebFavorite> = List(2) { WebFavorite() },
    val pinnedApps: Set<String> = emptySet(),
    val capacity: String = "",
    val consumption: String = "",
    val manualSoc: String = "",
    val vehicleBattery: Boolean = false,
    val targetSoc: String = "80",
    val tripEnabled: Boolean = false,
    val businessTrips: Boolean = false,
    val costPerKm: String = "",
    val retentionDays: Int = 90,
    val homeUrl: String = "",
    val homeEnabled: Boolean = false,
    val socEntity: String = "",
    val rangeEntity: String = "",
    val powerEntity: String = "",
    val temperatureEntity: String = "",
    val homeActions: List<HomeAction> = List(3) { HomeAction() },
) {
    fun validate(): String? {
        fun number(value: String, range: ClosedFloatingPointRange<Double>) = value.isBlank() || decimal(value)?.let { it in range } == true
        if (!number(capacity, 1.0..300.0) || !number(consumption, 1.0..100.0) || !number(manualSoc, 0.0..100.0) ||
            !number(targetSoc, 1.0..100.0) || !number(costPerKm, 0.0..100.0)) return "Controleer de getallen en eenheden."
        if (tiles.isEmpty() || tiles.any { it !in TILE_NAMES } || tiles.distinct().size != tiles.size) return "Kies minstens één cockpittegel."
        if (websites.any { it.url.isNotBlank() && !validWebUrl(it.url) }) return "Gebruik voor webfavorieten een volledige https://-URL."
        if (homeUrl.isNotBlank() && !validHomeUrl(homeUrl)) return "Home Assistant: gebruik een HTTPS-URL zonder pad, bijvoorbeeld je Nabu Casa-adres."
        val entities = listOf(socEntity, rangeEntity, powerEntity, temperatureEntity)
        if (entities.any { it.isNotBlank() && !validEntity(it) }) return "Ongeldige Home Assistant-entiteit."
        if (homeActions.any { it.entity.isNotBlank() && (!validEntity(it.entity) || it.service !in HOME_SERVICES ||
                it.entity.substringBefore('.') != it.service.substringBefore('.')) }) return "Controleer entiteit en actie: de domeinen moeten overeenkomen."
        if (homeEnabled && homeUrl.isBlank()) return "Vul eerst de Home Assistant-URL in."
        return null
    }

    fun json() = JSONObject().put("cockpitHome", cockpitHome).put("tiles", JSONArray(tiles))
        .put("destinations", JSONArray(destinations.map { JSONObject().put("label", it.label).put("address", it.address) }))
        .put("websites", JSONArray(websites.map { JSONObject().put("label", it.label).put("url", it.url) }))
        .put("pinnedApps", JSONArray(pinnedApps.toList())).put("capacity", capacity).put("consumption", consumption)
        .put("manualSoc", manualSoc).put("vehicleBattery", vehicleBattery).put("targetSoc", targetSoc)
        .put("tripEnabled", tripEnabled).put("businessTrips", businessTrips).put("costPerKm", costPerKm).put("retentionDays", retentionDays)
        .put("homeUrl", homeUrl).put("homeEnabled", homeEnabled).put("socEntity", socEntity).put("rangeEntity", rangeEntity)
        .put("powerEntity", powerEntity).put("temperatureEntity", temperatureEntity)
        .put("homeActions", JSONArray(homeActions.map { JSONObject().put("label", it.label).put("entity", it.entity).put("service", it.service) }))

    companion object {
        val TILE_NAMES = listOf("navigation", "music", "ev", "favorites")
        val HOME_SERVICES = listOf("light.turn_on", "light.turn_off", "switch.turn_on", "switch.turn_off", "cover.open_cover", "cover.close_cover", "scene.turn_on", "script.turn_on")
        fun decimal(value: String): Double? = value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
        fun validEntity(value: String) = value.matches(Regex("[a-z][a-z0-9_]*\\.[a-z0-9_]+"))
        fun validWebUrl(value: String): Boolean = runCatching {
            val uri = URI(value.trim())
            uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.rawUserInfo == null
        }.getOrDefault(false)
        fun validHomeUrl(value: String): Boolean = runCatching {
            val uri = URI(value.trim())
            val host = uri.host ?: return false
            uri.scheme == "https" && host.isNotBlank() && uri.rawUserInfo == null &&
                uri.query == null && uri.fragment == null && (uri.path.isNullOrBlank() || uri.path == "/")
        }.getOrDefault(false)
        fun fromJson(o: JSONObject): FeatureConfig {
            fun strings(name: String): List<String> = o.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
            return FeatureConfig(
                cockpitHome = o.optBoolean("cockpitHome", true),
                tiles = strings("tiles").filter { it in TILE_NAMES }.distinct().ifEmpty { TILE_NAMES },
                destinations = List(3) { i -> o.optJSONArray("destinations")?.optJSONObject(i)?.let { Destination(it.optString("label"), it.optString("address")) } ?: Destination() },
                websites = List(2) { i -> o.optJSONArray("websites")?.optJSONObject(i)?.let { WebFavorite(it.optString("label"), it.optString("url")) } ?: WebFavorite() },
                pinnedApps = strings("pinnedApps").toSet(), capacity = o.optString("capacity"), consumption = o.optString("consumption"),
                manualSoc = o.optString("manualSoc"), vehicleBattery = o.optBoolean("vehicleBattery"), targetSoc = o.optString("targetSoc", "80"),
                tripEnabled = o.optBoolean("tripEnabled"), businessTrips = o.optBoolean("businessTrips"), costPerKm = o.optString("costPerKm"),
                retentionDays = o.optInt("retentionDays", 90).coerceIn(1, 365), homeUrl = o.optString("homeUrl"), homeEnabled = o.optBoolean("homeEnabled"),
                socEntity = o.optString("socEntity"), rangeEntity = o.optString("rangeEntity"), powerEntity = o.optString("powerEntity"), temperatureEntity = o.optString("temperatureEntity"),
                homeActions = List(3) { i -> o.optJSONArray("homeActions")?.optJSONObject(i)?.let { HomeAction(it.optString("label"), it.optString("entity"), it.optString("service", "light.turn_on")) } ?: HomeAction() },
            )
        }
    }
}

class FeatureSettings private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("opendrive_features", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(runCatching { FeatureConfig.fromJson(JSONObject(prefs.getString("config", "{}")!!)) }.getOrDefault(FeatureConfig()))
    val config: StateFlow<FeatureConfig> = state
    fun save(config: FeatureConfig) {
        require(config.validate() == null) { config.validate().orEmpty() }
        prefs.edit().putString("config", config.json().toString()).apply()
        state.value = config
    }
    companion object {
        @Volatile private var instance: FeatureSettings? = null
        fun get(context: Context): FeatureSettings = instance ?: synchronized(this) {
            instance ?: FeatureSettings(context.applicationContext).also { instance = it }
        }
    }
}
