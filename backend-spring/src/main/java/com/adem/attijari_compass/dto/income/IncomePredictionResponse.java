package com.adem.attijari_compass.dto.income;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IncomePredictionResponse {

    private String predictedType;
    private double confidence;

    public IncomePredictionResponse() {
    }

    public IncomePredictionResponse(String predictedType, double confidence) {
        this.predictedType = predictedType;
        this.confidence = confidence;
    }

    public String getPredictedType() {
        return predictedType;
    }

    public void setPredictedType(String predictedType) {
        this.predictedType = predictedType;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "IncomePredictionResponse{" +
                "predictedType='" + predictedType + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}