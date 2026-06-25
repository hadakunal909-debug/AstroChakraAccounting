package com.astrochakra.accounting.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "change_requests")
@Getter
@Setter
public class ChangeRequest {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "change_type")
    private String changeType;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at")
    private Instant createdAt;
}
