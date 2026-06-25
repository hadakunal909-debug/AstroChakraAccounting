package com.astrochakra.accounting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.IncomeSource;

public interface IncomeSourceRepository extends JpaRepository<IncomeSource, UUID> {
    List<IncomeSource> findAllByOrderByCreatedAtAsc();
}
