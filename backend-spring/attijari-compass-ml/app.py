import io

from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from ml_core import (
    bootstrap_runtime,
    build_batch_prediction_table,
    build_training_dataframe,
    dataframe_to_excel_bytes,
    predict_with_runtime,
    read_input_table,
    summarize_available_sources,
    train_model,
)


app = FastAPI(title="Attijari Compass ML", version="2.0.0")


class PredictRequest(BaseModel):
    merchantName: str | None = Field(default="", max_length=255)
    description: str | None = Field(default="", max_length=1000)


class PredictResponse(BaseModel):
    category: str
    confidence: float
    normalizedText: str


RUNTIME = bootstrap_runtime()


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok" if RUNTIME.pipeline is not None or RUNTIME.merchant_names else "degraded",
        "message": RUNTIME.bootstrap_message,
        "trainingSamples": RUNTIME.training_samples,
        "merchantDictionarySize": len(RUNTIME.merchant_names),
        "availableSources": summarize_available_sources(),
    }


@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest) -> PredictResponse:
    category, confidence, normalized_text = predict_with_runtime(
        RUNTIME,
        request.merchantName,
        request.description,
    )

    return PredictResponse(
        category=category,
        confidence=confidence,
        normalizedText=normalized_text,
    )


@app.post("/predict-batch")
async def predict_batch(
    request: Request,
    x_file_name: str | None = Header(default="merchant-input.xlsx", alias="X-File-Name"),
) -> StreamingResponse:
    payload = await request.body()
    if not payload:
        raise HTTPException(status_code=400, detail="The uploaded file is empty.")

    try:
        input_dataframe = read_input_table(x_file_name or "merchant-input.xlsx", payload)
        prediction_dataframe = build_batch_prediction_table(RUNTIME, input_dataframe)
        excel_payload = dataframe_to_excel_bytes(prediction_dataframe)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Unable to process the uploaded file: {exc}") from exc

    output_name = "merchant_predictions_integrated.xlsx"
    return StreamingResponse(
        io.BytesIO(excel_payload),
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={
            "Content-Disposition": f'attachment; filename="{output_name}"',
        },
    )


@app.post("/retrain")
def retrain() -> dict:
    try:
        training_dataframe = build_training_dataframe()
        pipeline, sample_count = train_model(training_dataframe)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Unable to retrain the model: {exc}") from exc

    global RUNTIME
    RUNTIME = bootstrap_runtime()
    model_available = pipeline is not None

    return {
        "status": "ok" if model_available else "degraded",
        "message": "ML model retrained successfully." if model_available else "Retraining completed without a usable model.",
        "trainingSamples": sample_count,
        "availableSources": summarize_available_sources(),
    }
