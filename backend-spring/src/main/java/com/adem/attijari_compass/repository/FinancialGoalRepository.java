package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.FinancialGoal;
import com.adem.attijari_compass.entity.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Long> {

    @Query("SELECT g FROM FinancialGoal g JOIN FETCH g.user WHERE g.user.id = :userId ORDER BY g.targetDate ASC, g.createdAt DESC")
    List<FinancialGoal> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT g FROM FinancialGoal g JOIN FETCH g.user WHERE g.id = :id AND g.user.id = :userId")
    Optional<FinancialGoal> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT COUNT(g) FROM FinancialGoal g WHERE g.user.id = :userId AND g.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") GoalStatus status);
}

