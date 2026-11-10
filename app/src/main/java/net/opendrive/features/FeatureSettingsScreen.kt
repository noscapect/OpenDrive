package net.opendrive.features

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.host.CarAppCatalog

/** All long-form setup and exports happen on the phone, never through the car keyboard. */
@Composable
fun FeatureSettingsScreen() {
    val context = LocalContext.current
    val settings = remember { FeatureSettings.get(context) }
    val secrets = remember { FeatureSecrets(context) }
    val trips = remember { TripStore.get(context) }
    val records by trips.records.collectAsState()
    var config by remember { mutableStateOf(settings.config.value) }
    var tab by remember { mutableIntStateOf(0) }
    var token by remember { mutableStateOf("") }
    var hasToken by remember { mutableStateOf(secrets.hasToken) }
    var message by remember { mutableStateOf("") }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var export by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val session by produceState<FeatureSession?>(FeatureSession.active) {
        while (true) { value = FeatureSession.active; delay(1000) }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    checkNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter(Charsets.UTF_8).use { it.write(export) }
                }
                message = "Rittenboek geëxporteerd."
            } catch (_: Exception) { message = "Export mislukt. Controleer de gekozen opslaglocatie." }
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Jouw OpenDrive", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Cockpit", "EV", "Favorieten", "Thuis", "Ritten", "Bijrijder").forEachIndexed { index, title ->
                FilterChip(selected = tab == index, onClick = { tab = index; message = "" }, label = { Text(title) })
            }
        }
        when (tab) {
            0 -> {
                Toggle("Cockpit als startscherm", config.cockpitHome) { config = config.copy(cockpitHome = it) }
                Text("Kies je tegels en volgorde. Je kunt de kaart verbergen en alleen muziek, EV en favorieten tonen.")
                FeatureConfig.TILE_NAMES.forEach { tile ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = tile in config.tiles, onCheckedChange = { enabled ->
                            config = config.copy(tiles = if (enabled) config.tiles + tile else config.tiles - tile)
                        })
                        Text(tileLabel(tile), Modifier.weight(1f))
                        val index = config.tiles.indexOf(tile)
                        TextButton(enabled = index > 0, onClick = {
                            val ordered = config.tiles.toMutableList(); java.util.Collections.swap(ordered, index, index - 1)
                            config = config.copy(tiles = ordered)
                        }) { Text("Omhoog") }
                        TextButton(enabled = index >= 0 && index < config.tiles.lastIndex, onClick = {
                            val ordered = config.tiles.toMutableList(); java.util.Collections.swap(ordered, index, index + 1)
                            config = config.copy(tiles = ordered)
                        }) { Text("Omlaag") }
                    }
                }
            }
            1 -> {
                Text("Onbekende voertuigwaarden blijven leeg. Een hybride brandstofpercentage wordt niet automatisch als accupercentage gebruikt.")
                Field("Bruikbare accucapaciteit (kWh, optioneel)", config.capacity) { config = config.copy(capacity = it) }
                Field("Gemiddeld verbruik (kWh/100 km, optioneel)", config.consumption) { config = config.copy(consumption = it) }
                Field("Handmatige accu-momentopname (%, optioneel)", config.manualSoc) { config = config.copy(manualSoc = it) }
                Field("Laaddoel (%)", config.targetSoc) { config = config.copy(targetSoc = it) }
                Toggle("Het energieniveau van mijn auto is het accupercentage", config.vehicleBattery) { config = config.copy(vehicleBattery = it) }
                Text("Actieradius en laadtijd worden alleen geschat als voldoende invoer beschikbaar is. Voor laadvermogen en accutemperatuur kun je hieronder Home Assistant-entiteiten instellen.")
                Field("Accupercentage-entiteit (eenheid %)", config.socEntity) { config = config.copy(socEntity = it.trim()) }
                Field("Actieradius-entiteit (km)", config.rangeEntity) { config = config.copy(rangeEntity = it.trim()) }
                Field("Laadvermogen-entiteit (kW of W)", config.powerEntity) { config = config.copy(powerEntity = it.trim()) }
                Field("Accutemperatuur-entiteit (°C)", config.temperatureEntity) { config = config.copy(temperatureEntity = it.trim()) }
            }
            2 -> {
                config.destinations.forEachIndexed { index, item ->
                    FeatureCard("Bestemming ${index + 1}") {
                        Field("Naam, bijvoorbeeld Thuis", item.label) { value -> config = config.copy(destinations = config.destinations.mapIndexed { i, d -> if (i == index) d.copy(label = value) else d }) }
                        Field("Adres of plaats", item.address) { value -> config = config.copy(destinations = config.destinations.mapIndexed { i, d -> if (i == index) d.copy(address = value) else d }) }
                    }
                }
                config.websites.forEachIndexed { index, item ->
                    FeatureCard("Webdienst ${index + 1} · geparkeerd") {
                        Field("Naam", item.label) { value -> config = config.copy(websites = config.websites.mapIndexed { i, s -> if (i == index) s.copy(label = value) else s }) }
                        Field("HTTPS-URL", item.url) { value -> config = config.copy(websites = config.websites.mapIndexed { i, s -> if (i == index) s.copy(url = value.trim()) else s }) }
                    }
                }
                Text("Vaste apps op je cockpit", style = MaterialTheme.typography.titleMedium)
                val apps by produceState<List<Pair<String, String>>>(emptyList()) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            val packages = CarAppCatalog.query(context).map { it.packageName }.toMutableSet()
                            for (action in listOf("androidx.media3.session.MediaLibraryService", "android.media.browse.MediaBrowserService")) {
                                context.packageManager.queryIntentServices(Intent(action), 0).filter { it.serviceInfo.exported }.forEach { packages += it.serviceInfo.packageName }
                            }
                            packages.filter { it != context.packageName }.map { pkg ->
                                pkg to context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
                            }.sortedBy { it.second.lowercase() }
                        }.getOrDefault(emptyList())
                    }
                }
                if (apps.isEmpty()) Text("Geen ondersteunde apps gevonden. Installeer eerst je navigatie- of media-apps.")
                apps.forEach { (pkg, label) -> Toggle(label, pkg in config.pinnedApps) {
                    config = config.copy(pinnedApps = if (it) config.pinnedApps + pkg else config.pinnedApps - pkg)
                } }
            }
            3 -> {
                Toggle("Home Assistant inschakelen", config.homeEnabled) { config = config.copy(homeEnabled = it) }
                Field("Home Assistant HTTPS-URL (zonder /api)", config.homeUrl) { config = config.copy(homeUrl = it.trim()) }
                OutlinedTextField(value = token, onValueChange = { token = it.take(4096) }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (hasToken) "Nieuw toegangstoken (vervangen)" else "Langlevend toegangstoken") },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                Text("Het token wordt versleuteld op deze telefoon opgeslagen en gaat alleen naar jouw ingestelde Home Assistant-server. Gebruik een HTTPS-adres, bijvoorbeeld van Nabu Casa.")
                if (hasToken) TextButton(onClick = {
                    secrets.clear(); hasToken = false; token = ""; config = config.copy(homeEnabled = false)
                    settings.save(settings.config.value.copy(homeEnabled = false)); message = "Token verwijderd en verbinding uitgezet."
                }) { Text("Token verwijderen") }
                config.homeActions.forEachIndexed { index, action ->
                    FeatureCard("Thuisknop ${index + 1}") {
                        Field("Naam", action.label) { value -> config = config.copy(homeActions = config.homeActions.mapIndexed { i, a -> if (i == index) a.copy(label = value) else a }) }
                        Field("Entiteit, bijvoorbeeld light.oprit", action.entity) { value -> config = config.copy(homeActions = config.homeActions.mapIndexed { i, a -> if (i == index) a.copy(entity = value.trim()) else a }) }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FeatureConfig.HOME_SERVICES.forEach { service ->
                                FilterChip(selected = action.service == service, onClick = {
                                    config = config.copy(homeActions = config.homeActions.mapIndexed { i, a -> if (i == index) a.copy(service = service) else a })
                                }, label = { Text(service) })
                            }
                        }
                    }
                }
                Text("Je bevestigt een actie op het autoscherm voordat die wordt verstuurd. De app voert geen automatische thuiskomstacties uit.")
            }
            4 -> {
                Toggle("Ritten lokaal bewaren (opt-in)", config.tripEnabled) { config = config.copy(tripEnabled = it) }
                Toggle("Nieuwe ritten standaard zakelijk", config.businessTrips) { config = config.copy(businessTrips = it) }
                Field("Kilometerkosten (€ per km, optioneel)", config.costPerKm) { config = config.copy(costPerKm = it) }
                Text("Bewaartermijn")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30, 90, 365).forEach { days -> FilterChip(selected = config.retentionDays == days,
                        onClick = { config = config.copy(retentionDays = days) }, label = { Text("$days d") }) }
                }
                Text("Eén rit per autoverbinding. Alleen tijd, voertuig, categorie en kilometertellerafstand worden bewaard, zonder GPS-route. Zonder kilometertellergegevens blijft de afstand onbekend. Kosten zijn jouw eigen rekeninstelling, geen fiscale berekening.")
                Button(enabled = records.isNotEmpty(), onClick = { export = TripLogic.csv(records); exporter.launch("opendrive-ritten.csv") }) { Text("CSV exporteren (${records.size} ritten)") }
                TextButton(enabled = records.isNotEmpty(), onClick = { deleteId = "ALL" }) { Text("Alle ritten verwijderen") }
                records.take(50).forEach { record ->
                    FeatureCard(tripTitle(record)) {
                        Text(record.km?.let { "${format(it, 1)} km" } ?: "Afstand onbekend")
                        Toggle("Zakelijk", record.business) { value ->
                            runCatching { trips.category(record.id, value) }.onFailure { message = "Wijzigen mislukt." }
                        }
                        TextButton(onClick = { deleteId = record.id }) { Text("Verwijderen") }
                    }
                }
            }
            5 -> {
                Text("Laat een bijrijder muziek bedienen en een bestemming voorstellen via de browser op een tweede telefoon. Bestemmingen bevestig je in de auto.")
                Text("Beide telefoons moeten elkaar bereiken op hetzelfde vertrouwde wifi-/hotspotnetwerk. De tijdelijke lokale HTTP-link bevat een geheime sleutel; deel hem alleen met je bijrijder. Hij verloopt na 30 minuten.")
                if (session == null) Text("Verbind eerst de auto of open het voorbeeld van het autoscherm.")
                Button(enabled = session != null && session?.runtime?.vehicleMoving == false, onClick = { session?.startCompanion() }) { Text("Koppelingslink maken") }
                session?.pairingLinks?.forEachIndexed { index, link ->
                    Text(link.substringBefore('#'))
                    Button(onClick = {
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("OpenDrive bijrijder", link))
                        message = "Link ${index + 1} gekopieerd. Open hem op de telefoon van de bijrijder."
                    }) { Text("Link ${index + 1} kopiëren") }
                }
                if (session?.pairingLinks?.isNotEmpty() == true) TextButton(onClick = { session?.stopCompanion() }) { Text("Bijrijderbediening stoppen") }
                Text(session?.message.orEmpty())
            }
        }
        if (tab != 5) Button(onClick = {
            val error = config.validate()
            if (error != null) message = error
            else if (config.homeEnabled && !hasToken && token.isBlank()) message = "Voeg eerst je Home Assistant-token toe."
            else try {
                if (token.isNotBlank()) { secrets.save(token.trim()); token = ""; hasToken = true }
                settings.save(config)
                trips.prune(config.retentionDays)
                message = "Opgeslagen. De wijzigingen worden in de actieve cockpit overgenomen."
            } catch (_: Exception) { message = "Opslaan mislukt. Controleer instellingen en opslag." }
        }) { Text("OpenDrive-instellingen opslaan") }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
    }
    deleteId?.let { id -> AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Ritten verwijderen?") },
        text = { Text("Dit verwijdert ${if (id == "ALL") "alle opgeslagen ritten" else "deze rit"} van deze telefoon.") },
        confirmButton = { TextButton(onClick = {
            runCatching { if (id == "ALL") trips.clear() else trips.delete(id) }.onFailure { message = "Verwijderen mislukt." }
            deleteId = null
        }) { Text("Verwijderen") } }, dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Annuleren") } }) }
}

@Composable private fun Field(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { change(it.take(500)) }, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true)
}
@Composable private fun Toggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = change)
    }
}
