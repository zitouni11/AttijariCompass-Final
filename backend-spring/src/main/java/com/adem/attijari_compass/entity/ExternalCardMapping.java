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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "external_card_mapping",
        indexes = {
                @Index(name = "idx_external_card_mapping_user_card_id", columnList = "user_card_id"),
                @Index(name = "idx_external_card_mapping_source_system", columnList = "source_system")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_external_card_mapping_source_card",
                        columnNames = {"source_system", "external_card_id"}
                ),
                @UniqueConstraint(
                        name = "uk_external_card_mapping_user_card_source",
                        columnNames = {"user_card_id", "source_system"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalCardMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_card_id", nullable = false)
    private UserCard userCard;

    @NotBlank
    @Size(max = 120)
    @Column(name = "external_card_id", nullable = false, length = 120)
    private String externalCardId;

    @Size(max = 120)
    @Column(name = "external_customer_id", length = 120)
    private String externalCustomerId;

    @NotBlank
    @Size(max = 80)
    @Column(name = "source_system", nullable = false, length = 80)
    private String sourceSystem;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 30)
    private ExternalSyncStatus syncStatus;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
