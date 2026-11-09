from __future__ import annotations

import re
import time
import urllib.request
from datetime import datetime
from email.utils import parsedate_to_datetime
from html import unescape
from html.parser import HTMLParser

import feedparser

from .dedupe import canonical_url, dedupe_key
from .territory import article_territories


class _SummaryText(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.parts: list[str] = []

    def handle_data(self, data: str) -> None:
        self.parts.append(data)


def _clean_summary(value: str) -> str:
    parser = _SummaryText()
    try:
        parser.feed(value or "")
        parser.close()
        text = " ".join(parser.parts)
    except Exception:
        text = re.sub(r"<[^>]+>", " ", value or "")
    return re.sub(r"\s+", " ", unescape(text)).strip()


USER_AGENT = "CityNewsAI/0.1 (+local-news-aggregator)"


def fetch_feed(url: str, timeout: int = 20) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return response.read()


def _published(entry: dict) -> str | None:
    for key in ("published", "updated"):
        value = entry.get(key)
        if not value:
            continue
        try:
            return parsedate_to_datetime(value).isoformat()
        except (TypeError, ValueError, OverflowError):
            try:
                return datetime.fromisoformat(value.replace("Z", "+00:00")).isoformat()
            except (TypeError, ValueError, OverflowError):
                pass
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
        summary = _clean_summary(entry.get("summary") or entry.get("description") or "")
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
