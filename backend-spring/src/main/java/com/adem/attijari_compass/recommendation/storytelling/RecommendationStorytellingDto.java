package com.adem.attijari_compass.recommendation.storytelling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationStorytellingDto {

    private String summary;
    private String mainConcern;
    private String opportunity;
    private String action;
}
