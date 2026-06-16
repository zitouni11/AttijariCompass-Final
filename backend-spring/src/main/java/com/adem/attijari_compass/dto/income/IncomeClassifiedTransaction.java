package com.adem.attijari_compass.dto.income;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class IncomeClassifiedTransaction {

    private String type;
    private double confidence;
    private BigDecimal amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String source;

    public IncomeClassifiedTransaction() {
    }

    public IncomeClassifiedTransaction(String type,
                                       double confidence,
                                       BigDecimal amount,
                                       LocalDate date,
                                       String source) {
        this.type = type;
        this.confidence = confidence;
        this.amount = amount;
        this.date = date;
        this.source = source;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "IncomeClassifiedTransaction{" +
                "type='" + type + '\'' +
                ", confidence=" + confidence +
                ", amount=" + amount +
                ", date=" + date +
                ", source='" + source + '\'' +
                '}';
    }
}
