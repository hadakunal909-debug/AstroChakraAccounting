package com.astrochakra.accounting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.InventoryTransaction;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    List<InventoryTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(UUID productId);
}
