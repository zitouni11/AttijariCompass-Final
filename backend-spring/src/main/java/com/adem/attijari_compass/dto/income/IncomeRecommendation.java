package com.adem.attijari_compass.dto.income;

public class IncomeRecommendation {

    private String title;
    private String description;
    private String priority;
    private String type;

    public IncomeRecommendation() {
    }

    public IncomeRecommendation(String title, String description, String priority, String type) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "IncomeRecommendation{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", priority='" + priority + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
