package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    long countByUserId(Long userId);

    long countByUserEmail(String email);

    long countBySourceIn(List<TransactionSource> sources);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user WHERE t.user.id = :userId ORDER BY t.date DESC")
    List<Transaction> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId ORDER BY t.date DESC")
    Page<Transaction> findAllByUserIdPaginated(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user WHERE t.id = :id AND t.user.id = :userId")
    Optional<Transaction> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user WHERE t.user.id = :userId AND t.type = :type")
    List<Transaction> findAllByUserIdAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user WHERE t.user.id = :userId AND t.category = :category")
    List<Transaction> findAllByUserIdAndCategory(@Param("userId") Long userId, @Param("category") TransactionCategory category);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user WHERE t.user.id = :userId AND t.date BETWEEN :start AND :end")
    List<Transaction> findAllByUserIdAndDateBetween(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT t
            FROM Transaction t
            LEFT JOIN FETCH t.user
            LEFT JOIN FETCH t.userCard
            WHERE t.user.id = :userId
              AND t.date BETWEEN :start AND :end
            ORDER BY t.date DESC, t.createdAt DESC
            """)
    List<Transaction> findAllByUserIdAndDateBetweenOrderByDateDescCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
            SELECT t
            FROM Transaction t
            LEFT JOIN FETCH t.userCard
            WHERE t.user.id = :userId
              AND t.date BETWEEN :start AND :end
            ORDER BY t.date DESC, t.createdAt DESC
            """)
    List<Transaction> findDashboardTransactionsByUserIdAndDateBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :start AND :end")
    Double sumAmountByUserIdAndTypeAndDateBetween(@Param("userId") Long userId, @Param("type") TransactionType type, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.category = :category AND t.date BETWEEN :start AND :end")
    Double sumAmountByUserIdAndCategoryAndDateBetween(@Param("userId") Long userId, @Param("category") TransactionCategory category, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT t.category AS category,
                   COALESCE(SUM(ABS(t.amount)), 0) AS totalAmount
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.type = :type
              AND t.date BETWEEN :start AND :end
              AND t.category IN :categories
            GROUP BY t.category
            """)
    List<CategoryExpenseTotalProjection> sumAbsoluteAmountByUserIdAndTypeAndDateBetweenAndCategoryIn(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("categories") List<TransactionCategory> categories
    );

    @Query("""
            SELECT t
            FROM Transaction t
            LEFT JOIN FETCH t.user
            LEFT JOIN FETCH t.userCard
            WHERE t.user.id = :userId AND t.userCard.id = :userCardId
            ORDER BY t.date DESC, t.createdAt DESC
            """)
    List<Transaction> findAllByUserIdAndUserCardId(@Param("userId") Long userId, @Param("userCardId") Long userCardId);

    @Query("""
            SELECT t.externalReference
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.userCard.id = :userCardId
              AND t.externalReference IS NOT NULL
            """)
    List<String> findExternalReferencesByUserIdAndUserCardId(@Param("userId") Long userId, @Param("userCardId") Long userCardId);

    Optional<Transaction> findByUserCardIdAndExternalReference(Long userCardId, String externalReference);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM Transaction t
            WHERE t.user.id = :userId
            """)
    int deleteAllByUserId(@Param("userId") Long userId);
}
