package com.adem.attijari_compass.entity;

import com.adem.attijari_compass.util.CardMaskingUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "card_pool",
        indexes = {
                @Index(name = "idx_card_pool_card_catalog_id", columnList = "card_catalog_id"),
                @Index(name = "idx_card_pool_assigned", columnList = "assigned"),
                @Index(name = "idx_card_pool_assigned_user_id", columnList = "assigned_user_id"),
                @Index(name = "idx_card_pool_last4", columnList = "last4")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_card_pool_card_code", columnNames = "card_code")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"cardNumber", "cvv"})
public class CardPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_catalog_id", nullable = false)
    private CardCatalog cardCatalog;

    @NotBlank
    @Size(max = 150)
    @Column(name = "card_holder_name", nullable = false, length = 150)
    private String cardHolderName;

    @NotBlank
    @Size(max = 25)
    @Pattern(regexp = "^\\d{13,19}$", message = "cardNumber must contain between 13 and 19 digits")
    @Column(name = "card_number", nullable = false, length = 25)
    private String cardNumber;

    @Min(1)
    @Max(12)
    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @Min(2000)
    @Max(9999)
    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @Size(max = 4)
    @Column(length = 4)
    private String cvv;

    @NotBlank
    @Size(max = 25)
    @Column(name = "masked_card_number", nullable = false, length = 25)
    private String maskedCardNumber;

    @NotBlank
    @Pattern(regexp = "^\\d{4}$", message = "last4 must contain exactly 4 digits")
    @Column(nullable = false, length = 4)
    private String last4;

    @NotBlank
    @Size(max = 50)
    @Column(name = "card_code", nullable = false, length = 50)
    private String cardCode;

    @Builder.Default
    @Column(nullable = false)
    private boolean assigned = false;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        normalizeSensitiveFields();
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeSensitiveFields();
        updatedAt = LocalDateTime.now();
    }

    private void normalizeSensitiveFields() {
        if (cardHolderName != null) {
            cardHolderName = cardHolderName.trim().replaceAll("\\s+", " ").toUpperCase();
        }

        if (cardNumber != null) {
            cardNumber = CardMaskingUtil.normalizeCardNumber(cardNumber);
        }

        if ((maskedCardNumber == null || maskedCardNumber.isBlank()) && cardNumber != null) {
            maskedCardNumber = CardMaskingUtil.maskCardNumber(cardNumber);
        }

        if ((last4 == null || last4.isBlank()) && cardNumber != null) {
            last4 = CardMaskingUtil.extractLast4(cardNumber);
        }

        if (!assigned) {
            assignedUserId = null;
        }
    }
}
