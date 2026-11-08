from __future__ import annotations

import time
import urllib.request
from email.utils import parsedate_to_datetime

import feedparser

from .dedupe import canonical_url, dedupe_key
from .territory import article_territories


USER_AGENT = "CityNewsAI/0.1 (+local-news-aggregator)"


def fetch_feed(url: str, timeout: int = 20) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return response.read()


def _published(entry: dict) -> str | None:
    value = entry.get("published") or entry.get("updated")
    if not value:
        return None
    try:
        return parsedate_to_datetime(value).isoformat()
    except (TypeError, ValueError, OverflowError):
        parsed = entry.get("published_parsed") or entry.get("updated_parsed")
        if parsed:
            return time.strftime("%Y-%m-%dT%H:%M:%S%z", parsed)
        return None


def parse_feed(
    content: bytes,
    source: dict,
    territories: list[dict],
) -> list[dict]:
    parsed = feedparser.parse(content)
    if parsed.bozo and not parsed.entries:
        raise ValueError(f"Feed non valido per {source.get('name', source['id'])}")

    articles = []
    for entry in parsed.entries:
        title = (entry.get("title") or "").strip()
        url = canonical_url(entry.get("link", ""))
        summary = (entry.get("summary") or entry.get("description") or "").strip()
        if not title or not url:
            continue
        territory_ids = article_territories(title, summary, territories)
        if not territory_ids:
            continue
        articles.append({
            "dedupe_key": dedupe_key(url, title),
            "title": title,
            "url": url,
            "summary": summary,
            "published_at": _published(entry),
            "source_id": source["id"],
            "source_name": source["name"],
            "territory_ids": territory_ids,
        })
    return articles


def collect_source(source: dict, territories: list[dict], store) -> dict:
    content = fetch_feed(source["feed_url"])
    articles = parse_feed(content, source, territories)
    inserted = sum(1 for article in articles if store.insert_article(article))
    return {"source": source["name"], "parsed": len(articles), "inserted": inserted}
