import json

from citynews.dedupe import dedupe_key
from citynews.territory import article_territories, normalize_text
from citynews.collector import parse_feed


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
