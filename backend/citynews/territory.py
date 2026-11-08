from __future__ import annotations

import re
import unicodedata


def normalize_text(value: str) -> str:
    value = unicodedata.normalize("NFKD", value or "")
    value = "".join(ch for ch in value if not unicodedata.combining(ch))
    value = value.casefold()
    return re.sub(r"[^a-z0-9]+", " ", value).strip()


def article_territories(title: str, summary: str, territories: list[dict]) -> list[str]:
    """Return territory IDs whose explicit configured aliases occur in title/summary.

    Deliberately conservative: no city is inferred from a publisher's location.
    """
    text = f" {normalize_text(title)} {normalize_text(summary)} "
    matches: list[str] = []
    for territory in territories:
        aliases = territory.get("aliases", [])
        for alias in aliases:
            needle = f" {normalize_text(alias)} "
            if needle.strip() and needle in text:
                matches.append(territory["id"])
                break
    return matches
