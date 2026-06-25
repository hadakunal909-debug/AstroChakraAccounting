package com.astrochakra.accounting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.ExpenseCategory;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {
    List<ExpenseCategory> findAllByOrderByNameAsc();
}
