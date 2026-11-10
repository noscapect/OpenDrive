# Eigen Android-identiteit — 0.1.1

Vanaf de 0.1.1-ontwikkelstand gebruikt OpenDrive application ID **`io.github.noscapect.opendrive`**. De eerdere release 0.1.0 blijft ongewijzigd en gebruikte `net.prototux.aare`. Het app-label is OpenDrive; AARE blijft expliciet als bronproject vermeld.

Dit is een blijvende eigen identiteit voor het zelfstandige OpenDrive-project. Het is geen vastgestelde oplossing voor Play Protect: Google kan ook naar code, rechten, ondertekening en andere signalen kijken. De oorzaak van de gemelde blokkade is niet bewezen en acceptatie op de telefoon moet nog worden getest. Er worden geen rechten of functies weggelaten en er wordt geen goedkeuring door Google geclaimd.

## Installatie en gegevens

- Android behandelt de nieuwe identiteit als een aparte app, die naast AARE of OpenDrive 0.1.0 kan staan.
- De oude installatie en haar gegevens worden niet vervangen. Instellingen, toestemmingen, certificaatvertrouwen, API-sleutels, Home Assistant-configuratie en ritten worden niet automatisch overgezet.
- Stel de nieuwe installatie opnieuw in en kies bij de USB-accessoire de gewenste app. Twee apps kunnen niet tegelijk dezelfde projectiesessie gebruiken.
- De ondertekeningssleutel blijft gelijk; een andere application ID maakt dit desondanks een afzonderlijke installatie.

## Integraties

De car-control-provider gebruikt nu `content://io.github.noscapect.opendrive.car`. De Shizuku-provider gebruikt `io.github.noscapect.opendrive.shizuku`. Interne service-/launcheracties krijgen de OpenDrive application ID als prefix.

Externe assistenten die `content://net.prototux.aare.car` hardcoderen, moeten de nieuwe OpenDrive-authority ondersteunen voordat hun car-control-tools op OpenDrive werken. De ingebouwde OpenDrive-assistent heeft die externe providerkoppeling niet nodig.

De optionele Xposed-compatibiliteitsmodule en Magisk-permissionconfiguratie in deze fork verwijzen naar de nieuwe app-ID. De desktop-headunit-helper start de nieuwe OpenDrive-app met de bestaande, volledig gekwalificeerde Activity-klassenaam. Deze optionele helpers vereisen nog hun eigen apparaatvalidatie.

De Kotlin/Java-bronnamespace blijft `net.prototux.aare` voor de geërfde code. Dat voorkomt een grote bronhernoeming bij toekomstige upstream-merges en verandert de application ID in de uiteindelijke APK niet. Bronvermelding en licenties blijven behouden. Zie het onderscheid in de [Android-documentatie](https://developer.android.com/build/configure-app-module).
