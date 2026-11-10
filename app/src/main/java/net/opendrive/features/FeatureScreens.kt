package net.opendrive.features

import android.text.format.DateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.prototux.aare.car.CarData
import net.prototux.aare.ui.car.BrowserScreen
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarAppKind
import net.prototux.aare.ui.car.CarScreen

@Composable
fun FeatureScreen(session: FeatureSession, id: String, modifier: Modifier,
                  navContent: @Composable (CarAppEntry, Modifier) -> Unit) {
    val config by session.settings.config.collectAsState()
    val car by CarData.state.collectAsState()
    val ev = EvData.read(car, config, session.home, System.currentTimeMillis())
    val runtime = session.runtime
    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("OpenDrive · ${FeatureSession.labels[id].orEmpty()}", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            if (id != FeatureSession.COCKPIT) TextButton(onClick = { session.open(FeatureSession.COCKPIT) }) { Text("Cockpit") }
            TextButton(onClick = runtime::assistant) { Text("Assistent") }
        }
        if (session.webUrl != null && id == FeatureSession.FAVORITES) {
            TextButton(onClick = { session.webUrl = null }) { Text("Terug naar favorieten") }
            BrowserScreen(runtime, runtime.vehicleMoving, session.webUrl!!, Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        when (id) {
            FeatureSession.COCKPIT -> {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureSession.labels.filterKeys { it != id }.forEach { (key, label) ->
                        OutlinedButton(onClick = { session.open(key) }) { Text(label) }
                    }
                    OutlinedButton(onClick = { runtime.ui.screen = CarScreen.Dashboard }) { Text("Kaartdashboard") }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    LazyVerticalGrid(columns = GridCells.Fixed(if (maxWidth >= 600.dp) 2 else 1),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(config.tiles, key = { it }) { tile ->
                            FeatureCard(tileLabel(tile), Modifier.height(300.dp)) {
                                when (tile) {
                                    "navigation" -> {
                                        val nav = runtime.ui.app(runtime.ui.activeNavAppId)
                                        if (nav != null) {
                                            Box(Modifier.weight(1f).fillMaxWidth()) { navContent(nav, Modifier.fillMaxSize()) }
                                            TextButton(onClick = { runtime.actions.openApp(nav) }) { Text("${nav.label} openen") }
                                        } else {
                                            Text(runtime.ui.phoneGuidance?.let { "${it.primary} ${it.secondary}" } ?: "Kies je navigatieapp.")
                                            runtime.ui.apps.filter { it.kind == CarAppKind.NAVIGATION }.take(3).forEach { app ->
                                                TextButton(onClick = { runtime.actions.openApp(app) }) { Text(app.label) }
                                            }
                                            if (runtime.ui.apps.none { it.kind == CarAppKind.NAVIGATION }) Text("Installeer een ondersteunde navigatieapp of gebruik de bestaande telefoonprojectie.")
                                        }
                                    }
                                    "music" -> {
                                        Text(runtime.ui.nowPlaying?.title ?: "Kies je muziekapp", maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge)
                                        Text(runtime.ui.nowPlaying?.subtitle.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = runtime.media::previous) { Text("Vorige") }
                                            Button(onClick = runtime.media::playPause) { Text(if (runtime.ui.nowPlaying?.playing == true) "Pauze" else "Afspelen") }
                                            TextButton(onClick = runtime.media::next) { Text("Volgende") }
                                        }
                                        TextButton(onClick = {
                                            val app = runtime.ui.app(runtime.ui.activeMediaAppId) ?: runtime.ui.apps.firstOrNull { it.kind == CarAppKind.MEDIA }
                                            if (app != null) runtime.actions.openApp(app) else runtime.actions.openLauncher()
                                        }) { Text("Muziekbibliotheek") }
                                    }
                                    "ev" -> {
                                        Text(ev.soc?.let { "${format(it.value)}% · ${it.source}" } ?: "Accuniveau niet beschikbaar", style = MaterialTheme.typography.titleLarge)
                                        Text(ev.rangeKm?.let { "Actieradius: ${format(it.value)} km · ${it.source}" }
                                            ?: ev.estimatedRangeKm?.let { "Geschatte actieradius: ${format(it)} km" } ?: "Actieradius onbekend")
                                        Text(ev.powerKw?.let { "Laadvermogen: ${format(it.value, 1)} kW" } ?: "Laadvermogen onbekend")
                                        TextButton(onClick = { session.open(FeatureSession.EV) }) { Text("EV-dashboard openen") }
                                    }
                                    "favorites" -> {
                                        config.destinations.filter { it.address.isNotBlank() }.take(2).forEach { destination ->
                                            Button(onClick = { runtime.actions.navigateTo(destination.address) }) { Text(destination.label.ifBlank { destination.address }, maxLines = 1) }
                                        }
                                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            runtime.ui.apps.filter { it.packageName in config.pinnedApps && !it.id.startsWith("opendrive:") }.take(3).forEach { app ->
                                                OutlinedButton(onClick = { runtime.actions.openApp(app) }) { Text(app.label, maxLines = 1) }
                                            }
                                        }
                                        TextButton(onClick = { session.open(FeatureSession.FAVORITES) }) { Text("Alle favorieten en diensten") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            FeatureSession.EV -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { EvOverview(ev, config) }
                item { FeatureCard("Gegevensbron") {
                    Text(if (car.connected) "${car.identity?.make.orEmpty()} ${car.identity?.model.orEmpty()} · verbonden" else "Geen auto verbonden; er worden geen voertuigwaarden gesimuleerd.")
                    Text("Alleen gerapporteerde waarden worden getoond. Handmatige invoer is herkenbaar gemarkeerd. Configureer op de telefoon eventueel Home Assistant-sensoren voor extra accugegevens.")
                    Text(session.home.error.orEmpty())
                    if (config.homeEnabled) TextButton(onClick = session::refreshHome) { Text("Home Assistant verversen") }
                } }
            }
            FeatureSession.FAVORITES -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { FeatureCard("Bestemmingen") {
                    config.destinations.filter { it.address.isNotBlank() }.forEach { destination ->
                        Button(onClick = { runtime.actions.navigateTo(destination.address) }) { Text(destination.label.ifBlank { destination.address }) }
                    }
                    if (config.destinations.none { it.address.isNotBlank() }) Text("Voeg op de telefoon je favoriete bestemmingen toe.")
                } }
                item { FeatureCard("Eigen apps en media") {
                    val favorites = runtime.ui.apps.filter { it.packageName in config.pinnedApps && !it.id.startsWith("opendrive:") }
                    favorites.take(8).forEach { app -> Button(onClick = { runtime.actions.openApp(app) }) { Text(app.label) } }
                    if (favorites.isEmpty()) Text("Kies op de telefoon je eigen navigatie- en muziekapps. AARE ondersteunt ook eigen mediaservers via geschikte media-apps.")
                    TextButton(onClick = runtime.actions::openLauncher) { Text("Alle apps") }
                } }
                item { FeatureCard("Webdiensten · geparkeerd") {
                    config.websites.filter { it.url.isNotBlank() }.forEach { site ->
                        Button(enabled = !runtime.vehicleMoving, onClick = { session.openWeb(site.url) }) { Text(site.label.ifBlank { site.url }) }
                    }
                    if (config.websites.none { it.url.isNotBlank() }) Text("Voeg op de telefoon HTTPS-webfavorieten toe, bijvoorbeeld voor je eigen diensten.")
                } }
            }
            FeatureSession.HOME -> HomeControls(session, config, Modifier.weight(1f))
            FeatureSession.TRIPS -> {
                val records by session.trips.records.collectAsState()
                Text(if (session.tripRecording) "Ritopname actief · alleen op deze telefoon" else if (config.tripEnabled) "Ritopname staat aan; wacht op een autoverbinding." else "Ritopname staat uit. Zet deze op de telefoon aan als je ritten wilt bewaren.")
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(records.take(if (runtime.vehicleMoving) 6 else 30), key = { it.id }) { record ->
                        FeatureCard(tripTitle(record)) {
                            Text("${if (record.business) "Zakelijk" else "Privé"} · ${(record.end - record.start) / 60_000} min · ${record.km?.let { "${format(it, 1)} km" } ?: "afstand niet ontvangen"}")
                            if (record.km != null && record.costPerKm != null) Text("Ingestelde kilometerkosten: € ${format(record.km * record.costPerKm, 2)}")
                        }
                    }
                    item { Text("Categoriseren, verwijderen en CSV-export zijn beschikbaar op de telefoon. Alleen kilometertellergegevens worden voor de afstand gebruikt; er wordt geen route opgeslagen.") }
                }
            }
            FeatureSession.PASSENGER -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("De bijrijder kan muziek bedienen en bestemmingen voorstellen. Jij bevestigt een bestemming op het autoscherm.")
                Text("Verbind beide telefoons met hetzelfde vertrouwde wifi-/hotspotnetwerk. Kopieer de tijdelijke link op de telefoon bij OpenDrive → Bijrijder. De verbinding verloopt na 30 minuten.")
                Button(enabled = !runtime.vehicleMoving, onClick = session::startCompanion) { Text("Bijrijderverbinding starten") }
                if (session.pairingLinks.isNotEmpty()) {
                    Text("Actief. De geheime koppelingslink staat op je telefoon.")
                    Button(onClick = session::stopCompanion) { Text("Verbinding stoppen") }
                }
                Text(session.message)
            }
            FeatureSession.CHARGING -> {
                if (runtime.vehicleMoving) {
                    Text("Het laadpauzescherm is beschikbaar als je geparkeerd staat.")
                } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { EvOverview(ev, config) }
                    item { FeatureCard("Pauzetimer") {
                        if (session.timerSeconds > 0) Text("${session.timerSeconds / 60}:${(session.timerSeconds % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.headlineLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(10, 20, 30).forEach { minutes -> Button(onClick = { session.startTimer(minutes) }) { Text("$minutes min") } }
                        }
                        TextButton(onClick = session::cancelTimer) { Text("Timer stoppen") }
                        Text("Een pauzeherinnering, geen laadregeling. De timer werkt zolang deze OpenDrive-sessie actief is.")
                    } }
                    item { FeatureCard("Ontspanning tijdens parkeren") {
                        config.websites.filter { it.url.isNotBlank() }.forEach { site ->
                            Button(onClick = { session.openWeb(site.url) }) { Text(site.label.ifBlank { site.url }) }
                        }
                        Text("Gebruik je ingestelde webdiensten. Afspelen van video hangt af van de website en Android WebView.")
                        TextButton(onClick = { session.open(FeatureSession.FAVORITES) }) { Text("Favorieten openen") }
                    } }
                }
            }
        }
    }
}

@Composable
private fun HomeControls(session: FeatureSession, config: FeatureConfig, modifier: Modifier) {
    var pending by remember { mutableStateOf<HomeAction?>(null) }
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(if (config.homeEnabled) "Home Assistant" else "Stel Home Assistant op de telefoon in om je eigen knoppen te gebruiken.") }
        item { if (session.homeBusy) LinearProgressIndicator(Modifier.fillMaxWidth()) }
        items(config.homeActions.filter { it.entity.isNotBlank() }) { action ->
            FeatureCard(action.label.ifBlank { action.entity }) {
                Text(session.home.values[action.entity]?.state ?: "Status onbekend")
                Button(enabled = config.homeEnabled && !session.homeActionBusy, onClick = { pending = action }) { Text("Uitvoeren") }
            }
        }
        item { Text(session.home.error.orEmpty()); Text(session.message) }
        item { TextButton(onClick = session::refreshHome) { Text("Status verversen") } }
    }
    pending?.let { action -> AlertDialog(onDismissRequest = { pending = null }, title = { Text(action.label.ifBlank { action.entity }) },
        text = { Text("${action.service} uitvoeren voor ${action.entity}?") },
        confirmButton = { TextButton(onClick = { pending = null; session.runHomeAction(action) }) { Text("Uitvoeren") } },
        dismissButton = { TextButton(onClick = { pending = null }) { Text("Annuleren") } }) }
}

@Composable
private fun EvOverview(ev: EvData, config: FeatureConfig) {
    FeatureCard("Accu en laden") {
        ValueLine("Accu", ev.soc, "%")
        ev.soc?.let { LinearProgressIndicator(progress = { (it.value / 100).toFloat() }, modifier = Modifier.fillMaxWidth()) }
        ValueLine("Actieradius", ev.rangeKm, "km")
        ValueLine("Laadvermogen", ev.powerKw, "kW")
        ValueLine("Accutemperatuur", ev.temperatureC, "°C")
        ev.estimatedRangeKm?.let { Text("Geschatte actieradius: ${format(it)} km · op basis van jouw capaciteit en verbruik") }
        ev.minutesToTarget?.let { Text("Rekenschatting tot ${config.targetSoc}%: ${format(it)} min bij gelijkblijvend vermogen. De laadcurve is niet meegerekend.") }
        if (ev.soc == null) Text("Geen accugegevens ontvangen. Stel eventueel een Home Assistant-bron in of vul op de telefoon een handmatig accupercentage in.")
        if (ev.soc?.source == "Handmatig") Text("Handmatige momentopname; wordt niet automatisch bijgewerkt.", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable private fun ValueLine(label: String, value: EvValue?, unit: String) {
    val context = LocalContext.current
    val time = value?.at?.let { " (${DateFormat.getTimeFormat(context).format(it)})" }.orEmpty()
    Text("$label: ${value?.let { "${format(it.value, 1)} $unit · ${it.source}$time" } ?: "niet beschikbaar"}", style = MaterialTheme.typography.titleMedium)
}
@Composable internal fun FeatureCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
internal fun tileLabel(key: String) = when (key) { "navigation" -> "Navigatie"; "music" -> "Muziek"; "ev" -> "EV"; else -> "Favorieten" }
internal fun format(value: Double, decimals: Int = 0) = "%.${decimals}f".format(value)
@Composable internal fun tripTitle(record: TripRecord): String {
    val context = LocalContext.current
    return DateFormat.getDateFormat(context).format(record.start) + " · " + DateFormat.getTimeFormat(context).format(record.start)
}
