package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.BudgetTarget;
import com.adem.attijari_compass.entity.BudgetTargetStatus;
import com.adem.attijari_compass.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetTargetRepository extends JpaRepository<BudgetTarget, Long> {

    @Query("""
            SELECT bt
            FROM BudgetTarget bt
            WHERE bt.user.id = :userId
              AND bt.status = :status
            ORDER BY bt.createdAt DESC
            """)
    List<BudgetTarget> findAllByUserIdAndStatusOrderByCreatedAtDesc(@Param("userId") Long userId,
                                                                    @Param("status") BudgetTargetStatus status);

    @Query("""
            SELECT bt
            FROM BudgetTarget bt
            WHERE bt.user.id = :userId
              AND bt.category = :category
              AND bt.status = :status
            ORDER BY bt.createdAt DESC
            """)
    List<BudgetTarget> findAllByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(@Param("userId") Long userId,
                                                                                @Param("category") TransactionCategory category,
                                                                                @Param("status") BudgetTargetStatus status);

    @Query("""
            SELECT bt
            FROM BudgetTarget bt
            WHERE bt.user.id = :userId
              AND bt.category = :category
              AND bt.status = :status
              AND bt.id <> :excludedId
            ORDER BY bt.createdAt DESC
            """)
    List<BudgetTarget> findAllByUserIdAndCategoryAndStatusAndIdNotOrderByCreatedAtDesc(@Param("userId") Long userId,
                                                                                        @Param("category") TransactionCategory category,
                                                                                        @Param("status") BudgetTargetStatus status,
                                                                                        @Param("excludedId") Long excludedId);

    @Query("""
            SELECT bt
            FROM BudgetTarget bt
            WHERE bt.id = :id
              AND bt.user.id = :userId
            """)
    Optional<BudgetTarget> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
