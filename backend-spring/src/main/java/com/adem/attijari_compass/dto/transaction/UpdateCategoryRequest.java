package com.adem.attijari_compass.dto.transaction;

import com.adem.attijari_compass.entity.TransactionCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour corriger manuellement la catégorie d'une transaction
 * L'utilisateur peut ajuster la catégorisation automatique s'il la juge incorrecte
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCategoryRequest {

    @NotNull(message = "Category is required")
    private TransactionCategory category;
}

