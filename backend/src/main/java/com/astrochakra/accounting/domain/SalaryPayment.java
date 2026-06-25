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

/** A logged salary payment — maps the {@code salary_payments} table. */
@Entity
@Table(name = "salary_payments")
@Getter
@Setter
public class SalaryPayment {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "resource_name")
    private String resourceName;

    @Column(name = "pay_type")
    private String payType;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "units")
    private BigDecimal units;

    @Column(name = "fund_source")
    private String fundSource;

    @Column(name = "fund_label")
    private String fundLabel;

    @Column(name = "period")
    private String period;

    @Column(name = "note")
    private String note;

    @Column(name = "paid_by")
    private String paidBy;

    @Column(name = "created_at")
    private Instant createdAt;
}
