package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.UserNotificationState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationStateRepository extends JpaRepository<UserNotificationState, Long> {

    List<UserNotificationState> findAllByUserId(Long userId);

    Optional<UserNotificationState> findByUserIdAndNotificationKey(Long userId, String notificationKey);
}
