package com.adem.attijari_compass.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "test_card_catalog",
        indexes = {
                @Index(name = "idx_test_card_catalog_number", columnList = "test_card_number", unique = true),
                @Index(name = "idx_test_card_catalog_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCardCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String holderName;

    @Column(nullable = false, length = 25)
    private String maskedCardNumber;

    @Column(name = "test_card_number", nullable = false, unique = true, length = 25)
    private String testCardNumber;

    @Column(nullable = false)
    private Integer expiryMonth;

    @Column(nullable = false)
    private Integer expiryYear;

    @Column(nullable = false, length = 4)
    private String cvv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CardType cardType;

    @Column(nullable = false, length = 100)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CardStatus status;

    @Column(nullable = false)
    private Double initialBalance;
}
