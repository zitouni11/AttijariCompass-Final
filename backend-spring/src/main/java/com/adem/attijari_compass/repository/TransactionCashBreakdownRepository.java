package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.TransactionCashBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TransactionCashBreakdownRepository extends JpaRepository<TransactionCashBreakdown, Long> {

    List<TransactionCashBreakdown> findAllByTransaction_IdOrderByIdAsc(Long transactionId);

    List<TransactionCashBreakdown> findAllByTransaction_IdIn(Collection<Long> transactionIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM TransactionCashBreakdown item
            WHERE item.transaction.id = :transactionId
            """)
    void deleteAllByTransactionId(@Param("transactionId") Long transactionId);
}
