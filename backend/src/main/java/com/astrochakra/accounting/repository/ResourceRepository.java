package com.astrochakra.accounting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.Resource;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    List<Resource> findAllByOrderByCreatedAtAsc();
}
