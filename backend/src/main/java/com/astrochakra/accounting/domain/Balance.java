package com.astrochakra.accounting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps the existing single-row {@code balance} table:
 * columns balance, liquid_reserve, updated_at.
 */
@Entity
@Table(name = "balance")
public class Balance {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "balance")
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "liquid_reserve")
    private BigDecimal liquidReserve = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getLiquidReserve() { return liquidReserve; }
    public void setLiquidReserve(BigDecimal liquidReserve) { this.liquidReserve = liquidReserve; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
