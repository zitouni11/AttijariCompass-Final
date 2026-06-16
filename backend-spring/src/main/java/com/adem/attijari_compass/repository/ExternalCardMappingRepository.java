package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.ExternalCardMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalCardMappingRepository extends JpaRepository<ExternalCardMapping, Long> {

    List<ExternalCardMapping> findAllByUserCardId(Long userCardId);

    Optional<ExternalCardMapping> findBySourceSystemAndExternalCardId(String sourceSystem, String externalCardId);

    boolean existsBySourceSystemAndExternalCardId(String sourceSystem, String externalCardId);
}
