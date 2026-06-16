package com.adem.attijari_compass.dto.budget;

import com.adem.attijari_compass.entity.BudgetTargetStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BudgetTargetStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private BudgetTargetStatus status;
}
