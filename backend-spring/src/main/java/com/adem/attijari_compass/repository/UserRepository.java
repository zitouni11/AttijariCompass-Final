package com.adem.attijari_compass.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.entity.Role;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    long countByActiveTrue();
    long countByActiveFalse();
    long countByDeletedFalse();
    long countByActiveTrueAndDeletedFalse();
    long countByActiveFalseAndDeletedFalse();
    List<User> findAllByDeletedFalse();
    List<User> findAllByDeletedTrue();
    long countByRoleAndActiveTrueAndDeletedFalse(Role role);
}
