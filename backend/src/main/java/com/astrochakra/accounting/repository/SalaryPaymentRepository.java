package com.astrochakra.accounting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.SalaryPayment;

public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, UUID> {
    List<SalaryPayment> findAllByOrderByCreatedAtDesc();
}
