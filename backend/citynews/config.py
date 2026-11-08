from __future__ import annotations

import json
from pathlib import Path
from typing import Any


def load_json(path: str | Path) -> Any:
    with Path(path).open("r", encoding="utf-8") as handle:
        return json.load(handle)


def load_territories(path: str | Path) -> list[dict]:
    data = load_json(path)
    return data.get("territories", [])


def load_sources(path: str | Path) -> list[dict]:
    data = load_json(path)
    return [source for source in data.get("sources", []) if source.get("enabled") and source.get("feed_url")]
