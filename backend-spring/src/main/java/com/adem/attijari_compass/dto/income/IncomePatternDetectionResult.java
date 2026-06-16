package com.adem.attijari_compass.dto.income;

public class IncomePatternDetectionResult {

    private String detectedType;
    private String inferredType;
    private double confidence;
    private String reason;
    private String explanation;
    private int occurrenceCount;
    private boolean monthlyRecurring;
    private boolean recurring;
    private double amountStabilityScore;
    private double stabilityScore;
    private double sourceConsistencyScore;
    private String recurrenceType;

    public IncomePatternDetectionResult() {
    }

    public IncomePatternDetectionResult(String inferredType,
                                        double confidence,
                                        boolean recurring,
                                        double stabilityScore,
                                        String recurrenceType,
                                        String reason) {
        this(inferredType,
                confidence,
                reason,
                null,
                0,
                recurring,
                stabilityScore,
                0.0d,
                recurrenceType);
    }

    public IncomePatternDetectionResult(String detectedType,
                                        double confidence,
                                        String reason,
                                        String explanation,
                                        int occurrenceCount,
                                        boolean monthlyRecurring,
                                        double amountStabilityScore,
                                        double sourceConsistencyScore,
                                        String recurrenceType) {
        this.detectedType = detectedType;
        this.inferredType = detectedType;
        this.confidence = confidence;
        this.reason = reason;
        this.explanation = explanation;
        this.occurrenceCount = occurrenceCount;
        this.monthlyRecurring = monthlyRecurring;
        this.recurring = monthlyRecurring;
        this.amountStabilityScore = amountStabilityScore;
        this.stabilityScore = amountStabilityScore;
        this.sourceConsistencyScore = sourceConsistencyScore;
        this.recurrenceType = recurrenceType;
    }

    public String getDetectedType() {
        return detectedType;
    }

    public void setDetectedType(String detectedType) {
        this.detectedType = detectedType;
        this.inferredType = detectedType;
    }

    public String getInferredType() {
        return inferredType;
    }

    public void setInferredType(String inferredType) {
        this.inferredType = inferredType;
        this.detectedType = inferredType;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
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

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public boolean isMonthlyRecurring() {
        return monthlyRecurring;
    }

    public void setMonthlyRecurring(boolean monthlyRecurring) {
        this.monthlyRecurring = monthlyRecurring;
        this.recurring = monthlyRecurring;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
        this.monthlyRecurring = recurring;
    }

    public double getAmountStabilityScore() {
        return amountStabilityScore;
    }

    public void setAmountStabilityScore(double amountStabilityScore) {
        this.amountStabilityScore = amountStabilityScore;
        this.stabilityScore = amountStabilityScore;
    }

    public double getStabilityScore() {
        return stabilityScore;
    }

    public void setStabilityScore(double stabilityScore) {
        this.stabilityScore = stabilityScore;
        this.amountStabilityScore = stabilityScore;
    }

    public double getSourceConsistencyScore() {
        return sourceConsistencyScore;
    }

    public void setSourceConsistencyScore(double sourceConsistencyScore) {
        this.sourceConsistencyScore = sourceConsistencyScore;
    }

    public String getRecurrenceType() {
        return recurrenceType;
    }

    public void setRecurrenceType(String recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    @Override
    public String toString() {
        return "IncomePatternDetectionResult{" +
                "detectedType='" + detectedType + '\'' +
                ", inferredType='" + inferredType + '\'' +
                ", confidence=" + confidence +
                ", reason='" + reason + '\'' +
                ", explanation='" + explanation + '\'' +
                ", occurrenceCount=" + occurrenceCount +
                ", monthlyRecurring=" + monthlyRecurring +
                ", recurring=" + recurring +
                ", amountStabilityScore=" + amountStabilityScore +
                ", stabilityScore=" + stabilityScore +
                ", sourceConsistencyScore=" + sourceConsistencyScore +
                ", recurrenceType='" + recurrenceType + '\'' +
                '}';
    }
}
