# Cockpit, EV en extra functies

Deze functies zijn alleen toegevoegd aan OpenDrive. AARE blijft de upstreambasis; de oorspronkelijke checkout wordt niet aangepast en de push-URL van onze AARE-remote blijft uitgeschakeld.

Deze handleiding beschrijft [OpenDrive 0.1.0](https://github.com/noscapect/OpenDrive/releases/tag/v0.1.0). Zie ook de [release-status en validatie](opendrive-status.md) en de [roadmap](opendrive-roadmap.md).

## Installeren en starten

Download [opendrive-0.1.0.apk](https://github.com/noscapect/OpenDrive/releases/download/v0.1.0/opendrive-0.1.0.apk). Deze APK heeft versie `0.1.0`, Android version code `10401` en dezelfde lokale ondertekening en hetzelfde package als onze vorige lokale OpenDrive/AARE-APK. Hij kan daarover worden geïnstalleerd. Een officieel door AARE ondertekende installatie heeft een andere sleutel en kan hiermee niet rechtstreeks worden bijgewerkt.

Zelf bouwen kan vanuit de repositoryroot met `./tools/opendrive/build.ps1 -Release`. De uitvoer staat standaard in `build/opendrive/opendrive-debug.apk`; onze lokale workspace-wrapper gebruikt `opendrive/build/opendrive-debug.apk`. `-Release` bepaalt het versielabel en blijft een debug-ondertekende build.

Open op de telefoon de nieuwe categorie **OpenDrive**. Hier staan de tabbladen **Cockpit**, **EV**, **Favorieten**, **Thuis**, **Ritten** en **Bijrijder**. Druk na wijzigingen op **OpenDrive-instellingen opslaan**. Het bestaande **Voorbeeld autoscherm** kan worden gebruikt zonder auto. De preview genereert geen nep-voertuiggegevens en slaat geen ritten op.

## Cockpit

De cockpit is standaard het startscherm. Je kunt vier tegels inschakelen en ordenen: navigatie, muziek, EV en favorieten. De navigatietegel gebruikt de actieve gehoste kaart; zonder geschikte kaartapp biedt hij een appkeuze of de beschikbare telefoonguidance. De muziektegel toont de huidige speler en biedt afspelen/pauzeren en vorige/volgende.

Je kunt de kaarttegel verbergen voor een startscherm met alleen muziek, energie en snelkoppelingen. Op het autoscherm staan ook ingangen naar EV-dashboard, Favorieten, Thuis, Rittenboek, Bijrijder en Laadpauze. **Kaartdashboard** opent het oorspronkelijke dashboard. Zet **Cockpit als startscherm** uit om de oorspronkelijke Home-bediening te gebruiken; de beginpagina wordt bij de volgende sessie toegepast.

## EV-dashboard

Het EV-scherm toont accupercentage, actieradius, laadvermogen en accutemperatuur, met bronvermelding. Onbekende gegevens blijven onbekend. Bij een uitsluitend elektrische brandstofclassificatie wordt het energieniveau van de auto als accu gebruikt; bij een hybride of onbekend type gebeurt dit niet automatisch. Je kunt expliciet aangeven dat jouw auto inderdaad een accupercentage via dit veld doorgeeft.

Optioneel kun je een bruikbare accucapaciteit, gemiddeld verbruik, laaddoel en handmatig accupercentage invullen. Een handmatige waarde is een momentopname en wordt ook zo aangeduid. Een actieradiusschatting verschijnt alleen bij voldoende invoer. Een laadtijdschatting vereist een niet-handmatige accuwaarde, een capaciteit, een laaddoel en positief laadvermogen; zij rekent met gelijkblijvend vermogen en modelleert geen laadcurve.

Voor extra waarden kun je Home Assistant-sensoren instellen. Ondersteunde eenheden zijn `%`, `km`, `kW` of `W` en `°C`/`C`. De gegevens worden ongeveer iedere 30 seconden opnieuw opgevraagd tijdens de OpenDrive-sessie. Externe waarden ouder dan vijf minuten of met een onjuiste eenheid worden niet gebruikt. De tijd bij een externe waarde is het moment van ophalen; de bron kan zelf een oudere voertuigmeting bewaren. Controleer de actualiteit van je Kia-/voertuigintegratie in Home Assistant.

Er is nog geen rechtstreekse Bluetooth-OBD-driver of Kia-accountkoppeling toegevoegd. Welke gegevens de Niro via Android Auto doorgeeft moet in de auto worden vastgesteld.

## Favorieten en eigen diensten

Je kunt drie bestemmingen, twee HTTPS-webfavorieten en vaste ondersteunde navigatie-/media-apps instellen. De eerste bestemmingen en vaste apps zijn bereikbaar vanaf de cockpittegel. Alle ingestelde favorieten staan ook in de aparte Favorieten-app.

Webdiensten gebruiken de bestaande AARE-browser en worden gepauzeerd wanneer het ruwe rijsignaal beweging aangeeft, ook als de bestaande passagiersmodus de normale beperkingen opheft. Eigen muziekservers en alternatieve kaarten blijven afhankelijk van de daarvoor geïnstalleerde, ondersteunde apps. Er wordt geen willekeurige app automatisch aangepast voor het autoscherm.

## Home Assistant

Vul een HTTPS-basisadres zonder `/api` en een langlevend toegangstoken in. De token wordt met Android Keystore versleuteld opgeslagen buiten backups. Alleen de ingestelde server ontvangt hem; redirects worden niet gevolgd. HTTP-adressen worden in deze versie niet ondersteund.

Je kunt drie knoppen instellen met een naam, entiteit en gekozen actie. Ondersteunde acties zijn licht/schakelaar aan of uit, cover openen/sluiten en een scene of script activeren. De entiteit en actie moeten hetzelfde domein hebben. Op het autoscherm bevestig je de concrete actie voordat het verzoek wordt verstuurd. Er zijn geen automatische thuiskomstacties of algemene shell-/systeemopdrachten beschikbaar. Een aangemaakte scene of een script kan in Home Assistant zelf meerdere handelingen omvatten.

## Lokaal rittenboek

Ritopname staat standaard uit. Na inschakelen bewaart OpenDrive een rit per echte autoverbinding. De opgeslagen velden zijn start/eindtijd, voertuig, privé/zakelijk, optionele kilometertellerafstand en optionele kosten volgens jouw kilometertarief. Er wordt geen GPS-route bewaard. Zonder een bekende kilometerteller aan het begin en geldige vervolgmetingen wordt geen afstand verzonnen. Een dalende teller of onmogelijke sprong wordt geweigerd.

Op de telefoon kun je ritten categoriseren, individueel of allemaal verwijderen en via de Android-bestandskiezer als CSV exporteren. De bewaartermijn is 7, 30, 90 of 365 dagen; er worden maximaal 1000 ritten bewaard. De laatste 50 zijn op de telefoon zichtbaar en de volledige bewaarde lijst kan worden geëxporteerd. Kosten zijn een eigen gebruikersberekening, geen fiscale beoordeling. Uitschakelen van opname gooit een nog actieve, niet opgeslagen rit weg; reeds opgeslagen ritten blijven bestaan tot je ze verwijdert of de bewaartermijn verloopt.

## Bijrijderbediening

Start geparkeerd **Bijrijderverbinding** vanuit het autoscherm of de telefooninstellingen terwijl een OpenDrive-sessie actief is. Op de telefoon kun je een tijdelijke link kopiëren. Open die op de telefoon van je bijrijder. Beide apparaten moeten elkaar op hetzelfde wifi-/hotspotnetwerk kunnen bereiken; een geïsoleerd autonetwerk of hotspot kan dit verhinderen. Bij meerdere netwerkadressen worden meerdere links aangeboden.

De browser kan afspelen/pauzeren, vorige/volgende en een bestemmingsvoorstel versturen. Een bestemming wordt pas via de navigatieapp gezocht als de bestuurder op **Gebruiken** drukt. Er zijn geen berichten-, contact-, bestands- of algemene telefooncommando's beschikbaar. Alleen titel en afspeelstatus worden gedeeld.

De server accepteert uitsluitend lokale netwerkadressen en vereist een willekeurige 192-bit sleutel voor status en acties. De sleutel staat in het fragment van de koppelingslink, niet in de opgevraagde URL. Het browservenster bewaart hem alleen tijdens de pagina. Ververs de pagina niet zonder opnieuw de koppelingslink te openen. De lokale verbinding gebruikt HTTP: gebruik een vertrouwd netwerk en deel de geheime link uitsluitend met de bijrijder. Een nieuwe koppeling vervangt de vorige; stoppen, sessie-einde en na 30 minuten wordt de verbinding ingetrokken. Een port-forward of publieke server is niet nodig.

## Laadpauze

Het laadpauzescherm combineert EV-gegevens met een pauzetimer van 10, 20 of 30 minuten en de ingestelde webfavorieten. De timer geeft een melding binnen de actieve OpenDrive-sessie. Dit is geen laadregeling en geen achtergrondalarm na het sluiten van OpenDrive. Entertainment wordt onderbroken bij een bewegingssignaal; afspelen van video hangt af van de website en Android WebView, inclusief eventuele DRM-beperkingen.

## Validatiegrenzen

De 0.1.0-releasebuild heeft 201 lokale tests doorstaan: bestaande AARE-tests en eigen tests voor onder meer configuratie, gegevensbronnen en eenheden, schattingen, ritafstand, opslagselectie, CSV-escaping en de echte lokale HTTP-server. De APK-versie, ondertekening en publieke downloadchecksum zijn gecontroleerd. De API van Home Assistant is gebaseerd op de [officiële REST-documentatie](https://developers.home-assistant.io/docs/api/rest/).

Er is geen telefoon of Kia aangesloten voor een praktijktest. De nieuwe schermen, Android Keystore, audio, thuisknoppen en LAN-bereikbaarheid moeten op apparaten worden gecontroleerd. Deze versie voegt geen ChatGPT-accountlogin, Buddy-, Hermes- of Codex-agent toe; de eerder gebouwde OpenAI-assistent met eigen API-sleutel blijft beschikbaar.
