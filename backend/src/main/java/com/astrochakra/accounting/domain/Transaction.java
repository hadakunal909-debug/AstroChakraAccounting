package com.astrochakra.accounting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transactions")
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "project_code")
    private String projectCode;

    @Column(name = "person")
    private String person;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "kind")
    private String kind;

    @Column(name = "category")
    private String category;

    @Column(name = "note")
    private String note;

    @Column(name = "bill_url")
    private String billUrl;

    @Column(name = "bill_name")
    private String billName;

    @Column(name = "settled")
    private Boolean settled = Boolean.FALSE;

    @Column(name = "is_reversal")
    private Boolean isReversal = Boolean.FALSE;

    @Column(name = "original_id")
    private Long originalId;

    @Column(name = "reverses_kind")
    private String reversesKind;

    @Column(name = "fund_source")
    private String fundSource;

    @Column(name = "created_at")
    private Instant createdAt;
}
