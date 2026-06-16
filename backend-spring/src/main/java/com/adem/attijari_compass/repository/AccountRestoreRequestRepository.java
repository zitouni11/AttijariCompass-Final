package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.AccountRestoreRequest;
import com.adem.attijari_compass.entity.AccountRestoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccountRestoreRequestRepository extends JpaRepository<AccountRestoreRequest, Long> {
    Optional<AccountRestoreRequest> findTopByEmailOrderByRequestedAtDesc(String email);
    boolean existsByEmailAndStatusIn(String email, Collection<AccountRestoreStatus> statuses);
    List<AccountRestoreRequest> findAllByOrderByRequestedAtDesc();
}
