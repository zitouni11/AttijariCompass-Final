package com.adem.attijari_compass.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "card_transaction",
        indexes = {
                @Index(name = "idx_card_transaction_user_card_id", columnList = "user_card_id"),
                @Index(name = "idx_card_transaction_transaction_date", columnList = "transaction_date"),
                @Index(name = "idx_card_transaction_user_card_date", columnList = "user_card_id, transaction_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_card_transaction_user_card_external_reference",
                        columnNames = {"user_card_id", "external_reference"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_card_id", nullable = false)
    private UserCard userCard;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "value_date")
    private LocalDateTime valueDate;

    @NotNull
    @Digits(integer = 17, fraction = 2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Size(max = 150)
    @Column(name = "merchant_name", length = 150)
    private String merchantName;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Size(max = 100)
    @Column(length = 100)
    private String reference;

    @Size(max = 100)
    @Column(length = 100)
    private String category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status;

    @Size(max = 120)
    @Column(length = 120)
    private String city;

    @Size(max = 120)
    @Column(length = 120)
    private String country;

    @NotBlank
    @Pattern(regexp = "^TND$", message = "currency must always be TND")
    @Builder.Default
    @Column(nullable = false, length = 3, columnDefinition = "varchar(3) default 'TND'")
    private String currency = "TND";

    @Builder.Default
    @Column(nullable = false)
    private boolean installment = false;

    @PositiveOrZero
    @Column(name = "installment_index")
    private Integer installmentIndex;

    @PositiveOrZero
    @Column(name = "installment_total")
    private Integer installmentTotal;

    @Size(max = 120)
    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        currency = "TND";
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        currency = "TND";
        updatedAt = LocalDateTime.now();
    }

    @AssertTrue(message = "installment fields must be coherent with the installment flag")
    public boolean isInstallmentMetadataValid() {
        if (!installment) {
            return installmentIndex == null && installmentTotal == null;
        }
        if (installmentIndex == null || installmentTotal == null) {
            return false;
        }
        return installmentIndex > 0 && installmentTotal > 0 && installmentIndex <= installmentTotal;
    }

    @AssertTrue(message = "amount must be different from zero")
    public boolean isAmountNonZero() {
        return amount != null && amount.signum() != 0;
    }

    public boolean isCredit() {
        return amount != null && amount.signum() > 0;
    }

    public boolean isDebit() {
        return amount != null && amount.signum() < 0;
    }
}
