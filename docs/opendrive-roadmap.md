# OpenDrive-productfuncties

OpenDrive bouwt eigen gebruikersfuncties boven op de nieuwste opgehaalde AARE `main`. AARE blijft de onafhankelijke upstreambasis; onze wijzigingen worden niet naar de oorspronkelijke repository gepusht.

De eerste versies hieronder zijn opgenomen in [OpenDrive 0.1.0](https://github.com/noscapect/OpenDrive/releases/tag/v0.1.0), gepubliceerd op 10 november 2026. **201 lokale tests zijn geslaagd; praktijktests op telefoon en Kia blijven open.** Zie de [release-status](opendrive-status.md) voor de vaste bron- en APK-identiteit.

| Functie | Status | Eerstvolgende stap |
|---|---|---|
| OpenAI-spraakassistent | Eerste implementatie | API-toegang instellen en microfoon, antwoorden en onderbreken testen op een telefoon |
| Navigatie via spraak | Bestemmingszoekactie geïmplementeerd | Testen met de gekozen navigatieapp; later live plaatsen en tussenstops |
| Muziek via spraak | Zoeken, genummerde keuze en bediening geïmplementeerd | Testen met de muziekapps van de gebruiker |
| Doorlopende context en eigen voorkeuren | Geïmplementeerd per autosessie | Later optionele, expliciete opslag van favorieten |
| ChatGPT-accountlogin | Niet geïmplementeerd | Officiële inlogroute en ondersteuning voor de benodigde spraakfuncties onderzoeken |
| Bestaande ChatGPT-gesprekken | Geen koppeling | Accountlogin niet verwarren met toegang tot gespreksgeschiedenis |
| Buddy/Hermes/Codex-agent | Niet geïntegreerd | Keuze en apparaatbediening eerst afzonderlijk op de telefoon onderzoeken |
| Persoonlijke cockpit | Eerste implementatie | Vier instelbare en geordende tegels, eigen startscherm; controleren op de headunit |
| EV-dashboard | Eerste implementatie | Auto-/Home Assistant-gegevens en herkenbare handmatige invoer; Niro-databeschikbaarheid controleren, later eventueel OBD |
| Bijrijderbediening | Eerste implementatie | Tijdelijke lokale browserkoppeling, muziek en bestemmingsvoorstellen met bestuurdersbevestiging; testen op twee telefoons |
| Home Assistant | Eerste implementatie | HTTPS-koppeling, drie opgeslagen acties met bevestiging en EV-sensoren; testen met de eigen server |
| Eigen media en kaartdiensten | AARE plus OpenDrive-favorieten | Vaste ondersteunde apps, bestemmingen en geparkeerde HTTPS-webdiensten; testen met eigen diensten |
| Lokaal rittenboek | Eerste implementatie | Opt-in sessieritten, kilometertellerafstand, privé/zakelijk, CSV, verwijderen en bewaartermijn |
| Laadpauzescherm | Eerste implementatie | EV-gegevens, rekenschatting en sessiegebonden pauzetimer plus webfavorieten; geen laadregeling |

Zie de [functiehandleiding](opendrive-features.md) voor instellingen, mogelijkheden en de concrete grenzen van de eerste implementaties. Accountlogin, directe OBD, automatische thuiskomstacties en externe agentkoppelingen zijn nog geen beschikbare functies.

## Volgende validatie

1. Release 0.1.0 op de telefoon installeren en het voorbeeldscherm, machtigingen en instellingen controleren.
2. Geparkeerd de Kia-verbinding testen, de eerdere certificaatmelding controleren en het Connection log vastleggen.
3. Microfoon, TTS, echte OpenAI-aanroepen en de gekozen navigatie-/muziekapps testen.
4. Met echte gegevensbronnen EV/Home Assistant, twee telefoons voor bijrijderbediening en een complete rittenboekcyclus controleren.
5. Daarna vervolgintegraties, een eigen package-identiteit en productieondertekening uitwerken op basis van de testresultaten.
