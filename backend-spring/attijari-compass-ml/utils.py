import re
import unicodedata


LATIN_DIACRITICS_PATTERN = re.compile(r"[\u0300-\u036f]")
PUNCTUATION_PATTERN = re.compile(r"[^\w\s\u0600-\u06FF]", re.UNICODE)
WHITESPACE_PATTERN = re.compile(r"\s+")
LATIN_PATTERN = re.compile(r"[A-Za-z]")


def normalize_text(text: str | None) -> str:
    if text is None:
        text = ""

    normalized = unicodedata.normalize("NFKC", str(text)).strip()
    if LATIN_PATTERN.search(normalized):
        normalized = normalized.lower()
        normalized = unicodedata.normalize("NFD", normalized)
        normalized = LATIN_DIACRITICS_PATTERN.sub("", normalized)
        normalized = unicodedata.normalize("NFC", normalized)

    normalized = PUNCTUATION_PATTERN.sub(" ", normalized)
    normalized = WHITESPACE_PATTERN.sub(" ", normalized).strip()
    return normalized


def combine_text(merchant_name: str | None, description: str | None) -> str:
    merchant = normalize_text(merchant_name)
    details = normalize_text(description)
    return normalize_text(f"{merchant} {details}".strip())
