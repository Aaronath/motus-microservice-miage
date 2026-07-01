#!/usr/bin/env python3
"""Construit data/lexique-motus-5-9.csv depuis Lexique383 (style Motus TV)."""
from __future__ import annotations

import csv
import gzip
import re
import sys
import unicodedata
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = ROOT / "data"
OUT_CSV = DATA_DIR / "lexique-motus-5-9.csv"
OUT_GZ = DATA_DIR / "lexique-motus-5-9.csv.gz"
LEXIQUE_URL = "http://www.lexique.org/databases/Lexique383/Lexique383.tsv"
CACHE_TSV = DATA_DIR / "Lexique383.tsv"

MIN_LEN = 5
MAX_LEN = 9
ALLOWED_CGRAM = frozenset({"NOM", "ADJ", "VER", "ADV"})


def normalize(w: str) -> str:
    w = unicodedata.normalize("NFD", w.strip())
    w = "".join(c for c in w if not unicodedata.combining(c))
    return w.upper()


def fetch_lexique_tsv() -> Path:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    if CACHE_TSV.exists() and CACHE_TSV.stat().st_size > 1_000_000:
        return CACHE_TSV
    print(f"Téléchargement {LEXIQUE_URL} …")
    try:
        import ssl
        ctx = ssl.create_default_context()
        with urllib.request.urlopen(LEXIQUE_URL, context=ctx, timeout=120) as r:
            CACHE_TSV.write_bytes(r.read())
    except Exception as e:
        if CACHE_TSV.exists():
            print(f"Utilisation du cache local ({e})")
            return CACHE_TSV
        raise SystemExit(
            f"Impossible de télécharger Lexique383 : {e}\n"
            f"Placez Lexique383.tsv dans {CACHE_TSV}"
        ) from e
    return CACHE_TSV


def parse_float(s: str) -> float:
    try:
        return float(s.replace(",", "."))
    except (ValueError, AttributeError):
        return 0.0


def cgram_allowed(cgram: str) -> bool:
    if not cgram:
        return False
    tags = [t.strip() for t in cgram.split(",")]
    if any(t == "PRO" or t.startswith("PRO") for t in tags):
        return False
    return any(t in ALLOWED_CGRAM or any(t.startswith(a) for a in ALLOWED_CGRAM) for t in tags)


def build_lexicon(tsv_path: Path) -> dict[str, int]:
    words: dict[str, int] = {}
    with tsv_path.open(encoding="utf-8", errors="replace") as f:
        reader = csv.DictReader(f, delimiter="\t")
        if not reader.fieldnames or "ortho" not in reader.fieldnames:
            raise SystemExit("Lexique383.tsv : en-tête invalide (colonne ortho attendue)")
        has_freq = "freqlemfilms2" in reader.fieldnames
        has_nblettres = "nblettres" in reader.fieldnames
        for row in reader:
            ortho = row.get("ortho", "")
            w = normalize(re.sub(r"[^A-Za-z]", "", ortho))
            if not w.isalpha():
                continue
            if has_nblettres:
                try:
                    nlet = int(float(row["nblettres"]))
                except (ValueError, TypeError):
                    nlet = len(w)
            else:
                nlet = len(w)
            if nlet < MIN_LEN or nlet > MAX_LEN or len(w) != nlet:
                continue
            if not cgram_allowed(row.get("cgram", "")):
                continue
            if has_freq and parse_float(row.get("freqlemfilms2", "0")) <= 0:
                continue
            words[w] = nlet
    return words


def write_outputs(words: dict[str, int]) -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    rows = sorted(words.items())
    with OUT_CSV.open("w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["word", "length"])
        w.writerows(rows)
    with gzip.open(OUT_GZ, "wt", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["word", "length"])
        w.writerows(rows)
    by_len: dict[int, int] = {}
    for _, ln in rows:
        by_len[ln] = by_len.get(ln, 0) + 1
    print(f"Écrit {OUT_CSV} ({len(rows)} mots)")
    print(f"Écrit {OUT_GZ}")
    print("Par longueur:", dict(sorted(by_len.items())))


def main() -> None:
    tsv = fetch_lexique_tsv()
    words = build_lexicon(tsv)
    if len(words) < 5000:
        sys.exit(f"Lexique trop petit ({len(words)} mots) — vérifiez le fichier source")
    write_outputs(words)


if __name__ == "__main__":
    main()
