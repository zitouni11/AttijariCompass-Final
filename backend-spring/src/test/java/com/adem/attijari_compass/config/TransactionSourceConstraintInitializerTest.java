package com.adem.attijari_compass.config;

import com.adem.attijari_compass.entity.TransactionSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionSourceConstraintInitializerTest {

    @Test
    void shouldBuildConstraintClauseWithAllTransactionSourceValues() {
        String sql = TransactionSourceConstraintInitializer.buildAddConstraintSql();

        assertTrue(sql.contains("source IS NULL"));
        for (TransactionSource source : TransactionSource.values()) {
            assertTrue(sql.contains("'" + source.name() + "'"));
        }
    }
}
