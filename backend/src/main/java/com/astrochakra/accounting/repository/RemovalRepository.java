package com.astrochakra.accounting.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.Removal;

public interface RemovalRepository extends JpaRepository<Removal, Long> {
    List<Removal> findAllByOrderByCreatedAtDesc();
}
