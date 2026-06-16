package com.adem.attijari_compass.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "test_card_transaction",
        indexes = {
                @Index(name = "idx_test_card_transaction_card_date", columnList = "test_card_id, transaction_date"),
                @Index(name = "idx_test_card_transaction_external_reference", columnList = "external_reference", unique = true)
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_card_id", nullable = false)
    private TestCardCatalog testCard;

    @Column(nullable = false, length = 120)
    private String merchantName;

    @Column(name = "raw_label", nullable = false, length = 255)
    private String rawLabel;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private SandboxTransactionType transactionType;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_suggestion", nullable = false, length = 40)
    private TransactionCategory categorySuggestion;

    @Column(length = 255)
    private String description;

    @Column(name = "external_reference", nullable = false, unique = true, length = 100)
    private String externalReference;
}
