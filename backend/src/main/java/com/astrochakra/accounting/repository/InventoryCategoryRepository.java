package com.astrochakra.accounting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.InventoryCategory;

public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, UUID> {
    List<InventoryCategory> findAllByOrderByNameAsc();
}
