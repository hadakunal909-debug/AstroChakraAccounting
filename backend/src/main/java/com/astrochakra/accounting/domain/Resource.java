package com.astrochakra.accounting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Team member / payroll resource — maps the {@code resources} table. */
@Entity
@Table(name = "resources")
@Getter
@Setter
public class Resource {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "role")
    private String role;

    @Column(name = "pay_type")
    private String payType;

    @Column(name = "pay_amount")
    private BigDecimal payAmount;

    @Column(name = "commission_percent")
    private BigDecimal commissionPercent;

    @Column(name = "job_label")
    private String jobLabel;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
