# OpenDrive-productfuncties

OpenDrive bouwt eigen gebruikersfuncties boven op de nieuwste opgehaalde AARE `main`. AARE blijft de onafhankelijke upstreambasis; onze wijzigingen worden niet naar de oorspronkelijke repository gepusht.

| Functie | Status | Eerstvolgende stap |
|---|---|---|
| OpenAI-spraakassistent | Eerste implementatie | API-toegang instellen en microfoon, antwoorden en onderbreken testen op een telefoon |
| Navigatie via spraak | Bestemmingszoekactie geïmplementeerd | Testen met de gekozen navigatieapp; later live plaatsen en tussenstops |
| Muziek via spraak | Zoeken, genummerde keuze en bediening geïmplementeerd | Testen met de muziekapps van de gebruiker |
| Doorlopende context en eigen voorkeuren | Geïmplementeerd per autosessie | Later optionele, expliciete opslag van favorieten |
| ChatGPT-account en bestaande gesprekken | Onderzoek | Officiële toegang en productmogelijkheden verifiëren; geen toegang veronderstellen |
| Persoonlijke cockpit | Eerste implementatie | Vier instelbare en geordende tegels, eigen startscherm; controleren op de headunit |
| EV-dashboard | Eerste implementatie | Auto-/Home Assistant-gegevens en herkenbare handmatige invoer; Niro-databeschikbaarheid controleren, later eventueel OBD |
| Bijrijderbediening | Eerste implementatie | Tijdelijke lokale browserkoppeling, muziek en bestemmingsvoorstellen met bestuurdersbevestiging; testen op twee telefoons |
| Home Assistant | Eerste implementatie | HTTPS-koppeling, drie opgeslagen acties met bevestiging en EV-sensoren; testen met de eigen server |
| Eigen media en kaartdiensten | AARE plus OpenDrive-favorieten | Vaste ondersteunde apps, bestemmingen en geparkeerde HTTPS-webdiensten; testen met eigen diensten |
| Lokaal rittenboek | Eerste implementatie | Opt-in sessieritten, kilometertellerafstand, privé/zakelijk, CSV, verwijderen en bewaartermijn |
| Laadpauzescherm | Eerste implementatie | EV-gegevens, rekenschatting en sessiegebonden pauzetimer plus webfavorieten; geen laadregeling |

De gebruiker heeft na de eerste assistentversie ook cockpit, EV en de overige bovenstaande productfuncties laten toevoegen. Zie `opendrive-features.md` voor instellingen, mogelijkheden en de concrete grenzen van deze eerste implementaties. Praktijktesten en geavanceerde vervolgstappen blijven open; accountlogin, directe OBD, automatische thuiskomstacties en externe agentkoppelingen zijn nog geen beschikbare functies.
