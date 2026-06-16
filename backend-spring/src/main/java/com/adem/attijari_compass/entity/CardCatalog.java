package com.adem.attijari_compass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "card_catalog",
        indexes = {
                @Index(name = "idx_card_catalog_code", columnList = "code", unique = true),
                @Index(name = "idx_card_catalog_active", columnList = "active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_card_catalog_code", columnNames = "code")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String brand;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CardScope scope;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 17, fraction = 2)
    @Column(name = "max_payment_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxPaymentLimit;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 17, fraction = 2)
    @Column(name = "max_withdrawal_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxWithdrawalLimit;

    @Builder.Default
    @Column(name = "allows_online_payment", nullable = false)
    private boolean allowsOnlinePayment = false;

    @Builder.Default
    @Column(name = "allows_international_payment", nullable = false)
    private boolean allowsInternationalPayment = false;

    @Builder.Default
    @Column(name = "allows_installments", nullable = false)
    private boolean allowsInstallments = false;

    @Min(0)
    @Max(60)
    @Column(name = "installment_months_max")
    private Integer installmentMonthsMax;

    @Size(max = 500)
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @AssertTrue(message = "installmentMonthsMax must be provided only when installments are allowed")
    public boolean isInstallmentConfigurationValid() {
        if (!allowsInstallments) {
            return installmentMonthsMax == null || installmentMonthsMax == 0;
        }
        return installmentMonthsMax != null && installmentMonthsMax > 0;
    }
}
