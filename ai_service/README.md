# CityNewsAI briefing service

Microservizio HTTP che genera rassegne provinciali con OpenAI Responses API e ricerca web. La chiave OpenAI resta esclusivamente come variabile segreta sul server.

## Endpoint
- `GET /health`
- `POST /api/briefing`

Esempio JSON:
```json
{
  "city": "Piacenza",
  "province": "Piacenza",
  "region": "Emilia-Romagna",
  "period": "24h"
}
```

Periodi ammessi: `24h`, `3d`, `7d`, `1m`, `6m`, `1y`.

La risposta contiene il testo della rassegna e le citazioni URL restituite dalla ricerca web. La presenza di una citazione non costituisce garanzia assoluta di veridicità: l'app deve mantenere visibili fonti e collegamenti originali.

## Avvio locale
Richiede Python 3.12 e la variabile d'ambiente `OPENAI_API_KEY`.

```bash
cd ai_service
OPENAI_API_KEY=... python server.py
```

## Pubblicazione
Il file `render.yaml` descrive un servizio Render. Dopo la pubblicazione, configurare `OPENAI_API_KEY` tra le variabili segrete del servizio e copiare l'URL HTTPS assegnato. L'app Android dovrà usare quell'URL come endpoint. Non inserire mai la chiave API nell'APK o nel repository.

Il servizio applica un limite base di 5 richieste al minuto per IP e una cache di 15 minuti per provincia/periodo. Per un utilizzo pubblico più ampio sono necessari rate limit persistenti, monitoraggio dei consumi e protezione anti-abuso a livello hosting.
