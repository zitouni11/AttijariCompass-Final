package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.CardPool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardPoolRepository extends JpaRepository<CardPool, Long> {

    Optional<CardPool> findByCardCode(String cardCode);

    Optional<CardPool> findFirstByCardCatalog_IdAndCardNumberAndExpiryMonthAndExpiryYearAndCardHolderNameIgnoreCase(
            Long cardCatalogId,
            String cardNumber,
            Integer expiryMonth,
            Integer expiryYear,
            String cardHolderName
    );

    List<CardPool> findAllByAssignedFalseOrderByCardCodeAsc();
}
