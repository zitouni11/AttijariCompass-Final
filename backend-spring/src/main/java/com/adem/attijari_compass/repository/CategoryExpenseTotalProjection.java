package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.TransactionCategory;

public interface CategoryExpenseTotalProjection {

    TransactionCategory getCategory();

    Double getTotalAmount();
}
