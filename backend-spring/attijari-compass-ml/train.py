from ml_core import MODEL_PATH, build_training_dataframe, train_model


def main() -> None:
    training_dataframe = build_training_dataframe()
    if training_dataframe.empty:
        raise SystemExit("No integrated training data found. Provide the CSV and/or Excel dataset first.")

    _, sample_count = train_model(training_dataframe)
    print(f"Model trained successfully with {sample_count} samples.")
    print(f"Saved to {MODEL_PATH}")


if __name__ == "__main__":
    main()
