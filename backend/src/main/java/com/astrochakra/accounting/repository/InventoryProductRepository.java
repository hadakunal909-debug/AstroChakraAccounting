package com.astrochakra.accounting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.InventoryProduct;

public interface InventoryProductRepository extends JpaRepository<InventoryProduct, UUID> {
    List<InventoryProduct> findAllByOrderByCreatedAtDesc();
}
