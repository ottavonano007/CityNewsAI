import json
from pathlib import Path

import pytest

from citynews.dedupe import dedupe_key
from citynews.territory import article_territories, normalize_text
from citynews.collector import parse_feed


ROOT = Path(__file__).resolve().parents[1]
TERRITORIES = [
    {"id": "piacenza", "aliases": ["Piacenza", "provincia di Piacenza", "piacentino"]},
    {"id": "como", "aliases": ["Como", "provincia di Como", "comasco"]},
]


def test_normalize_accents_and_case():
    assert normalize_text("Città di PIACENZA") == "citta di piacenza"


def test_territory_matching():
    assert article_territories("Nuovi lavori a Piacenza", "", TERRITORIES) == ["piacenza"]
    assert article_territories("Intervento nel Comasco", "", TERRITORIES) == ["como"]
    assert article_territories("Notizia nazionale", "", TERRITORIES) == []


def test_all_current_italian_provinces_are_registered():
    data = json.loads((ROOT / "config/territories.json").read_text(encoding="utf-8"))
    territories = data["territories"]
    assert len(territories) == 110
    assert {item["province_code"] for item in territories} >= {"PC", "CO", "RM", "MI", "CA"}
    assert next(item for item in territories if item["province_code"] == "PC")["id"] == "piacenza"
    assert next(item for item in territories if item["province_code"] == "CO")["id"] == "como"


def test_dedupe_url_ignores_trailing_slash_and_fragment():
    assert dedupe_key("https://example.org/news/#top", "Titolo") == dedupe_key(
        "https://example.org/news", "Altro titolo"
    )


def test_parse_feed_filters_by_territory_and_keeps_source():
    xml = b"""<?xml version="1.0"?><rss version="2.0"><channel>
    <item><title>Nuovo ponte a Piacenza</title><link>https://example.org/1</link>
    <description>Intervento locale</description><pubDate>Sat, 07 Nov 2026 09:00:00 +0100</pubDate></item>
    <item><title>Notizia nazionale</title><link>https://example.org/2</link></item>
    </channel></rss>"""
    source = {"id": "test", "name": "Testata di prova"}
    result = parse_feed(xml, source, TERRITORIES)
    assert len(result) == 1
    assert result[0]["territory_ids"] == ["piacenza"]
    assert result[0]["source_name"] == "Testata di prova"


def test_parse_atom_iso_date_and_html_summary():
    xml = b"""<?xml version="1.0" encoding="utf-8"?>
    <feed xmlns="http://www.w3.org/2005/Atom">
      <title>Feed di prova</title>
      <entry>
        <title>Nuovo servizio a Como</title>
        <link href="https://example.org/atom-1" rel="alternate"/>
        <updated>2026-11-09T12:34:56+01:00</updated>
        <summary type="html">&lt;p&gt;Un &lt;strong&gt;nuovo servizio&lt;/strong&gt; per la città.&lt;/p&gt;</summary>
      </entry>
    </feed>"""
    source = {"id": "atom-test", "name": "Feed Atom"}
    result = parse_feed(xml, source, TERRITORIES)
    assert len(result) == 1
    assert result[0]["published_at"] == "2026-11-09T12:34:56+01:00"
    assert result[0]["summary"] == "Un nuovo servizio per la città."


def test_malformed_feed_without_entries_is_rejected():
    with pytest.raises(ValueError, match="Feed non valido"):
        parse_feed(b"<not-a-feed>", {"id": "broken", "name": "Rotto"}, TERRITORIES)
