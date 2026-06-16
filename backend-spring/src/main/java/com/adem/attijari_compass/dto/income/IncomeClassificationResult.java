package com.adem.attijari_compass.dto.income;

public class IncomeClassificationResult {

    private String finalType;
    private double finalConfidence;
    private String source;
    private double confidence;
    private String reason;
    private String explanation;
    private String mlPredictedType;
    private double mlConfidence;
    private boolean patternDetected;
    private String patternType;
    private double patternConfidence;

    public IncomeClassificationResult() {
    }

    public IncomeClassificationResult(String finalType,
                                      String source,
                                      double confidence,
                                      String mlPredictedType,
                                      double mlConfidence,
                                      boolean patternDetected,
                                      String patternType,
                                      double patternConfidence,
                                      String reason) {
        this(finalType,
                confidence,
                source,
                reason,
                null,
                mlPredictedType,
                mlConfidence,
                patternDetected,
                patternType,
                patternConfidence);
    }

    public IncomeClassificationResult(String finalType,
                                      double finalConfidence,
                                      String source,
                                      String reason,
                                      String explanation,
                                      String mlPredictedType,
                                      double mlConfidence,
                                      boolean patternDetected,
                                      String patternType,
                                      double patternConfidence) {
        this.finalType = finalType;
        this.finalConfidence = finalConfidence;
        this.confidence = finalConfidence;
        this.source = source;
        this.reason = reason;
        this.explanation = explanation;
        this.mlPredictedType = mlPredictedType;
        this.mlConfidence = mlConfidence;
        this.patternDetected = patternDetected;
        this.patternType = patternType;
        this.patternConfidence = patternConfidence;
    }

    public String getFinalType() {
        return finalType;
    }

    public void setFinalType(String finalType) {
        this.finalType = finalType;
    }

    public double getFinalConfidence() {
        return finalConfidence;
    }

    public void setFinalConfidence(double finalConfidence) {
        this.finalConfidence = finalConfidence;
        this.confidence = finalConfidence;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
        this.finalConfidence = confidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getMlPredictedType() {
        return mlPredictedType;
    }

    public void setMlPredictedType(String mlPredictedType) {
        this.mlPredictedType = mlPredictedType;
    }

    public double getMlConfidence() {
        return mlConfidence;
    }

    public void setMlConfidence(double mlConfidence) {
        this.mlConfidence = mlConfidence;
    }

    public boolean isPatternDetected() {
        return patternDetected;
    }

    public void setPatternDetected(boolean patternDetected) {
        this.patternDetected = patternDetected;
    }

    public String getPatternType() {
        return patternType;
    }

    public void setPatternType(String patternType) {
        this.patternType = patternType;
    }

    public double getPatternConfidence() {
        return patternConfidence;
    }

    public void setPatternConfidence(double patternConfidence) {
        this.patternConfidence = patternConfidence;
    }

    @Override
    public String toString() {
        return "IncomeClassificationResult{" +
                "finalType='" + finalType + '\'' +
                ", finalConfidence=" + finalConfidence +
                ", source='" + source + '\'' +
                ", confidence=" + confidence +
                ", reason='" + reason + '\'' +
                ", explanation='" + explanation + '\'' +
                ", mlPredictedType='" + mlPredictedType + '\'' +
                ", mlConfidence=" + mlConfidence +
                ", patternDetected=" + patternDetected +
                ", patternType='" + patternType + '\'' +
                ", patternConfidence=" + patternConfidence +
                '}';
    }
}
