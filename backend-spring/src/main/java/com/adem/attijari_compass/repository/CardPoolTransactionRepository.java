package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.CardPoolTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardPoolTransactionRepository extends JpaRepository<CardPoolTransaction, Long> {

    List<CardPoolTransaction> findAllByCardPoolIdOrderByTransactionDateDesc(Long cardPoolId);

    Optional<CardPoolTransaction> findByCardPoolIdAndExternalReference(Long cardPoolId, String externalReference);

    long countByCardPoolId(Long cardPoolId);
}
