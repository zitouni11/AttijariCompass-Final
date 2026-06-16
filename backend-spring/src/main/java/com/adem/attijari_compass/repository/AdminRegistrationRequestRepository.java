package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.AdminRegistrationRequest;
import com.adem.attijari_compass.entity.AdminRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdminRegistrationRequestRepository extends JpaRepository<AdminRegistrationRequest, Long> {
    Optional<AdminRegistrationRequest> findTopByEmailOrderByCreatedAtDesc(String email);
    boolean existsByEmailAndStatusIn(String email, Collection<AdminRegistrationStatus> statuses);
    List<AdminRegistrationRequest> findAllByOrderByCreatedAtDesc();
}
