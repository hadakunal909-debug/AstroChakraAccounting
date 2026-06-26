package com.astrochakra.accounting.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.astrochakra.accounting.domain.ChangeRequest;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {
    List<ChangeRequest> findAllByOrderByCreatedAtDesc();
    List<ChangeRequest> findByRequestedByOrderByCreatedAtDesc(String requestedBy);
}
