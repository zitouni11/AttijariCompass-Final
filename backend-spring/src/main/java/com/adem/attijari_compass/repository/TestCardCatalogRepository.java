package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.TestCardCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TestCardCatalogRepository extends JpaRepository<TestCardCatalog, Long> {

    Optional<TestCardCatalog> findByTestCardNumber(String testCardNumber);

    boolean existsByTestCardNumber(String testCardNumber);
}
