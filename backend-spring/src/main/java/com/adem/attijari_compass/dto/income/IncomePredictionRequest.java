package com.adem.attijari_compass.dto.income;

public class IncomePredictionRequest {

    private String merchantName;
    private String description;
    private double threshold;

    public IncomePredictionRequest() {
    }

    public IncomePredictionRequest(String merchantName, String description, double threshold) {
        this.merchantName = merchantName;
        this.description = description;
        this.threshold = threshold;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
}