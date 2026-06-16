package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.TestCardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCardTransactionRepository extends JpaRepository<TestCardTransaction, Long> {

    List<TestCardTransaction> findByTestCardIdOrderByTransactionDateDesc(Long testCardId);

    long countByTestCardId(Long testCardId);
}
