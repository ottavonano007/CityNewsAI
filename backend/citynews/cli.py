from __future__ import annotations

import argparse
import json
from pathlib import Path

from .collector import collect_source, fetch_feed, parse_feed
from .config import load_sources, load_territories
from .storage import ArticleStore


ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    parser = argparse.ArgumentParser(description="City News AI RSS collector")
    parser.add_argument("--territories", default=str(ROOT / "config/territories.json"))
    parser.add_argument("--sources", default=str(ROOT / "config/sources.json"))
    parser.add_argument("--database", default=str(ROOT / "data/citynews.sqlite"))
    parser.add_argument("--list", action="store_true", help="List stored articles")
    args = parser.parse_args()

    territories = load_territories(args.territories)
    store = ArticleStore(args.database)
    if args.list:
        print(json.dumps(store.list_articles(), ensure_ascii=False, indent=2))
        return

    sources = load_sources(args.sources)
    if not sources:
        print("Nessuna fonte RSS attiva. Verificare e abilitare i feed in config/sources.json.")
        return
    for source in sources:
        try:
            print(json.dumps(collect_source(source, territories, store), ensure_ascii=False))
        except Exception as error:
            print(json.dumps({"source": source["name"], "error": str(error)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
