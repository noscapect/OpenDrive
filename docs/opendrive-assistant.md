# OpenDrive-spraakassistent

Dit is een uitbreiding van OpenDrive, boven op AARE. De oorspronkelijke AARE-checkout en ontwikkelaar ontvangen deze wijzigingen niet.

## Instellen

1. Installeer de lokaal gebouwde `opendrive/build/opendrive-debug.apk` over onze vorige lokale APK.
2. Open op de telefoon **Spraakassistent**. Voeg in **OpenDrive-assistent** je eigen OpenAI API-sleutel toe en druk op **Opslaan**.
3. Kies een model waarvoor je API-account toegang heeft. De standaard is `gpt-6.1-sol` met lage redeneerinspanning; transcriptie gebruikt `gpt-transcribe`. Je kunt het antwoordmodel aanpassen.
4. Zet **OpenDrive gebruiken voor de spraakknop** aan. Geef OpenDrive toegang tot de microfoon via de bestaande telefooninstellingen als dat nog niet is gebeurd.
5. Probeer eerst **Voorbeeld autoscherm**. Druk op de microfoon, spreek en wacht een korte stilte af. Nogmaals drukken verstuurt de opname eerder. Druk opnieuw voor een vervolgvraag; het gesprek blijft behouden.
6. Configureer de bestaande auto-audiofunctie voordat je gesproken antwoorden via de projectie verwacht. Anders hangt de uitvoer af van de Bluetooth-/telefoonaudioroutering. Android tekst-naar-spraak en een geschikte stem moeten beschikbaar zijn.

De assistent is standaard uit. Uitgeschakeld gebruikt de spraakknop de bestaande AARE-assistentkeuze. Het instellen van de API-sleutel koppelt geen ChatGPT-account of ChatGPT-gesprekken. API-gebruik loopt via het ingestelde API-account; stel daar passende gebruikslimieten in.

## Wat werkt in de implementatie

- Spraakinvoer via AARE's MicRouter: de automicrofoon wanneer beschikbaar, anders de telefoonmicrofoon. De preview gebruikt de telefoonmicrofoon.
- Antwoorden via OpenAI Responses API en Android tekst-naar-spraak. Iedere beurt begint met een knopdruk; er is geen altijd luisterend activeringswoord.
- Gesprekscontext voor vervolgvragen. **Nieuw gesprek** wist die context en de laatste muziekresultaten. **Sluiten** stopt opname, verwerking en voorlezen, maar bewaart de gesprekshistorie tot het volgende gesprek of sessie-einde.
- “Navigeer naar Utrecht”: bestemming zoeken via de actieve gehoste navigatieapp. De navigatieapp bepaalt hoe resultaten en routebevestiging verlopen. Er is nog geen terugval naar een telefoonapp wanneer geen gehoste navigatieapp beschikbaar is.
- “Pauzeer de muziek”, “Volgende nummer”, “Wat speelt er?”: bestaande afspeelbediening en actuele gegevens gebruiken.
- “Zoek Miles Davis in [muziekapp]”, gevolgd door “Speel de tweede”: maximaal acht afspeelbare resultaten via de bestaande media-browserkoppeling. De muziekapp moet zoeken ondersteunen en ingelogd/beschikbaar zijn. Bij meerdere apps zonder duidelijke keuze vraagt de assistent welke app bedoeld wordt.
- Vragen over rit of voertuig gebruiken uitsluitend gegevens die AARE op dat moment heeft. Ontbrekende actieradius, laadpaalbeschikbaarheid of locatiegegevens mogen niet worden verzonnen.
- Eigen voorkeuren voor taal, muziek en reizen kunnen op de telefoon worden ingesteld. Dit is geen automatische opbouw van een gebruikersprofiel.

Er is geen live web-/plaatsenzoekdienst, routeplanning met laadstops, berichtenfunctie, koppeling aan ChatGPT-geschiedenis of streaming gesprek met gelijktijdig spreken en luisteren toegevoegd. De stem is van Android tekst-naar-spraak, niet van ChatGPT Voice. Echte latentie en spraakkwaliteit moeten op de telefoon en in de auto worden beoordeeld.

## Gegevens en grenzen

De persoonlijke API-sleutel staat versleuteld met Android Keystore in `noBackupFilesDir`; hij wordt niet meegebouwd of gelogd. Dit is een persoonlijke ontwikkelbuild met een door de gebruiker beheerde sleutel. Voor publieke distributie moet een beheerde authenticatie-/backendoplossing worden ontworpen; een gedeelde servicesleutel hoort nooit in een mobiele APK.

Opnemen begint uitsluitend na de microfoonknop. De opname is maximaal twintig seconden en blijft lokaal in geheugen tot verzending; hij wordt niet op schijf opgeslagen. De opname gaat naar OpenAI voor transcriptie. Transcript, recente gesprekscontext, voorkeuren en aangevraagde rit-/muziekgegevens gaan naar OpenAI voor antwoorden. Audio en gesprek worden door OpenDrive niet in diagnostiek geschreven. Responses gebruikt `store=false`; dat is geen claim dat alle providerretentie is uitgeschakeld.

OpenDrive bewaart maximaal zes volledige gespreksbeurten en begrenst de lokale context. Uitschakelen en het beëindigen van de autosessie wissen het gesprek. Onderbreken of afsluiten annuleert lopende netwerkaanroepen en stopt het voorlezen. Een bedieningsactie die al is verstuurd kan daarmee niet worden teruggedraaid; controleer navigatie/muziek voordat je een mislukte beurt herhaalt.

De assistent krijgt alleen een expliciete lijst met navigatie- en muziekacties. Berichten lezen/versturen, bellen, willekeurige intents en andere bestaande AARE-tools zijn niet beschikbaar voor deze integratie. Argumenten worden lokaal gecontroleerd. Onvolledige antwoorden, onverwachte parallelle acties en herhaalde identieke bedieningsacties in dezelfde beurt worden geweigerd.

## Validatie

De automatische tests controleren WAV-formaat, begrenzing en stilte-afhandeling, gesprekscontext, toolresultaten, wissen van geschiedenis, fout-/annuleringsgedrag, actieschema's en argumentvalidatie. De bestaande AARE-protocol- en apptests draaien mee via `../build.ps1`.

De uiteindelijke buildinformatie en testresultaten staan in `opendrive/build/build-info.json` en `opendrive/build/tests`. Er is zonder ingestelde sleutel geen echte OpenAI-aanroep uitgevoerd. Android Keystore, opname, TTS, muziekapps en de Kia Niro zijn nog niet samen op een apparaat getest.

## Gebruikte OpenAI-documentatie

- [Responses function calling](https://developers.openai.com/api/docs/guides/function-calling)
- [Audio-transcriptie](https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create)
- [GPT-6.1 Sol](https://developers.openai.com/api/docs/models/gpt-6.1-sol)
- [Stateless reasoning-context](https://developers.openai.com/api/docs/guides/reasoning)
