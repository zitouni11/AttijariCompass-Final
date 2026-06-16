package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.CardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {

    List<CardTransaction> findAllByUserCardIdOrderByTransactionDateDesc(Long userCardId);

    @Query("""
            SELECT ct.externalReference
            FROM CardTransaction ct
            WHERE ct.userCard.id = :userCardId
              AND ct.externalReference IS NOT NULL
            """)
    List<String> findExternalReferencesByUserCardId(@Param("userCardId") Long userCardId);

    @Query("""
            SELECT ct.externalReference
            FROM CardTransaction ct
            WHERE ct.userCard.id = :userCardId
              AND ct.externalReference IS NOT NULL
              AND ct.externalReference LIKE CONCAT(:prefix, '%')
            """)
    List<String> findExternalReferencesByUserCardIdAndPrefix(
            @Param("userCardId") Long userCardId,
            @Param("prefix") String prefix
    );

    @Query("""
            SELECT ct
            FROM CardTransaction ct
            WHERE ct.userCard.id = :cardId
              AND ct.userCard.user.id = :userId
            ORDER BY ct.transactionDate DESC, ct.createdAt DESC
            """)
    List<CardTransaction> findAllByUserIdAndCardIdOrderByTransactionDateDesc(
            @Param("userId") Long userId,
            @Param("cardId") Long cardId
    );

    List<CardTransaction> findAllByUserCardUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("""
            SELECT ct
            FROM CardTransaction ct
            JOIN FETCH ct.userCard uc
            WHERE uc.user.id = :userId
              AND uc.isActive = true
              AND ct.transactionDate BETWEEN :startDate AND :endDate
            ORDER BY ct.transactionDate DESC, ct.createdAt DESC
            """)
    List<CardTransaction> findDashboardTransactionsByUserIdAndTransactionDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    Optional<CardTransaction> findByUserCardIdAndExternalReference(Long userCardId, String externalReference);
}
