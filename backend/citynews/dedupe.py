from __future__ import annotations

import hashlib
import re
import unicodedata


def canonical_url(url: str) -> str:
    return (url or "").strip().split("#", 1)[0].rstrip("/")


def title_fingerprint(title: str) -> str:
    value = unicodedata.normalize("NFKD", title or "")
    value = "".join(ch for ch in value if not unicodedata.combining(ch)).casefold()
    value = re.sub(r"[^a-z0-9]+", " ", value).strip()
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def dedupe_key(url: str, title: str) -> str:
    normalized_url = canonical_url(url)
    if normalized_url:
        return hashlib.sha256(normalized_url.encode("utf-8")).hexdigest()
    return title_fingerprint(title)
