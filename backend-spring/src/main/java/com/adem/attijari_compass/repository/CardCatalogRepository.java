package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.CardCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardCatalogRepository extends JpaRepository<CardCatalog, Long> {

    Optional<CardCatalog> findByCode(String code);

    Optional<CardCatalog> findByIdAndActiveTrue(Long id);

    List<CardCatalog> findAllByActiveTrueOrderByNameAsc();
}
