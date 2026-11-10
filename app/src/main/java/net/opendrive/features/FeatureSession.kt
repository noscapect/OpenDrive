package net.opendrive.features

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.prototux.aare.car.CarData
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarAppKind
import net.prototux.aare.ui.car.CarScreen
import java.net.NetworkInterface

/** Owns feature work for the lifetime of a real car session or preview. */
class FeatureSession(val runtime: CarRuntime, parent: CoroutineScope, private val primary: Boolean) {
    val settings = FeatureSettings.get(runtime.context)
    val trips = TripStore.get(runtime.context)
    private val secrets = FeatureSecrets(runtime.context)
    private val scope = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private val main = Handler(Looper.getMainLooper())
    private var recording: TripAccumulator? = null
    private var homeJob: Job? = null
    private var homeGeneration = 0
    private var companion: CompanionServer? = null
    private var companionStarted = 0L
    private var timerEnd = 0L
    private var stopped = false
    var home by mutableStateOf(HomeSnapshot()); private set
    var homeBusy by mutableStateOf(false); private set
    var homeActionBusy by mutableStateOf(false); private set
    var message by mutableStateOf(""); private set
    var pairingLinks by mutableStateOf<List<String>>(emptyList()); private set
    var timerSeconds by mutableStateOf(0L); private set
    var tripRecording by mutableStateOf(false); private set
    var webUrl by mutableStateOf<String?>(null)

    fun start() {
        sessions += this
        var lastHome = ""
        var lastRefresh = 0L
        var lastRetention = -1
        scope.launch {
            while (true) {
                val config = settings.config.value
                val now = System.currentTimeMillis()
                val car = CarData.state.value
                if (lastRetention != config.retentionDays) {
                    runCatching { trips.prune(config.retentionDays) }.onFailure { message = "Ritten konden niet worden bijgewerkt." }
                    lastRetention = config.retentionDays
                }
                if (primary && CarRuntime.active === runtime && config.tripEnabled && car.connected) {
                    if (recording == null) recording = TripAccumulator(now, car.odometer?.value?.km?.toDouble(), config,
                        car.identity?.let { "${it.make} ${it.model}" } ?: runtime.ui.headUnitName)
                    recording?.sample(car.odometer?.value?.km?.toDouble())
                } else if (!config.tripEnabled) recording = null
                else if (recording != null && (!car.connected || CarRuntime.active !== runtime)) finishTrip()
                tripRecording = recording != null
                val homeKey = config.json().toString()
                if (!config.homeEnabled) {
                    homeGeneration++
                    homeJob?.cancel(); homeJob = null; homeBusy = false; home = HomeSnapshot()
                } else if (homeKey != lastHome || now - lastRefresh >= 30_000) {
                    refreshHome(); lastHome = homeKey; lastRefresh = now
                }
                if (timerEnd > 0) {
                    timerSeconds = ((timerEnd - SystemClock.elapsedRealtime()).coerceAtLeast(0) + 999) / 1000
                    if (timerSeconds == 0L) {
                        timerEnd = 0
                        runtime.notice("OpenDrive", "Je laadpauzetimer is afgelopen.", durationMs = 15_000)
                    }
                }
                if (companion != null && SystemClock.elapsedRealtime() - companionStarted >= 30 * 60_000) stopCompanion()
                companion?.publish(org.json.JSONObject().put("title", runtime.ui.nowPlaying?.title.orEmpty())
                    .put("playing", runtime.ui.nowPlaying?.playing == true))
                delay(1000)
            }
        }
    }

    fun refreshHome() {
        val config = settings.config.value
        val generation = ++homeGeneration
        homeJob?.cancel()
        if (!config.homeEnabled || config.homeUrl.isBlank() || !secrets.hasToken) {
            home = HomeSnapshot(error = if (config.homeEnabled) "Stel de URL en toegangstoken op de telefoon in." else null)
            homeBusy = false; return
        }
        homeJob = scope.launch {
            homeBusy = true
            try {
                val client = HomeAssistantClient(config.homeUrl, secrets.read())
                val entities = (listOf(config.socEntity, config.rangeEntity, config.powerEntity, config.temperatureEntity) +
                    config.homeActions.map { it.entity }).filter { it.isNotBlank() }.distinct()
                val readings = coroutineScope {
                    entities.map { entity -> async {
                        try { entity to client.state(entity) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { null }
                    } }.awaitAll()
                }
                val values = readings.filterNotNull().toMap()
                val failures = entities.size - values.size
                home = HomeSnapshot(values, if (failures > 0) "$failures entiteit(en) niet beschikbaar. Controleer instellingen en verbinding." else null)
                if (entities.isEmpty()) { client.ping(); message = "Verbinding met Home Assistant gelukt; voeg entiteiten of acties toe." }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { home = HomeSnapshot(error = "Home Assistant niet bereikbaar. Controleer HTTPS-URL, token en verbinding.") }
            finally { if (homeGeneration == generation) homeBusy = false }
        }
    }

    fun runHomeAction(action: HomeAction) {
        val config = settings.config.value
        if (!config.homeEnabled || homeActionBusy || action !in config.homeActions || action.entity.isBlank()) return
        homeActionBusy = true
        scope.launch {
            try {
                HomeAssistantClient(config.homeUrl, secrets.read()).action(action)
                message = "Actie aangevraagd: ${action.label.ifBlank { action.entity }}"
                refreshHome()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "Actie niet bevestigd. Controleer Home Assistant voordat je opnieuw probeert." }
            finally { homeActionBusy = false }
        }
    }

    fun startCompanion() {
        stopCompanion()
        try {
            lateinit var server: CompanionServer
            server = CompanionServer { action -> main.post {
                if (stopped || companion !== server) return@post
                when (action.getString("action")) {
                    "destination" -> {
                        val destination = action.getString("destination")
                        runtime.notice("Bijrijder stelt voor", destination, durationMs = 30_000,
                            actions = listOf("Gebruiken" to { if (!stopped) runtime.actions.navigateTo(destination) }, "Negeren" to {}))
                    }
                    "play_pause" -> runtime.media.playPause()
                    "next" -> runtime.media.next()
                    "previous" -> runtime.media.previous()
                }
            } }
            val addresses = NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }.filter { it.address.size == 4 && it.isSiteLocalAddress }
                .mapNotNull { it.hostAddress }.distinct()
            if (addresses.isEmpty()) { server.close(); message = "Verbind telefoon en bijrijder met hetzelfde lokale netwerk."; return }
            companion = server; companionStarted = SystemClock.elapsedRealtime()
            pairingLinks = addresses.map { "http://$it:${server.port}/#${server.token}" }
            server.start()
            message = "Bijrijderbediening actief voor maximaal 30 minuten."
        } catch (_: Exception) { stopCompanion(); message = "Bijrijderverbinding kon niet starten. Controleer lokale netwerktoegang." }
    }
    fun stopCompanion() { companion?.close(); companion = null; pairingLinks = emptyList() }
    fun startTimer(minutes: Int) { timerEnd = SystemClock.elapsedRealtime() + minutes.coerceIn(1, 180) * 60_000L; timerSeconds = minutes * 60L }
    fun cancelTimer() { timerEnd = 0; timerSeconds = 0 }
    fun open(id: String) { runtime.ui.screen = CarScreen.App(id) }
    fun openWeb(url: String) { if (FeatureConfig.validWebUrl(url)) { webUrl = url; open(FAVORITES) } }
    private fun finishTrip() {
        val trip = recording ?: return
        recording = null
        if (CarRuntime.active === runtime) trip.sample(CarData.state.value.odometer?.value?.km?.toDouble())
        runCatching { trips.add(trip.finish(System.currentTimeMillis()), settings.config.value.retentionDays) }
            .onFailure { message = "Rit opslaan mislukt. Controleer de opslag op je telefoon." }
    }
    fun stop() {
        stopped = true
        if (settings.config.value.tripEnabled) finishTrip() else recording = null
        stopCompanion(); cancelTimer(); sessions.remove(this); scope.cancel()
    }

    companion object {
        const val COCKPIT = "opendrive:cockpit"
        const val EV = "opendrive:ev"
        const val TRIPS = "opendrive:trips"
        const val HOME = "opendrive:home"
        const val CHARGING = "opendrive:charging"
        const val PASSENGER = "opendrive:passenger"
        const val FAVORITES = "opendrive:favorites"
        val labels = linkedMapOf(COCKPIT to "Cockpit", EV to "EV-dashboard", FAVORITES to "Favorieten", HOME to "Thuis", TRIPS to "Rittenboek", PASSENGER to "Bijrijder", CHARGING to "Laadpauze")
        private val sessions = ArrayList<FeatureSession>()
        val active: FeatureSession? get() = sessions.lastOrNull { it.primary } ?: sessions.lastOrNull()
        fun entries(context: android.content.Context, icon: android.graphics.Bitmap?): List<CarAppEntry> = labels.map { (id, label) ->
            CarAppEntry(id, context.packageName, label, icon, CarAppKind.SETTINGS)
        }
    }
}
