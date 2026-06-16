package com.adem.attijari_compass.dto.income;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Snapshot minimal d'une transaction de revenu utilisee pour la classification.")
public class IncomeTransactionSnapshot {

    @Schema(example = "virement recu")
    private String merchantName;

    @Schema(example = "fin de mois stable")
    private String description;

    @Schema(example = "2450")
    private BigDecimal amount;

    @JsonAlias("date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(
            description = "Date metier de la transaction. Utiliser preferentiellement 'transactionDate'. "
                    + "L'alias JSON 'date' est aussi accepte en entree pour compatibilite de test.",
            example = "2026-04-30",
            type = "string",
            format = "date"
    )
    private LocalDate transactionDate;

    public IncomeTransactionSnapshot() {
    }

    public IncomeTransactionSnapshot(String merchantName, String description, BigDecimal amount, LocalDate transactionDate) {
        this.merchantName = merchantName;
        this.description = description;
        this.amount = amount;
        this.transactionDate = transactionDate;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    @Override
    public String toString() {
        return "IncomeTransactionSnapshot{" +
                "merchantName='" + merchantName + '\'' +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", transactionDate=" + transactionDate +
                '}';
    }
}
