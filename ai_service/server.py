from __future__ import annotations

import json
import os
import time
import urllib.error
import urllib.request
from collections import defaultdict, deque
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Lock

OPENAI_URL = "https://api.openai.com/v1/responses"
MODEL = os.getenv("OPENAI_MODEL", "gpt-6.1-terra")
PORT = int(os.getenv("PORT", "8080"))
MAX_BODY = 8192

PERIODS = {
    "24h": "ultime 24 ore",
    "3d": "ultimi 3 giorni",
    "7d": "ultima settimana",
    "1m": "ultimo mese",
    "6m": "ultimi 6 mesi",
    "1y": "ultimo anno",
}

# Lightweight process-local protection. Production deployments should also
# enable provider-level rate limits and monitoring.
_requests: dict[str, deque[float]] = defaultdict(deque)
_cache: dict[str, tuple[float, dict]] = {}
_guard = Lock()


def build_prompt(city: str, province: str, region: str, period: str) -> str:
    return f"""Agisci come una redazione giornalistica professionale.

Dammi le notizie più importanti, interessanti e rilevanti della città di {city}
e della relativa provincia di {province} ({region}), relative al periodo di {period}.

Organizza le notizie per tipologia e disponile in ordine decrescente di importanza
pubblica, privilegiando l'impatto concreto sulla comunità locale.

Per ogni notizia:
- Scrivi un titolo chiaro e un riassunto accurato, imparziale e autonomo.
- Indica data e luogo dei fatti quando verificabili.
- Cita le fonti originali con collegamenti diretti agli articoli o ai comunicati.
- Distingui fatti accertati, dichiarazioni, previsioni e ipotesi.

Verifica le informazioni consultando le fonti originali e, quando possibile,
almeno un riscontro indipendente. Dai priorità a testate locali autorevoli,
agenzie di stampa e fonti istituzionali. Verifica che ogni collegamento citato
esista e sia pertinente alla notizia.

Non inventare fatti, dichiarazioni, numeri, date, luoghi o fonti. Non dedurre
dettagli non presenti nelle fonti. Escludi le notizie prive di riscontri
sufficienti e segnala esplicitamente eventuali incertezze. Elimina i duplicati.
Se non trovi notizie attendibili, dichiaralo senza riempire la rassegna.

Usa sezioni tematiche pertinenti (cronaca, politica e amministrazione, economia
e lavoro, territorio e ambiente, sanità, scuola, cultura e società, sport).
Non creare sezioni vuote. Ordina le notizie per rilevanza pubblica, non soltanto
per data. Concludi con fonti consultate e data/ora della ricerca.

La ricerca deve riguardare fatti pubblicati o avvenuti nel periodo indicato.
Rispondi in italiano, in formato leggibile su smartphone."""


def extract_response(data: dict) -> tuple[str, list[dict]]:
    texts: list[str] = []
    sources: list[dict] = []
    seen: set[str] = set()
    for item in data.get("output", []):
        if item.get("type") != "message":
            continue
        for part in item.get("content", []):
            if part.get("type") != "output_text":
                continue
            if part.get("text"):
                texts.append(part["text"])
            for annotation in part.get("annotations", []):
                if annotation.get("type") != "url_citation":
                    continue
                url = annotation.get("url", "")
                if not isinstance(url, str) or not url.startswith("https://") or url in seen:
                    continue
                seen.add(url)
                sources.append({
                    "title": annotation.get("title") or url,
                    "url": url,
                })
    return "\n\n".join(texts).strip(), sources


class Handler(BaseHTTPRequestHandler):
    server_version = "CityNewsAI/1.0"

    def _send(self, status: int, payload: dict) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        if self.path == "/health":
            self._send(200, {"ok": True, "service": "citynewsai-briefing"})
        else:
            self._send(404, {"error": "Not found"})

    def do_POST(self) -> None:
        if self.path != "/api/briefing":
            self._send(404, {"error": "Not found"})
            return
        api_key = os.getenv("OPENAI_API_KEY", "").strip()
        if not api_key:
            self._send(503, {"error": "Servizio AI non configurato"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_BODY:
                self._send(413, {"error": "Richiesta non valida"})
                return
            payload = json.loads(self.rfile.read(length))
        except Exception:
            self._send(400, {"error": "JSON non valido"})
            return

        city = str(payload.get("city", "")).strip()[:100]
        province = str(payload.get("province", "")).strip()[:100]
        region = str(payload.get("region", "")).strip()[:100]
        period_id = str(payload.get("period", "24h")).strip()
        if not city or not province or not region or period_id not in PERIODS:
            self._send(400, {"error": "Provincia, città, regione e periodo sono obbligatori"})
            return

        client_ip = self.client_address[0]
        now = time.time()
        with _guard:
            bucket = _requests[client_ip]
            while bucket and now - bucket[0] > 60:
                bucket.popleft()
            if len(bucket) >= 5:
                self._send(429, {"error": "Troppe ricerche ravvicinate. Riprova tra poco."})
                return
            bucket.append(now)

        cache_key = f"{city}|{province}|{period_id}".lower()
        with _guard:
            cached = _cache.get(cache_key)
            if cached and now - cached[0] < 900:
                self._send(200, cached[1])
                return

        prompt = build_prompt(city, province, region, PERIODS[period_id])
        request_body = {
            "model": MODEL,
            "tools": [{"type": "web_search"}],
            "tool_choice": "auto",
            "input": [{
                "role": "user",
                "content": prompt
            }],
            "max_output_tokens": 3500,
        }
        request = urllib.request.Request(
            OPENAI_URL,
            data=json.dumps(request_body).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                result = json.loads(response.read().decode("utf-8"))
            text, sources = extract_response(result)
            if not text:
                raise ValueError("La ricerca non ha prodotto un testo leggibile")
            output = {
                "text": text,
                "sources": sources,
                "city": city,
                "province": province,
                "period": period_id,
                "generated_at": datetime.now(timezone.utc).isoformat(),
                "model": MODEL,
            }
            with _guard:
                _cache[cache_key] = (time.time(), output)
            self._send(200, output)
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")[:1000]
            print(f"OpenAI HTTP {error.code}: {detail}", flush=True)
            self._send(502, {"error": "Il servizio di ricerca AI non ha risposto correttamente"})
        except Exception as error:
            print(f"Briefing error: {error}", flush=True)
            self._send(502, {"error": "Impossibile generare la rassegna. Riprova più tardi."})

    def log_message(self, format: str, *args) -> None:
        print("%s - %s" % (self.address_string(), format % args), flush=True)


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
