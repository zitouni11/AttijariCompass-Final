package com.adem.attijari_compass.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalStorytellingResponse {
    private Long goalId;
    private Boolean realistic;
    private String statusLabel;
    private String summary;
    private String assistantPerspective;
    private String priorityAction;
    private List<String> blockingCategories;
}
