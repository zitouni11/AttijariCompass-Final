package com.adem.attijari_compass.config;

import com.adem.attijari_compass.entity.TransactionSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TransactionSourceConstraintInitializer implements CommandLineRunner {

    static final String TABLE_NAME = "transaction";
    static final String COLUMN_NAME = "source";
    static final String CONSTRAINT_NAME = "transaction_source_check";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " DROP CONSTRAINT IF EXISTS " + CONSTRAINT_NAME);
        jdbcTemplate.execute(buildAddConstraintSql());
        log.info("Synchronized {} constraint with enum values {}", CONSTRAINT_NAME, Arrays.toString(TransactionSource.values()));
    }

    static String buildAddConstraintSql() {
        return "ALTER TABLE " + TABLE_NAME + " ADD CONSTRAINT " + CONSTRAINT_NAME
                + " CHECK (" + COLUMN_NAME + " IS NULL OR " + COLUMN_NAME + " IN (" + buildAllowedValuesClause() + "))";
    }

    static String buildAllowedValuesClause() {
        return Arrays.stream(TransactionSource.values())
                .map(Enum::name)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(", "));
    }
}
