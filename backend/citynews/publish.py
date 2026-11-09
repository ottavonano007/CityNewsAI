from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

from .collector import collect_source
from .config import load_sources, load_territories
from .storage import ArticleStore

ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = ROOT.parent
OUTPUT = REPO_ROOT / "docs" / "data" / "news.json"


def main() -> None:
    territories = load_territories(ROOT / "config" / "territories.json")
    sources = load_sources(ROOT / "config" / "sources.json")
    store = ArticleStore(ROOT / "data" / "citynews.sqlite")
    results = []
    for source in sources:
        try:
            results.append({"source": source["name"], **collect_source(source, territories, store), "ok": True})
        except Exception as error:
            results.append({"source": source["name"], "ok": False, "error": str(error)[:240]})

    territory_by_id = {item["id"]: item for item in territories}
    articles = []
    for article in store.list_articles(limit=1000):
        codes = []
        for territory_id in article.get("territory_ids", []):
            territory = territory_by_id.get(territory_id)
            if territory:
                codes.append(territory.get("province_code", "").upper())
        article["territory_codes"] = codes
        article.pop("dedupe_key", None)
        article.pop("id", None)
        article.pop("collected_at", None)
        articles.append(article)

    payload = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "source_status": results,
        "articles": articles[:500],
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"published_articles": len(articles[:500]), "sources": results}, ensure_ascii=False))


if __name__ == "__main__":
    main()
