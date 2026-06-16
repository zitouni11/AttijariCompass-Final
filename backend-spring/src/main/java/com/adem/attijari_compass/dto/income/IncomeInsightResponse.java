package com.adem.attijari_compass.dto.income;

public class IncomeInsightResponse {

    private String primaryIncomeType;
    private String incomeStability;
    private String incomeRegularity;
    private Integer incomeConfidenceScore;
    private Integer salaryLikeCount;
    private Integer freelanceLikeCount;
    private Integer transferLikeCount;
    private Integer cashDepositLikeCount;
    private Integer otherIncomeCount;
    private Double dominantIncomeShare;
    private Boolean hasSecondaryIncome;
    private String insightSummary;

    public IncomeInsightResponse() {
    }

    public IncomeInsightResponse(String primaryIncomeType,
                                 String incomeStability,
                                 String incomeRegularity,
                                 Integer incomeConfidenceScore,
                                 Integer salaryLikeCount,
                                 Integer freelanceLikeCount,
                                 Integer transferLikeCount,
                                 Integer cashDepositLikeCount,
                                 Integer otherIncomeCount,
                                 Double dominantIncomeShare,
                                 Boolean hasSecondaryIncome,
                                 String insightSummary) {
        this.primaryIncomeType = primaryIncomeType;
        this.incomeStability = incomeStability;
        this.incomeRegularity = incomeRegularity;
        this.incomeConfidenceScore = incomeConfidenceScore;
        this.salaryLikeCount = salaryLikeCount;
        this.freelanceLikeCount = freelanceLikeCount;
        this.transferLikeCount = transferLikeCount;
        this.cashDepositLikeCount = cashDepositLikeCount;
        this.otherIncomeCount = otherIncomeCount;
        this.dominantIncomeShare = dominantIncomeShare;
        this.hasSecondaryIncome = hasSecondaryIncome;
        this.insightSummary = insightSummary;
    }

    public String getPrimaryIncomeType() {
        return primaryIncomeType;
    }

    public void setPrimaryIncomeType(String primaryIncomeType) {
        this.primaryIncomeType = primaryIncomeType;
    }

    public String getIncomeStability() {
        return incomeStability;
    }

    public void setIncomeStability(String incomeStability) {
        this.incomeStability = incomeStability;
    }

    public String getIncomeRegularity() {
        return incomeRegularity;
    }

    public void setIncomeRegularity(String incomeRegularity) {
        this.incomeRegularity = incomeRegularity;
    }

    public Integer getIncomeConfidenceScore() {
        return incomeConfidenceScore;
    }

    public void setIncomeConfidenceScore(Integer incomeConfidenceScore) {
        this.incomeConfidenceScore = incomeConfidenceScore;
    }

    public Integer getSalaryLikeCount() {
        return salaryLikeCount;
    }

    public void setSalaryLikeCount(Integer salaryLikeCount) {
        this.salaryLikeCount = salaryLikeCount;
    }

    public Integer getFreelanceLikeCount() {
        return freelanceLikeCount;
    }

    public void setFreelanceLikeCount(Integer freelanceLikeCount) {
        this.freelanceLikeCount = freelanceLikeCount;
    }

    public Integer getTransferLikeCount() {
        return transferLikeCount;
    }

    public void setTransferLikeCount(Integer transferLikeCount) {
        this.transferLikeCount = transferLikeCount;
    }

    public Integer getCashDepositLikeCount() {
        return cashDepositLikeCount;
    }

    public void setCashDepositLikeCount(Integer cashDepositLikeCount) {
        this.cashDepositLikeCount = cashDepositLikeCount;
    }

    public Integer getOtherIncomeCount() {
        return otherIncomeCount;
    }

    public void setOtherIncomeCount(Integer otherIncomeCount) {
        this.otherIncomeCount = otherIncomeCount;
    }

    public Double getDominantIncomeShare() {
        return dominantIncomeShare;
    }

    public void setDominantIncomeShare(Double dominantIncomeShare) {
        this.dominantIncomeShare = dominantIncomeShare;
    }

    public Boolean getHasSecondaryIncome() {
        return hasSecondaryIncome;
    }

    public void setHasSecondaryIncome(Boolean hasSecondaryIncome) {
        this.hasSecondaryIncome = hasSecondaryIncome;
    }

    public String getInsightSummary() {
        return insightSummary;
    }

    public void setInsightSummary(String insightSummary) {
        this.insightSummary = insightSummary;
    }

    @Override
    public String toString() {
        return "IncomeInsightResponse{" +
                "primaryIncomeType='" + primaryIncomeType + '\'' +
                ", incomeStability='" + incomeStability + '\'' +
                ", incomeRegularity='" + incomeRegularity + '\'' +
                ", incomeConfidenceScore=" + incomeConfidenceScore +
                ", salaryLikeCount=" + salaryLikeCount +
                ", freelanceLikeCount=" + freelanceLikeCount +
                ", transferLikeCount=" + transferLikeCount +
                ", cashDepositLikeCount=" + cashDepositLikeCount +
                ", otherIncomeCount=" + otherIncomeCount +
                ", dominantIncomeShare=" + dominantIncomeShare +
                ", hasSecondaryIncome=" + hasSecondaryIncome +
                ", insightSummary='" + insightSummary + '\'' +
                '}';
    }
}
