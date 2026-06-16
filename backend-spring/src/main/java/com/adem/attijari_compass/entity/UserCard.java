package com.adem.attijari_compass.entity;

import com.adem.attijari_compass.util.CardMaskingUtil;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
        name = "user_card",
        indexes = {
                @Index(name = "idx_user_card_user_id", columnList = "user_id"),
                @Index(name = "idx_user_card_user_connected_at", columnList = "user_id, connected_at"),
                @Index(name = "idx_user_card_linked_test_card", columnList = "linked_test_card_id"),
                @Index(name = "idx_user_card_card_catalog_id", columnList = "card_catalog_id"),
                @Index(name = "idx_user_card_linked_at", columnList = "linked_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_card_user_test_card",
                        columnNames = {"user_id", "linked_test_card_id"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_catalog_id")
    private CardCatalog cardCatalog;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_pool_id")
    private CardPool cardPool;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_test_card_id")
    private TestCardCatalog linkedTestCard;

    @Size(max = 150)
    @Column(name = "card_holder_name", length = 150)
    private String cardHolderName;

    @Column(nullable = false, length = 25)
    private String maskedCardNumber;

    @Column(name = "card_number", length = 25)
    private String cardNumber;

    @Pattern(regexp = "^\\d{4}$", message = "last4 must contain exactly 4 digits")
    @Column(length = 4)
    private String last4;

    @Min(1)
    @Max(12)
    @Column(name = "expiry_month")
    private Integer expiryMonth;

    @Min(2000)
    @Max(9999)
    @Column(name = "expiry_year")
    private Integer expiryYear;

    @Size(max = 100)
    @Column(length = 100)
    private String nickname;

    @Size(max = 50)
    @Column(name = "card_code", length = 50)
    private String cardCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_status", length = 30)
    private CardStatus cardStatus;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Builder.Default
    @Column(name = "primary_card", nullable = false, columnDefinition = "boolean default false")
    private boolean primaryCard = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private CardSourceType sourceType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false, length = 100)
    private String holderName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CardType cardType;

    @Column(length = 100)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CardStatus status;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        synchronizeDomainFields();
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        synchronizeDomainFields();
        updatedAt = LocalDateTime.now();
    }

    private void synchronizeDomainFields() {
        if (!hasText(cardHolderName) && hasText(holderName)) {
            cardHolderName = holderName;
        }
        if (!hasText(holderName) && hasText(cardHolderName)) {
            holderName = cardHolderName;
        }

        if (cardStatus == null && status != null) {
            cardStatus = status;
        }
        if (status == null && cardStatus != null) {
            status = cardStatus;
        }

        if (linkedAt == null && connectedAt != null) {
            linkedAt = connectedAt;
        }
        if (connectedAt == null && linkedAt != null) {
            connectedAt = linkedAt;
        }

        if (sourceType == null) {
            if (linkedTestCard != null) {
                sourceType = CardSourceType.SANDBOX;
            } else if (cardPool != null) {
                sourceType = CardSourceType.DEMO_POOL;
            } else {
                sourceType = CardSourceType.MANUAL;
            }
        }

        if (!hasText(last4) && hasText(maskedCardNumber)) {
            String extractedLast4 = CardMaskingUtil.extractLast4(maskedCardNumber);
            last4 = hasText(extractedLast4) ? extractedLast4 : null;
        }

        if (!hasText(maskedCardNumber) && hasText(cardNumber)) {
            maskedCardNumber = CardMaskingUtil.maskCardNumber(cardNumber);
        }

        if (!hasText(last4) && hasText(cardNumber)) {
            String extractedLast4 = CardMaskingUtil.extractLast4(cardNumber);
            last4 = hasText(extractedLast4) ? extractedLast4 : null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
