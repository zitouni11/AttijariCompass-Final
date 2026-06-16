package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.CardSourceType;
import com.adem.attijari_compass.entity.UserCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCardRepository extends JpaRepository<UserCard, Long> {

    List<UserCard> findAllByUserIdAndSourceTypeOrderByLinkedAtDesc(Long userId, CardSourceType sourceType);

    @Query("""
            SELECT uc
            FROM UserCard uc
            LEFT JOIN FETCH uc.cardPool
            WHERE uc.isActive = true
              AND uc.cardPool IS NOT NULL
              AND uc.sourceType = :sourceType
            ORDER BY uc.user.id ASC, uc.id ASC
            """)
    List<UserCard> findActiveManagedCardsWithPoolBySourceType(@Param("sourceType") CardSourceType sourceType);

    @Query("""
            SELECT uc
            FROM UserCard uc
            JOIN FETCH uc.cardCatalog cc
            LEFT JOIN FETCH uc.cardPool
            WHERE uc.cardCatalog IS NOT NULL
              AND uc.isActive = true
              AND uc.sourceType = :sourceType
              AND cc.code IN :catalogCodes
            ORDER BY uc.user.id ASC, uc.id ASC
            """)
    List<UserCard> findActiveManagedCardsBySourceTypeAndCatalogCodeIn(
            @Param("sourceType") CardSourceType sourceType,
            @Param("catalogCodes") List<String> catalogCodes
    );

    @Query("""
            SELECT uc
            FROM UserCard uc
            LEFT JOIN FETCH uc.cardCatalog
            LEFT JOIN FETCH uc.cardPool
            WHERE uc.user.id = :userId
              AND uc.cardCatalog IS NOT NULL
              AND uc.isActive = true
            ORDER BY uc.primaryCard DESC, uc.linkedAt DESC
            """)
    List<UserCard> findActiveManagedCardsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT uc
            FROM UserCard uc
            LEFT JOIN FETCH uc.cardCatalog
            LEFT JOIN FETCH uc.cardPool
            WHERE uc.id = :cardId
              AND uc.cardCatalog IS NOT NULL
              AND uc.isActive = true
            """)
    Optional<UserCard> findManagedCardById(@Param("cardId") Long cardId);

    @Query("""
            SELECT uc
            FROM UserCard uc
            LEFT JOIN FETCH uc.cardCatalog
            LEFT JOIN FETCH uc.cardPool
            WHERE uc.id = :cardId
              AND uc.user.id = :userId
              AND uc.cardCatalog IS NOT NULL
              AND uc.isActive = true
            """)
    Optional<UserCard> findManagedCardByIdAndUserId(@Param("cardId") Long cardId, @Param("userId") Long userId);

    @Query("""
            SELECT uc
            FROM UserCard uc
            LEFT JOIN FETCH uc.cardCatalog
            LEFT JOIN FETCH uc.cardPool
            WHERE uc.user.id = :userId
              AND uc.cardPool.id = :cardPoolId
            ORDER BY uc.id DESC
            """)
    List<UserCard> findAllByUserIdAndCardPoolIdOrderByIdDesc(@Param("userId") Long userId, @Param("cardPoolId") Long cardPoolId);

    @Query("""
            SELECT uc
            FROM UserCard uc
            LEFT JOIN FETCH uc.cardCatalog
            WHERE uc.user.id = :userId
              AND uc.cardCatalog.id = :catalogId
              AND uc.last4 = :last4
              AND uc.expiryMonth = :expiryMonth
              AND uc.expiryYear = :expiryYear
            """)
    Optional<UserCard> findManagedCardForLink(
            @Param("userId") Long userId,
            @Param("catalogId") Long catalogId,
            @Param("last4") String last4,
            @Param("expiryMonth") Integer expiryMonth,
            @Param("expiryYear") Integer expiryYear
    );

    @Query("""
            SELECT uc
            FROM UserCard uc
            LEFT JOIN FETCH uc.linkedTestCard
            WHERE uc.user.id = :userId
            ORDER BY uc.connectedAt DESC
            """)
    List<UserCard> findAllByUserIdOrderByConnectedAtDesc(@Param("userId") Long userId);

    @Query("""
            SELECT uc
            FROM UserCard uc
            LEFT JOIN FETCH uc.linkedTestCard
            WHERE uc.id = :id AND uc.user.id = :userId
            """)
    Optional<UserCard> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT uc
            FROM UserCard uc
            LEFT JOIN FETCH uc.linkedTestCard
            WHERE uc.user.id = :userId AND uc.linkedTestCard.id = :testCardId
            """)
    Optional<UserCard> findByUserIdAndLinkedTestCardId(@Param("userId") Long userId, @Param("testCardId") Long testCardId);
}
