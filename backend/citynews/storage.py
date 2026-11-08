from __future__ import annotations

import json
import sqlite3
from pathlib import Path


SCHEMA = """
CREATE TABLE IF NOT EXISTS articles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dedupe_key TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    summary TEXT NOT NULL DEFAULT '',
    published_at TEXT,
    source_id TEXT NOT NULL,
    source_name TEXT NOT NULL,
    territory_ids TEXT NOT NULL DEFAULT '[]',
    collected_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_articles_published ON articles(published_at);
"""


class ArticleStore:
    def __init__(self, database_path: str | Path):
        self.database_path = str(database_path)
        Path(self.database_path).parent.mkdir(parents=True, exist_ok=True)
        with self.connect() as connection:
            connection.executescript(SCHEMA)

    def connect(self) -> sqlite3.Connection:
        connection = sqlite3.connect(self.database_path)
        connection.row_factory = sqlite3.Row
        return connection

    def insert_article(self, article: dict) -> bool:
        with self.connect() as connection:
            cursor = connection.execute(
                """INSERT OR IGNORE INTO articles
                (dedupe_key, title, url, summary, published_at, source_id, source_name, territory_ids)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                (
                    article["dedupe_key"], article["title"], article["url"],
                    article.get("summary", ""), article.get("published_at"),
                    article["source_id"], article["source_name"],
                    json.dumps(article.get("territory_ids", []), ensure_ascii=False),
                ),
            )
            return cursor.rowcount == 1

    def list_articles(self, limit: int = 50) -> list[dict]:
        with self.connect() as connection:
            rows = connection.execute(
                "SELECT * FROM articles ORDER BY COALESCE(published_at, collected_at) DESC LIMIT ?",
                (limit,),
            ).fetchall()
        result = []
        for row in rows:
            item = dict(row)
            item["territory_ids"] = json.loads(item["territory_ids"])
            result.append(item)
        return result
