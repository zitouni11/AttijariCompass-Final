package com.adem.attijari_compass.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transaction",
        indexes = {
                @Index(name = "idx_transaction_user_date", columnList = "user_id, date"),
                @Index(name = "idx_transaction_user_card", columnList = "user_card_id"),
                @Index(name = "idx_transaction_external_reference", columnList = "external_reference")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transaction_user_card_external_reference",
                        columnNames = {"user_card_id", "external_reference"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // Nouveaux champs pour paiement carte
    @Column(length = 100)
    private String merchantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.CARD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    @Builder.Default
    private TransactionSource source = TransactionSource.MANUAL_ENTRY;

    @Column(length = 10)
    private String cardLast4;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "categorization_confidence")
    private Double categorizationConfidence;

    @Column(name = "categorization_source", length = 50)
    private String categorizationSource;

    @Column(name = "categorization_normalized_text", length = 1000)
    private String categorizationNormalizedText;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)

    private User user;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_card_id")
    private UserCard userCard;
}
