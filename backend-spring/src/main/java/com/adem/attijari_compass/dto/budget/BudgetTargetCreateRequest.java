package com.adem.attijari_compass.dto.budget;

import com.adem.attijari_compass.entity.BudgetTargetLevel;
import com.adem.attijari_compass.entity.BudgetTargetSource;
import com.adem.attijari_compass.entity.TransactionCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetTargetCreateRequest {

    @NotNull(message = "Category is required")
    private TransactionCategory category;

    @Size(max = 100, message = "Category label must not exceed 100 characters")
    private String categoryLabel;

    @NotNull(message = "Selected level is required")
    private BudgetTargetLevel selectedLevel;

    @DecimalMin(value = "0.0", inclusive = true, message = "Suggested monthly amount must be greater than or equal to 0")
    @Digits(integer = 17, fraction = 2, message = "Suggested monthly amount must contain at most 2 decimal places")
    private BigDecimal suggestedMonthlyAmount;

    @NotNull(message = "Source is required")
    private BudgetTargetSource source;

    @Size(max = 150, message = "Recommendation id must not exceed 150 characters")
    private String recommendationId;

    @Size(max = 255, message = "Recommendation title must not exceed 255 characters")
    private String recommendationTitle;

    @Size(max = 1000, message = "Summary must not exceed 1000 characters")
    private String summary;
}
