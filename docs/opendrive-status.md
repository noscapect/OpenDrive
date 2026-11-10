# OpenDrive-status — 10 november 2026

Dit overzicht beschrijft de gepubliceerde **OpenDrive 0.1.0**. De ontwikkeltak kan nieuwere commits bevatten; de release en de bijbehorende tag blijven de vaste referentie voor deze APK.

## Download en broncode

- [Publieke repository](https://github.com/noscapect/OpenDrive)
- [Release 0.1.0 en installatiebestand](https://github.com/noscapect/OpenDrive/releases/tag/v0.1.0)
- [Directe APK-download](https://github.com/noscapect/OpenDrive/releases/download/v0.1.0/opendrive-0.1.0.apk)
- [SHA256SUMS](https://github.com/noscapect/OpenDrive/releases/download/v0.1.0/SHA256SUMS)
- [Wijzigingen per versie](../CHANGELOG.md)

| Onderdeel | Gepubliceerde waarde |
|---|---|
| OpenDrive-versie | `0.1.0` |
| Android version code | `10401` |
| Release-tag | `v0.1.0` |
| OpenDrive-broncommit | `a507d0a7d5d864ab0ba13dca59691543a359c8a0` |
| AARE-basiscommit | `5d932aec4cffda124fe0b42dd5e9ed80de4f007f` |
| AARE-basisrelease | `1.4.0`, plus latere upstreamwijzigingen |
| Android-package | `net.prototux.aare` |
| Minimum Android-versie | Android 8.0 / API 26 |
| Ondertekening | Dezelfde lokale Android-debugsleutel als eerdere lokale builds |

APK SHA-256:

```text
192a922b5bf70fb6dadc506911e2455375a2092293d4680dd770aebcae7e1978
```

OpenDrive heeft een eigen versienummer, onafhankelijk van AARE. Het interne Android-buildnummer is hoger dan de eerdere lokale builds met code `10400`, zodat `0.1.0` daarover kan worden geïnstalleerd. Een APK met een andere ondertekening kan niet rechtstreeks worden bijgewerkt. Een eigen package-identiteit is nog niet ingevoerd.

## Beschikbare implementaties

| Functie | In 0.1.0 | Instellen |
|---|---|---|
| Cockpit | Vier tegels kiezen, verbergen en ordenen; eigen startscherm | OpenDrive → Cockpit |
| EV-dashboard | Auto-/Home Assistant-bronnen, bronlabels, herkenbare handmatige waarden en schattingen | OpenDrive → EV |
| Favorieten | Bestemmingen, ondersteunde apps en geparkeerde HTTPS-webdiensten | OpenDrive → Favorieten |
| Home Assistant | Drie opgeslagen knoppen met bevestiging en optionele EV-sensoren | OpenDrive → Thuis |
| Rittenboek | Opt-in, lokale sessieritten, categorieën, CSV, verwijderen en bewaartermijn | OpenDrive → Ritten |
| Bijrijder | Tijdelijke lokale browserkoppeling voor muziek en bestemmingsvoorstellen | OpenDrive → Bijrijder |
| Laadpauze | Energiegegevens, rekenschatting, sessietimer en webfavorieten | Laadpauze op het autoscherm |
| OpenAI-assistent | Spraak, vervolgvragen, bestemmingszoekactie en muziek zoeken/bedienen | Spraakassistent → OpenDrive-assistent |

Zie de [functiehandleiding](opendrive-features.md) en [assistenthandleiding](opendrive-assistant.md) voor de concrete mogelijkheden en grenzen. Ontbrekende voertuigmetingen worden niet gesimuleerd. De optionele assistent gebruikt de eigen API-sleutel van de gebruiker; Home Assistant vereist een ingestelde HTTPS-server en token.

## Gecontroleerd

- Releasebuild succesvol: **201 lokale tests geslaagd** — 48 protocoltests en 153 apptests, zonder fouten of overgeslagen tests.
- De ingebouwde APK-versie en version code zijn gecontroleerd.
- APK-ondertekening gecontroleerd; ondertekeningscertificaat SHA-256: `2d075a8e9e16fc402caaee9a4ef374245a2c7d9709d5f3425018f9ffb03d297a`.
- De APK is vanaf de publieke release opnieuw gedownload en de SHA-256 komt overeen met het lokale bestand.
- Broncode, bouwscripts, documentatie en de release-tag staan op GitHub. Sleutels en lokale buildcaches staan niet in de repository.

Dit zijn lokale testresultaten, geen bevestiging dat alle GitHub Actions-checks of hardwaretests zijn geslaagd.

## Nog te valideren of te bouwen

Er is nog geen praktijktest van deze release op een telefoon of in de Kia Niro vastgelegd. De eerdere certificaatmelding is dus nog niet aantoonbaar opgelost. UI, Android Keystore, microfoon/TTS, auto-audio, echte OpenAI-/Home Assistant-aanroepen en bijrijderbereikbaarheid moeten op apparaten worden gecontroleerd.

ChatGPT-accountlogin, toegang tot bestaande ChatGPT-gesprekken, Buddy-/Hermes-/Codex-agentkoppelingen, directe OBD/Kia-integratie, automatische thuiskomstacties, laadstopplanning en berichten versturen zijn niet ingebouwd. De vervolgstappen staan op de [roadmap](opendrive-roadmap.md).

## Bouwen en upstream

Vanuit een zelfstandige repositoryclone:

```powershell
./tools/opendrive/build.ps1             # ontwikkelversie met commitnummer
./tools/opendrive/build.ps1 -Release    # exact versienummer uit opendrive-version.properties
```

Beide opdrachten testen en bouwen een debug-ondertekende APK; `-Release` publiceert niets. Standaard wordt eerst AARE `upstream/main` opgehaald en samengevoegd. Dit is geen achtergrondupdate: iedere build controleert opnieuw, en een netwerkfout of mergeconflict stopt de build. Een bewuste herbouw zonder ophalen kan met `-SkipUpdate`, als upstream al is ingesteld. Zie de [README](../README.md) voor SDK/JDK en uitvoerpaden.

`origin` is onze publieke OpenDrive-repository. `upstream` is AARE en heeft push-URL `DISABLED`. De oorspronkelijke AARE-checkout en [AARE-README](../README.AARE.md) blijven referentiemateriaal; onze aanvullingen gaan niet naar de oorspronkelijke ontwikkelaar.
