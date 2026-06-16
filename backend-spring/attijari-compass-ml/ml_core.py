from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import io
import re
import unicodedata

import joblib
import pandas as pd
from rapidfuzz import fuzz, process
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline


BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
MODEL_DIR = BASE_DIR / "model"
MODEL_PATH = MODEL_DIR / "transaction_category_model.joblib"
CSV_DATASET_PATH = DATA_DIR / "transactions_training_dataset.csv"
MERCHANT_EXCEL_PATH = DATA_DIR / "final_cleaned_and_updated.xlsx"
GOVERNORATE_EXCEL_PATH = DATA_DIR / "municipality_governorate.xlsx"

DEFAULT_CATEGORY = "AUTRES"

EXCEL_TO_COMPASS_CATEGORY = {
    "alimentation": "ALIMENTATION",
    "supermarche": "SUPERMARCHE",
    "supermarches": "SUPERMARCHE",
    "distribution": "DISTRIBUTION",
    "cafes": "CAFES",
    "cafe": "CAFES",
    "restaurant": "RESTAURANT",
    "restauration": "RESTAURANT",
    "restaurants": "RESTAURANT",
    "hotel": "HOTEL",
    "logement": "LOGEMENT",
    "loyer": "LOGEMENT",
    "voyage": "VOYAGE",
    "transport": "TRANSPORT",
    "shopping": "SHOPPING",
    "beaute": "BEAUTE",
    "sante": "SANTE",
    "education": "EDUCATION",
    "technologie": "TECHNOLOGIE",
    "livraison": "LIVRAISON",
    "operateurs telephoniques": "OPERATEURS_TELEPHONIQUES",
    "operateur telephonique": "OPERATEURS_TELEPHONIQUES",
    "steg sonede": "STEG_SONEDE",
    "nettoyage": "NETTOYAGE",
    "service auto": "SERVICE_AUTO",
    "station service": "STATION_SERVICES",
    "station services": "STATION_SERVICES",
    "banque": "BANQUE",
    "import export": "IMPORT_EXPORT",
    "autres": "AUTRES",
    "autre": "AUTRES",
    "divertissement": "DIVERTISSEMENT",
    "loisirs": "DIVERTISSEMENT",
    "facture": "FACTURES",
    "factures": "FACTURES",
    "abonnement": "TECHNOLOGIE",
    "abonnements": "TECHNOLOGIE",
    "transfert": "BANQUE",
    "frais bancaires": "BANQUE",
    "salaire": "SALAIRE",
    "epargne": "EPARGNE",
}

KNOWN_COMPASS_CATEGORIES = {
    "ALIMENTATION",
    "AUTRES",
    "BANQUE",
    "BEAUTE",
    "CAFES",
    "DISTRIBUTION",
    "DIVERTISSEMENT",
    "EPARGNE",
    "FACTURES",
    "HOTEL",
    "IMPORT_EXPORT",
    "LIVRAISON",
    "LOGEMENT",
    "NETTOYAGE",
    "OPERATEURS_TELEPHONIQUES",
    "RESTAURANT",
    "SALAIRE",
    "SANTE",
    "SERVICE_AUTO",
    "SHOPPING",
    "STATION_SERVICES",
    "STEG_SONEDE",
    "SUPERMARCHE",
    "TECHNOLOGIE",
    "TRANSPORT",
    "VOYAGE",
    "EDUCATION",
}


@dataclass(slots=True)
class RuntimeArtifacts:
    pipeline: Pipeline | None
    merchant_category_map: dict[str, str]
    merchant_names: list[str]
    governorate_map: dict[str, str]
    governorate_keys: list[str]
    training_samples: int
    bootstrap_message: str


def strip_accents(value: str | None) -> str:
    if value is None:
        return ""

    normalized = unicodedata.normalize("NFKD", str(value))
    return "".join(char for char in normalized if not unicodedata.combining(char))


def normalize_label(value: str | None) -> str:
    normalized = strip_accents(value).lower().strip()
    normalized = re.sub(r"[^\w\s/+-]", " ", normalized)
    normalized = re.sub(r"[_/+-]+", " ", normalized)
    normalized = re.sub(r"\s+", " ", normalized)
    return normalized.strip()


def normalize_text(value: str | None) -> str:
    normalized = strip_accents(value).lower().strip()
    normalized = re.sub(r"[^\w\s]", " ", normalized)
    normalized = re.sub(r"\s+", " ", normalized)
    return normalized.strip()


def clean_merchant_name(value: str | None) -> str:
    text = normalize_text(value)
    if not text:
        return ""

    if ">" in str(value or ""):
        text = normalize_text(str(value).split(">")[0])

    text = re.sub(r"\b(ste|sas|societe|ets|ltd|llc|inc|sa|sarl|company|co)\b", " ", text)
    text = re.sub(
        r"\b(tunis|sfax|sousse|nabeul|monastir|gabes|kairouan|ben arous|bizerte|gafsa|medenine|beja|jendouba|kasserine|kebili|mahdia|siliana|tozeur|zaghouan|manouba|tatouine|ariana)\b",
        " ",
        text,
    )
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def combine_text(merchant_name: str | None, description: str | None) -> str:
    merchant = clean_merchant_name(merchant_name)
    description_text = normalize_text(description)
    return normalize_text(f"{merchant} {description_text}".strip())


def map_external_category(value: str | None) -> str:
    normalized = normalize_label(value)

    if not normalized:
        return DEFAULT_CATEGORY

    upper_value = normalized.upper().replace(" ", "_")
    if upper_value in KNOWN_COMPASS_CATEGORIES:
        return upper_value

    return EXCEL_TO_COMPASS_CATEGORY.get(normalized, DEFAULT_CATEGORY)


def pipeline_uses_legacy_taxonomy(pipeline: Pipeline) -> bool:
    classifier = pipeline.named_steps.get("clf") if hasattr(pipeline, "named_steps") else None
    classes = getattr(classifier, "classes_", None)

    if classes is None:
        return False

    for value in classes:
        normalized_existing = normalize_label(str(value)).upper().replace(" ", "_")
        mapped = map_external_category(str(value))
        if mapped not in KNOWN_COMPASS_CATEGORIES or mapped != normalized_existing:
            return True

    return False


def load_csv_training_rows() -> pd.DataFrame:
    if not CSV_DATASET_PATH.exists():
        return pd.DataFrame(columns=["merchantName", "description", "category"])

    dataframe = pd.read_csv(CSV_DATASET_PATH)
    dataframe["merchantName"] = dataframe.get("merchantName", "").fillna("").astype(str)
    dataframe["description"] = dataframe.get("description", "").fillna("").astype(str)
    dataframe["category"] = dataframe.get("category", DEFAULT_CATEGORY).fillna(DEFAULT_CATEGORY).astype(str)
    dataframe["category"] = dataframe["category"].map(map_external_category)
    return dataframe[["merchantName", "description", "category"]]


def load_excel_merchant_rows() -> pd.DataFrame:
    if not MERCHANT_EXCEL_PATH.exists():
        return pd.DataFrame(columns=["merchantName", "description", "category"])

    dataframe = pd.read_excel(MERCHANT_EXCEL_PATH)
    if len(dataframe.columns) < 2:
        return pd.DataFrame(columns=["merchantName", "description", "category"])

    merchant_column = dataframe.columns[0]
    category_column = dataframe.columns[1]

    normalized = pd.DataFrame(
        {
            "merchantName": dataframe[merchant_column].fillna("").astype(str),
            "description": dataframe[merchant_column].fillna("").astype(str),
            "category": dataframe[category_column].fillna(DEFAULT_CATEGORY).astype(str).map(map_external_category),
        }
    )
    return normalized


def build_training_dataframe() -> pd.DataFrame:
    sources = [load_csv_training_rows(), load_excel_merchant_rows()]
    dataframe = pd.concat(sources, ignore_index=True)

    if dataframe.empty:
        return pd.DataFrame(columns=["merchantName", "description", "category", "text"])

    dataframe["merchantName"] = dataframe["merchantName"].fillna("").astype(str)
    dataframe["description"] = dataframe["description"].fillna("").astype(str)
    dataframe["category"] = dataframe["category"].fillna(DEFAULT_CATEGORY).astype(str)
    dataframe["text"] = dataframe.apply(
        lambda row: combine_text(row["merchantName"], row["description"]),
        axis=1,
    )
    dataframe = dataframe[dataframe["text"].str.len() > 1]
    dataframe = dataframe[dataframe["category"].isin(KNOWN_COMPASS_CATEGORIES)]
    dataframe = dataframe.drop_duplicates(subset=["text", "category"]).reset_index(drop=True)
    return dataframe


def build_merchant_category_map() -> tuple[dict[str, str], list[str]]:
    merchant_rows = load_excel_merchant_rows()
    if merchant_rows.empty:
        return {}, []

    merchant_rows["normalizedMerchant"] = merchant_rows["merchantName"].map(clean_merchant_name)
    merchant_rows = merchant_rows[merchant_rows["normalizedMerchant"].str.len() > 1]

    grouped = (
        merchant_rows.groupby("normalizedMerchant")["category"]
        .agg(lambda values: values.mode().iat[0] if not values.mode().empty else values.iloc[0])
        .to_dict()
    )
    merchant_names = sorted(grouped.keys())
    return grouped, merchant_names


def build_pipeline() -> Pipeline:
    return Pipeline(
        steps=[
            (
                "tfidf",
                TfidfVectorizer(
                    analyzer="char_wb",
                    ngram_range=(3, 5),
                    lowercase=True,
                ),
            ),
            (
                "clf",
                LogisticRegression(
                    max_iter=2000,
                    random_state=42,
                ),
            ),
        ]
    )


def train_model(dataframe: pd.DataFrame | None = None) -> tuple[Pipeline, int]:
    training_dataframe = dataframe if dataframe is not None else build_training_dataframe()
    if training_dataframe.empty:
        raise ValueError("No training data available for the categorization model.")

    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    pipeline = build_pipeline()
    pipeline.fit(training_dataframe["text"], training_dataframe["category"])
    joblib.dump(pipeline, MODEL_PATH)
    return pipeline, int(len(training_dataframe))


def load_pipeline() -> Pipeline | None:
    if not MODEL_PATH.exists():
        return None

    loaded = joblib.load(MODEL_PATH)
    return loaded if isinstance(loaded, Pipeline) else None


def ensure_pipeline() -> tuple[Pipeline | None, int, str]:
    training_dataframe = build_training_dataframe()
    if training_dataframe.empty:
        return None, 0, "No ML dataset found. Predictions will rely on fuzzy matching only."

    pipeline = load_pipeline()
    if pipeline is not None and not pipeline_uses_legacy_taxonomy(pipeline):
        return pipeline, int(len(training_dataframe)), "Existing ML model loaded successfully."

    pipeline, sample_count = train_model(training_dataframe)
    return pipeline, sample_count, "ML model trained automatically from the integrated datasets."


def load_governorate_lookup() -> tuple[dict[str, str], list[str]]:
    if not GOVERNORATE_EXCEL_PATH.exists():
        return {}, []

    dataframe = pd.read_excel(GOVERNORATE_EXCEL_PATH)
    if len(dataframe.columns) < 2:
        return {}, []

    municipality_column = dataframe.columns[0]
    governorate_column = dataframe.columns[1]
    dataframe = dataframe[[municipality_column, governorate_column]].copy()
    dataframe.columns = ["Municipality", "Governorate"]
    dataframe["Municipality"] = dataframe["Municipality"].fillna("").astype(str).map(normalize_text)
    dataframe["Governorate"] = dataframe["Governorate"].fillna("").astype(str).map(lambda value: strip_accents(value).upper().strip())
    dataframe = dataframe[dataframe["Municipality"].str.len() > 1]

    known_map = dict(zip(dataframe["Municipality"], dataframe["Governorate"]))
    return known_map, sorted(known_map.keys())


def bootstrap_runtime() -> RuntimeArtifacts:
    pipeline, sample_count, message = ensure_pipeline()
    merchant_category_map, merchant_names = build_merchant_category_map()
    governorate_map, governorate_keys = load_governorate_lookup()

    return RuntimeArtifacts(
        pipeline=pipeline,
        merchant_category_map=merchant_category_map,
        merchant_names=merchant_names,
        governorate_map=governorate_map,
        governorate_keys=governorate_keys,
        training_samples=sample_count,
        bootstrap_message=message,
    )


def predict_with_runtime(
    runtime: RuntimeArtifacts,
    merchant_name: str | None,
    description: str | None,
) -> tuple[str, float, str]:
    normalized_text = combine_text(merchant_name, description)
    normalized_merchant = clean_merchant_name(merchant_name)

    if normalized_merchant and normalized_merchant in runtime.merchant_category_map:
        return map_external_category(runtime.merchant_category_map[normalized_merchant]), 0.99, normalized_text

    if normalized_merchant and runtime.merchant_names:
        fuzzy_match = process.extractOne(
            normalized_merchant,
            runtime.merchant_names,
            scorer=fuzz.token_set_ratio,
        )
        if fuzzy_match and fuzzy_match[1] >= 92:
            matched_name = str(fuzzy_match[0])
            matched_category = runtime.merchant_category_map.get(matched_name, DEFAULT_CATEGORY)
            return map_external_category(matched_category), round(float(fuzzy_match[1]) / 100, 2), normalized_text

    if runtime.pipeline is not None and normalized_text:
        predicted_category = str(runtime.pipeline.predict([normalized_text])[0])
        confidence = 0.55

        classifier = runtime.pipeline.named_steps.get("clf") if hasattr(runtime.pipeline, "named_steps") else None
        if classifier is not None and hasattr(classifier, "predict_proba"):
            probabilities = runtime.pipeline.predict_proba([normalized_text])[0]
            best_index = int(probabilities.argmax())
            predicted_category = str(classifier.classes_[best_index])
            confidence = float(probabilities[best_index])

        return map_external_category(predicted_category), round(confidence, 2), normalized_text

    if normalized_merchant and runtime.merchant_names:
        fuzzy_match = process.extractOne(
            normalized_merchant,
            runtime.merchant_names,
            scorer=fuzz.token_set_ratio,
        )
        if fuzzy_match:
            matched_name = str(fuzzy_match[0])
            matched_category = runtime.merchant_category_map.get(matched_name, DEFAULT_CATEGORY)
            return map_external_category(matched_category), round(float(fuzzy_match[1]) / 100, 2), normalized_text

    return DEFAULT_CATEGORY, 0.0, normalized_text


def extract_location_fragment(value: str | None) -> str:
    raw_value = str(value or "")
    parts = [part.strip() for part in raw_value.split(">") if part.strip()]

    if len(parts) >= 2 and parts[-1].upper() in {"TN", "TUN"}:
        return normalize_text(parts[-2])

    if len(parts) >= 1:
        return normalize_text(parts[-1])

    return normalize_text(raw_value)


def predict_governorate(runtime: RuntimeArtifacts, value: str | None) -> tuple[str | None, float | None]:
    location_fragment = extract_location_fragment(value)

    if not location_fragment or not runtime.governorate_keys:
        return None, None

    direct_match = runtime.governorate_map.get(location_fragment)
    if direct_match:
        return direct_match, 1.0

    fuzzy_match = process.extractOne(
        location_fragment,
        runtime.governorate_keys,
        scorer=fuzz.token_set_ratio,
    )
    if not fuzzy_match or fuzzy_match[1] < 70:
        return None, None

    municipality = str(fuzzy_match[0])
    governorate = runtime.governorate_map.get(municipality)
    if not governorate:
        return None, None

    return governorate, round(float(fuzzy_match[1]) / 100, 2)


def read_input_table(file_name: str, payload: bytes) -> pd.DataFrame:
    suffix = Path(file_name or "input.xlsx").suffix.lower()
    buffer = io.BytesIO(payload)

    if suffix == ".csv":
        return pd.read_csv(buffer)

    return pd.read_excel(buffer)


def resolve_merchant_column(dataframe: pd.DataFrame) -> str:
    normalized_columns = {
        column: normalize_label(column)
        for column in dataframe.columns
    }

    preferred_labels = [
        "merchantname",
        "merchant name",
        "merchant",
        "affil name",
        "affiliation",
        "description",
    ]

    for preferred_label in preferred_labels:
        for original_name, normalized_name in normalized_columns.items():
            if normalized_name == preferred_label:
                return original_name

    return dataframe.columns[0]


def build_batch_prediction_table(runtime: RuntimeArtifacts, input_dataframe: pd.DataFrame) -> pd.DataFrame:
    if input_dataframe.empty:
        return pd.DataFrame(
            columns=[
                "Merchant Name",
                "Predicted Category",
                "Confidence",
                "Localization",
                "Localization Confidence",
            ]
        )

    merchant_column = resolve_merchant_column(input_dataframe)
    working_copy = input_dataframe.copy()
    working_copy[merchant_column] = working_copy[merchant_column].fillna("").astype(str)
    working_copy = working_copy[working_copy[merchant_column].str.strip().str.len() > 0]

    rows: list[dict[str, object]] = []
    for merchant_name in working_copy[merchant_column].tolist():
        predicted_category, confidence, _ = predict_with_runtime(runtime, merchant_name, "")
        governorate, governorate_confidence = predict_governorate(runtime, merchant_name)
        rows.append(
            {
                "Merchant Name": merchant_name,
                "Predicted Category": predicted_category,
                "Confidence": confidence,
                "Localization": governorate or "",
                "Localization Confidence": governorate_confidence if governorate_confidence is not None else "",
            }
        )

    return pd.DataFrame(rows)


def dataframe_to_excel_bytes(dataframe: pd.DataFrame) -> bytes:
    buffer = io.BytesIO()
    with pd.ExcelWriter(buffer, engine="openpyxl") as writer:
        dataframe.to_excel(writer, index=False, sheet_name="Predictions")
    buffer.seek(0)
    return buffer.getvalue()


def summarize_available_sources() -> dict[str, bool]:
    return {
        "csvDataset": CSV_DATASET_PATH.exists(),
        "merchantExcelDataset": MERCHANT_EXCEL_PATH.exists(),
        "governorateExcelDataset": GOVERNORATE_EXCEL_PATH.exists(),
        "modelFile": MODEL_PATH.exists(),
    }
