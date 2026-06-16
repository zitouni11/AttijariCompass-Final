package com.adem.attijari_compass.dto.goal;

import com.adem.attijari_compass.entity.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoalResponse {
    private Long id;
    private String name;
    private String description;
    private Double targetAmount;
    private Double currentAmount;
    private LocalDate targetDate;
    private GoalStatus status;
    private LocalDateTime createdAt;
    private Double progressPercentage;
    private Double remainingAmount;
    private Double monthlySavingsRequired;
}

