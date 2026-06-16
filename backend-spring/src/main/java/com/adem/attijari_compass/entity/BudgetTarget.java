package com.adem.attijari_compass.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "budget_target",
        indexes = {
                @Index(name = "idx_budget_target_user_status_created", columnList = "user_id, status, created_at"),
                @Index(name = "idx_budget_target_user_category_status", columnList = "user_id, category, status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionCategory category;

    @NotBlank
    @Size(max = 100)
    @Column(name = "category_label", nullable = false, length = 100)
    private String categoryLabel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BudgetTargetSource source;

    @Size(max = 150)
    @Column(name = "recommendation_id", length = 150)
    private String recommendationId;

    @Size(max = 255)
    @Column(name = "recommendation_title", length = 255)
    private String recommendationTitle;

    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 17, fraction = 2)
    @Column(name = "suggested_monthly_amount", precision = 19, scale = 2)
    private BigDecimal suggestedMonthlyAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "selected_level", nullable = false, length = 20)
    private BudgetTargetLevel selectedLevel;

    @Size(max = 1000)
    @Column(length = 1000)
    private String summary;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetTargetStatus status = BudgetTargetStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (categoryLabel == null || categoryLabel.trim().isEmpty()) {
            categoryLabel = category != null ? category.name() : null;
        }
        if (status == null) {
            status = BudgetTargetStatus.ACTIVE;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
