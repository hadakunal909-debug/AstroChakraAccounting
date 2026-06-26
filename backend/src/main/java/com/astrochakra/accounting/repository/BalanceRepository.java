package com.astrochakra.accounting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.Balance;

public interface BalanceRepository extends JpaRepository<Balance, Long> {
}
