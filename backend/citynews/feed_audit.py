from __future__ import annotations

import json
import urllib.error
import urllib.request
from pathlib import Path

import feedparser

from .territory import article_territories


ROOT = Path(__file__).resolve().parents[1]
CANDIDATES = ROOT / "config" / "feed_candidates.json"
TERRITORIES = ROOT / "config" / "territories.json"
USER_AGENT = "CityNewsAI-FeedAudit/0.1 (RSS compatibility check)"
TIMEOUT_SECONDS = 15
MAX_BYTES = 5 * 1024 * 1024


def inspect_feed(url: str, source: dict, territory: dict) -> dict:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/rss+xml, application/atom+xml, application/xml, text/xml, */*",
        },
    )
    result = {
        "territory": territory["id"],
        "source_id": source["id"],
        "source": source["name"],
        "url": url,
        "http_status": None,
        "content_type": None,
        "feed_title": None,
        "entries": 0,
        "territory_matches": 0,
        "sample_entries": [],
        "latest_title": None,
        "latest_link": None,
        "valid_feed": False,
        "error": None,
    }
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            result["http_status"] = response.status
            result["content_type"] = response.headers.get("Content-Type", "")
            payload = response.read(MAX_BYTES + 1)
            if len(payload) > MAX_BYTES:
                raise ValueError(f"Risposta superiore al limite di {MAX_BYTES} byte")
        parsed = feedparser.parse(payload)
        result["feed_title"] = parsed.feed.get("title")
        result["entries"] = len(parsed.entries)
        if parsed.entries:
            latest = parsed.entries[0]
            result["latest_title"] = latest.get("title")
            result["latest_link"] = latest.get("link")
            matched = 0
            for entry in parsed.entries:
                title = (entry.get("title") or "").strip()
                summary = (entry.get("summary") or entry.get("description") or "").strip()
                assigned = article_territories(title, summary, [territory])
                if assigned:
                    matched += 1
                if len(result["sample_entries"]) < 5:
                    result["sample_entries"].append({
                        "title": title,
                        "territory_match": bool(assigned),
                    })
            result["territory_matches"] = matched
        result["valid_feed"] = bool(
            result["http_status"] == 200
            and not (parsed.bozo and not parsed.entries)
            and parsed.entries
            and result["feed_title"]
        )
        if parsed.bozo:
            result["error"] = str(parsed.bozo_exception)[:240]
    except urllib.error.HTTPError as exc:
        result["http_status"] = exc.code
        result["error"] = f"HTTP {exc.code}: {exc.reason}"
    except Exception as exc:
        result["error"] = f"{type(exc).__name__}: {exc}"[:300]
    return result


def main() -> None:
    config = json.loads(CANDIDATES.read_text(encoding="utf-8"))
    territory_config = json.loads(TERRITORIES.read_text(encoding="utf-8"))
    territory_by_id = {item["id"]: item for item in territory_config["territories"]}
    results = []
    for territory_group in config["territories"]:
        territory = territory_by_id[territory_group["id"]]
        for source in territory_group["sources"]:
            for url in source["urls"]:
                item = inspect_feed(url, source, territory)
                results.append(item)
                print(json.dumps(item, ensure_ascii=False))
    valid = sum(1 for item in results if item["valid_feed"])
    print(json.dumps({
        "summary": {
            "checked": len(results),
            "valid": valid,
            "invalid_or_unavailable": len(results) - valid,
        }
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
