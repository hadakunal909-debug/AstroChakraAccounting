package com.astrochakra.accounting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.ChangeRequest;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, UUID> {
    List<ChangeRequest> findAllByOrderByCreatedAtDesc();
    List<ChangeRequest> findByRequestedByOrderByCreatedAtDesc(String requestedBy);
}
